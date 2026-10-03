package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.audio.SoundEffects
import com.jesuskrastev.bali.domain.model.StudySchedule
import com.jesuskrastev.bali.domain.model.StudySlot
import com.jesuskrastev.bali.domain.repository.NotificationsRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.ui.screens.stats.calendarDaysBetween
import com.jesuskrastev.bali.ui.screens.stats.localDayFromPickerMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * How the user answered the offer of study reminders.
 *
 * @property analyticsValue the value reported as the `result` of the analytics event
 */
enum class NotificationsAnswer(val analyticsValue: String) {
    /** Said yes and Android allowed notifications. */
    GRANTED("granted"),

    /** Said yes, then refused the system dialog. */
    DENIED("denied"),

    /** Tapped "Ahora no"; the system dialog was never shown. */
    DECLINED("declined")
}

/**
 * Everything the user tells us during onboarding.
 *
 * Only [name], [experience] and [examDate] outlive the flow in the profile, and [studyTime] on
 * the device for the reminders; the rest exists to personalise the copy of the later screens and
 * to make the generated plan read as the user's own.
 *
 * @property examDate the exam day picked (local midnight), or null while unanswered or when the
 *   user has no date yet
 * @property studyTime the option picked on the study time screen, a key of
 *   [OnboardingConfig.studyTimes]
 * @property notifications the answer to the reminders offer, null until given
 * @property learningPreference the [LearningStyle.key] picked
 * @property quizAnswers the answers given in the mini-test, in order; empty if it was skipped
 */
data class OnboardingData(
    val name: String? = null,
    val experience: String? = null,
    val theoryBlocker: String? = null,
    val concern: String? = null,
    val readiness: String? = null,
    val motivation: String? = null,
    val examDate: Long? = null,
    val province: String? = null,
    val weeklyStudy: String? = null,
    val studyTime: String? = null,
    val notifications: NotificationsAnswer? = null,
    val learningPreference: String? = null,
    val quizAnswers: List<QuizAnswer> = emptyList(),
) {
    /**
     * The part of the day [studyTime] books the reminder in.
     *
     * @return the slot, or null while the study time is unanswered
     */
    fun studySlot(): StudySlot? = studyTime?.let { OnboardingConfig.studyTimes[it] }

    /** The mini-test questions for the worry the user picked. */
    fun quizQuestions(): List<QuizQuestion> = OnboardingQuiz.questionsFor(concern)

    /** The mini-test questions answered wrong, to flag their topics in the plan. */
    fun failedQuizQuestions(): List<QuizQuestion> = OnboardingQuiz.failedQuestions(concern, quizAnswers)

    /** How many mini-test questions were answered right. */
    fun quizScore(): Int = quizAnswers.count { it.isCorrect }
}

/**
 * A single screen of the onboarding flow.
 *
 * The flow is deliberately built as an emotional arc rather than a questionnaire:
 * what Bali is → the user's "why" and what it costs → diagnosis → a three-question test of their
 * own worry → the road from that score to the exam → the life the licence unlocks → the concrete
 * plan → close.
 *
 * @param analyticsName slug of the step in analytics; empty for terminal states that are
 *   not part of the funnel. The position prefix is added when the event is logged, so
 *   reordering the flow renumbers the funnel on its own.
 */
sealed class OnboardingStep(val analyticsName: String = "") {

    /**
     * Steps that only present content. They have nothing to validate, so the bottom
     * button is always shown and always enabled.
     */
    sealed class Informational(analyticsName: String) : OnboardingStep(analyticsName)

    // ── Qué es Bali ────────────────────────────────────────────────────────
    // The four cards. It owns its controls (the cards swipe and the button walks
    // through them), so it is not informational.
    data object Intro : OnboardingStep("intro")

    // ── El porqué ──────────────────────────────────────────────────────────
    data object Motivation : OnboardingStep("motivation")
    data object TheoryBlocker : OnboardingStep("reasons")

