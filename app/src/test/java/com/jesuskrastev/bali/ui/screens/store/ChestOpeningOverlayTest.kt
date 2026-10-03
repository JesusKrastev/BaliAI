package com.jesuskrastev.bali.ui.screens.store

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.airbnb.lottie.LottieCompositionFactory
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class ChestOpeningOverlayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Shows the overlay for [prize] and counts the times it is dismissed in [dismissals]. */
    private fun showOverlay(prize: Int, dismissals: MutableList<Unit>) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                ChestOpeningOverlay(prize = prize, onDismiss = { dismissals += Unit })
            }
        }
    }

    @Test
    fun theChestOpensBeforeTheUserCanCollectThePrize() {
        showOverlay(prize = 73, dismissals = mutableListOf())

        composeTestRule.onNodeWithText("Abriendo cofre…").assertExists()
        composeTestRule.onNodeWithText("RECOGER").assertIsNotEnabled()
        composeTestRule.onNodeWithText("+73").assertDoesNotExist()

        composeTestRule.waitUntilExactlyOneExists(hasText("+73"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithText("¡Has ganado 73 monedas!").assertExists()
        composeTestRule.onNodeWithText("Abriendo cofre…").assertDoesNotExist()
        composeTestRule.onNodeWithText("RECOGER").assertIsEnabled()
    }

    /** The overlay plays these markers by name; a renamed one would make it fall back silently. */
    @Test
    fun theAnimationFileHasTheThreeMarkersTheOverlayPlays() {
        val result = LottieCompositionFactory.fromRawResSync(
            composeTestRule.activity,
            R.raw.bali_chest_opening
        )

        val composition = result.value
        assertThat(composition).isNotNull()
        listOf("closed_idle", "opening", "opened_idle").forEach { marker ->
            assertThat(composition!!.getMarker(marker)).isNotNull()
        }
    }

    @Test
    fun collectingThePrizeDismissesTheOverlay() {
        val dismissals = mutableListOf<Unit>()
        showOverlay(prize = 40, dismissals = dismissals)
        composeTestRule.waitUntilExactlyOneExists(hasText("+40"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithText("RECOGER").performClick()

        assertThat(dismissals).hasSize(1)
    }
}
