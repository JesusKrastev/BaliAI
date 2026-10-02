package com.jesuskrastev.bali.ui.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.screens.stats.PlanSummary
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

/**
 * Captures Home's top bar with the plan chip and the plan sheet's content in each state, in light
 * and dark. The class is named `*ScreenshotTest` so release builds skip it (brain E-021).
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class HomePlanChipScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** A plan whose date is [daysLeft] days after Wednesday 7 October 2026. */
    private fun plan(daysLeft: Int, isExamDate: Boolean = false) = PlanSummary(
        targetMillis = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 7 + daysLeft)
        }.timeInMillis,
        isExamDate = isExamDate,
        daysLeft = daysLeft
    )

    /** Draws the top bar as Home does, with the chip at the start, over the sheet's content. */
    private fun capture(
        plan: PlanSummary,
        studiedToday: Boolean = false,
        datePassed: Boolean = false,
        darkTheme: Boolean = false
    ) {
        val copy = homePlanCopyOf(plan, datePassed, studiedToday)
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                // The app's Scaffold gives its content the on-background colour; do the same here.
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UserStatusRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            streak = 12,
                            practicedToday = studiedToday,
                            coinsCount = 1250,
                            leading = { PlanChip(copy = copy, onClick = {}) }
                        )
                        Box(Modifier.fillMaxWidth()) {
                            HomePlanSheetContent(copy = copy, canStartSession = true, onAction = {})
                        }
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun capturePlan_noDate() = capture(PlanSummary())

    @Test
    fun capturePlan_datePassed() = capture(PlanSummary(), datePassed = true)

    @Test
    fun capturePlan_farAwayPending() = capture(plan(45))

    @Test
    fun capturePlan_farAwayDone() = capture(plan(45), studiedToday = true)

    @Test
    fun capturePlan_finalWeekPending() = capture(plan(5, isExamDate = true))

    @Test
    fun capturePlan_tomorrow() = capture(plan(1, isExamDate = true))

    @Test
    fun capturePlan_examDay() = capture(plan(0, isExamDate = true))

    @Test
    fun capturePlan_darkFarAway() = capture(plan(30), darkTheme = true)

    @Test
    fun capturePlan_darkFinalWeekDone() = capture(plan(5, isExamDate = true), studiedToday = true, darkTheme = true)

    /** The narrowest phones: the chip has to fit beside the streak and the coins. */
    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h568dp-xxhdpi")
    fun capturePlan_smallScreen() = capture(plan(5, isExamDate = true))
}