    // ── Diagnóstico ────────────────────────────────────────────────────────
    data object Concern : OnboardingStep("concern")
    data object Experience : OnboardingStep("experience")
    data object Readiness : OnboardingStep("readiness")

    // ── La prueba ──────────────────────────────────────────────────────────
    // The first taste of the product: three questions picked by the worry just named. The step
    // owns its controls (options, explanation, "Siguiente"), so it is not informational.
    data object Quiz : OnboardingStep("quiz")
    data object QuizResult : Informational("quiz_result")

    // ── El arco: what blocks the user and what it costs, the way out, and the life after ──
    data object Pain : Informational("pain")
    data object MethodComparison : Informational("method_comparison")
    data object Gain : Informational("gain")

    // ── El plan ────────────────────────────────────────────────────────────
    // The name opens this block: the whole emotional arc is answered with taps, and the
    // keyboard only shows up once the user is invested and the plan is about to be built.
    data object Name : OnboardingStep("name")
    data object ExamDate : OnboardingStep("exam_date")
    data object Province : OnboardingStep("province")
    data object WeeklyStudy : OnboardingStep("weekly_study")
    // Right after the rhythm, so the reminder is the natural follow-up to what the user just
    // committed to, and five screens before the paywall so the system dialog never sits
    // next to the purchase.
    data object StudyTime : OnboardingStep("study_time")
    data object Notifications : OnboardingStep("notifications")
    data object LearningPreference : OnboardingStep("learning_preference")

    // ── Cierre ─────────────────────────────────────────────────────────────
    data object Processing : OnboardingStep("processing")
    data object Comparison : Informational("comparison")
    data object PlanReveal : Informational("plan_reveal")
    data object SocialProof : Informational("social_proof")
    data object Pact : OnboardingStep("pact")

    data object PaywallPending : OnboardingStep()
    data object Completed : OnboardingStep()
}

