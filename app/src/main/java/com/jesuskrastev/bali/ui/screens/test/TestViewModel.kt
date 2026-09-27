package com.jesuskrastev.bali.ui.screens.test

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
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import com.jesuskrastev.bali.domain.repository.PathRepository
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.GeminiQuestionParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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


data class TestUiState(
    val category: String = "",
    val questions: List<QuestionUiState> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isAnswerChecked: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionStreak: Int = 0
)

data class TestSummary(
    val score: Int,
    val total: Int,
    val xpGained: Int,
    val durationSeconds: Int,
    val accuracy: Int,
    val baseXp: Int,
    val bonusPerfection: Int?,
    val bonusFast: Int?,
    val bonusStreak: Int?,
    val leveledUp: Boolean,
    val coinsGained: Int,
    val newWeekSessions: Int
)

@HiltViewModel
class TestViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val testResultRepository: TestResultRepository,
    private val answerRepository: AnswerRepository,
    @QuestionsModel private val gemini: GenerativeModel,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val pathRepository: PathRepository,
    private val analytics: AnalyticsTracker,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()
    private var startTime: Long = 0
    private var sessionStreak: Int = 0
    private var currentTopic: String? = null
    private var aiNodeTitle: String? = null
    private var aiNodeDescription: String? = null
    private var aiNodeId: String? = null
    private var aiNodeType: String? = null
    private var testFinished = false

    private val jsonContent = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    init {
        restoreSession()
    }

    /**
     * Restores a previously generated session (topic/node routing plus questions and
     * progress) from [savedStateHandle], if one is there. Populating [currentTopic] /
     * [aiNodeTitle] here is what makes the guard at the top of [setTopic] and
     * [setAiNodeParams] recognize the restored state and skip calling Gemini again when
     * `AppNavigation`'s `LaunchedEffect(route)` replays the same route after a process
     * restart.
     */
    private fun restoreSession() {
        val json = savedStateHandle.get<String>(KEY_SESSION) ?: return
        val session = runCatching { jsonContent.decodeFromString<SavedTestSession>(json) }
            .getOrNull() ?: return
        if (session.questions.isEmpty()) return

        currentTopic = session.currentTopic
        aiNodeTitle = session.aiNodeTitle
        aiNodeDescription = session.aiNodeDescription
        aiNodeId = session.aiNodeId
        aiNodeType = session.aiNodeType
        sessionStreak = session.sessionStreak
        startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                category = session.category,
                questions = session.questions,
                currentQuestionIndex = session.currentQuestionIndex,
                selectedAnswers = session.selectedAnswers,
                isAnswerChecked = session.isAnswerChecked,
                sessionStreak = session.sessionStreak,
                isLoading = false,
                error = null
            )
        }
    }

    /** Snapshots the restorable parts of [TestUiState] plus the routing fields into [savedStateHandle]. */
    private fun persistSession() {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        val session = SavedTestSession(
            category = state.category,
            questions = state.questions,
            currentQuestionIndex = state.currentQuestionIndex,
            selectedAnswers = state.selectedAnswers,
            isAnswerChecked = state.isAnswerChecked,
            sessionStreak = state.sessionStreak,
            currentTopic = currentTopic,
            aiNodeTitle = aiNodeTitle,
            aiNodeDescription = aiNodeDescription,
            aiNodeId = aiNodeId,
            aiNodeType = aiNodeType
        )
        savedStateHandle[KEY_SESSION] = jsonContent.encodeToString(session)
    }

    /**
     * Tells apart a screen's first-ever generation from one triggered by a ViewModel
     * recreated mid-session with nothing left to restore — i.e. Android killed the
     * process while a test was in flight or on screen.
     */
    private fun determineReason(): String =
        if (savedStateHandle.get<Boolean>(KEY_GENERATION_STARTED) == true) "process_restart" else "initial"

    fun setTopic(topic: String?) {
        if (currentTopic == topic && _uiState.value.questions.isNotEmpty()) return
        currentTopic = topic
        aiNodeTitle = null
        aiNodeDescription = null
        aiNodeId = null
        aiNodeType = null
        generateGeminiTest()
    }

    fun setAiNodeParams(title: String, desc: String?, id: String?, nodeType: String?) {
        if (aiNodeTitle == title && _uiState.value.questions.isNotEmpty()) return
        aiNodeTitle = title
        aiNodeDescription = desc
        aiNodeId = id
        aiNodeType = nodeType
        currentTopic = null

        // Route based on node type
        when (nodeType) {
            "LESSON" -> loadStaticQuestions(id)
            "REVIEW", "EXAM" -> generateGeminiTest()
            else -> generateGeminiTest()
        }
    }

    // Loads questions from the static bank without calling Gemini
    private fun loadStaticQuestions(nodeId: String?) {
        if (nodeId == null) {
            generateGeminiTest()
            return
        }

        val staticQuestions = LessonQuestionBank.getQuestionsForNode(nodeId)

        if (staticQuestions.isEmpty()) {
            // No static questions for this node → fall back to Gemini
            generateGeminiTest()
            return
        }

        val questionUiStates = staticQuestions.map { q ->
            QuestionUiState(
                text = q.text,
                options = q.options,
                correctAnswerIndex = q.correctAnswerIndex,
                explanation = q.explanation,
                imageUrl = q.imageUrl
            )
        }

        startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                category = aiNodeTitle ?: "Lección",
                questions = questionUiStates,
                isLoading = false,
                error = null
            )
        }
        persistSession()
    }

    fun onEvent(event: TestEvent) {
        when (event) {
            TestEvent.Retry -> retry()
            is TestEvent.SelectOption -> selectOption(event.optionIndex)
            TestEvent.CheckAnswer -> checkAnswer()
            TestEvent.NextQuestion -> nextQuestion()
            is TestEvent.FinishTest -> {
                viewModelScope.launch {
                    val result = calculateResult()
                    testFinished = true
                    event.onResult(result)
                }
            }
        }
    }

    private fun retry() {
        sessionStreak = 0
        testFinished = false
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                questions = emptyList(),
                currentQuestionIndex = 0,
                selectedAnswers = emptyMap(),
                isAnswerChecked = false,
                sessionStreak = 0
            )
        }
        // Respect node type on retry
        when (aiNodeType) {
            "LESSON" -> loadStaticQuestions(aiNodeId)
            else -> generateGeminiTest(reason = "retry")
        }
    }

    /**
     * Calls Gemini for a fresh 10-question set.
     *
     * @param reason why this call is happening — `"initial"`/`"process_restart"` (from
     *   [determineReason]) or `"retry"` — logged alongside the real token cost once the
     *   response comes back, so cost spikes can be traced to the reason that caused them.
     */
    private fun generateGeminiTest(reason: String = determineReason()) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val user = userRepository.get().first()
                val lastTests = testResultRepository.getRecent().first()
                val totalTests = testResultRepository.count()
                val license = user?.licenseType?.takeIf { it.isNotBlank() } ?: "B (Coche)"
                val difficultTopics =
                    user?.difficultTopics?.takeIf { it.isNotBlank() } ?: "Ninguno específico"
                val studentLevel = user?.level ?: 1
                val experience = user?.experience?.takeIf { it.isNotBlank() } ?: "Desconocida"
                val historyContext = if (lastTests.isNotEmpty()) {
                    "Resultados recientes: " + lastTests.joinToString(", ") { "${it.category}: ${it.score}/${it.total}" }
                } else {
                    "Sin tests previos."
                }

                // Context of failures for REVIEW and EXAM
                val sectionFailuresContext = if (aiNodeType == "REVIEW" || aiNodeType == "EXAM") {
                    buildSectionFailuresContext()
                } else ""

                val topicInstruction = when {
                    aiNodeTitle != null && (aiNodeType == "REVIEW" || aiNodeType == "EXAM") -> """
                        Tu misión es generar un ${if (aiNodeType == "EXAM") "EXAMEN DE SECCIÓN" else "REPASO"} de 10 preguntas sobre: $aiNodeTitle.
                        $sectionFailuresContext
                        INSTRUCCIÓN CLAVE: Basa el 60% de las preguntas en los conceptos donde el usuario ha fallado más.
                        El 40% restante cubre el resto de la sección para una revisión completa.
                    """.trimIndent()
                    aiNodeTitle != null -> "Tu misión es generar una SESIÓN DE PRÁCTICA de 10 preguntas EXCLUSIVAMENTE enfocada en: Titulo: $aiNodeTitle. Descripción: ${aiNodeDescription ?: ""}. Adapta la dificultad al nivel del alumno."
                    currentTopic != null -> "Tu misión es generar una SESIÓN DE PRÁCTICA de 10 preguntas EXCLUSIVAMENTE sobre el tema: $currentTopic. Adapta la dificultad al nivel del alumno."
                    else -> "Tu misión es generar una SESIÓN DE PRÁCTICA de 10 preguntas. Analiza su historial: $historyContext. ELIGE UNA categoría de esta lista (Prioriza las que NO se han practicado recientemente o cruza con los temas que más le cuestan: $difficultTopics): Alumbrado, Prioridad, Maniobras, Velocidad, El conductor, Mecánica, Documentación, Usuarios de la vía, Señales, Marcas viales."
                }

                val prompt = """
                    Eres un Profesor Experto de la DGT (Dirección General de Tráfico) en España.
                    $topicInstruction
                    
                    CONTEXTO DEL ALUMNO (PERSONALIZACIÓN):
                    - Permiso al que aspira: Permiso $license.
                    - Nivel actual en la app: $studentLevel (A mayor nivel, usa distractores más complejos y sutiles).
                    - Total de tests realizados: $totalTests (Si son pocos, haz explicaciones más didácticas paso a paso).
                    - Experiencia previa: $experience.
                    
                    REGLAS DE LA PREGUNTA:
                    - Estilo DGT oficial: Preguntas directas, a veces con situaciones hipotéticas.
                    - 3 opciones por pregunta con el TEXTO REAL de la respuesta. Solo una es correcta.
                    - Las respuestas incorrectas deben ser creíbles.
                    - EXPLICACIÓN: Máximo 20 palabras. Debe ser clara y lógica. Intenta darle un toque pedagógico adaptado a su nivel.
                    
                    REGLAS DE CALIDAD Y ACTUALIZACIÓN (¡ESTRICTAMENTE OBLIGATORIO!):
                    - NORMATIVA VIGENTE: Usa SIEMPRE la ley de tráfico española más reciente. Ejemplos obligatorios: uso de la baliza luminosa V-16 (los triángulos ya no son obligatorios en autopista/autovía), límites de velocidad a 30 km/h en vías urbanas de un único carril, nuevas normativas de VMP (patinetes eléctricos) y las señales de tráfico de nueva creación. Cero información obsoleta.
                    - TRAMPAS TÍPICAS DGT: Haz que las respuestas incorrectas sean muy atractivas usando el lenguaje de la DGT. Juega con matices como "siempre", "nunca", "sólo", o "como norma general" para poner a prueba la atención del alumno.
                    
                    REGLAS DE IMÁGENES (SISTEMA FILEPATH):
                    - Usa imágenes SOLO si la pregunta describe una situación visual o una señal física.
                    - Formato obligatorio: https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal[codigo].svg
                    - Codigos ejemplo: r1 (ceda), r2 (stop), p1 (peligro), r301 (velocidad 40), s1 (autopista).
                    - Si la pregunta es puramente teórica (ej: tasa de alcohol), usa null.
                    
                    Genera EXACTAMENTE 10 preguntas e indica en "selectedCategory" la categoría elegida.
                    """.trimIndent()

                savedStateHandle[KEY_GENERATION_STARTED] = true
                val response = gemini.generateContent(prompt)

                // Logged as soon as the response is back — tokens are billed the moment
                // Gemini answers, whether or not the JSON below turns out parseable.
                val usage = response.usageMetadata
                analytics.testGenerated(
                    reason = reason,
                    inputTokens = usage?.promptTokenCount ?: 0,
                    outputTokens = usage?.candidatesTokenCount ?: 0
                )

                val rawText = response.text ?: throw Exception("Sin respuesta")

                val jsonStartIndex = rawText.indexOf('{')
                val jsonEndIndex = rawText.lastIndexOf('}')
                if (jsonStartIndex == -1 || jsonEndIndex == -1) throw Exception("Formato inválido")

                val jsonString = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
                val root = jsonContent.parseToJsonElement(jsonString).jsonObject

                val category = root["selectedCategory"]?.jsonPrimitive?.content ?: currentTopic
                ?: "Práctica General"
                
                val questionUiStates = GeminiQuestionParser.parse(rawText)

                startTime = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        category = category,
                        questions = questionUiStates,
                        isLoading = false
                    )
                }
                persistSession()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    // Builds context of recent failures for REVIEW/EXAM prompts
    private suspend fun buildSectionFailuresContext(): String {
        return try {
            val allAnswers = answerRepository.getAll().first()
            val mistakes = allAnswers.filter { !it.isCorrect }.takeLast(50)
            if (mistakes.isEmpty()) return ""

            val frequentMistakes = mistakes
                .groupBy { it.questionText }
                .entries
                .sortedByDescending { it.value.size }
                .take(5)
                .joinToString("\n") { (questionText, answers) ->
                    "- Falló ${answers.size} veces en: '${questionText.take(80)}'"
                }

            """
            CONTEXTO DE FALLOS RECIENTES DEL USUARIO:
            $frequentMistakes
            """.trimIndent()
        } catch (e: Exception) {
            ""
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

    private fun nextQuestion() {
        val currentState = _uiState.value
        if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = it.currentQuestionIndex + 1,
                    isAnswerChecked = false
                )
            }
            persistSession()
        }
    }

    private suspend fun calculateResult(): TestSummary {
        val state = _uiState.value
        val correct = state.questions.indices.count { index ->
            state.selectedAnswers[index] == state.questions[index].correctAnswerIndex
        }
        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val accuracy =
            if (state.questions.isNotEmpty()) ((correct.toFloat() / state.questions.size) * 100).toInt() else 0
        var newWeekSessions = -1

        val userId = userRepository.get().first()?.id ?: ""
        // Read the node's status BEFORE this attempt overwrites it below — repeating an
        // already-COMPLETED node earns reduced XP (see IncrementXpUseCase's isRepeat handling).
        val isRepeat = aiNodeId?.let { nodeId ->
            pathRepository.getPathNodes(userId).first().find { it.id == nodeId }?.status == NodeStatus.COMPLETED
        } ?: false

        val xpEarned = incrementXpUseCase(
            mode = TestMode.PRACTICE,
            correctAnswers = correct,
            totalQuestions = state.questions.size,
            durationSeconds = durationSeconds,
            isRepeat = isRepeat
        )

        val coinsGained = incrementCoinsUseCase(accuracy)

        withContext(Dispatchers.IO) {
            // 1. Save test result
            val testId = testResultRepository.insert(
                TestResult(
                    category = state.category,
                    score = correct,
                    total = state.questions.size,
                    date = Date(),
                    isPassed = accuracy >= 90
                )
            )

            // 2. Save individual answers
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

            // 3. Increment streak
            newWeekSessions = incrementStreakUseCase()

            // 4. Update path if coming from a path node: always record the attempt on the
            // current node, but only unlock the next one once the score clears the bar.
            if (aiNodeId != null) {

                // 4a. Mark current node as COMPLETED (with its real score) and WAIT for confirmation
                pathRepository.updateNodeStatus(
                    userId,
                    aiNodeId!!,
                    NodeStatus.COMPLETED.name,
                    accuracy
                )

                // 4b. Read updated state (update already finished)
                val allNodes = pathRepository.getPathNodes(userId).first()
                val currentNode = allNodes.find { it.id == aiNodeId }

                if (currentNode != null && accuracy >= PASSING_ACCURACY) {
                    // Find the next LOCKED node with immediately higher orderIndex
                    val nextLockedNode = allNodes
                        .filter { it.orderIndex > currentNode.orderIndex }
                        .minByOrNull { it.orderIndex }

                    if (nextLockedNode != null && nextLockedNode.status == NodeStatus.LOCKED) {
                        // 4c. Unlock the next node
                        pathRepository.updateNodeStatus(
                            userId,
                            nextLockedNode.id,
                            NodeStatus.UNLOCKED.name,
                            null
                        )
                    }
                }
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
            coinsGained = coinsGained,
            newWeekSessions = newWeekSessions
        )
    }

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        private const val KEY_SESSION = "test_saved_session"
        private const val KEY_GENERATION_STARTED = "test_generation_started"

        /** Minimum accuracy percentage required to unlock the next node on the learning path. */
        private const val PASSING_ACCURACY = 70
    }

    @Serializable
    private data class SavedTestSession(
        val category: String,
        val questions: List<QuestionUiState>,
        val currentQuestionIndex: Int,
        val selectedAnswers: Map<Int, Int>,
        val isAnswerChecked: Boolean,
        val sessionStreak: Int,
        val currentTopic: String?,
        val aiNodeTitle: String?,
        val aiNodeDescription: String?,
        val aiNodeId: String?,
        val aiNodeType: String?
    )
}
