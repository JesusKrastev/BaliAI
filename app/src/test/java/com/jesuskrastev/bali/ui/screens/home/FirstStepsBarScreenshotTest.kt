package com.jesuskrastev.bali.ui.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of every face of the first-steps bar.
 * Record with: ./gradlew recordRoborazziDebug
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp")
@RunWith(RobolectricTestRunner::class)
class FirstStepsBarScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val oneDone = FirstStepsProgress.startingAt(1_000L).copy(completed = setOf(FirstStepTask.FIRST_TEST))

    /**
     * Draws the bar on the app background, with room around it for its shadow.
     *
     * @param progress the card's progress
     * @param reward coins waiting to be celebrated
     * @param darkTheme whether to use the dark colour scheme
     */
    @Composable
    private fun BarOnBackground(
        progress: FirstStepsProgress?,
        reward: FirstStepReward? = null,
        darkTheme: Boolean = false,
    ) {
        BaliTheme(darkTheme = darkTheme) {
            Box(
                modifier = Modifier
                    .testTag(PREVIEW_TAG)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 24.dp, bottom = 16.dp)
            ) {
                FirstStepsBar(
                    progress = progress,
                    reward = reward,
                    onTaskClick = {},
                    onExamClick = {},
                    onDismissClick = {},
                    onRewardShown = {},
                    onShown = {},
                )
            }
        }
    }

    @Test
    fun captureFolded() {
        composeTestRule.setContent { BarOnBackground(oneDone) }
        composeTestRule.onNodeWithTag(PREVIEW_TAG).captureRoboImage()
    }

    @Test
    fun captureFoldedDark() {
        composeTestRule.setContent { BarOnBackground(oneDone, darkTheme = true) }
        composeTestRule.onNodeWithTag(PREVIEW_TAG).captureRoboImage()
    }

    @Test
    fun captureUnfolded() {
        composeTestRule.setContent { BarOnBackground(oneDone) }
        composeTestRule.onNodeWithText("Tus primeros pasos").performClick()
        composeTestRule.onNodeWithTag(PREVIEW_TAG).captureRoboImage()
    }

    @Test
    fun captureExamInvitation() {
        composeTestRule.setContent {
            BarOnBackground(FirstStepsProgress.startingAt(1_000L).copy(completed = FirstStepTask.entries.toSet()))
        }
        composeTestRule.onNodeWithTag(PREVIEW_TAG).captureRoboImage()
    }

    @Test
    fun captureCelebration() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BarOnBackground(oneDone, reward = FirstStepReward(FirstStepTask.FIRST_TEST, coins = 30, completedAll = false))
        }
        composeTestRule.mainClock.advanceTimeBy(900)
        composeTestRule.onNodeWithTag(PREVIEW_TAG).captureRoboImage()
    }

    private companion object {
        const val PREVIEW_TAG = "first_steps_bar_preview"
    }
}
