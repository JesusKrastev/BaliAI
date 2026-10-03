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
import com.jesuskrastev.bali.domain.model.ChestReward
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
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
class ChestOpeningOverlayScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Shows the overlay for [reward] and counts the times it is dismissed in [dismissals]. */
    private fun showOverlay(reward: ChestReward, dismissals: MutableList<Unit>) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                ChestOpeningOverlay(reward = reward, onDismiss = { dismissals += Unit })
            }
        }
    }

    /** Verifies the closed phase blocks collection until the opening sequence completes. */
    @Test
    fun theChestOpensBeforeTheUserCanCollectThePrize() {
        showOverlay(reward = ChestReward.Coins(73), dismissals = mutableListOf())

        composeTestRule.onNodeWithText("Abriendo cofre…").assertExists()
        composeTestRule.onNodeWithText("RECOGER").assertIsNotEnabled()
        composeTestRule.onNodeWithText("+73").assertDoesNotExist()

        composeTestRule.waitUntilExactlyOneExists(hasText("+73"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithText("¡Has ganado 73 monedas!").assertExists()
        composeTestRule.onNodeWithText("Abriendo cofre…").assertDoesNotExist()
        composeTestRule.onNodeWithText("RECOGER").assertIsEnabled()
    }

    /** Verifies the bundled Lottie keeps the markers required by the overlay state machine. */
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

    /** Verifies collecting an already revealed reward dismisses the overlay exactly once. */
    @Test
    fun collectingThePrizeDismissesTheOverlay() {
        val dismissals = mutableListOf<Unit>()
        showOverlay(reward = ChestReward.Coins(40), dismissals = dismissals)
        composeTestRule.waitUntilExactlyOneExists(hasText("+40"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithText("RECOGER").performClick()

        assertThat(dismissals).hasSize(1)
    }

    /** Verifies inventory prizes use their own label and accessible result message. */
    @Test
    fun anInventoryRewardShowsItsOwnItemAndMessage() {
        showOverlay(
            reward = ChestReward.Inventory(ShopInventoryItem.DOUBLE_XP),
            dismissals = mutableListOf()
        )

        composeTestRule.waitUntilExactlyOneExists(hasText("Doble XP"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithText("¡Recompensa rara: doble XP!").assertExists()
        composeTestRule.onNodeWithText("RECOGER").assertIsEnabled()
    }
}
