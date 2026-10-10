package com.jesuskrastev.bali.ui.screens.exam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.GenerativeModel
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.di.QuestionsModel
import com.jesuskrastev.bali.domain.audio.SoundEffects
import com.jesuskrastev.bali.domain.exam.ExamScope
import com.jesuskrastev.bali.domain.exam.ImagePrefetcher
import com.jesuskrastev.bali.domain.exam.PendingMistakes
import com.jesuskrastev.bali.domain.exam.ScopedExamComposer
import com.jesuskrastev.bali.domain.exam.ScopedExamPrompt
import com.jesuskrastev.bali.domain.exam.unloadableAmong
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.PathRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.AnswerMode
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.ResultMilestones
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.CompletePathNodeUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.GeminiQuestionParser
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import com.jesuskrastev.bali.ui.screens.test.TestSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val pathRepository: PathRepository,
    @QuestionsModel private val gemini: GenerativeModel,
    private val imagePrefetcher: ImagePrefetcher,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val completePathNodeUseCase: CompletePathNodeUseCase,
    private val analytics: AnalyticsTracker,
    private val soundEffects: SoundEffects,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    /** The path node this exam belongs to, from the route's `nodeId` argument. */
    private val examNodeId: String? = savedStateHandle.get<String>(KEY_NODE_ID)

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
            is ExamEvent.FinishExam -> finishExam(event.onResult)

            ExamEvent.ToggleQuestionReview -> toggleReviewGrid()
            ExamEvent.CheckAnswer -> checkAnswer()
        }
    }

    /**
     * Reveals whether the selected option is right: updates the session streak, marks the answer
     * as checked and plays the matching sound. Does nothing when no option is selected or the
     * answer was already checked, so a double tap neither double-counts nor plays twice.
     */
    private fun checkAnswer() {
        val currentState = _uiState.value
        if (currentState.isAnswerChecked) return
        val selectedOption = currentState.selectedAnswers[currentState.currentQuestionIndex]
        if (selectedOption != null) {
            val isCorrect =
                selectedOption == currentState.questions[currentState.currentQuestionIndex].correctAnswerIndex
            if (isCorrect) sessionStreak++ else sessionStreak = 0
            if (isCorrect) soundEffects.playCorrect() else soundEffects.playWrong()
            _uiState.update { it.copy(isAnswerChecked = true, sessionStreak = sessionStreak) }
            persistSession()
        }
    }

    /**
     * Scores the exam and hands the summary to [onResult]. A second call before [retry] is
     * ignored, so a double tap on the last button cannot pay the rewards twice.
     *
     * @param onResult receives the summary once everything is saved
     */
    private fun finishExam(onResult: (TestSummary) -> Unit) {
        if (examFinished) return
        examFinished = true
        viewModelScope.launch {
            val summary = try {
                calculateResult()
            } catch (error: Exception) {
                examFinished = false
                throw error
            }
            onResult(summary)
        }
    }

    /** Throws the current exam away and generates a fresh one. */
    private fun retry() {
        timerJob?.cancel()
        sessionStreak = 0
        examFinished = false
        _uiState.update { ExamUiState() }
        generateExam(reason = "retry")
    }

    /**
     * Builds the exam from the lessons this exam's path node covers, see [ExamScope].
     *
     * Gemini rewrites part of those lessons' own questions while their pictures are checked at the
     * same time; the result is put together by [ScopedExamComposer]. If Gemini fails the exam is
     * made of bank questions alone, so a bad connection to the model no longer costs the student
     * the exam. Only a student who has studied nothing in scope, or whose pictures cannot be
     * loaded and leave the exam short, gets an error.
     *
     * @param reason why this build is happening — `"initial"`/`"process_restart"` (from
     *   [determineReason]) or `"retry"` — logged alongside the real token cost of the Gemini call.
     */
    private fun generateExam(reason: String = determineReason()) {
        viewModelScope.launch {
            try {
                val nodeId = examNodeId ?: error("Este examen no pertenece a ninguna unidad.")
                val user = userRepository.get().first()
                val path = pathRepository.getPathNodes(user?.id.orEmpty()).first()
                val examNode = path.find { it.id == nodeId } ?: error("No se encontró la unidad de este examen.")

                val lessons = ExamScope.lessonsFor(examNode, path)
                check(lessons.isNotEmpty()) { "Completa alguna lección de esta unidad para poder examinarte." }

                val failedIds = PendingMistakes.idsOf(answerRepository.getAll().first())
                val plan = ScopedExamComposer.plan(lessons, failedIds)

                savedStateHandle[KEY_GENERATION_STARTED] = true
                val prompt = ScopedExamPrompt.build(
                    scopeTitle = examNode.title,
                    sources = plan.sources,
                    student = studentOf(user),
                    questionCount = ScopedExamComposer.GENERATED_REQUESTED
                )
                // The model and the picture checks do not depend on each other, so they overlap.
                val (variants, brokenUrls) = coroutineScope {
                    val variants = async { requestVariants(prompt, reason) }
                    val broken = async { imagePrefetcher.unloadableAmong(plan.verbatim.mapNotNull { it.imageUrl }) }
                    variants.await() to broken.await()
                }

                val assembly = ScopedExamComposer.assemble(plan, variants ?: emptyList())
                val questions = ScopedExamComposer.replaceBrokenImages(
                    assembly.questions, assembly.reserve, brokenUrls
                )
                analytics.examAssembled(
                    nodeId = nodeId,
                    variantsAccepted = assembly.variantsAccepted,
                    bankFill = assembly.bankFill,
                    imagesReplaced = assembly.questions.count { it.imageUrl in brokenUrls },
                    modelFailed = variants == null
                )
                check(questions.size == ExamRules.QUESTION_COUNT) {
                    "No se pudo preparar el examen. Revisa tu conexión e inténtalo de nuevo."
                }

                startTime = System.currentTimeMillis()
                examEndAtMillis = System.currentTimeMillis() + EXAM_DURATION_SECONDS * 1000L
                _uiState.update {
                    it.copy(
                        questions = questions,
                        isLoading = false,
                        timeLeftSeconds = EXAM_DURATION_SECONDS
                    )
                }
                startTimer()
                persistSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    /**
     * Asks Gemini for new questions from the study material in [prompt].
     *
     * @param prompt what [ScopedExamPrompt.build] wrote
     * @param reason why the call happens, logged with the tokens it cost
     * @return the questions Gemini wrote, or null when the call failed or came back unreadable
     */
    private suspend fun requestVariants(prompt: String, reason: String): List<QuestionUiState>? {
        return try {
            val response = gemini.generateContent(prompt)

            // Logged as soon as the response is back — tokens are billed the moment
            // Gemini answers, whether or not the JSON below turns out parseable.
            val usage = response.usageMetadata
            analytics.examGenerated(
                reason = reason,
                inputTokens = usage?.promptTokenCount ?: 0,
                outputTokens = usage?.candidatesTokenCount ?: 0
            )

            val rawText = response.text ?: return null
            GeminiQuestionParser.parse(rawText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gathers what the prompt tells Gemini about the student.
     *
     * @param user the signed-in user, or null before the profile has loaded
     * @return the student's permit, level, experience, weak topics and days to the exam
     */
    private suspend fun studentOf(user: User?): ScopedExamPrompt.Student =
        ScopedExamPrompt.Student(
            license = user?.licenseType?.takeIf { it.isNotBlank() } ?: "B (Coche)",
            level = user?.level ?: 1,
            experience = user?.experience?.takeIf { it.isNotBlank() } ?: "Desconocida",
            difficultTopics = user?.difficultTopics?.takeIf { it.isNotBlank() } ?: "Ninguno específico",
            totalTests = testResultRepository.count().first(),
            daysToExam = user?.examDateMillis?.let {
                ((it - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            }
        )

    /**
     * Counts the clock down to [examEndAtMillis] once a second and flags [ExamUiState.isTimeUp]
     * when it runs out. The reading comes from the wall clock rather than from counting ticks,
     * so a delayed tick (the phone dozing, the app in the background) cannot hand out extra time.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val secondsLeft = secondsUntilDeadline()
                _uiState.update { it.copy(timeLeftSeconds = secondsLeft) }
                if (secondsLeft == 0) break
                delay(1000)
            }
            _uiState.update { it.copy(isTimeUp = true) }
        }
    }

    /**
     * Whole seconds left until [examEndAtMillis], rounded up so the clock shows 00:00 only when
     * the time is really over.
     *
     * @return the seconds left, never negative
     */
    private fun secondsUntilDeadline(): Int =
        ((examEndAtMillis - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)

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
     * Scores the finished exam: pays XP and coins, saves the result and every answer, advances the
     * streak and, when the exam belongs to a path node, completes it (unlocking the next on a pass).
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
        val isPassed = ExamRules.isPassed(correct)
        var newStreakDays = -1

        val userId = userRepository.get().first()?.id.orEmpty()
        // Read the node's status BEFORE this attempt overwrites it below: repeating an exam node
        // that is already COMPLETED earns reduced XP, same as repeating a lesson.
        val isRepeat = examNodeId?.let { nodeId ->
            pathRepository.getPathNodes(userId).first().find { it.id == nodeId }?.status == NodeStatus.COMPLETED
        } ?: false
        val previousResults = runCatching { testResultRepository.get().first() }.getOrNull()
        // Only judged when the earlier results could be read, so a failed read never invents a record.
        val previousBest = previousResults?.let { ResultMilestones.bestExamScore(it) }
        val isNewRecord = ResultMilestones.isNewExamRecord(correct, previousBest)
        val isFirstWin = previousResults != null && ResultMilestones.isFirstWin(previousResults, isPassed)

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
                    category = ExamRules.OFFICIAL_EXAM_CATEGORY,
                    score = correct,
                    total = state.questions.size,
                    date = Date(),
                    isPassed = isPassed
                )
            )

            state.questions.forEachIndexed { index, question ->
                val selectedOption = state.selectedAnswers[index]
                if (selectedOption != null) {
                    // The official exam mixes every topic and its questions carry none, so the
                    // answer has no topic; the question id and the mode are still saved.
                    answerRepository.insert(
                        Answer(
                            date = Date(),
                            testId = testId,
                            questionText = question.text,
                            selectedOption = selectedOption,
                            isCorrect = selectedOption == question.correctAnswerIndex,
                            questionId = QuestionId.of(question.text),
                            topic = null,
                            mode = AnswerMode.OFFICIAL_EXAM
                        )
                    )
                }
            }

            newStreakDays = incrementStreakUseCase()
            examNodeId?.let { completePathNodeUseCase(userId, it, accuracy) }
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
            newLevel = xpEarned.newLevel,
            newTotalXp = xpEarned.newTotalXp,
            coinsGained = coinsGained,
            newStreakDays = newStreakDays,
            isFailedExam = !isPassed,
            isPassedExam = isPassed,
            isFirstWin = isFirstWin,
            isNewRecord = isNewRecord,
            previousBestScore = previousBest ?: -1
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        /** The exam's fixed time limit, matching [ExamUiState]'s default `timeLeftSeconds`. */
        private const val EXAM_DURATION_SECONDS = 1800

        /** Name of the route argument carrying the exam's path node, see `ExamRoute`. */
        private const val KEY_NODE_ID = "nodeId"

        private const val KEY_SESSION = "exam_saved_session"
        private const val KEY_GENERATION_STARTED = "exam_generation_started"
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
