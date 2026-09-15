package com.jesuskrastev.bali.ui.screens.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.GenerativeModel
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
    private val incrementCoinsUseCase: IncrementCoinsUseCase
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
                val difficultTopics = user?.difficultTopics ?: "Ninguno especÃ­fico (distribuciÃ³n estÃ¡ndar)"
                val studentLevel = user?.level
                val experience = user?.experience?.takeIf { it.isNotBlank() } ?: "Desconocida"
                val daysToExam = user?.examDateMillis?.let {
                    val diffMillis = it - System.currentTimeMillis()
                    (diffMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                }
                val totalTests = testResultRepository.count()
                val examUrgency = if (daysToExam != null && daysToExam in 1..15) {
                    "Â¡El examen es en $daysToExam dÃ­as! SÃ© estricto y pon preguntas de alta probabilidad de fallo."
                } else {
                    "Modo simulacro estÃ¡ndar."
                }

                val prompt = """
                    Eres el Examinador Jefe de la DGT (DirecciÃ³n General de TrÃ¡fico) en EspaÃ±a. Tu misiÃ³n es generar un EXAMEN OFICIAL COMPLETO y riguroso de EXACTAMENTE 30 preguntas.
                    
                    CONTEXTO DEL ALUMNO (PERSONALIZACIÃ“N):
                    - Permiso al que aspira: Permiso $license.
                    - Nivel actual en la app: $studentLevel (A mayor nivel, usa distractores mÃ¡s complejos y sutiles).
                    - Total de tests realizados: $totalTests (Si son pocos, haz explicaciones mÃ¡s didÃ¡cticas paso a paso. Si son muchos, asume que tiene experiencia y usa un tono mÃ¡s exigente).
                    - Experiencia previa: $experience.
                    - Temas que mÃ¡s le cuestan: $difficultTopics. (IMPORTANTE: AsegÃºrate de que varias preguntas del examen ataquen estos puntos dÃ©biles especÃ­ficos para que practique).
                    - Urgencia: $examUrgency
                    
                    REGLAS DE DISTRIBUCIÃ“N (ESTRICTAS PARA 30 PREGUNTAS):
                    - Debe ser un simulacro exacto del examen real para el permiso $license.
                    - Variedad obligatoria. Distribuye las preguntas asÃ­: SeÃ±ales (aprox. 6), Normativa y Velocidad (aprox. 6), Seguridad Vial y Accidentes (aprox. 5), Maniobras e Intersecciones (aprox. 5), El Conductor, fatiga y Alcohol/Drogas (aprox. 5), MecÃ¡nica bÃ¡sica y Mantenimiento (aprox. 3).
                    - Si los "Temas que mÃ¡s le cuestan" encajan en alguna de estas categorÃ­as, aumenta la dificultad de esas preguntas especÃ­ficas.
                    
                    REGLAS DE LA PREGUNTA Y OPCIONES:
                    - Estilo DGT oficial: Lenguaje tÃ©cnico, preciso y con situaciones hipotÃ©ticas ("Circula por una vÃ­a...", "Como norma general...").
                    - 3 opciones por pregunta con el TEXTO REAL de la respuesta (no pongas solo "A", "B" o "C"). Solo una es correcta.
                    - Las respuestas incorrectas (distractores) deben ser muy creÃ­bles y usar trampas tÃ­picas de la DGT (ej. usar absolutos como "siempre" o "nunca" para confundir).
                    - EXPLICACIÃ“N: MÃ¡ximo 20 palabras. Debe ser clara, pedagÃ³gica y justificar la norma. Intenta darle un toque motivador o de tutor si falla en sus temas difÃ­ciles.
                    
                    REGLAS DE CALIDAD Y ACTUALIZACIÃ“N (Â¡MUY IMPORTANTE!):
                    - NORMATIVA VIGENTE: Usa SIEMPRE la ley de trÃ¡fico espaÃ±ola mÃ¡s reciente (ej. baliza V-16 en lugar de triÃ¡ngulos en autopista, lÃ­mites de 30 km/h en vÃ­as urbanas de un carril, nueva normativa de VMP/patinetes, 0,0 alcohol para menores).
                    
                    REGLAS DE IMÃGENES (SISTEMA FILEPATH):
                    - Usa imÃ¡genes SOLO si la pregunta describe una situaciÃ³n visual o una seÃ±al fÃ­sica. (MÃ¡ximo 10-12 imÃ¡genes en todo el examen para no saturar).
                    - Formato obligatorio: https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal[codigo].svg
                    - CÃ³digos vÃ¡lidos de ejemplo: r1 (ceda), r2 (stop), p1 (peligro), r301 (velocidad 40), s1 (autopista).
                    - Si la pregunta es puramente teÃ³rica (ej: tasa de alcohol, mecÃ¡nica), usa null.
                    
                    Genera EXACTAMENTE 30 preguntas.
                """.trimIndent()

                val response = gemini.generateContent(prompt)
                val rawText = response.text ?: throw Exception("Sin respuesta")

                val questionUiStates = GeminiQuestionParser.parse(rawText)

                startTime = System.currentTimeMillis()
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
        var newWeekSessions = -1

        val xpEarned = incrementXpUseCase(
            mode = TestMode.EXAM,
            correctAnswers = correct,
            totalQuestions = state.questions.size,
            durationSeconds = durationSeconds
        )

        val coinsGained = incrementCoinsUseCase(accuracy)

        withContext(Dispatchers.IO) {
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
