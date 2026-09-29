package com.jesuskrastev.bali.ui.screens.exam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.GenerativeModel
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.di.QuestionsModel
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.GeminiQuestionParser
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import com.jesuskrastev.bali.ui.screens.test.TestSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import javax.inject.Inject

data class ExamUiState(
    val questions: List<QuestionUiState> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val timeLeftSeconds: Int = 1800, // 30 minutes
    val isTimeUp: Boolean = false,
    val showReviewGrid: Boolean = false,
    val sessionStreak: Int = 0,
    val isAnswerChecked: Boolean = false
)

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val testResultRepository: TestResultRepository,
    private val answerRepository: AnswerRepository,
    @QuestionsModel private val gemini: GenerativeModel,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val analytics: AnalyticsTracker,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startTime: Long = 0
    private var sessionStreak: Int = 0
    private var examFinished = false

    /** Wall-clock instant the exam's 30-minute window ends, so a restart can recompute
     *  the time actually left instead of resetting to a fresh 30 minutes. */
    private var examEndAtMillis: Long = 0

    private val jsonContent = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    init {
        if (!restoreSession()) {
            generateExam()
        }
    }

    /**
     * Restores a previously generated exam — questions, progress and the real time left
     * on the clock — from [savedStateHandle], if one is there. This is what lets a
     * process restart mid-exam (Android killing the app in the background, then
     * Navigation replaying the back stack) put the student back where they were instead
     * of paying for a brand new 30-question generation.
     *
     * @return true when a session was restored and no generation is needed.
     */
    private fun restoreSession(): Boolean {
        val json = savedStateHandle.get<String>(KEY_SESSION) ?: return false
        val session = runCatching { jsonContent.decodeFromString<SavedExamSession>(json) }
            .getOrNull() ?: return false
        if (session.questions.isEmpty()) return false

        examEndAtMillis = session.examEndAtMillis
        sessionStreak = session.sessionStreak
        startTime = System.currentTimeMillis()
        val remainingSeconds =
            ((examEndAtMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)

        _uiState.update {
            it.copy(
                questions = session.questions,
                currentQuestionIndex = session.currentQuestionIndex,
                selectedAnswers = session.selectedAnswers,
                isAnswerChecked = session.isAnswerChecked,
                sessionStreak = session.sessionStreak,
                isLoading = false,
                error = null,
                timeLeftSeconds = remainingSeconds,
                isTimeUp = remainingSeconds <= 0
            )
        }
        if (remainingSeconds > 0) startTimer()
        return true
    }

    /** Snapshots the restorable parts of [ExamUiState] plus the exam deadline into [savedStateHandle]. */
    private fun persistSession() {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        val session = SavedExamSession(
            questions = state.questions,
            currentQuestionIndex = state.currentQuestionIndex,
            selectedAnswers = state.selectedAnswers,
            isAnswerChecked = state.isAnswerChecked,
            sessionStreak = state.sessionStreak,
            examEndAtMillis = examEndAtMillis
        )
        savedStateHandle[KEY_SESSION] = jsonContent.encodeToString(session)
    }

    /**
     * Tells apart a screen's first-ever generation from one triggered by a ViewModel
     * recreated mid-session with nothing left to restore — i.e. Android killed the
     * process while an exam was in flight or on screen.
     */
    private fun determineReason(): String =
        if (savedStateHandle.get<Boolean>(KEY_GENERATION_STARTED) == true) "process_restart" else "initial"

    fun onEvent(event: ExamEvent) {
        when (event) {
            ExamEvent.Retry -> retry()
            is ExamEvent.SelectOption -> selectOption(event.optionIndex)
            is ExamEvent.GoToQuestion -> goToQuestion(event.index)
            ExamEvent.NextQuestion -> nextQuestion()
            ExamEvent.PreviousQuestion -> previousQuestion()
            is ExamEvent.FinishExam -> {
                viewModelScope.launch {
                    val result = calculateResult()
                    examFinished = true
                    event.onResult(result)
                }
            }

            ExamEvent.ToggleQuestionReview -> toggleReviewGrid()
            ExamEvent.CheckAnswer -> checkAnswer()
        }
    }

    private fun checkAnswer() {
        val currentState = _uiState.value
        val selectedOption = currentState.selectedAnswers[currentState.currentQuestionIndex]
        if (selectedOption != null) {
            val isCorrect =
                selectedOption == currentState.questions[currentState.currentQuestionIndex].correctAnswerIndex
            if (isCorrect) sessionStreak++ else sessionStreak = 0
            _uiState.update { it.copy(isAnswerChecked = true, sessionStreak = sessionStreak) }
            persistSession()
        }
    }

    private fun retry() {
        timerJob?.cancel()
        sessionStreak = 0
        examFinished = false
        _uiState.update { ExamUiState() }
        generateExam(reason = "retry")
    }

    /**
     * Calls Gemini for a fresh 30-question exam — the single most expensive generation in
     * the app.
     *
     * @param reason why this call is happening — `"initial"`/`"process_restart"` (from
     *   [determineReason]) or `"retry"` — logged alongside the real token cost once the
     *   response comes back, so cost spikes can be traced to the reason that caused them.
     */
    private fun generateExam(reason: String = determineReason()) {
        viewModelScope.launch {
            try {
                val user = userRepository.get().first()
                val license = user?.licenseType?.takeIf { it.isNotBlank() } ?: "B (Coche)"
                val difficultTopics = user?.difficultTopics ?: "Ninguno específico (distribución estándar)"
                val studentLevel = user?.level
                val experience = user?.experience?.takeIf { it.isNotBlank() } ?: "Desconocida"
                val daysToExam = user?.examDateMillis?.let {
                    val diffMillis = it - System.currentTimeMillis()
                    (diffMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                }
                val totalTests = testResultRepository.count()
                val examUrgency = if (daysToExam != null && daysToExam in 1..15) {
                    "¡El examen es en $daysToExam días! Sé estricto y pon preguntas de alta probabilidad de fallo."
                } else {
                    "Modo simulacro estándar."
                }

                val prompt = """
                    Eres el Examinador Jefe de la DGT (Dirección General de Tráfico) en España. Tu misión es generar un EXAMEN OFICIAL COMPLETO y riguroso de EXACTAMENTE 30 preguntas.
                    
                    CONTEXTO DEL ALUMNO (PERSONALIZACIÓN):
                    - Permiso al que aspira: Permiso $license.
                    - Nivel actual en la app: $studentLevel (A mayor nivel, usa distractores más complejos y sutiles).
                    - Total de tests realizados: $totalTests (Si son pocos, haz explicaciones más didácticas paso a paso. Si son muchos, asume que tiene experiencia y usa un tono más exigente).
                    - Experiencia previa: $experience.
                    - Temas que más le cuestan: $difficultTopics. (IMPORTANTE: Asegúrate de que varias preguntas del examen ataquen estos puntos débiles específicos para que practique).
                    - Urgencia: $examUrgency
                    
                    REGLAS DE DISTRIBUCIÓN (ESTRICTAS PARA 30 PREGUNTAS):
                    - Debe ser un simulacro exacto del examen real para el permiso $license.
                    - Variedad obligatoria. Distribuye las preguntas así: Señales (aprox. 6), Normativa y Velocidad (aprox. 6), Seguridad Vial y Accidentes (aprox. 5), Maniobras e Intersecciones (aprox. 5), El Conductor, fatiga y Alcohol/Drogas (aprox. 5), Mecánica básica y Mantenimiento (aprox. 3).
                    - Si los "Temas que más le cuestan" encajan en alguna de estas categorías, aumenta la dificultad de esas preguntas específicas.
                    
                    REGLAS DE LA PREGUNTA Y OPCIONES:
                    - Estilo DGT oficial: Lenguaje técnico, preciso y con situaciones hipotéticas ("Circula por una vía...", "Como norma general...").
                    - 3 opciones por pregunta con el TEXTO REAL de la respuesta (no pongas solo "A", "B" o "C"). Solo una es correcta.
                    - Las respuestas incorrectas (distractores) deben ser muy creíbles y usar trampas típicas de la DGT (ej. usar absolutos como "siempre" o "nunca" para confundir).
                    - EXPLICACIÓN: Máximo 20 palabras. Debe ser clara, pedagógica y justificar la norma. Intenta darle un toque motivador o de tutor si falla en sus temas difíciles.
                    
                    REGLAS DE CALIDAD Y ACTUALIZACIÓN (¡MUY IMPORTANTE!):
                    - NORMATIVA VIGENTE: Usa SIEMPRE la ley de tráfico española más reciente (ej. baliza V-16 en lugar de triángulos en autopista, límites de 30 km/h en vías urbanas de un carril, nueva normativa de VMP/patinetes, 0,0 alcohol para menores).
                    
                    REGLAS DE IMÁGENES (SISTEMA FILEPATH):
                    - Usa imágenes SOLO si la pregunta describe una situación visual o una señal física. (Máximo 10-12 imágenes en todo el examen para no saturar).
                    - Formato obligatorio: https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal[codigo].svg
                    - Códigos válidos de ejemplo: r1 (ceda), r2 (stop), p1 (peligro), r301 (velocidad 40), s1 (autopista).
                    - Si la pregunta es puramente teórica (ej: tasa de alcohol, mecánica), usa null.
                    
                    Genera EXACTAMENTE 30 preguntas.
                """.trimIndent()

                savedStateHandle[KEY_GENERATION_STARTED] = true
                val response = gemini.generateContent(prompt)

                // Logged as soon as the response is back — tokens are billed the moment
                // Gemini answers, whether or not the JSON below turns out parseable.
                val usage = response.usageMetadata
                analytics.examGenerated(
                    reason = reason,
                    inputTokens = usage?.promptTokenCount ?: 0,
                    outputTokens = usage?.candidatesTokenCount ?: 0
                )

                val rawText = response.text ?: throw Exception("Sin respuesta")

                val questionUiStates = GeminiQuestionParser.parse(rawText)

                startTime = System.currentTimeMillis()
                examEndAtMillis = System.currentTimeMillis() + EXAM_DURATION_SECONDS * 1000L
                _uiState.update {
                    it.copy(
                        questions = questionUiStates,
                        isLoading = false,
                        timeLeftSeconds = EXAM_DURATION_SECONDS
                    )
                }
                startTimer()
                persistSession()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeftSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(timeLeftSeconds = it.timeLeftSeconds - 1) }
            }
            _uiState.update { it.copy(isTimeUp = true) }
        }
    }

    private fun selectOption(optionIndex: Int) {
        if (_uiState.value.isAnswerChecked) return
        _uiState.update {
            val newAnswers = it.selectedAnswers.toMutableMap()
            newAnswers[it.currentQuestionIndex] = optionIndex
            it.copy(selectedAnswers = newAnswers)
        }
        persistSession()
    }

    private fun goToQuestion(index: Int) {
        _uiState.update {
            it.copy(
                currentQuestionIndex = index,
                showReviewGrid = false,
                isAnswerChecked = false
            )
        }
        persistSession()
    }

    private fun nextQuestion() {
        if (_uiState.value.currentQuestionIndex < _uiState.value.questions.size - 1) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = it.currentQuestionIndex + 1,
                    isAnswerChecked = false
                )
            }
            persistSession()
        }
    }

    private fun previousQuestion() {
        if (_uiState.value.currentQuestionIndex > 0) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = it.currentQuestionIndex - 1,
                    isAnswerChecked = false
                )
            }
            persistSession()
        }
    }

    private fun toggleReviewGrid() {
        _uiState.update { it.copy(showReviewGrid = !it.showReviewGrid) }
    }

    /**
     * Scores the finished exam and applies everything it earns: XP, coins, the saved result and
     * answers and the weekly streak. It passes with 27 correct answers out of 30 (three mistakes
     * or fewer). Reports the attempt, and the pass if there was one, to analytics once it is all
     * saved.
     *
     * @return the summary the result screen shows
     */
    private suspend fun calculateResult(): TestSummary {
        timerJob?.cancel()
        val state = _uiState.value
        val correct = state.questions.indices.count { index ->
            state.selectedAnswers[index] == state.questions[index].correctAnswerIndex
        }
        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val accuracy =
            if (state.questions.isNotEmpty()) ((correct.toFloat() / state.questions.size) * 100).toInt() else 0

        // Un examen de la DGT de 30 preguntas se aprueba con 3 fallos o menos (27 correctas)
        val isPassed = correct >= 27
        var newWeekSessions = -1

        // Every EXAM path node opens this same generic simulator (no specific node is tracked
        // here), so "repeat" means "not this user's first official exam" rather than "this exact
        // content again" — otherwise a 100-coin exam would silently pay full XP every time.
        val previousAttempts = testResultRepository.get().first().count { it.category == OFFICIAL_EXAM_CATEGORY }
        val isRepeat = previousAttempts > 0

        val xpEarned = incrementXpUseCase(
            mode = TestMode.EXAM,
            correctAnswers = correct,
            totalQuestions = state.questions.size,
            durationSeconds = durationSeconds,
            isRepeat = isRepeat
        )

        val coinsGained = incrementCoinsUseCase(accuracy)

        withContext(Dispatchers.IO) {
            val testId = testResultRepository.insert(
                TestResult(
                    category = OFFICIAL_EXAM_CATEGORY,
                    score = correct,
                    total = state.questions.size,
                    date = Date(),
                    isPassed = isPassed
                )
            )

            state.questions.forEachIndexed { index, question ->
                val selectedOption = state.selectedAnswers[index]
                if (selectedOption != null) {
                    answerRepository.insert(
                        Answer(
                            date = Date(),
                            testId = testId,
                            questionText = question.text,
                            selectedOption = selectedOption,
                            isCorrect = selectedOption == question.correctAnswerIndex
                        )
                    )
                }
            }

            newWeekSessions = incrementStreakUseCase()
        }

        trackCompletion(
            correct = correct,
            total = state.questions.size,
            accuracy = accuracy,
            durationSeconds = durationSeconds,
            isPassed = isPassed,
            xpGained = xpEarned.xpGained,
            timeRanOut = state.isTimeUp,
            attemptNumber = previousAttempts + 1
        )

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

    /**
     * Reports the finished exam, and then either the pass or the failure, once the result has
     * been saved.
     *
     * @param correct number of correct answers
     * @param total number of questions in the exam
     * @param accuracy correct answers as a whole percentage, 0 to 100
     * @param durationSeconds time spent on the exam
     * @param isPassed whether the score met the DGT bar (three mistakes or fewer)
     * @param xpGained XP awarded for this attempt
     * @param timeRanOut whether the clock ran out before the user finished
     * @param attemptNumber 1 for the user's first official exam, 2 for the second, and so on
     */
    private fun trackCompletion(
        correct: Int,
        total: Int,
        accuracy: Int,
        durationSeconds: Int,
        isPassed: Boolean,
        xpGained: Int,
        timeRanOut: Boolean,
        attemptNumber: Int
    ) {
        analytics.examCompleted(
            score = correct,
            total = total,
            accuracy = accuracy,
            durationSeconds = durationSeconds,
            passed = isPassed,
            xpGained = xpGained,
            attemptNumber = attemptNumber
        )
        if (isPassed) {
            analytics.examPassed(
                score = correct,
                total = total,
                durationSeconds = durationSeconds,
                attemptNumber = attemptNumber
            )
        } else {
            analytics.examFailed(
                score = correct,
                total = total,
                mistakes = total - correct,
                durationSeconds = durationSeconds,
                timeRanOut = timeRanOut,
                attemptNumber = attemptNumber
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        /** The exam's fixed time limit, matching [ExamUiState]'s default `timeLeftSeconds`. */
        private const val EXAM_DURATION_SECONDS = 1800

        private const val KEY_SESSION = "exam_saved_session"
        private const val KEY_GENERATION_STARTED = "exam_generation_started"

        /** [TestResult.category] used for every official-exam attempt, win or lose. */
        private const val OFFICIAL_EXAM_CATEGORY = "Examen Oficial"
    }

    @Serializable
    private data class SavedExamSession(
        val questions: List<QuestionUiState>,
        val currentQuestionIndex: Int,
        val selectedAnswers: Map<Int, Int>,
        val isAnswerChecked: Boolean,
        val sessionStreak: Int,
        val examEndAtMillis: Long
    )
}
