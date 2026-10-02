package com.jesuskrastev.bali.ui.screens.test

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures of the answer feedback: the run label at each tier and, after the animations settle,
 * the label that just crossed five and the option card of a correct answer. The class name ends in `ScreenshotTest` because the release build
 * only keeps those (brain E-021).
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class AnswerAnimationScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureComboLabel_everyTier() {
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(3, 7, 12).forEach { run ->
                        QuizProgressTitle(
                            currentIndex = run,
                            totalQuestions = 30,
                            sessionStreak = run,
                            isAnswerChecked = true
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureComboLabel_afterCrossingFive() {
        var run by mutableStateOf(4)
        var checked by mutableStateOf(false)
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) {
                Column(modifier = Modifier.padding(16.dp)) {
                    QuizProgressTitle(
                        currentIndex = 4,
                        totalQuestions = 30,
                        sessionStreak = run,
                        isAnswerChecked = checked
                    )
                }
            }
        }
        composeTestRule.runOnUiThread {
            run = 5
            checked = true
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureOptionCard_afterCorrectAnswerChecked() {
        var checked by mutableStateOf(false)
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OptionCard(
                        text = "Reducir la velocidad y ceder el paso",
                        isSelected = true,
                        isCorrect = if (checked) true else null,
                        wasSelectedAndIncorrect = false,
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.runOnUiThread { checked = true }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }
}
