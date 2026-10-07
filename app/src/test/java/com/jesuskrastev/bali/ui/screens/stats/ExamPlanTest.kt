package com.jesuskrastev.bali.ui.screens.stats

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExamPlanTest {

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
    fun `without an exam the countdown goes to the plan date`() {
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
    fun `with no date at all the countdown asks for one`() {
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
    fun `the countdown escalates as the date approaches and turns red in the last week`() {
        val date = at(2026, 10, 20)
        fun urgency(daysLeft: Int) = PlanSummary(date, daysLeft = daysLeft).urgency()

        assertThat(PlanSummary().urgency()).isEqualTo(PlanUrgency.NO_DATE)
        assertThat(urgency(31)).isEqualTo(PlanUrgency.ON_TRACK)
        assertThat(urgency(30)).isEqualTo(PlanUrgency.MONTH)
        assertThat(urgency(15)).isEqualTo(PlanUrgency.MONTH)
        assertThat(urgency(14)).isEqualTo(PlanUrgency.TWO_WEEKS)
        assertThat(urgency(8)).isEqualTo(PlanUrgency.TWO_WEEKS)
        assertThat(urgency(7)).isEqualTo(PlanUrgency.FINAL_WEEK)
        assertThat(urgency(2)).isEqualTo(PlanUrgency.FINAL_WEEK)
        assertThat(urgency(1)).isEqualTo(PlanUrgency.TOMORROW)
        assertThat(urgency(0)).isEqualTo(PlanUrgency.TODAY)

        assertThat(urgency(8).isClose).isFalse()
        assertThat(urgency(7).isClose).isTrue()
        assertThat(urgency(1).isClose).isTrue()
        assertThat(urgency(0).isClose).isTrue()
    }
}
