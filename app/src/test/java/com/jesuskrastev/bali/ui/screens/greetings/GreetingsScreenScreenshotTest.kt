package com.jesuskrastev.bali.ui.screens.greetings

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
 * Screenshot test for GreetingsScreen using Roborazzi
 * Run with: ./gradlew recordRoborazziDebug
 * Verify with: ./gradlew verifyRoborazziDebug
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp")
@RunWith(RobolectricTestRunner::class)
class GreetingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Captures the fully settled welcome screen after its entrance animations complete. */
    @Test
    fun captureGreetingsScreen() {
        captureSettledGreetings()
    }

    /**
     * Captures the welcome screen on a small 360x800dp phone, where the hero, copy and buttons
     * have the least room and any excess spacing is most visible.
     */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun captureGreetingsScreenSmallPhone() {
        captureSettledGreetings()
    }

    /** Renders the welcome screen, lets its entrance animations settle and captures it. */
    private fun captureSettledGreetings() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        GreetingsScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onStartClick = {},
                            onAuthClick = {}
                        )
                    }
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(3_000L)
        composeTestRule.onRoot().captureRoboImage()
    }
}
