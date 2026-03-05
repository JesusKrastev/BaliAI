package com.jesuskrastev.bali.ui.screens.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.usecase.DecrementEnergyUseCase
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
    private val gemini: GenerativeModel,
    private val decrementEnergyUseCase: DecrementEnergyUseCase,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val analyticsTracker: FirebaseAnalyticsTracker
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
        generateMistakesTest()
        analyticsTracker.reviewStarted()
    }

    fun onEvent(event: MistakesEvent) {
        when (event) {
            MistakesEvent.Retry -> retry()
            is MistakesEvent.SelectOption -> selectOption(event.optionIndex)
            MistakesEvent.CheckAnswer -> checkAnswer()
            MistakesEvent.NextQuestion -> nextQuestion()
            is MistakesEvent.FinishReview -> {
                viewModelScope.launch {
                    val result = calculateResult()
                    analyticsTracker.reviewCompleted()
                    event.onResult(result)
                }
            }
        }
    }

    private fun retry() {
        sessionStreak = 0
        _uiState.update { it.copy(isLoading = true, error = null, questions = emptyList(), currentQuestionIndex = 0, selectedAnswers = emptyMap(), isAnswerChecked = false, sessionStreak = 0) }
        generateMistakesTest()
    }

    private fun generateMistakesTest() {
        viewModelScope.launch {
            try {
                val recentMistakes = answerRepository.getRecentMistakes().first()
                if (recentMistakes.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "Â¡Felicidades! No tienes errores pendientes por repasar.") }
                    return@launch
                }

                val mistakesContext = recentMistakes.joinToString("\n") { "- $it" }
                val user = userRepository.get().first()
                val license = user?.licenseType?.takeIf { it.isNotBlank() } ?: "B (Coche)"
                val studentLevel = user?.level ?: 1
                val totalTests = testResultRepository.count()

                val prompt = """
                    Eres un Profesor Experto y Tutor Personal de la DGT (DirecciÃ³n General de TrÃ¡fico) en EspaÃ±a. 
                    Tu alumno ha fallado recientemente estas preguntas en sus tests:
                    $mistakesContext
    
                    Tu misiÃ³n es generar una SESIÃ“N DE REPASO de EXACTAMENTE ${recentMistakes.size} preguntas, enfocada EXCLUSIVAMENTE en corregir estos conceptos.
                    
                    CONTEXTO DEL ALUMNO (PERSONALIZACIÃ“N):
                    - Permiso al que aspira: Permiso $license.
                    - Nivel actual en la app: $studentLevel (A mayor nivel, usa distractores mÃ¡s complejos).
                    - Total de tests realizados: $totalTests.
                    
                    REGLAS DE LA PREGUNTA (REPASO DE ERRORES):
                    - Genera EXACTAMENTE UNA pregunta por cada concepto fallado en la lista proporcionada.
                    - REFORMULA la pregunta y las opciones para que NO sean idÃ©nticas a las originales, pero evalÃºen la misma norma o situaciÃ³n. Obliga al alumno a pensar, no a memorizar la respuesta correcta anterior.
                    - Estilo DGT oficial: Lenguaje tÃ©cnico, preciso.
                    - 3 opciones por pregunta con el TEXTO REAL de la respuesta (nada de "A", "B", "C"). Solo una es correcta.
                    - EXPLICACIÃ“N: MÃ¡ximo 25 palabras. Al ser un test de repaso, la explicaciÃ³n debe ser muy didÃ¡ctica, aclarando la "trampa" o el concepto que el alumno suele confundir.
                    
                    REGLAS DE CALIDAD Y ACTUALIZACIÃ“N (Â¡ESTRICTAMENTE OBLIGATORIO!):
                    - ALEATORIEDAD EXTREMA: El valor de "correctAnswerIndex" (0, 1 o 2) DEBE ser completamente aleatorio. ESTÃ PROHIBIDO repetir la misma posiciÃ³n correcta mÃ¡s de 2 veces seguidas.
                    - NORMATIVA VIGENTE: Usa SIEMPRE la ley de trÃ¡fico espaÃ±ola mÃ¡s reciente (ej. baliza V-16, lÃ­mites de 30 km/h en vÃ­as urbanas de un carril, nueva normativa de patinetes VMP).
                    - TRAMPAS TÃPICAS DGT: Haz que las respuestas incorrectas atraigan el error tÃ­pico que el alumno cometiÃ³ antes.
                    
                    REGLAS DE IMÃGENES (SISTEMA FILEPATH):
                    - Usa imÃ¡genes SOLO si la pregunta reformulada describe una situaciÃ³n visual o una seÃ±al fÃ­sica.
                    - Formato obligatorio: https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal[codigo].svg
                    - CÃ³digos vÃ¡lidos de ejemplo: r1 (ceda), r2 (stop), p1 (peligro), r301 (velocidad 40), s1 (autopista).
                    - Si la pregunta es puramente teÃ³rica (ej: tasa de alcohol, documentaciÃ³n), usa null.
    
                    FORMATO DE RESPUESTA (JSON PURO):
                    {
                      "questions": [
                        {
                          "text": "Â¿Pregunta reformulada con estilo DGT?",
                          "options": ["Texto detallado de la opciÃ³n 1", "Texto detallado de la opciÃ³n 2", "Texto detallado de la opciÃ³n 3"],
                          "correctAnswerIndex": 0,
                          "explanation": "ExplicaciÃ³n didÃ¡ctica sobre el error frecuente...",
                          "imageUrl": "URL_O_NULL"
                        }
                      ]
                    }
                    Responde SOLO el JSON vÃ¡lido y cierra correctamente todas las llaves y corchetes.
                """.trimIndent()

                val response = gemini.generateContent(prompt)
                analyticsTracker.geminiUsage(
                    inputTokens  = response.usageMetadata?.promptTokenCount     ?: 0,
                    outputTokens = response.usageMetadata?.candidatesTokenCount ?: 0,
                    feature      = "MISTAKES"
                )
                val rawText = response.text ?: throw Exception("Sin respuesta de la IA")

                val jsonStartIndex = rawText.indexOf('{')
                val jsonEndIndex = rawText.lastIndexOf('}')
                if (jsonStartIndex == -1 || jsonEndIndex == -1) throw Exception("Formato JSON invÃ¡lido devuelto por la IA")

                val jsonString = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
                
                val questionUiStates = GeminiQuestionParser.parse(rawText)

                startTime = System.currentTimeMillis()
                analyticsTracker.testStarted("MISTAKES")
                _uiState.update { it.copy(questions = questionUiStates, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    private fun selectOption(optionIndex: Int) {
        if (_uiState.value.isAnswerChecked) return
        _uiState.update {
            val newAnswers = it.selectedAnswers.toMutableMap()
            newAnswers[it.currentQuestionIndex] = optionIndex
            it.copy(selectedAnswers = newAnswers)
        }
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
        }
    }

    private fun nextQuestion() {
        if (_uiState.value.currentQuestionIndex < _uiState.value.questions.size - 1) {
            _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex + 1, isAnswerChecked = false) }
        }
    }

    private suspend fun calculateResult(): TestSummary {
        val state = _uiState.value
        val correct = state.questions.indices.count { index ->
            state.selectedAnswers[index] == state.questions[index].correctAnswerIndex
        }
        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val accuracy = if (state.questions.isNotEmpty()) ((correct.toFloat() / state.questions.size) * 100).toInt() else 0

        val xpEarned = incrementXpUseCase(
            mode = TestMode.PRACTICE,
            correctAnswers = correct,
            totalQuestions = state.questions.size,
            durationSeconds = durationSeconds
        )

        val coinsGained = incrementCoinsUseCase(accuracy)
        analyticsTracker.coinsEarned(coinsGained)

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

            decrementEnergyUseCase().also { newEnergy ->
                analyticsTracker.energyConsumed(newEnergy)
                if (newEnergy == 0) analyticsTracker.energyDepleted()
            }
            incrementStreakUseCase().also { streak ->
                if (streak > 0) analyticsTracker.streakRecorded(streak)
            }
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
            coinsGained = coinsGained
        )
    }
}
