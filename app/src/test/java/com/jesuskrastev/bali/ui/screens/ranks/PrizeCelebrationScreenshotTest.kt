package com.jesuskrastev.bali.ui.screens.ranks

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class PrizeCelebrationScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun reward(id: String) = RankProgression.rewardFor(id)!!

    /** Names the earned rank and the next newly introduced consumable prize. */
    @Test
    fun `a rank prize names the rank reached`() {
        val celebration = prizeCelebrationOf(reward("xp_300"), xp = 320, claimedIds = setOf("xp_300"), coinsAfter = 210)

        assertThat(celebration.rank?.name).isEqualTo("Explorador")
        assertThat(celebration.next?.id).isEqualTo("double_coins_400")
        assertThat(celebration.xpToNext).isEqualTo(80)
    }

    /** Includes newly added inventory prizes when counting unclaimed milestones. */
    @Test
    fun `a prize on the way has no rank and counts the prizes still waiting`() {
        val celebration = prizeCelebrationOf(reward("xp_50"), xp = 220, claimedIds = setOf("xp_50"), coinsAfter = 70)

        assertThat(celebration.rank).isNull()
        assertThat(celebration.stillClaimable).isEqualTo(5) // Three new aids plus the two old coin prizes.
    }

    @Test
    fun `the starting rank never counts as a rank prize`() {
        val celebration = prizeCelebrationOf(reward("xp_50"), xp = 60, claimedIds = setOf("xp_50"), coinsAfter = 20)

        assertThat(celebration.rank).isNull()
    }

    private fun show(celebration: PrizeCelebration, dismissals: MutableList<Unit>, name: String) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                PrizeCelebrationOverlay(celebration = celebration, onDismiss = { dismissals += Unit })
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun rankPrizeCelebration() {
        val dismissals = mutableListOf<Unit>()
        show(prizeCelebrationOf(reward("xp_300"), 320, setOf("xp_50", "xp_100", "xp_200", "xp_300"), 260), dismissals, "rank")

        composeTestRule.onNodeWithText("¡Ya eres Explorador!").assertExists()
        composeTestRule.onNodeWithText("+50").assertExists()
        composeTestRule.onNodeWithTag(PRIZE_CELEBRATION_TAG).performClick()

        assertThat(dismissals).hasSize(1)
    }

    /** Names the XP milestone for an intermediate coin prize. */
    @Test
    fun roadPrizeCelebration() {
        show(prizeCelebrationOf(reward("xp_1900"), 2000, emptySet(), 315), mutableListOf(), "road")

        composeTestRule.onNodeWithText("Por llegar a 1900 XP").assertExists()
    }

    /** Shows a double-XP prize with its real quantity and no zero-coin counter. */
    @Test
    fun inventoryPrizeCelebration() {
        show(prizeCelebrationOf(reward("double_xp_250"), 260, emptySet(), 100), mutableListOf(), "inventory")
        composeTestRule.onNodeWithText("+1 doble XP").assertExists()
        composeTestRule.onNodeWithText("+0").assertDoesNotExist()
        composeTestRule.onNodeWithText(reward("double_xp_250").usageText()).assertExists()
    }

    /** Shows every guaranteed item in a pack after it has been added to the inventory. */
    @Test
    fun practicePackCelebration() {
        show(prizeCelebrationOf(reward("practice_pack_600"), 610, emptySet(), 100), mutableListOf(), "pack")
        composeTestRule.onNodeWithText("+2 pistas").assertExists()
        composeTestRule.onNodeWithText("+1 50/50").assertExists()
        composeTestRule.onNodeWithText("+0").assertDoesNotExist()
    }

    /** Captures the actual inventory prize celebration for visual inspection. */
    @Test
    fun captureInventoryPrize() = capture(prizeCelebrationOf(reward("double_xp_250"), 260, emptySet(), 100), "inventory")

    /** Checks the pack celebration at a narrow screen width and large font size. */
    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h640dp-xhdpi")
    fun captureNarrowPack() = capture(prizeCelebrationOf(reward("practice_pack_600"), 610, emptySet(), 100), "narrow_pack", fontScale = 1.3f)

    /** Captures [celebration] under [name], with [fontScale] for accessibility checks; returns Unit. */
    /* Draws the celebration without its dialog window, which Robolectric can't capture. */
    private fun capture(celebration: PrizeCelebration, name: String, fontScale: Float = 1f) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                BaliTheme(darkTheme = true) {
                    PrizeCelebrationContent(celebration, shownCoins = celebration.reward.coins, badgeScale = 1f, canClose = true, onTap = {})
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/prize_celebration_$name.png")
    }

    @Test
    fun captureRankPrize() = capture(prizeCelebrationOf(reward("xp_300"), 320, setOf("xp_50", "xp_100", "xp_200", "xp_300"), 260), "rank")

    @Test
    fun captureRoadPrize() = capture(prizeCelebrationOf(reward("xp_1900"), 2000, emptySet(), 315), "road")

    @Test
    fun captureMorePrizesWaiting() = capture(prizeCelebrationOf(reward("xp_50"), 320, setOf("xp_50"), 70), "more_waiting")
}
