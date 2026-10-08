package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.screens.exam.ExamBottomBar
import com.jesuskrastev.bali.ui.screens.exam.ExamContent
import com.jesuskrastev.bali.ui.screens.exam.ExamUiState
import com.jesuskrastev.bali.ui.screens.games.GamesScreen
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import com.jesuskrastev.bali.ui.screens.test.QuizProgressTitle
import com.jesuskrastev.bali.ui.screens.test.TestContentView
import com.jesuskrastev.bali.ui.screens.test.TestUiState
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real screens the onboarding intro shows as "this is what you get", in the dark theme and
 * with sample data, so the pictures in `res/drawable-nodpi/onboarding_shot_*.webp` come from the app itself.
 *
 * To refresh them after a redesign: `./gradlew recordRoborazziDebug --tests "*OnboardingShowcaseScreenshotTest"`
 * plus `StatsScreenScreenshotTest.captureOnboardingShowcase` for the statistics, then convert the PNGs
 * in `app/build/onboarding-showcase/` to WebP at 450×975.
 */
@OptIn(ExperimentalMaterial3Api::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xhdpi")
@RunWith(RobolectricTestRunner::class)
class OnboardingShowcaseScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** A practice question answered wrong, with its explanation open: the feature users value most. */
    @Test
    fun capturePracticeWithExplanation() {
        val question = QuestionUiState(
            text = "Con lluvia o asfalto mojado, la distancia de seguridad debe ser…",
            options = listOf(
                "La misma que en seco",
                "Mayor, porque la distancia de frenado aumenta",
                "Menor, porque el agua frena el vehículo"
            ),
            correctAnswerIndex = 1,
            explanation = "En mojado los neumáticos agarran menos y la distancia de frenado puede " +
                "duplicarse. Por eso hay que dejar más espacio con el coche de delante: con lluvia, " +
                "cuenta al menos 4 segundos en vez de 2."
        )
        // Checked only after the first frame, as in the app: the screen scrolls to the explanation
        // when the answer is checked, and there is nothing to scroll to before it is laid out.
        var state by mutableStateOf(
            TestUiState(
                questions = List(10) { question },
                currentQuestionIndex = 3,
                selectedAnswers = mapOf(3 to 0),
                isAnswerChecked = false,
                isLoading = false
            )
        )

        capture("shot_practice", beforeCapture = { state = state.copy(isAnswerChecked = true) }) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            QuizProgressTitle(
                                currentIndex = state.currentQuestionIndex,
                                totalQuestions = state.questions.size,
                                sessionStreak = 0,
                                isAnswerChecked = state.isAnswerChecked
                            )
                        },
                        navigationIcon = { CloseIcon() }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    TestContentView(
                        uiState = state,
                        onOptionSelect = {},
                        onCheckClick = {},
                        onNextClick = {}
                    )
                }
            }
        }
    }

    /** A mock exam halfway through, with its 30-minute clock running. */
    @Test
    fun captureMockExam() {
        val question = QuestionUiState(
            text = "¿Cuál es la velocidad máxima para un turismo en autopista?",
            options = listOf("120 km/h", "130 km/h", "110 km/h"),
            correctAnswerIndex = 0,
            explanation = ""
        )
        val state = ExamUiState(
            questions = List(30) { question },
            currentQuestionIndex = 11,
            selectedAnswers = mapOf(11 to 0),
            isLoading = false,
            timeLeftSeconds = 18 * 60 + 42
        )

        capture("shot_exam") {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            QuizProgressTitle(
                                currentIndex = state.currentQuestionIndex,
                                totalQuestions = state.questions.size,
                                sessionStreak = 0,
                                isAnswerChecked = false
                            ) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "18:42",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        },
                        navigationIcon = { CloseIcon() },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(Icons.Rounded.GridView, contentDescription = null)
                            }
                        }
                    )
                },
                bottomBar = {
                    ExamBottomBar(
                        currentIndex = state.currentQuestionIndex,
                        totalCount = state.questions.size,
                        isAnswerChecked = false,
                        onCheck = {},
                        onPrevious = {},
                        onNext = {},
                        onFinish = {}
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    ExamContent(uiState = state, onOptionSelect = {})
                }
            }
        }
    }

    /** The dedicated Bali Drive entry, including its live preview and play button. */
    @Test
    fun captureGames() = capture("shot_games") {
        GamesScreen(onGameClick = {})
    }

    /** The close button every quiz screen carries in its top bar. */
    @Composable
    private fun CloseIcon() {
        IconButton(onClick = {}) {
            Icon(Icons.Rounded.Close, contentDescription = null)
        }
    }

    /**
     * Draws [content] in the dark theme and saves it under `build/onboarding-showcase/`.
     *
     * @param name file name without extension
     * @param beforeCapture state change applied once the first frame is laid out
     * @param content the screen to draw
     */
    private fun capture(name: String, beforeCapture: () -> Unit = {}, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        composeTestRule.waitForIdle()
        beforeCapture()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("build/onboarding-showcase/$name.png")
    }
}
