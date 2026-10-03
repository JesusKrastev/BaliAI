package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeNotificationsRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.theme.BaliTheme
import com.jesuskrastev.bali.util.FakeSoundEffects
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real onboarding screen to check that its back arrow and the system back gesture
 * reach the ViewModel. `GoToPreviousStep` used to exist with nothing sending it, so the gesture
 * closed the app and the progress was lost.
 *
 * The name ends in `ScreenshotTest` because the build only runs Compose rule tests in the debug
 * variant: release has no test manifest to declare the activity they need.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class OnboardingBackNavigationScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        viewModel = OnboardingViewModel(
            userRepository = FakeUserRepository(),
            analyticsTracker = FakeAnalyticsTracker(mock(), mock(), mock()),
            notificationsRepository = FakeNotificationsRepository(),
            soundEffects = FakeSoundEffects()
        )
    }

    /** Shows the onboarding on its first screen. */
    private fun showOnboarding() {
        composeTestRule.setContent {
            BaliTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        OnboardingScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            viewModel = viewModel,
                            onComplete = {}
                        )
                    }
                }
            }
        }
    }

    /** Taps the first answer of the first question, which moves the flow to the second screen. */
    private fun answerFirstQuestion() {
        // The card shows the label without the emoji that opens the option's text.
        composeTestRule.onNodeWithText(OnboardingConfig.MOTIVATION_INDEPENDENCE.substringAfter(' ')).performClick()
        composeTestRule.waitForIdle()
    }

    /** Sends the system back gesture to the activity, the way Android delivers it. */
    private fun pressSystemBack() {
        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * Answers every question and taps through every screen until the plan starts being built,
     * driving the ViewModel directly instead of tapping twenty screens one by one.
     */
    private fun answerUpToTheBuildingScreen() {
        with(viewModel) {
            onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
            onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))
            onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.experiences.first()))
            onEvent(OnboardingEvent.GoToNextStep)
            onEvent(OnboardingEvent.SelectReadiness(OnboardingConfig.readinessLevels.first()))
            onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))
            // The mini-test is skipped, and its result with it.
            onEvent(OnboardingEvent.SkipQuiz)
            // Empathy, the three losses, the method and the three gains lead to the name.
            repeat(8) { onEvent(OnboardingEvent.GoToNextStep) }
            onEvent(OnboardingEvent.SetName("Jesus"))
            onEvent(OnboardingEvent.GoToNextStep)
            onEvent(OnboardingEvent.SetExamDate(null))
            onEvent(OnboardingEvent.SelectProvince("Almería"))
            // The province waits for the bottom button.
            onEvent(OnboardingEvent.GoToNextStep)
            onEvent(OnboardingEvent.SelectWeeklyStudy(OnboardingConfig.WEEKLY_STUDY_OFTEN))
            onEvent(OnboardingEvent.SelectStudyTime(OnboardingConfig.studyTimes.keys.last()))
            onEvent(OnboardingEvent.AnswerNotifications(accepted = false))
            onEvent(OnboardingEvent.SelectLearningPreference(OnboardingConfig.STYLE_MOCK_EXAMS.key))
            onEvent(OnboardingEvent.GoToNextStep)
        }
    }

    @Test
    fun theFirstScreenHasNoBackArrow() {
        showOnboarding()

        composeTestRule.onNodeWithContentDescription(BACK_ARROW).assertDoesNotExist()
    }

    @Test
    fun theBackArrowReturnsToThePreviousQuestion() {
        showOnboarding()
        answerFirstQuestion()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.TheoryBlocker)

        composeTestRule.onNodeWithContentDescription(BACK_ARROW).performClick()
        composeTestRule.waitForIdle()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Motivation)
        composeTestRule.onNodeWithContentDescription(BACK_ARROW).assertDoesNotExist()
    }

    @Test
    fun theSystemBackGestureStepsBackInsteadOfClosingTheApp() {
        showOnboarding()
        answerFirstQuestion()

        pressSystemBack()
        composeTestRule.waitForIdle()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Motivation)
        assertThat(composeTestRule.activity.isFinishing).isFalse()
    }

    @Test
    fun theSystemBackGestureIsSwallowedWhileThePlanIsBeingBuilt() {
        showOnboarding()
        answerUpToTheBuildingScreen()
        composeTestRule.waitForIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Processing)

        pressSystemBack()
        composeTestRule.waitForIdle()

        // It neither closed the app nor sent the user back to a screen with nothing to tap.
        assertThat(composeTestRule.activity.isFinishing).isFalse()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Processing)
    }

    @Test
    fun onTheFirstScreenTheSystemBackGestureStillClosesTheApp() {
        showOnboarding()

        pressSystemBack()

        assertThat(composeTestRule.activity.isFinishing).isTrue()
    }

    @Test
    fun captureSecondScreenWithTheBackArrow() {
        showOnboarding()
        answerFirstQuestion()
        // Lets the mascot's speech bubble finish typing its line.
        composeTestRule.mainClock.advanceTimeBy(3_000L)

        composeTestRule.onRoot().captureRoboImage()
    }

    private companion object {
        /** The arrow's content description, which is how a screen reader and this test find it. */
        const val BACK_ARROW = "Volver"
    }
}
