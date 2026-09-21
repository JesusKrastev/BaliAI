package com.jesuskrastev.bali.ui.screens.mistakes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.GenerativeModel
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.di.QuestionsModel
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.GeminiQuestionParser
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import com.jesuskrastev.bali.ui.screens.test.TestSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import javax.inject.Inject

data class MistakesUiState(
    val questions: List<QuestionUiState> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isAnswerChecked: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionStreak: Int = 0
)

@HiltViewModel
class MistakesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val answerRepository: AnswerRepository,
    private val testResultRepository: TestResultRepository,
    @QuestionsModel private val gemini: GenerativeModel,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val analytics: AnalyticsTracker,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MistakesUiState())
    val uiState: StateFlow<MistakesUiState> = _uiState.asStateFlow()
    private var startTime: Long = 0
    private var sessionStreak: Int = 0
    private var originalQuestions: List<String> = emptyList()

    private val jsonContent = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    init {
        if (!restoreSession()) {
            generateMistakesTest()
        }
    }

    /**
     * Restores a previously generated review session from [savedStateHandle], if one is
     * there. This is what lets a process restart mid-review (Android killing the app in
     * the background, then Navigation replaying the back stack) put the same questions
     * back on screen instead of paying for a brand new Gemini generation.
     *
     * @return true when a session was restored and no generation is needed.
     */
    private fun restoreSession(): Boolean {
        val json = savedStateHandle.get<String>(KEY_SESSION) ?: return false
        val session = runCatching { jsonContent.decodeFromString<SavedMistakesSession>(json) }
            .getOrNull() ?: return false
        if (session.questions.isEmpty()) return false

        sessionStreak = session.sessionStreak
        startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                questions = session.questions,
                currentQuestionIndex = session.currentQuestionIndex,
                selectedAnswers = session.selectedAnswers,
                isAnswerChecked = session.isAnswerChecked,
                sessionStreak = session.sessionStreak,
                isLoading = false,
                error = null
            )
        }
        return true
    }

    /** Snapshots the restorable parts of [MistakesUiState] into [savedStateHandle]. */
    private fun persistSession() {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        val session = SavedMistakesSession(
            questions = state.questions,
            currentQuestionIndex = state.currentQuestionIndex,
            selectedAnswers = state.selectedAnswers,
            isAnswerChecked = state.isAnswerChecked,
            sessionStreak = state.sessionStreak
        )
        savedStateHandle[KEY_SESSION] = jsonContent.encodeToString(session)
    }

    /**
     * Tells apart a screen's first-ever generation from one triggered by a ViewModel
     * recreated mid-session with nothing left to restore — i.e. Android killed the
     * process while a review was in flight or on screen.
     */
    private fun determineReason(): String =
        if (savedStateHandle.get<Boolean>(KEY_GENERATION_STARTED) == true) "process_restart" else "initial"

    fun onEvent(event: MistakesEvent) {
        when (event) {
            MistakesEvent.Retry -> retry()
            is MistakesEvent.SelectOption -> selectOption(event.optionIndex)
            MistakesEvent.CheckAnswer -> checkAnswer()
            MistakesEvent.NextQuestion -> nextQuestion()
            is MistakesEvent.FinishReview -> {
                viewModelScope.launch {
                    val result = calculateResult()
                    event.onResult(result)
                }
            }
        }
    }

    private fun retry() {
        sessionStreak = 0
        _uiState.update { it.copy(isLoading = true, error = null, questions = emptyList(), currentQuestionIndex = 0, selectedAnswers = emptyMap(), isAnswerChecked = false, sessionStreak = 0) }
        generateMistakesTest(reason = "retry")
    }

    /**
     * Builds the review session: takes the student's most recent unresolved mistakes and
     * asks Gemini for one reformulated question per mistake.
     *
     * The list is capped at [MAX_MISTAKES_PER_REVIEW] before it reaches the prompt. The
     * remote source returns every uncorrected answer ever recorded, with no limit, and
     * the prompt asks for exactly one question per item — so without this cap a student
     * with a long history triggers a generation of a hundred-plus questions that costs a
     * fortune and, past the output limit, comes back truncated and unparseable anyway.
     *
     * @param reason why this call is happening — `"initial"`/`"process_restart"` (from
     *   [determineReason]) or `"retry"` — logged alongside the real token cost once the
     *   response comes back, so cost spikes can be traced to the reason that caused them.
     */
    private fun generateMistakesTest(reason: String = determineReason()) {
        viewModelScope.launch {
            try {
                val recentMistakes = answerRepository.getRecentMistakes().first()
                    .sortedByDescending { it.date }
                    .take(MAX_MISTAKES_PER_REVIEW)
                if (recentMistakes.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "¡Felicidades! No tienes errores pendientes por repasar.") }
                    return@launch
                }

                // Only the question text: interpolating the whole Answer would ship
                // ids, option indexes and dates the model has no use for.
                val mistakesContext = recentMistakes.joinToString("\n") { "- ${it.questionText}" }
                val user = userRepository.get().first()
                val license = user?.licenseType?.takeIf { it.isNotBlank() } ?: "B (Coche)"
                val studentLevel = user?.level ?: 1
                val totalTests = testResultRepository.count()

                val prompt = """
                    Eres un Profesor Experto y Tutor Personal de la DGT (Dirección General de Tráfico) en España. 
                    Tu alumno ha fallado recientemente estas preguntas en sus tests:
                    $mistakesContext
    
                    Tu misión es generar una SESIÓN DE REPASO de EXACTAMENTE ${recentMistakes.size} preguntas, enfocada EXCLUSIVAMENTE en corregir estos conceptos.
                    
                    CONTEXTO DEL ALUMNO (PERSONALIZACIÓN):
                    - Permiso al que aspira: Permiso $license.
                    - Nivel actual en la app: $studentLevel (A mayor nivel, usa distractores más complejos).
                    - Total de tests realizados: $totalTests.
                    
                    REGLAS DE LA PREGUNTA (REPASO DE ERRORES):
                    - Genera EXACTAMENTE UNA pregunta por cada concepto fallado en la lista proporcionada.
                    - REFORMULA la pregunta y las opciones para que NO sean idénticas a las originales, pero evalúen la misma norma o situación. Obliga al alumno a pensar, no a memorizar la respuesta correcta anterior.
                    - Estilo DGT oficial: Lenguaje técnico, preciso.
                    - 3 opciones por pregunta con el TEXTO REAL de la respuesta (nada de "A", "B", "C"). Solo una es correcta.
                    - EXPLICACIÓN: Máximo 25 palabras. Al ser un test de repaso, la explicación debe ser muy didáctica, aclarando la "trampa" o el concepto que el alumno suele confundir.
                    
                    REGLAS DE CALIDAD Y ACTUALIZACIÓN (¡ESTRICTAMENTE OBLIGATORIO!):
                    - NORMATIVA VIGENTE: Usa SIEMPRE la ley de tráfico española más reciente (ej. baliza V-16, límites de 30 km/h en vías urbanas de un carril, nueva normativa de patinetes VMP).
                    - TRAMPAS TÍPICAS DGT: Haz que las respuestas incorrectas atraigan el error típico que el alumno cometió antes.
                    
                    REGLAS DE IMÁGENES (SISTEMA FILEPATH):
                    - Usa imágenes SOLO si la pregunta reformulada describe una situación visual o una señal física.
                    - Formato obligatorio: https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal[codigo].svg
                    - Códigos válidos de ejemplo: r1 (ceda), r2 (stop), p1 (peligro), r301 (velocidad 40), s1 (autopista).
                    - Si la pregunta es puramente teórica (ej: tasa de alcohol, documentación), usa null.
    
                """.trimIndent()

                savedStateHandle[KEY_GENERATION_STARTED] = true
                val response = gemini.generateContent(prompt)

                // Logged as soon as the response is back — tokens are billed the moment
                // Gemini answers, whether or not the JSON below turns out parseable.
                val usage = response.usageMetadata
                analytics.mistakesGenerated(
                    reason = reason,
                    inputTokens = usage?.promptTokenCount ?: 0,
                    outputTokens = usage?.candidatesTokenCount ?: 0
                )

                val rawText = response.text ?: throw Exception("Sin respuesta de la IA")

                val jsonStartIndex = rawText.indexOf('{')
                val jsonEndIndex = rawText.lastIndexOf('}')
                if (jsonStartIndex == -1 || jsonEndIndex == -1) throw Exception("Formato JSON inválido devuelto por la IA")


                val questionUiStates = GeminiQuestionParser.parse(rawText)

                startTime = System.currentTimeMillis()
                _uiState.update { it.copy(questions = questionUiStates, isLoading = false) }
                persistSession()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    companion object {
        /**
         * How many mistakes a single review session covers. One generated question per
         * mistake, so this is also the size — and the cost — of the Gemini call. Lowered
         * from 30 to 10 (2026-09-20): at 30 this session cost as much as a full paid exam
         * while being free and regenerated on every screen open.
         */
        private const val MAX_MISTAKES_PER_REVIEW = 10

        private const val KEY_SESSION = "mistakes_saved_session"
        private const val KEY_GENERATION_STARTED = "mistakes_generation_started"
    }

    @Serializable
    private data class SavedMistakesSession(
        val questions: List<QuestionUiState>,
        val currentQuestionIndex: Int,
        val selectedAnswers: Map<Int, Int>,
        val isAnswerChecked: Boolean,
        val sessionStreak: Int
    )

    private fun selectOption(optionIndex: Int) {
        if (_uiState.value.isAnswerChecked) return
        _uiState.update {
            val newAnswers = it.selectedAnswers.toMutableMap()
            newAnswers[it.currentQuestionIndex] = optionIndex
            it.copy(selectedAnswers = newAnswers)
        }
        persistSession()
    }

    private fun checkAnswer() {
        val currentState = _uiState.value
        val selectedOption = currentState.selectedAnswers[currentState.currentQuestionIndex]
        if (selectedOption != null) {
            val question = currentState.questions[currentState.currentQuestionIndex]
            val isCorrect = selectedOption == question.correctAnswerIndex
            
            if (isCorrect) {
                sessionStreak++
                viewModelScope.launch {
                    val originalText = originalQuestions.getOrNull(currentState.currentQuestionIndex)
                    if (originalText != null) {
                        answerRepository.markAsCorrected(originalText)
                    }
                }
            } else {
                sessionStreak = 0
            }
            
            _uiState.update { it.copy(isAnswerChecked = true, sessionStreak = sessionStreak) }
            persistSession()
        }
    }

    private fun nextQuestion() {
        if (_uiState.value.currentQuestionIndex < _uiState.value.questions.size - 1) {
            _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex + 1, isAnswerChecked = false) }
            persistSession()
        }
    }

    private suspend fun calculateResult(): TestSummary {
        val state = _uiState.value
        val correct = state.questions.indices.count { index ->
            state.selectedAnswers[index] == state.questions[index].correctAnswerIndex
        }
        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val accuracy = if (state.questions.isNotEmpty()) ((correct.toFloat() / state.questions.size) * 100).toInt() else 0
        var newWeekSessions = -1

        val xpEarned = incrementXpUseCase(
            mode = TestMode.PRACTICE,
            correctAnswers = correct,
            totalQuestions = state.questions.size,
            durationSeconds = durationSeconds
        )

        val coinsGained = incrementCoinsUseCase(accuracy)

        withContext(Dispatchers.IO) {
            testResultRepository.insert(
                TestResult(
                    category = "Repaso de Fallos",
                    score = correct,
                    date = Date(),
                    total = state.questions.size,
                    isPassed = accuracy >= 90
                )
            )

            newWeekSessions = incrementStreakUseCase()
        }

        return TestSummary(
            score = correct,
            total = state.questions.size,
            xpGained = xpEarned.xpGained,
            durationSeconds = durationSeconds,
            accuracy = accuracy,
            baseXp = xpEarned.baseXp,
            bonusPerfection = xpEarned.bonusPerfection,
            bonusFast = xpEarned.bonusFast,
            bonusStreak = xpEarned.bonusStreak,
            leveledUp = xpEarned.levelUp,
            coinsGained = coinsGained,
            newWeekSessions = newWeekSessions
        )
    }
}
