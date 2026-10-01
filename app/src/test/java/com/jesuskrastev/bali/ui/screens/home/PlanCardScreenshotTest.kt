package com.jesuskrastev.bali.ui.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import java.util.Calendar

/** Captures each state of Home's plan card, from far away to the day itself. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class PlanCardScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val target = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.NOVEMBER, 14)
    }.timeInMillis

    /** A Monday-to-Sunday strip with these statuses; "today" is [todayIndex]. */
    private fun week(todayIndex: Int, vararg statuses: StreakStatus) =
        statuses.mapIndexed { index, status ->
            DailyStreakState(
                dayOfWeek = "LMXJVSD"[index].toString(),
                dayOfMonth = index + 1,
                status = status,
                isToday = index == todayIndex
            )
        }

    private val thursdayPending = week(
        3,
        StreakStatus.COMPLETED, StreakStatus.FAILED, StreakStatus.COMPLETED,
        StreakStatus.TODAY, StreakStatus.FUTURE, StreakStatus.FUTURE, StreakStatus.FUTURE
    )

    private val thursdayDone = week(
        3,
        StreakStatus.COMPLETED, StreakStatus.FAILED, StreakStatus.COMPLETED,
        StreakStatus.COMPLETED, StreakStatus.FUTURE, StreakStatus.FUTURE, StreakStatus.FUTURE
    )

    private fun capture(
        name: String,
        plan: PlanSummary,
        week: List<DailyStreakState>,
        darkTheme: Boolean = false
    ) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    PlanCard(
                        plan = plan,
                        week = week,
                        weekSessions = week.count { it.status == StreakStatus.COMPLETED },
                        weeklyGoal = 5,
                        canStudy = true,
                        onStudyClick = {},
                        onDateClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/plan_card_$name.png")
    }

    @Test
    fun onTrack() = capture("on_track", PlanSummary(target, isExamDate = false, daysLeft = 45), thursdayPending)

    @Test
    fun onTrackDark() =
        capture("on_track_dark", PlanSummary(target, isExamDate = false, daysLeft = 45), thursdayPending, darkTheme = true)

    @Test
    fun finalWeekDone() = capture("final_week_done", PlanSummary(target, isExamDate = true, daysLeft = 5), thursdayDone)

    @Test
    fun examToday() = capture("exam_today", PlanSummary(target, isExamDate = true, daysLeft = 0), thursdayPending)

    @Test
    fun noDate() = capture("no_date", PlanSummary(), thursdayPending)
}
