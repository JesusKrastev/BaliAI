package com.jesuskrastev.bali.ui.screens.home

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HomePlanTest {

    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun useSpanishTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    /** Local instant for the given day at [hour]:00. Months are 1-based. */
    private fun at(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, 0)
        }.timeInMillis

    private val now = at(2026, 9, 30, hour = 18)

    @Test
    fun `a booked exam keeps the onboarding promise`() {
        val estimate = at(2026, 10, 10, hour = 18)

        val plan = planSummaryOf(examDateMillis = estimate, planTargetMillis = estimate, now = now)

        assertThat(plan).isEqualTo(PlanSummary(estimate, isExamDate = false, daysLeft = 10))
    }

    @Test
    fun `without an exam the card counts down to the plan date`() {
        val promise = at(2026, 11, 25, hour = 18)

        val plan = planSummaryOf(examDateMillis = null, planTargetMillis = promise, now = now)

        assertThat(plan).isEqualTo(PlanSummary(promise, isExamDate = false, daysLeft = 56))
    }

    @Test
    fun `an exam date the student set takes over from the promise`() {
        val exam = at(2026, 10, 20)
        val promise = at(2026, 11, 25, hour = 18)

        val plan = planSummaryOf(examDateMillis = exam, planTargetMillis = promise, now = now)

        assertThat(plan).isEqualTo(PlanSummary(exam, isExamDate = true, daysLeft = 20))
    }

    @Test
    fun `the exam day itself still counts as ahead`() {
        val exam = at(2026, 9, 30)

        val plan = planSummaryOf(examDateMillis = exam, planTargetMillis = null, now = now)

        assertThat(plan).isEqualTo(PlanSummary(exam, isExamDate = true, daysLeft = 0))
    }

    @Test
    fun `a past exam asks for a new date instead of falling back to the promise`() {
        val exam = at(2026, 9, 20)
        val promise = at(2026, 11, 25, hour = 18)

        val plan = planSummaryOf(examDateMillis = exam, planTargetMillis = promise, now = now)

        assertThat(plan.targetMillis).isNull()
    }

    @Test
    fun `with no date at all the card asks for one`() {
        assertThat(planSummaryOf(examDateMillis = null, planTargetMillis = null, now = now).targetMillis)
            .isNull()
    }

    @Test
    fun `a daylight saving change does not lose a day`() {
        // Spain moves the clocks forward on 28 March 2027, so that day lasts 23 hours.
        assertThat(calendarDaysBetween(at(2027, 3, 20, hour = 12), at(2027, 4, 1))).isEqualTo(12)
    }

    @Test
    fun `a picked date lands on the same local day and back`() {
        val pickerMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 14)
        }.timeInMillis

        val localDay = localDayFromPickerMillis(pickerMillis)

        assertThat(localDay).isEqualTo(at(2026, 11, 14))
        assertThat(pickerMillisFromLocalDay(localDay)).isEqualTo(pickerMillis)
    }

    @Test
    fun `the card turns to its final stretch in the last week`() {
        val date = at(2026, 10, 20)

        assertThat(PlanSummary().urgency()).isEqualTo(PlanUrgency.NO_DATE)
        assertThat(PlanSummary(date, daysLeft = 8).urgency()).isEqualTo(PlanUrgency.ON_TRACK)
        assertThat(PlanSummary(date, daysLeft = 7).urgency()).isEqualTo(PlanUrgency.FINAL_WEEK)
        assertThat(PlanSummary(date, daysLeft = 1).urgency()).isEqualTo(PlanUrgency.FINAL_WEEK)
        assertThat(PlanSummary(date, daysLeft = 0).urgency()).isEqualTo(PlanUrgency.TODAY)
    }

    /** A Monday-to-Sunday strip with these statuses; "today" is the day marked TODAY, or [todayIndex]. */
    private fun week(vararg statuses: StreakStatus, todayIndex: Int = statuses.indexOf(StreakStatus.TODAY)) =
        statuses.mapIndexed { index, status ->
            DailyStreakState(dayOfWeek = "LMXJVSD"[index].toString(), dayOfMonth = index + 1, status = status, isToday = index == todayIndex)
        }

    @Test
    fun `today still counts as a day left until it has a session`() {
        val thursdayPending = week(
            StreakStatus.COMPLETED, StreakStatus.FAILED, StreakStatus.COMPLETED,
            StreakStatus.TODAY, StreakStatus.FUTURE, StreakStatus.FUTURE, StreakStatus.FUTURE
        )

        val pace = weekPaceOf(thursdayPending, sessions = 2, weeklyGoal = 5)

        assertThat(pace).isEqualTo(WeekPace(sessions = 2, goal = 5, daysLeft = 4, practicedToday = false))
        assertThat(pace.missing).isEqualTo(3)
    }

    @Test
    fun `once today has a session only the days after it are left`() {
        val thursdayDone = week(
            StreakStatus.COMPLETED, StreakStatus.FAILED, StreakStatus.COMPLETED,
            StreakStatus.COMPLETED, StreakStatus.FUTURE, StreakStatus.FUTURE, StreakStatus.FUTURE,
            todayIndex = 3
        )

        val pace = weekPaceOf(thursdayDone, sessions = 3, weeklyGoal = 5)

        assertThat(pace).isEqualTo(WeekPace(sessions = 3, goal = 5, daysLeft = 3, practicedToday = true))
    }

    @Test
    fun `the pace line says what is missing and how much time is left`() {
        fun message(sessions: Int, goal: Int, daysLeft: Int) =
            weekPaceMessage(WeekPace(sessions, goal, daysLeft, practicedToday = false))

        assertThat(message(sessions = 2, goal = 5, daysLeft = 4)).isEqualTo("Te faltan 3 sesiones y quedan 4 días")
        assertThat(message(sessions = 4, goal = 5, daysLeft = 1)).isEqualTo("Te falta 1 sesión y queda 1 día: que no se te pase")
        assertThat(message(sessions = 2, goal = 5, daysLeft = 3)).isEqualTo("Te faltan 3 sesiones y quedan 3 días: no te saltes ninguno")
        assertThat(message(sessions = 1, goal = 5, daysLeft = 2)).isEqualTo("Te faltan 4 sesiones y solo quedan 2 días")
        assertThat(message(sessions = 3, goal = 5, daysLeft = 0)).isEqualTo("Esta semana te has quedado a 2 sesiones. El lunes, otra oportunidad.")
        assertThat(message(sessions = 6, goal = 5, daysLeft = 2)).isEqualTo("¡Objetivo de la semana cumplido!")
    }
}
