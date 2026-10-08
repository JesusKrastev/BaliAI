package com.jesuskrastev.bali.ui.screens.ranks

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

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class RankRewardsScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String, xp: Int, claimed: Set<String>, darkTheme: Boolean) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                RankRewardsContent(
                    state = RankRewardsUiState(xp = xp, claimedIds = claimed, isLoaded = true),
                    onBackClick = {},
                    onClaim = {},
                    onMessageShown = {}
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/rank_road_$name.png")
    }

    /** Mid-road on a phone: one prize to collect, the road scrolled to it. */
    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneLight() = capture("phone_light", xp = 560, claimed = setOf("xp_50", "xp_100", "xp_200", "xp_300"), darkTheme = false)

    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
    fun phoneDark() = capture("phone_dark", xp = 560, claimed = setOf("xp_50", "xp_100", "xp_200", "xp_300"), darkTheme = true)

    /** The first part of the road for a new user, tall enough to see several ranks. */
    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h2600dp-xhdpi")
    fun newUserTall() = capture("new_user_tall", xp = 70, claimed = emptySet(), darkTheme = false)
}