/**
 * Everything the onboarding screen draws.
 *
 * @property canGoBack whether the back arrow and the system back gesture can step back a screen
 * @property isMovingBack whether the last move went backwards, so the screens slide the other way
 * @property quizIndex the mini-test question on screen, counted from 0
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Intro,
    val data: OnboardingData = OnboardingData(),
    val progress: Float = 0f,
    val processingProgress: Float = 0f,
    val mascotMessage: String = "",
    val canGoBack: Boolean = false,
    val canGoNext: Boolean = false,
    val isProcessingFinished: Boolean = false,
    val isRequestingNotifications: Boolean = false,
    val isMovingBack: Boolean = false,
    val quizIndex: Int = 0
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val notificationsRepository: NotificationsRepository,
    private val soundEffects: SoundEffects
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val reducer = OnboardingReducer()

    private val stepsOrder = listOf(
        // Qué es Bali, before any question
        OnboardingStep.Intro,
        // El porqué, and what it costs: the pain answers the blocker just named
        OnboardingStep.Motivation, OnboardingStep.TheoryBlocker, OnboardingStep.Pain,
        // Diagnóstico: the experience comes first because the real failure rate and the test
        // result both speak to it.
        OnboardingStep.Experience, OnboardingStep.Comparison, OnboardingStep.Readiness,
        OnboardingStep.Concern,
        // La prueba, then the road from its score to the exam, and the life at the end of it
        OnboardingStep.Quiz, OnboardingStep.QuizResult,
        OnboardingStep.MethodComparison, OnboardingStep.Gain,
        // El plan
        OnboardingStep.Name, OnboardingStep.ExamDate, OnboardingStep.Province,
        OnboardingStep.WeeklyStudy, OnboardingStep.StudyTime, OnboardingStep.Notifications,
        OnboardingStep.LearningPreference,
        // Cierre
        OnboardingStep.SocialProof, OnboardingStep.Processing, OnboardingStep.PlanReveal,
        OnboardingStep.Pact
    )

    /**
     * Steps already reported to analytics in this run, so that going back and forward again
     * counts each screen once, as the funnel expects.
     */
    private val reportedSteps = mutableSetOf<OnboardingStep>()

    /** When the current mini-test question appeared, to report how long the answer took. */
    private var quizQuestionShownAt = 0L

    init {
        updateMascotMessage()
        analyticsTracker.onboardingStarted()
        trackStepReached(stepsOrder.first())
    }

    /**
     * Reports that a screen was shown, naming the event after its position in the flow.
     *
     * The number is derived from [stepsOrder] rather than stored on the step, so it can
     * never drift out of sync with the real order. The `o` prefix is not decoration:
     * Firebase silently drops events whose name starts with a digit.
     *
     * A screen is reported only the first time it is reached: coming back to it with the back
     * arrow and moving on again must not count it twice.
     *
     * @param step the step that just became visible
     */
    private fun trackStepReached(step: OnboardingStep) {
        val position = stepsOrder.indexOf(step)
        if (position < 0 || step.analyticsName.isBlank()) return
        if (!reportedSteps.add(step)) return
        analyticsTracker.onboardingStepReached(funnelEventName(position, step.analyticsName))
    }

    /**
     * Builds the ordered event name for a step.
     *
     * @param position zero-based index of the step in [stepsOrder]
     * @param slug the step's semantic name
     * @return a name such as `o07_future_impact`
     */
    private fun funnelEventName(position: Int, slug: String): String =
        "o%02d_%s".format(position + 1, slug)

    /**
     * Entry point for every user interaction in the onboarding flow.
     *
     * @param event the action performed by the user
     */
    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.SetName -> updateData { it.copy(name = event.name) }
            is OnboardingEvent.SelectExperience -> selectStepItem { it.copy(experience = event.experience) }
            is OnboardingEvent.SelectTheoryBlocker -> selectStepItem { it.copy(theoryBlocker = event.blocker) }
            is OnboardingEvent.SelectConcern -> selectConcern(event.concern)
            is OnboardingEvent.SelectReadiness -> selectStepItem { it.copy(readiness = event.readiness) }
            is OnboardingEvent.SelectMotivation -> selectStepItem { it.copy(motivation = event.motivation) }
            is OnboardingEvent.SetExamDate -> setExamDate(event.pickerMillis)
            // The province needs a confirmation tap, so it does not advance on its own.
            is OnboardingEvent.SelectProvince -> updateData { it.copy(province = event.province) }
            is OnboardingEvent.SelectWeeklyStudy -> selectStepItem { it.copy(weeklyStudy = event.weeklyStudy) }
            is OnboardingEvent.SelectStudyTime -> selectStudyTime(event.studyTime)
            is OnboardingEvent.AnswerNotifications -> answerNotifications(event.accepted)
            is OnboardingEvent.SelectLearningPreference -> selectStepItem { it.copy(learningPreference = event.key) }
            is OnboardingEvent.IntroCardShown -> analyticsTracker.onboardingIntroCardShown(event.position + 1)
            is OnboardingEvent.AnswerQuiz -> answerQuiz(event.optionIndex)
            OnboardingEvent.NextQuizQuestion -> nextQuizQuestion()
            OnboardingEvent.SkipQuiz -> skipQuiz()
            OnboardingEvent.GoToNextStep -> goToNextStep()
            OnboardingEvent.GoToPreviousStep -> goToPreviousStep()
            OnboardingEvent.CompleteOnboarding -> saveDataAndComplete()
        }
    }

    /**
     * Applies a single-choice answer and immediately advances, so the user never has
     * to tap twice on a selection screen.
     *
     * @param update transformation applied to the collected onboarding data
     */
    private fun selectStepItem(update: (OnboardingData) -> OnboardingData) {
        updateData(update)
        goToNextStep()
    }

    /**
     * Keeps the worry and moves on to the mini-test it picks. A different worry than before means
     * different questions, so a test started for the old one starts over.
     *
     * @param concern one of [OnboardingConfig.concerns]
     */
    private fun selectConcern(concern: String) {
        if (concern != _uiState.value.data.concern) {
            _uiState.update { it.copy(quizIndex = 0, data = it.data.copy(quizAnswers = emptyList())) }
        }
        selectStepItem { it.copy(concern = concern) }
    }

    /**
     * Keeps the exam date and moves on. A picked date is also reported as the moment the student
     * set their exam, the same event Home and Statistics send.
     *
     * @param pickerMillis the date picker's selection (midnight UTC of the day), or null for
     *   "Aún no tengo fecha"
     */
    private fun setExamDate(pickerMillis: Long?) {
        val examDay = pickerMillis?.let(::localDayFromPickerMillis)
        if (examDay != null) {
            analyticsTracker.examDateSet(
                daysUntil = calendarDaysBetween(System.currentTimeMillis(), examDay),
                hadPlanDate = false,
                source = "onboarding"
            )
        }
        selectStepItem { it.copy(examDate = examDay) }
    }

    /**
     * Marks the current mini-test question with the option tapped: plays the right or wrong sound
     * and reveals the explanation. A question is answered once; later taps are ignored.
     *
     * @param optionIndex the option tapped
     */
    private fun answerQuiz(optionIndex: Int) {
        val state = _uiState.value
        if (state.currentStep != OnboardingStep.Quiz) return
        val index = state.quizIndex
        if (state.data.quizAnswers.size > index) return
        val question = state.data.quizQuestions().getOrNull(index) ?: return

        val isCorrect = optionIndex == question.correctIndex
        if (isCorrect) soundEffects.playCorrect() else soundEffects.playWrong()
        analyticsTracker.onboardingQuizAnswered(
            questionId = question.id,
            topic = question.topic,
            isCorrect = isCorrect,
            position = index + 1,
            seconds = ((System.currentTimeMillis() - quizQuestionShownAt) / 1000).toInt(),
            concern = state.data.concern?.let(::optionLabel)
        )
        updateData { it.copy(quizAnswers = it.quizAnswers + QuizAnswer(question.id, optionIndex, isCorrect)) }
    }

    /** Shows the next mini-test question, or the result after the last one. Waits for an answer. */
    private fun nextQuizQuestion() {
        val state = _uiState.value
        if (state.currentStep != OnboardingStep.Quiz) return
        if (state.data.quizAnswers.size <= state.quizIndex) return

        if (state.quizIndex < state.data.quizQuestions().lastIndex) {
            _uiState.update { it.copy(quizIndex = it.quizIndex + 1) }
            quizQuestionShownAt = System.currentTimeMillis()
            updateMascotMessage()
        } else {
            goToNextStep()
        }
    }

    /** Leaves the mini-test before answering anything; its result screen is skipped too. */
    private fun skipQuiz() {
        val state = _uiState.value
        if (state.currentStep != OnboardingStep.Quiz || state.data.quizAnswers.isNotEmpty()) return
        analyticsTracker.onboardingQuizSkipped()
        goToNextStep()
    }

    /**
     * Whether [step] is left out of this run: the test result when the test was skipped.
     *
     * @param step a step of the flow
     * @param data the answers collected so far
     * @return true when [step] must be jumped over in both directions
     */
    private fun isSkipped(step: OnboardingStep, data: OnboardingData): Boolean =
        step == OnboardingStep.QuizResult && data.quizAnswers.isEmpty()

    /**
     * Keeps the study time and saves it on the device straight away, so the reminder is set
     * up however the user answers the next screen and even if they quit before paying.
     *
     * @param option the tapped option, a key of [OnboardingConfig.studyTimes]
     */
    private fun selectStudyTime(option: String) {
        val rhythm = OnboardingConfig.studyRhythmFor(_uiState.value.data.weeklyStudy)
        OnboardingConfig.studyTimes[option]?.let { slot ->
            viewModelScope.launch {
                notificationsRepository.saveStudySchedule(StudySchedule(slot, rhythm))
            }
        }
        selectStepItem { it.copy(studyTime = option) }
    }

    /**
     * Records the answer to the reminders offer and moves on. Saying yes shows Android's
     * permission dialog first. Taps that arrive while the dialog is open, or from the screen
     * as it slides away, are ignored so they cannot skip the next step.
     *
     * @param accepted true for "Sí, avísame", false for "Ahora no"
     */
    private fun answerNotifications(accepted: Boolean) {
        val state = _uiState.value
        if (state.currentStep != OnboardingStep.Notifications || state.isRequestingNotifications) return

        if (!accepted) {
            notificationsRepository.optOut()
            recordNotificationsAnswer(NotificationsAnswer.DECLINED)
            return
        }

        _uiState.update { it.copy(isRequestingNotifications = true) }
        viewModelScope.launch {
            val granted = notificationsRepository.requestPermission()
            _uiState.update { it.copy(isRequestingNotifications = false) }
            recordNotificationsAnswer(if (granted) NotificationsAnswer.GRANTED else NotificationsAnswer.DENIED)
        }
    }

    /**
     * Reports the reminders answer and advances past the screen.
     *
     * @param answer how the user answered
     */
    private fun recordNotificationsAnswer(answer: NotificationsAnswer) {
        analyticsTracker.notificationsPermissionAnswered(
            result = answer.analyticsValue,
            studySlot = _uiState.value.data.studySlot()?.tag
        )
        selectStepItem { it.copy(notifications = answer) }
    }

    /**
     * Applies a transformation to the collected data and refreshes the derived UI state.
     *
     * @param update transformation applied to the collected onboarding data
     */
    private fun updateData(update: (OnboardingData) -> OnboardingData) {
        _uiState.update { it.copy(data = update(it.data)) }
        updateNavigationState()
        updateMascotMessage()
    }

    /**
     * Advances to the next step, logging the completion of the current one.
     * Reaching the end of the flow hands over to the paywall.
     */
    private fun goToNextStep() {
        val currentIndex = stepsOrder.indexOf(_uiState.value.currentStep)

        if (currentIndex == stepsOrder.lastIndex) {
            analyticsTracker.onboardingFlowCompleted(_uiState.value.data.toAnalyticsProfile())
            persistDataAndShowPaywall()
            return
        }

        val data = _uiState.value.data
        val nextIndex = (currentIndex + 1 until stepsOrder.size)
            .firstOrNull { !isSkipped(stepsOrder[it], data) }
            ?: return
        val nextStep = stepsOrder[nextIndex]
        if (nextStep == OnboardingStep.Processing) {
            startProcessing()
            return
        }

        _uiState.update {
            it.copy(
                currentStep = nextStep,
                progress = reducer.calculateProgress(nextIndex, stepsOrder.size),
                isMovingBack = false
            )
        }
        if (nextStep == OnboardingStep.Quiz) quizQuestionShownAt = System.currentTimeMillis()
        trackStepReached(nextStep)
        updateMascotMessage()
        updateNavigationState()
    }

    /** Persists the collected profile before showing the paywall so it survives app closure. */
    private fun persistDataAndShowPaywall() {
        viewModelScope.launch {
            userRepository.insert(_uiState.value.data.toUser())
            if (BuildConfig.DEBUG) {
                // Debug builds skip the RevenueCat paywall so it doesn't block manual testing;
                // release always requires a confirmed purchase here.
                saveDataAndComplete()
            } else {
                // The paywall reports its own arrival through paywallShown().
                _uiState.update { it.copy(currentStep = OnboardingStep.PaywallPending) }
            }
        }
    }

    /** Logs completion after RevenueCat confirms premium and ends the flow. */
    private fun saveDataAndComplete() {
        analyticsTracker.onboardingCompleted()
        _uiState.update { it.copy(currentStep = OnboardingStep.Completed) }
    }

    /**
     * Converts collected onboarding answers into the local profile persisted before payment,
     * including the date the plan reveal just promised, so Home can keep showing it.
     */
    private fun OnboardingData.toUser(): User = User(
        name = name,
        experience = experience,
        examDateMillis = examDate,
        planTargetMillis = OnboardingConfig.planTargetMillis(examDate, weeklyStudy),
        lastPracticeTimestamp = 0,
        currentStreak = 0
    )

    /**
     * Picks the answers worth segmenting analytics by.
     *
     * Only options the user tapped are included, never [OnboardingData.name], so the profile
     * carries nothing that identifies the person.
     *
     * @return the answered questions keyed by their analytics property name
     */
    private fun OnboardingData.toAnalyticsProfile(): Map<String, String> = buildMap {
        experience?.let { put("experience", it) }
        theoryBlocker?.let { put("theory_blocker", it) }
        concern?.let { put("concern", it) }
        readiness?.let { put("readiness", it) }
        motivation?.let { put("motivation", it) }
        put("exam_timing", OnboardingConfig.examTimingTag(examDate))
        examDate?.let { put("exam_days_left", calendarDaysBetween(System.currentTimeMillis(), it).toString()) }
        put("quiz_score", if (quizAnswers.isEmpty()) "skipped" else "${quizScore()}/${quizAnswers.size}")
        province?.let { put("province", it) }
        weeklyStudy?.let { put("weekly_study", it) }
        studySlot()?.let { put("study_slot", it.tag) }
        notifications?.let { put("notifications", it.analyticsValue) }
        learningPreference?.let { put("learning_preference", it) }
    }

    /**
     * Moves back one step and keeps every answer given so far.
     *
     * The move is ignored where [OnboardingReducer.previousStep] refuses it (the first step,
     * and the plan being built) and while Android's permission dialog is open, because its
     * answer would then land on a different screen from the one that asked.
     */
    private fun goToPreviousStep() {
        val state = _uiState.value
        if (state.isRequestingNotifications) return
        val previousStep = reducer.previousStep(stepsOrder, state.currentStep) { isSkipped(it, state.data) }
            ?: return

        _uiState.update {
            it.copy(
                currentStep = previousStep,
                progress = reducer.calculateProgress(stepsOrder.indexOf(previousStep), stepsOrder.size),
                isMovingBack = true
            )
        }
        updateMascotMessage()
        updateNavigationState()
    }

    /** Runs the faked "building your plan" progress bar and then continues the flow. */
    private fun startProcessing() {
        _uiState.update {
            it.copy(
                currentStep = OnboardingStep.Processing,
                progress = 1f,
                isProcessingFinished = false,
                processingProgress = 0f,
                isMovingBack = false
            )
        }
        trackStepReached(OnboardingStep.Processing)
        updateMascotMessage()
        updateNavigationState()
        viewModelScope.launch {
            val totalSteps = 100
            for (i in 1..totalSteps) {
                delay(Random.nextLong(20, 50))
                _uiState.update { it.copy(processingProgress = i / totalSteps.toFloat()) }
            }
            delay(300)
            _uiState.update { it.copy(isProcessingFinished = true) }
            delay(800)
            goToNextStep()
        }
    }

    /** Recomputes whether the user may move forward or backward from the current step. */
    private fun updateNavigationState() {
        val state = _uiState.value
        val canGoNext = reducer.shouldEnableNextButton(state.currentStep, state.data)
        val canGoBack = reducer.previousStep(stepsOrder, state.currentStep) { isSkipped(it, state.data) } != null
        _uiState.update { it.copy(canGoNext = canGoNext, canGoBack = canGoBack) }
    }

    /** Refreshes the mascot line for the current step and collected data. */
    private fun updateMascotMessage() {
        val state = _uiState.value
        val message = reducer.updateMascotMessage(state.currentStep, state.data, state.quizIndex)
        _uiState.update { it.copy(mascotMessage = message) }
    }

    override fun onCleared() {
        super.onCleared()
        val lastStep = _uiState.value.currentStep
        val position = stepsOrder.indexOf(lastStep)
        if (lastStep !is OnboardingStep.Completed && position >= 0) {
            // Reported with the same name the funnel event uses, so the two can be joined.
            analyticsTracker.onboardingAbandoned(
                lastStep = funnelEventName(position, lastStep.analyticsName),
                stepIndex = position
            )
        }
    }
}
