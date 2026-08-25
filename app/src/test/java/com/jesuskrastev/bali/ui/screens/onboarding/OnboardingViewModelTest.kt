package com.jesuskrastev.bali.ui.screens.onboarding

import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock())

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        viewModel = OnboardingViewModel(
            userRepository = fakeUserRepository,
            analyticsTracker = fakeAnalyticsTracker
        )
    }

    @Test
    fun `the flow opens on a tappable question, not on the keyboard`() = runTest {
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Motivation)
    }

    @Test
    fun `the name is only asked once the emotional arc is over`() = runTest {
        advanceThroughArc()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Name)
    }

    @Test
    fun `name update changes canGoNext`() = runTest {
        advanceThroughArc()

        assertThat(viewModel.uiState.value.canGoNext).isFalse()
        viewModel.onEvent(OnboardingEvent.SetName("Jesus"))
        assertThat(viewModel.uiState.value.canGoNext).isTrue()
    }

    @Test
    fun `the diagnosis runs before the emotional arc`() = runTest {
        advanceThroughDiagnosis()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.FutureImpact)

        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(OnboardingConfig.futureImpacts.first()))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Empathy)
    }

    @Test
    fun `the loss block leads into the method comparison`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(OnboardingConfig.futureImpacts.first()))

        val expectedArc = listOf(
            OnboardingStep.LossTime,
            OnboardingStep.LossOpportunity,
            OnboardingStep.LossAutonomy,
            OnboardingStep.MethodComparison,
            OnboardingStep.GainFreedom
        )

        expectedArc.forEach { expectedStep ->
            viewModel.onEvent(OnboardingEvent.GoToNextStep)
            assertThat(viewModel.uiState.value.currentStep).isEqualTo(expectedStep)
        }
    }

    @Test
    fun `the why answers are kept for the plan reveal`() = runTest {
        val motivation = OnboardingConfig.motivations.first()
        val impact = OnboardingConfig.futureImpacts.first()

        viewModel.onEvent(OnboardingEvent.SelectMotivation(motivation))
        viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))
        viewModel.onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.experiences.first()))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectReadiness(OnboardingConfig.readinessLevels.first()))
        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(impact))

        assertThat(viewModel.uiState.value.data.motivation).isEqualTo(motivation)
        assertThat(viewModel.uiState.value.data.futureImpact).isEqualTo(impact)
    }

    @Test
    fun `a booked exam becomes a date the plan can count down from`() = runTest {
        viewModel.onEvent(OnboardingEvent.SelectExamTiming(OnboardingConfig.examTimings.first()))

        val examDate = viewModel.uiState.value.data.examDate
        assertThat(examDate).isNotNull()
        assertThat(examDate!!).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `an unbooked exam leaves the plan without a date`() = runTest {
        viewModel.onEvent(OnboardingEvent.SelectExamTiming(OnboardingConfig.EXAM_TIMING_UNBOOKED))

        assertThat(viewModel.uiState.value.data.examTiming)
            .isEqualTo(OnboardingConfig.EXAM_TIMING_UNBOOKED)
        assertThat(viewModel.uiState.value.data.examDate).isNull()
    }

    @Test
    fun `picking a province does not advance on its own`() = runTest {
        // The user has to see which province landed in the field before moving on.
        val stepBefore = viewModel.uiState.value.currentStep

        viewModel.onEvent(OnboardingEvent.SelectProvince("Almería"))

        assertThat(viewModel.uiState.value.data.province).isEqualTo("Almería")
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(stepBefore)
    }

    @Test
    fun `the first screen is counted before the user does anything`() = runTest {
        assertThat(fakeAnalyticsTracker.onboardingSteps).containsExactly("o01_motivation")
    }

    @Test
    fun `every screen reached emits exactly one funnel event, numbered in order`() = runTest {
        advanceThroughDiagnosis()

        assertThat(fakeAnalyticsTracker.onboardingSteps)
            .containsExactly(
                "o01_motivation",
                "o02_reasons",
                "o03_concern",
                "o04_experience",
                "o05_comparison",
                "o06_readiness",
                "o07_future_impact"
            )
            .inOrder()
    }

    @Test
    fun `a screen the user only lands on is still counted`() = runTest {
        advanceThroughDiagnosis()

        // The user goes no further: the screen must appear even though it was never answered.
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.FutureImpact)
        assertThat(fakeAnalyticsTracker.onboardingSteps).contains("o07_future_impact")
    }

    @Test
    fun `event names are zero padded so they sort in flow order`() = runTest {
        // Without padding "o10_" would sort before "o02_" and the funnel would read wrong.
        assertThat(fakeAnalyticsTracker.onboardingSteps.first()).startsWith("o01_")
    }

    @Test
    fun `event names start with a letter, which Firebase requires`() = runTest {
        advanceThroughDiagnosis()

        fakeAnalyticsTracker.onboardingSteps.forEach { eventName ->
            assertThat(eventName.first().isLetter()).isTrue()
        }
    }

    /** Answers the whole diagnosis block, leaving the flow on [OnboardingStep.FutureImpact]. */
    private fun advanceThroughDiagnosis() {
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))
        viewModel.onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.experiences.first()))
        // Comparison is informational and needs the bottom button.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectReadiness(OnboardingConfig.readinessLevels.first()))
    }

    /**
     * Answers everything up to and including the gain block, leaving the flow on
     * [OnboardingStep.Name] — the first screen that asks the user to type.
     */
    private fun advanceThroughArc() {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(OnboardingConfig.futureImpacts.first()))
        // Empathy, the three losses, the method and the three gains are all tap-to-continue.
        repeat(8) { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
    }
}
