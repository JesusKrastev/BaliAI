package com.jesuskrastev.bali.ui.screens.store

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

    /** Verifies the shut chest waits for a tap: no prize and no button until the user opens it. */
    @Test
    fun theChestWaitsForATapBeforeOpening() {
        showOverlay(reward = ChestReward.Coins(73), dismissals = mutableListOf())

        composeTestRule.waitUntilExactlyOneExists(hasText("¡Toca el cofre para abrirlo!"), timeoutMillis = 15_000)
        composeTestRule.mainClock.advanceTimeBy(3_000)

        composeTestRule.onNodeWithText("+73").assertDoesNotExist()
        composeTestRule.onNodeWithText("RECOGER").assertDoesNotExist()

        composeTestRule.onNodeWithTag(CHEST_OVERLAY_TAG).performClick()
        composeTestRule.waitUntilExactlyOneExists(hasText("+73"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithText("¡Has ganado 73 monedas!").assertExists()
        composeTestRule.onNodeWithText("¡Toca el cofre para abrirlo!").assertDoesNotExist()
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

    /** Verifies a tap opens the chest and a second tap, once the prize is out, closes it once. */
    @Test
    fun aSecondTapAfterThePrizeClosesTheOverlay() {
        val dismissals = mutableListOf<Unit>()
        showOverlay(reward = ChestReward.Coins(40), dismissals = dismissals)
        composeTestRule.waitUntilExactlyOneExists(hasText("¡Toca el cofre para abrirlo!"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithTag(CHEST_OVERLAY_TAG).performClick()
        composeTestRule.waitUntilExactlyOneExists(hasText("+40"), timeoutMillis = 15_000)
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Toca para continuar").assertExists()
        assertThat(dismissals).isEmpty()

        composeTestRule.onNodeWithTag(CHEST_OVERLAY_TAG).performClick()

        assertThat(dismissals).hasSize(1)
    }

    /** Verifies inventory prizes use their own label and accessible result message. */
    @Test
    fun anInventoryRewardShowsItsOwnItemAndMessage() {
        showOverlay(
            reward = ChestReward.Inventory(ShopInventoryItem.DOUBLE_XP),
            dismissals = mutableListOf()
        )
        composeTestRule.waitUntilExactlyOneExists(hasText("¡Toca el cofre para abrirlo!"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithTag(CHEST_OVERLAY_TAG).performClick()
        composeTestRule.waitUntilExactlyOneExists(hasText("Doble XP"), timeoutMillis = 15_000)

        composeTestRule.onNodeWithText("¡Recompensa rara: doble XP!").assertExists()
    }
}
