package com.jesuskrastev.bali.ui.screens.test

import androidx.activity.ComponentActivity
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

/** Captures the result screen's big celebrations: the passed-exam stamp and the level-up overlay. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class ResultCelebrationsScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Shows a result and captures it after [atMillis] of animation.
     *
     * @param name file name suffix of the capture
     * @param atMillis animation time to let pass before the capture
     */
    private fun capture(
        name: String,
        atMillis: Long,
        darkTheme: Boolean = false,
        isPassedExam: Boolean = false,
        leveledUp: Boolean = false,
        score: Int = 28,
        total: Int = 30
    ) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                TestResultScreen(
                    xpGained = 64,
                    baseXp = 50,
                    bonusFast = 4,
                    bonusStreak = 10,
                    leveledUp = leveledUp,
                    newLevel = 7,
                    durationSeconds = 1_284,
                    accuracy = score * 100 / total,
                    score = score,
                    total = total,
                    isPassedExam = isPassedExam,
                    onContinueClick = {}
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(atMillis)
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/result_$name.png")
    }

    @Test
    fun stampFalling() = capture("stamp_falling", atMillis = 450, isPassedExam = true)

    @Test
    fun passedExam() = capture("passed_exam", atMillis = 1_400, isPassedExam = true)

    @Test
    fun passedExamDark() = capture("passed_exam_dark", atMillis = 1_400, darkTheme = true, isPassedExam = true)

    @Test
    fun levelUp() = capture("level_up", atMillis = 1_500, leveledUp = true, score = 8, total = 10)
}
