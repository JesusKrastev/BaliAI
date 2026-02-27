package com.jesuskrastev.bali.ui.screens.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.data.repository.AnswerRepositoryImpl
import com.jesuskrastev.bali.data.repository.TestResultRepositoryImpl
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.usecase.DecrementEnergyUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import com.jesuskrastev.bali.ui.screens.test.TestSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val userRepository: UserRepositoryImpl,
    private val testResultRepository: TestResultRepositoryImpl,
    private val answerRepository: AnswerRepositoryImpl,
    private val gemini: GenerativeModel,
    private val decrementEnergyUseCase: DecrementEnergyUseCase,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val analyticsTracker: FirebaseAnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startTime: Long = 0
    private var sessionStreak: Int = 0
    private var examFinished = false

    private val jsonContent = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    init {
        generateExam()
    }

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
                    analyticsTracker.testCompleted("EXAM")
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
            analyticsTracker.questionAnswered(isCorrect)
            _uiState.update { it.copy(isAnswerChecked = true, sessionStreak = sessionStreak) }
        }
    }

    private fun retry() {
        timerJob?.cancel()
        sessionStreak = 0
        examFinished = false
        _uiState.update { ExamUiState() }
        generateExam()
    }

    private fun generateExam() {
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
                    - ALEATORIEDAD EXTREMA: El valor de "correctAnswerIndex" (0, 1 o 2) DEBE ser completamente aleatorio a lo largo de las 30 preguntas. ESTÁ PROHIBIDO repetir la misma posición correcta más de 2 veces seguidas.
                    - NORMATIVA VIGENTE: Usa SIEMPRE la ley de tráfico española más reciente (ej. baliza V-16 en lugar de triángulos en autopista, límites de 30 km/h en vías urbanas de un carril, nueva normativa de VMP/patinetes, 0,0 alcohol para menores).
                    
                    REGLAS DE IMÁGENES (SISTEMA FILEPATH):
                    - Usa imágenes SOLO si la pregunta describe una situación visual o una señal física. (Máximo 10-12 imágenes en todo el examen para no saturar).
                    - Formato obligatorio: https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal[codigo].svg
                    - Códigos válidos de ejemplo: r1 (ceda), r2 (stop), p1 (peligro), r301 (velocidad 40), s1 (autopista).
                    - Si la pregunta es puramente teórica (ej: tasa de alcohol, mecánica), usa null.
                    
                    FORMATO DE RESPUESTA (JSON PURO):
                    {
                      "questions": [
                        {
                          "text": "¿Pregunta real con estilo DGT?",
                          "options": ["Texto detallado de la opción 1", "Texto detallado de la opción 2", "Texto detallado de la opción 3"],
                          "correctAnswerIndex": 0,
                          "explanation": "Breve justificación de la norma...",
                          "imageUrl": "URL_O_NULL"
                        }
                      ]
                    }
                    Responde SOLO con el JSON válido. Asegúrate de que el array "questions" tenga EXACTAMENTE 30 elementos y cierra correctamente todas las llaves y corchetes.
                """.trimIndent()

                val response = gemini.generateContent(prompt)
                analyticsTracker.geminiUsage(
                    inputTokens = response.usageMetadata?.promptTokenCount ?: 0,
                    outputTokens = response.usageMetadata?.candidatesTokenCount ?: 0,
                    feature = "EXAM"
                )
                val rawText = response.text ?: throw Exception("Sin respuesta")

                val jsonStartIndex = rawText.indexOf('{')
                val jsonEndIndex = rawText.lastIndexOf('}')
                val jsonString = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
                val root = jsonContent.parseToJsonElement(jsonString).jsonObject

                val questionUiStates = root["questions"]?.jsonArray?.map { element ->
                    val obj = element.jsonObject
                    QuestionUiState(
                        text = obj["text"]?.jsonPrimitive?.content ?: "",
                        options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }
                            ?: emptyList(),
                        correctAnswerIndex = obj["correctAnswerIndex"]?.jsonPrimitive?.content?.toInt()
                            ?: 0,
                        explanation = obj["explanation"]?.jsonPrimitive?.content ?: "",
                        imageUrl = obj["imageUrl"]?.jsonPrimitive?.content.takeIf {
                            it != "null" && it != null && it.startsWith(
                                "http"
                            )
                        }
                    )
                } ?: emptyList()

                startTime = System.currentTimeMillis()
                analyticsTracker.testStarted("EXAM")
                _uiState.update { it.copy(questions = questionUiStates, isLoading = false) }
                startTimer()
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
    }

    private fun goToQuestion(index: Int) {
        _uiState.update {
            it.copy(
                currentQuestionIndex = index,
                showReviewGrid = false,
                isAnswerChecked = false
            )
        }
    }

    private fun nextQuestion() {
        if (_uiState.value.currentQuestionIndex < _uiState.value.questions.size - 1) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = it.currentQuestionIndex + 1,
                    isAnswerChecked = false
                )
            }
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
        }
    }

    private fun toggleReviewGrid() {
        _uiState.update { it.copy(showReviewGrid = !it.showReviewGrid) }
    }

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

        val xpEarned = incrementXpUseCase(
            mode = TestMode.EXAM,
            correctAnswers = correct,
            totalQuestions = state.questions.size,
            durationSeconds = durationSeconds
        )

        val coinsGained = incrementCoinsUseCase()

        viewModelScope.launch {
            val testId = testResultRepository.insert(
                TestResult(
                    category = "Examen Oficial",
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

            decrementEnergyUseCase()
            incrementStreakUseCase()
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        if (!examFinished && _uiState.value.questions.isNotEmpty()) {
            analyticsTracker.testAbandoned("EXAM", _uiState.value.currentQuestionIndex + 1)
        }
    }
}
