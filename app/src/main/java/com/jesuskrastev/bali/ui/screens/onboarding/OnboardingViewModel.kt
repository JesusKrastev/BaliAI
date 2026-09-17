package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * Everything the user tells us during onboarding.
 *
 * Only [name] and [experience] outlive the flow; the rest exists to personalise the copy
 * of the later screens and to make the generated plan read as the user's own.
 *
 * @property examTiming the bucket the user picked, kept for display
 * @property examDate the date [examTiming] estimates, used by the countdown and the plan
 */
data class OnboardingData(
    val name: String? = null,
    val experience: String? = null,
    val theoryBlocker: String? = null,
    val concern: String? = null,
    val readiness: String? = null,
    val motivation: String? = null,
    val futureImpact: String? = null,
    val examTiming: String? = null,
    val examDate: Long? = null,
    val province: String? = null,
    val weeklyStudy: String? = null,
    val learningPreference: String? = null,
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

    // ── El porqué ──────────────────────────────────────────────────────────
    data object Motivation : OnboardingStep("motivation")
    data object TheoryBlocker : OnboardingStep("reasons")

    // ── Diagnóstico ────────────────────────────────────────────────────────
    data object Concern : OnboardingStep("concern")
    data object Experience : OnboardingStep("experience")
    data object Readiness : OnboardingStep("readiness")
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
    // The name opens this block: the whole emotional arc is answered with taps, and the
    // keyboard only shows up once the user is invested and the plan is about to be built.
    data object Name : OnboardingStep("name")
    data object ExamDate : OnboardingStep("exam_date")
    data object Province : OnboardingStep("province")
    data object ProvinceConfirmed : Informational("province_confirmed")
    data object WeeklyStudy : OnboardingStep("weekly_study")
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

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Motivation,
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
        // El porqué
        OnboardingStep.Motivation, OnboardingStep.TheoryBlocker,
        // Diagnóstico
        OnboardingStep.Concern, OnboardingStep.Experience, OnboardingStep.Comparison,
        OnboardingStep.Readiness, OnboardingStep.FutureImpact, OnboardingStep.Empathy,
        // Lo que cuesta no tenerlo
        OnboardingStep.LossTime, OnboardingStep.LossOpportunity, OnboardingStep.LossAutonomy,
        // La solución y lo que ganas
        OnboardingStep.MethodComparison,
        OnboardingStep.GainFreedom, OnboardingStep.GainExperiences, OnboardingStep.GainLevelUp,
        // El plan
        OnboardingStep.Name, OnboardingStep.ExamDate,
        OnboardingStep.Province, OnboardingStep.ProvinceConfirmed,
        OnboardingStep.WeeklyStudy, OnboardingStep.LearningPreference,
        // Cierre
        OnboardingStep.SocialProof, OnboardingStep.Processing, OnboardingStep.PlanReveal,
        OnboardingStep.Pact
    )

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
     * @param step the step that just became visible
     */
    private fun trackStepReached(step: OnboardingStep) {
        val position = stepsOrder.indexOf(step)
        if (position < 0 || step.analyticsName.isBlank()) return
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
            is OnboardingEvent.SelectConcern -> selectStepItem { it.copy(concern = event.concern) }
            is OnboardingEvent.SelectReadiness -> selectStepItem { it.copy(readiness = event.readiness) }
            is OnboardingEvent.SelectMotivation -> selectStepItem { it.copy(motivation = event.motivation) }
            is OnboardingEvent.SelectFutureImpact -> selectStepItem { it.copy(futureImpact = event.impact) }
            is OnboardingEvent.SelectExamTiming -> selectStepItem {
                it.copy(
                    examTiming = event.timing,
                    examDate = OnboardingConfig.examDateFor(event.timing)
                )
            }
            // The province needs a confirmation tap, so it does not advance on its own.
            is OnboardingEvent.SelectProvince -> updateData { it.copy(province = event.province) }
            is OnboardingEvent.SelectWeeklyStudy -> selectStepItem { it.copy(weeklyStudy = event.weeklyStudy) }
            is OnboardingEvent.SelectLearningPreference -> selectStepItem { it.copy(learningPreference = event.preference) }
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
     * Advances to the next step, logging the completion of the current one.
     * Reaching the end of the flow hands over to the paywall.
     */
    private fun goToNextStep() {
        val currentIndex = stepsOrder.indexOf(_uiState.value.currentStep)

        if (currentIndex == stepsOrder.lastIndex) {
            persistDataAndShowPaywall()
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

    /** Converts collected onboarding answers into the local profile persisted before payment. */
    private fun OnboardingData.toUser(): User = User(
        name = name,
        experience = experience,
        examDateMillis = examDate,
        lastPracticeTimestamp = 0,
        currentStreak = 0
    )

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
