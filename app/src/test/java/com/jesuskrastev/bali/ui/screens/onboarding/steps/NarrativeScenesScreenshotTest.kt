package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
 * Captures the hand-drawn scenes of the emotional arc at the moments that matter: the red stamp
 * landing on a lost offer, and the green one on the offer won.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h400dp-xhdpi")
@RunWith(RobolectricTestRunner::class)
class NarrativeScenesScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(scene: NarrativeScene, atMillis: Long, name: String) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NarrativeSceneView(scene, Modifier.fillMaxWidth().height(260.dp))
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(atMillis)
        composeTestRule.onRoot().captureRoboImage("build/onboarding-steps/scene_$name.png")
    }

    @Test
    fun offerLostStamped() = capture(NarrativeScene.JobOffersLost, 1_600, "lost")

    @Test
    fun offerWon() = capture(NarrativeScene.JobOfferWon, 2_000, "won")
}
