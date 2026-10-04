package com.jesuskrastev.bali.ui.screens.test

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures of the practice quiz with the aid chips in the top bar (a hint revealed, a 50/50 used)
 * and of Bali's cheer on a run of correct answers. The class name ends in `ScreenshotTest` because
 * the release build only keeps those (brain E-021).
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class PracticeAidsScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val question = QuestionUiState(
        text = "¿Qué debe hacer al aproximarse a un paso para peatones sin semáforo?",
        options = listOf(
            "Reducir la velocidad y ceder el paso",
            "Tocar el claxon para avisar",
            "Mantener la velocidad",
            "Acelerar para pasar antes"
        ),
        correctAnswerIndex = 0,
        explanation = "Ante un paso de peatones debes moderar la velocidad y detenerte si hay peatones cruzando o a punto de hacerlo."
    )

    @Test
    fun captureAids_beforeUsing() {
        composeTestRule.setContent {
            Quiz(TestUiState(questions = List(10) { question }, currentQuestionIndex = 2, isLoading = false, hints = 3, fiftyFifties = 2))
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureAids_hintAndFiftyFiftyInUse() {
        composeTestRule.setContent {
            Quiz(
                TestUiState(
                    questions = List(10) { question },
                    currentQuestionIndex = 2,
                    isLoading = false,
                    hints = 2,
                    fiftyFifties = 0,
                    isHintVisible = true,
                    eliminatedOptionIndices = setOf(2, 3)
                )
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureCheer_onFifthInARow() {
        var state by mutableStateOf(
            TestUiState(
                questions = List(10) { question },
                currentQuestionIndex = 4,
                selectedAnswers = mapOf(4 to 0),
                isLoading = false,
                sessionStreak = 4,
                hints = 1,
                fiftyFifties = 1
            )
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Quiz(state, withCheer = true) }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.runOnUiThread { state = state.copy(isAnswerChecked = true, sessionStreak = 5) }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onRoot().captureRoboImage()
    }

    /** The practice quiz as `TestScreen` lays it out, without its ViewModel. */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Quiz(state: TestUiState, withCheer: Boolean = false) {
        BaliTheme(darkTheme = false) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            QuizProgressTitle(
                                currentIndex = state.currentQuestionIndex,
                                totalQuestions = state.questions.size,
                                sessionStreak = state.sessionStreak,
                                isAnswerChecked = state.isAnswerChecked
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {}) { Icon(Icons.Rounded.Close, contentDescription = null) }
                        },
                        actions = {
                            PracticeAidChips(
                                hints = state.hints,
                                fiftyFifties = state.fiftyFifties,
                                isHintVisible = state.isHintVisible,
                                isFiftyFiftyUsed = state.eliminatedOptionIndices.isNotEmpty(),
                                isAnswerChecked = state.isAnswerChecked,
                                onUseHint = {},
                                onUseFiftyFifty = {}
                            )
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    TestContentView(uiState = state, onOptionSelect = {}, onCheckClick = {}, onNextClick = {})
                    if (withCheer) {
                        StreakCheer(
                            currentIndex = state.currentQuestionIndex,
                            sessionStreak = state.sessionStreak,
                            isAnswerChecked = state.isAnswerChecked,
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 92.dp)
                        )
                    }
                }
            }
        }
    }
}
