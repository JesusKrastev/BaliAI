package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class OnboardingData(
    val name: String? = null,
    val licenseType: String? = null,
    val experience: String? = null,
    val theoryBlocker: String? = null,
    val motivation: String? = null,
    val futureImpact: String? = null,
    val examDate: Long? = null,
    val dailyGoal: String? = null,
    val learningPreference: String? = null,
    val difficultTopics: Set<String> = emptySet(),
)

/**
 * A single screen of the onboarding flow.
 *
 * The flow is deliberately built as an emotional arc rather than a questionnaire:
 * diagnosis → the user's "why" → what not having the licence costs them →
 * the method → the life the licence unlocks → the concrete plan → close.
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

    // ── Diagnóstico ────────────────────────────────────────────────────────
    data object Name : OnboardingStep("name")
    data object License : OnboardingStep("license")
    data object Experience : OnboardingStep("experience")
    data object DialogueExperience : Informational("dialogue_experience")
    data object TheoryBlocker : OnboardingStep("reasons")

    // ── El porqué ──────────────────────────────────────────────────────────
    data object Motivation : OnboardingStep("motivation")
    data object FutureImpact : OnboardingStep("future_impact")
    data object Empathy : Informational("empathy")

    // ── Lo que cuesta no tenerlo ───────────────────────────────────────────
    data object LossTime : Informational("loss_time")
    data object LossOpportunity : Informational("loss_opportunity")
    data object LossAutonomy : Informational("loss_autonomy")

    // ── La solución ────────────────────────────────────────────────────────
    data object MethodComparison : Informational("method_comparison")

    // ── Lo que ganas ───────────────────────────────────────────────────────
    data object GainFreedom : Informational("gain_freedom")
    data object GainExperiences : Informational("gain_experiences")
    data object GainLevelUp : Informational("gain_level_up")

    // ── El plan ────────────────────────────────────────────────────────────
    data object ExamDate : OnboardingStep("exam_date")
    data object DifficultTopics : OnboardingStep("difficult_topics")
    data object DialogueDifficultTopics : Informational("dialogue_difficult_topics")
    data object DailyGoal : OnboardingStep("daily_goal")
    data object LearningPreference : OnboardingStep("learning_preference")

    // ── Cierre ─────────────────────────────────────────────────────────────
    data object Processing : OnboardingStep("processing")
    data object Comparison : Informational("comparison")
    data object PlanReveal : Informational("plan_reveal")
    data object SocialProof : Informational("social_proof")
    data object Pact : OnboardingStep("pact")
    data object Preview : Informational("preview")

    data object PaywallPending : OnboardingStep()
    data object Completed : OnboardingStep()
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Name,
    val data: OnboardingData = OnboardingData(),
    val progress: Float = 0f,
    val processingProgress: Float = 0f,
    val mascotMessage: String = "",
    val canGoBack: Boolean = false,
    val canGoNext: Boolean = false,
    val isProcessingFinished: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val reducer = OnboardingReducer()

    private val stepsOrder = listOf(
        OnboardingStep.Name, OnboardingStep.License, OnboardingStep.Experience,
        OnboardingStep.DialogueExperience, OnboardingStep.TheoryBlocker,
        OnboardingStep.Motivation, OnboardingStep.FutureImpact, OnboardingStep.Empathy,
        OnboardingStep.LossTime, OnboardingStep.LossOpportunity, OnboardingStep.LossAutonomy,
        OnboardingStep.MethodComparison,
        OnboardingStep.GainFreedom, OnboardingStep.GainExperiences, OnboardingStep.GainLevelUp,
        OnboardingStep.ExamDate, OnboardingStep.DifficultTopics,
        OnboardingStep.DialogueDifficultTopics, OnboardingStep.DailyGoal,
        OnboardingStep.LearningPreference,
        OnboardingStep.Processing, OnboardingStep.Comparison, OnboardingStep.Preview,
        OnboardingStep.PlanReveal, OnboardingStep.SocialProof, OnboardingStep.Pact
    )

    init {
        updateMascotMessage()
        analyticsTracker.onboardingStarted()
        trackStepReached(OnboardingStep.Name)
    }

    /**
     * Reports that a screen was shown, naming the event after its position in the flow.
     *
     * The number is derived from [stepsOrder] rather than stored on the step, so it can
     * never drift out of sync with the real order. The `p` prefix is not decoration:
     * Firebase silently drops events whose name starts with a digit.
     *
     * @param step the step that just became visible
     */
    private fun trackStepReached(step: OnboardingStep) {
        val position = stepsOrder.indexOf(step)
        if (position < 0 || step.analyticsName.isBlank()) return
        analyticsTracker.onboardingStepReached(funnelEventName(position, step.analyticsName))
        if (step == OnboardingStep.Preview) analyticsTracker.onboardingPreviewViewed()
    }

    /**
     * Builds the ordered event name for a step.
     *
     * @param position zero-based index of the step in [stepsOrder]
     * @param slug the step's semantic name
     * @return a name such as `p07_future_impact`
     */
    private fun funnelEventName(position: Int, slug: String): String =
        "p%02d_%s".format(position + 1, slug)

    /**
     * Entry point for every user interaction in the onboarding flow.
     *
     * @param event the action performed by the user
     */
    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.SetName -> updateData { it.copy(name = event.name) }
            is OnboardingEvent.SelectLicense -> selectStepItem { it.copy(licenseType = event.license) }
            is OnboardingEvent.SelectExperience -> selectStepItem { it.copy(experience = event.experience) }
            is OnboardingEvent.SelectTheoryBlocker -> selectStepItem { it.copy(theoryBlocker = event.blocker) }
            is OnboardingEvent.SelectMotivation -> selectStepItem { it.copy(motivation = event.motivation) }
            is OnboardingEvent.SelectFutureImpact -> selectStepItem { it.copy(futureImpact = event.impact) }
            is OnboardingEvent.SelectExamDate -> selectStepItem { it.copy(examDate = event.dateMillis) }
            is OnboardingEvent.SelectDailyGoal -> selectStepItem { it.copy(dailyGoal = event.goal) }
            is OnboardingEvent.SelectLearningPreference -> selectStepItem { it.copy(learningPreference = event.preference) }
            is OnboardingEvent.ToggleDifficultTopic -> toggleDifficultTopic(event.topic)
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
     * Adds or removes a topic from the multi-choice "difficult topics" answer.
     *
     * @param topic label of the toggled topic
     */
    private fun toggleDifficultTopic(topic: String) {
        val current = _uiState.value.data.difficultTopics
        val newSet = if (current.contains(topic)) current - topic else current + topic
        updateData { it.copy(difficultTopics = newSet) }
    }

    /**
     * Advances to the next step, logging the completion of the current one.
     * Reaching the end of the flow hands over to the paywall.
     */
    private fun goToNextStep() {
        if (_uiState.value.currentStep == OnboardingStep.Preview) {
            analyticsTracker.onboardingPreviewContinueClicked()
        }
        val currentIndex = stepsOrder.indexOf(_uiState.value.currentStep)

        if (currentIndex == stepsOrder.lastIndex) {
            // The paywall reports its own arrival through paywallShown().
            _uiState.update { it.copy(currentStep = OnboardingStep.PaywallPending) }
            return
        }

        val nextStep = stepsOrder[currentIndex + 1]
        if (nextStep == OnboardingStep.Processing) {
            startProcessing()
            return
        }

        _uiState.update {
            it.copy(
                currentStep = nextStep,
                progress = reducer.calculateProgress(currentIndex + 1, stepsOrder.size)
            )
        }
        trackStepReached(nextStep)
        updateMascotMessage()
        updateNavigationState()
    }

    /** Persists the collected profile, logs completion and ends the flow. */
    private fun saveDataAndComplete() {
        viewModelScope.launch {
            val data = _uiState.value.data
            val preferences = User(
                name = data.name,
                licenseType = data.licenseType,
                experience = data.experience,
                examDateMillis = data.examDate,
                difficultTopics = data.difficultTopics.joinToString(","),
                lastPracticeTimestamp = 0,
                currentStreak = 0
            )
            userRepository.insert(preferences)
            analyticsTracker.onboardingCompleted()
            _uiState.update { it.copy(currentStep = OnboardingStep.Completed) }
        }
    }

    /** Moves back one step, if the current one is not the first. */
    private fun goToPreviousStep() {
        val currentIndex = stepsOrder.indexOf(_uiState.value.currentStep)
        if (currentIndex > 0) {
            val prevStep = stepsOrder[currentIndex - 1]
            _uiState.update {
                it.copy(
                    currentStep = prevStep,
                    progress = reducer.calculateProgress(currentIndex - 1, stepsOrder.size)
                )
            }
            updateMascotMessage()
            updateNavigationState()
        }
    }

    /** Runs the faked "building your plan" progress bar and then continues the flow. */
    private fun startProcessing() {
        _uiState.update {
            it.copy(
                currentStep = OnboardingStep.Processing,
                progress = 1f,
                isProcessingFinished = false,
                processingProgress = 0f
            )
        }
        trackStepReached(OnboardingStep.Processing)
        updateMascotMessage()
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
        _uiState.update {
            it.copy(
                canGoNext = canGoNext,
                canGoBack = stepsOrder.indexOf(state.currentStep) > 0
            )
        }
    }

    /** Refreshes the mascot line for the current step and collected data. */
    private fun updateMascotMessage() {
        val state = _uiState.value
        val message = reducer.updateMascotMessage(state.currentStep, state.data)
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
