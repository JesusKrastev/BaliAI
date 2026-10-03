package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeNotificationsRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.onboarding.steps.StepPlanReveal
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
import java.util.concurrent.TimeUnit

/**
 * Captures the onboarding screens that depend on the user's answers, driven through the real
 * screen and ViewModel, so a change of copy or layout can be reviewed as a picture.
 *
 * Record with `./gradlew recordRoborazziDebug --tests "*OnboardingStepsScreenshotTest"`; the
 * images land in `app/build/onboarding-steps/`.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xhdpi")
@RunWith(RobolectricTestRunner::class)
class OnboardingStepsScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        viewModel = OnboardingViewModel(
            userRepository = FakeUserRepository(),
            analyticsTracker = FakeAnalyticsTracker(mock(), mock()),
            notificationsRepository = FakeNotificationsRepository(),
            soundEffects = FakeSoundEffects()
        )
    }

    @Test
    fun captureRealFailureRate() {
        answerDiagnosisUpTo(OnboardingStep.Comparison)
        capture("comparison")
    }

    @Test
    fun captureQuizQuestion() {
        answerDiagnosisUpTo(OnboardingStep.Concern)
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_SILLY_MISTAKES))
        capture("quiz_question")
    }

    @Test
    fun captureQuizExplanation() {
        answerDiagnosisUpTo(OnboardingStep.Concern)
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_SILLY_MISTAKES))
        viewModel.onEvent(OnboardingEvent.AnswerQuiz(1))
        capture("quiz_explanation")
    }

    @Test
    fun captureQuizResult() {
        answerQuiz()
        capture("quiz_result")
    }

    @Test
    fun captureIntro() {
        capture("intro")
        // The cards are there from the first frame; there is no chest to open.
        composeTestRule.onNodeWithText("Aprueba a la primera").assertExists()
    }

    @Test
    fun captureIntroDark() = capture("intro_dark", darkTheme = true)

    /** The other three intro cards, reached with the button, in the dark theme. */
    @Test
    fun captureIntroCardsDark() {
        capture("intro_dark", darkTheme = true)
        repeat(3) { page ->
            composeTestRule.onNodeWithText("Siguiente →").performClick()
            composeTestRule.mainClock.advanceTimeBy(2_000L)
            composeTestRule.onRoot().captureRoboImage("build/onboarding-steps/intro_dark_${page + 2}.png")
        }
    }

    @Test
    fun capturePain() {
        answerDiagnosisUpTo(OnboardingStep.Pain)
        capture("pain")
    }

    @Test
    fun captureRoadToTheExam() {
        answerQuiz()
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        capture("method")
    }

    @Test
    fun captureGain() {
        answerQuiz()
        repeat(2) { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
        capture("gain")
    }

    @Test
    fun captureExamDatePicker() {
        answerUpToName()
        viewModel.onEvent(OnboardingEvent.SetName("Lucía"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        capture("exam_date")
    }

    @Test
    fun captureProvincePicked() {
        answerUpToName()
        viewModel.onEvent(OnboardingEvent.SetName("Lucía"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SetExamDate(null))
        viewModel.onEvent(OnboardingEvent.SelectProvince("Valencia"))
        capture("province")
    }

    @Test
    fun captureLearningStyle() {
        answerUpToName()
        viewModel.onEvent(OnboardingEvent.SetName("Lucía"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SetExamDate(null))
        viewModel.onEvent(OnboardingEvent.SelectProvince("Valencia"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectWeeklyStudy(OnboardingConfig.WEEKLY_STUDY_DAILY))
        viewModel.onEvent(OnboardingEvent.SelectStudyTime(OnboardingConfig.studyTimes.keys.last()))
        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = false))
        capture("learning_style")
    }

    @Test
    fun capturePlanTop() = capturePlan("plan_top")

    @Test
    @Config(qualifiers = "w360dp-h3200dp-xhdpi")
    fun capturePlanWhole() = capturePlan("plan_whole")

    /**
     * Draws the plan for a student with an exam in four weeks who missed one question.
     *
     * @param name file name without extension
     */
    private fun capturePlan(name: String) {
        val questions = OnboardingQuiz.questionsFor(OnboardingConfig.CONCERN_SILLY_MISTAKES)
        val data = OnboardingData(
            name = "Lucía",
            experience = OnboardingConfig.EXPERIENCE_FIRST_TIME,
            concern = OnboardingConfig.CONCERN_SILLY_MISTAKES,
            examDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(27),
            province = "Valencia",
            weeklyStudy = OnboardingConfig.WEEKLY_STUDY_DAILY,
            studyTime = OnboardingConfig.studyTimes.keys.last(),
            notifications = NotificationsAnswer.GRANTED,
            learningPreference = OnboardingConfig.STYLE_MOCK_EXAMS.key,
            quizAnswers = questions.mapIndexed { index, question ->
                QuizAnswer(question.id, if (index == 1) 0 else question.correctIndex, isCorrect = index != 1)
            }
        )
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    StepPlanReveal(data = data, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000L)
        composeTestRule.onRoot().captureRoboImage("build/onboarding-steps/$name.png")
    }

    /**
     * Answers the first questions until [stop] is on screen.
     *
     * @param stop one of the diagnosis steps
     */
    private fun answerDiagnosisUpTo(stop: OnboardingStep) {
        with(viewModel) {
            onEvent(OnboardingEvent.GoToNextStep)
            onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.MOTIVATION_WORK))
            onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.BLOCKER_NO_PROGRESS))
            if (stop == OnboardingStep.Pain) return
            onEvent(OnboardingEvent.GoToNextStep)
            onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.EXPERIENCE_FIRST_TIME))
            if (stop == OnboardingStep.Comparison) return
            onEvent(OnboardingEvent.GoToNextStep)
            onEvent(OnboardingEvent.SelectReadiness(OnboardingConfig.readinessLevels.first()))
        }
    }

    /** Takes the mini-test for "detalles tontos", missing the second question, and lands on the result. */
    private fun answerQuiz() {
        answerDiagnosisUpTo(OnboardingStep.Concern)
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_SILLY_MISTAKES))
        viewModel.uiState.value.data.quizQuestions().forEachIndexed { index, question ->
            viewModel.onEvent(OnboardingEvent.AnswerQuiz(if (index == 1) 0 else question.correctIndex))
            viewModel.onEvent(OnboardingEvent.NextQuizQuestion)
        }
    }

    /** Takes the test and taps through the road and the gain, landing on the name. */
    private fun answerUpToName() {
        answerQuiz()
        repeat(3) { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
    }

    /**
     * Shows the onboarding where the ViewModel is, lets the mascot finish typing and saves it.
     *
     * @param name file name without extension
     */
    private fun capture(name: String, darkTheme: Boolean = false) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
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
        composeTestRule.mainClock.advanceTimeBy(5_000L)
        composeTestRule.onRoot().captureRoboImage("build/onboarding-steps/$name.png")
    }
}
