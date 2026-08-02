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
    fun `current step initializes correctly`() = runTest {
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Name)
    }

    @Test
    fun `name update changes canGoNext`() = runTest {
        assertThat(viewModel.uiState.value.canGoNext).isFalse()
        viewModel.onEvent(OnboardingEvent.SetName("Jesus"))
        assertThat(viewModel.uiState.value.canGoNext).isTrue()
    }

    @Test
    fun `the why block runs before the emotional arc`() = runTest {
        advanceThroughDiagnosis()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Motivation)

        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.FutureImpact)

        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(OnboardingConfig.futureImpacts.first()))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Empathy)
    }

    @Test
    fun `the loss block leads into the method comparison`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
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
        advanceThroughDiagnosis()
        val motivation = OnboardingConfig.motivations.first()
        val impact = OnboardingConfig.futureImpacts.first()

        viewModel.onEvent(OnboardingEvent.SelectMotivation(motivation))
        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(impact))

        assertThat(viewModel.uiState.value.data.motivation).isEqualTo(motivation)
        assertThat(viewModel.uiState.value.data.futureImpact).isEqualTo(impact)
    }

    @Test
    fun `the first screen is counted before the user does anything`() = runTest {
        assertThat(fakeAnalyticsTracker.onboardingSteps).containsExactly("p01_name")
    }

    @Test
    fun `every screen reached emits exactly one funnel event, numbered in order`() = runTest {
        advanceThroughDiagnosis()

        assertThat(fakeAnalyticsTracker.onboardingSteps)
            .containsExactly(
                "p01_name",
                "p02_license",
                "p03_experience",
                "p04_dialogue_experience",
                "p05_reasons",
                "p06_motivation"
            )
            .inOrder()
    }

    @Test
    fun `a screen the user only lands on is still counted`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))

        // The user goes no further: the screen must appear even though it was never answered.
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.FutureImpact)
        assertThat(fakeAnalyticsTracker.onboardingSteps).contains("p07_future_impact")
    }

    @Test
    fun `event names are zero padded so they sort in flow order`() = runTest {
        // Without padding "p10_" would sort before "p02_" and the funnel would read wrong.
        assertThat(fakeAnalyticsTracker.onboardingSteps.first()).startsWith("p01_")
    }

    @Test
    fun `event names start with a letter, which Firebase requires`() = runTest {
        advanceThroughDiagnosis()

        fakeAnalyticsTracker.onboardingSteps.forEach { eventName ->
            assertThat(eventName.first().isLetter()).isTrue()
        }
    }

    /** Answers name, licence, experience and theory blocker, leaving the flow on [OnboardingStep.Motivation]. */
    private fun advanceThroughDiagnosis() {
        viewModel.onEvent(OnboardingEvent.SetName("Jesus"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectLicense(OnboardingConfig.licenses.first()))
        viewModel.onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.experiences.first()))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))
    }
}
