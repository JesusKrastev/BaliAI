package com.jesuskrastev.bali.ui.screens.stats

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

/** Captures the statistics countdown card in each stretch, from far away to the exam day, and without a date. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class ExamCountdownScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Wednesday 7 October 2026, 18:00: a fixed "now" so the dates look the same every run. */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 7, 18, 0) }.timeInMillis

    private val noVerdict = CalculateProgressStatsUseCase(CalculateReadinessUseCase())
        .invoke(emptyList(), emptyList(), User(), now).readiness

    /** A plan whose date is [daysLeft] days after [now]. */
    private fun plan(daysLeft: Int, isExamDate: Boolean = true) = PlanSummary(
        targetMillis = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 7 + daysLeft)
        }.timeInMillis,
        isExamDate = isExamDate,
        daysLeft = daysLeft
    )

    private fun capture(plan: PlanSummary, studiedToday: Boolean = false, darkTheme: Boolean = false) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).padding(20.dp)) {
                    ExamCountdownCard(
                        plan = plan,
                        readiness = noVerdict,
                        studiedToday = studiedToday,
                        onDateClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureCountdown_farAwayPromise() = capture(plan(45, isExamDate = false))

    @Test
    fun captureCountdown_lastMonth() = capture(plan(21))

    @Test
    fun captureCountdown_twoWeeksPending() = capture(plan(12))

    @Test
    fun captureCountdown_twoWeeksStudied() = capture(plan(12), studiedToday = true)

    @Test
    fun captureCountdown_finalWeekPending() = capture(plan(5))

    @Test
    fun captureCountdown_finalWeekStudied() = capture(plan(5), studiedToday = true)

    @Test
    fun captureCountdown_tomorrow() = capture(plan(1))

    @Test
    fun captureCountdown_today() = capture(plan(0))

    @Test
    fun captureCountdown_noDate() = capture(PlanSummary())

    @Test
    fun captureCountdown_darkThemeTwoWeeks() = capture(plan(12), darkTheme = true)

    @Test
    fun captureCountdown_darkThemeFinalWeek() = capture(plan(5), darkTheme = true)
}
