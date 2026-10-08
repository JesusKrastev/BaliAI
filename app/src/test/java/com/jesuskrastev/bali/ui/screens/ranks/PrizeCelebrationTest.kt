package com.jesuskrastev.bali.ui.screens.ranks

import androidx.activity.ComponentActivity
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
class PrizeCelebrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun reward(id: String) = RankProgression.rewardFor(id)!!

    @Test
    fun `a rank prize names the rank reached`() {
        val celebration = prizeCelebrationOf(reward("xp_300"), xp = 320, claimedIds = setOf("xp_300"), coinsAfter = 210)

        assertThat(celebration.rank?.name).isEqualTo("Explorador")
        assertThat(celebration.next?.id).isEqualTo("xp_500")
        assertThat(celebration.xpToNext).isEqualTo(180)
    }

    @Test
    fun `a prize on the way has no rank and counts the prizes still waiting`() {
        val celebration = prizeCelebrationOf(reward("xp_50"), xp = 220, claimedIds = setOf("xp_50"), coinsAfter = 70)

        assertThat(celebration.rank).isNull()
        assertThat(celebration.stillClaimable).isEqualTo(2) // xp_100 and xp_200
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

    @Test
    fun roadPrizeCelebration() {
        show(prizeCelebrationOf(reward("xp_500"), 560, setOf("xp_50", "xp_100", "xp_200", "xp_300", "xp_500"), 315), mutableListOf(), "road")

        composeTestRule.onNodeWithText("Por llegar a 500 XP").assertExists()
    }

    /** Draws the celebration without its dialog window, which Robolectric can't capture. */
    private fun capture(celebration: PrizeCelebration, name: String) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                PrizeCelebrationContent(celebration, shownCoins = celebration.reward.coins, badgeScale = 1f, canClose = true, onTap = {})
            }
        }
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/prize_celebration_$name.png")
    }

    @Test
    fun captureRankPrize() = capture(prizeCelebrationOf(reward("xp_300"), 320, setOf("xp_50", "xp_100", "xp_200", "xp_300"), 260), "rank")

    @Test
    fun captureRoadPrize() = capture(prizeCelebrationOf(reward("xp_500"), 560, setOf("xp_50", "xp_100", "xp_200", "xp_300", "xp_500"), 315), "road")

    @Test
    fun captureMorePrizesWaiting() = capture(prizeCelebrationOf(reward("xp_50"), 320, setOf("xp_50"), 70), "more_waiting")
}
