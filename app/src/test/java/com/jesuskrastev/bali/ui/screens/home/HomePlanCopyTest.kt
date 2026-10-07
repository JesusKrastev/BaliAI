package com.jesuskrastev.bali.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase
import com.jesuskrastev.bali.ui.screens.stats.PlanSummary
import com.jesuskrastev.bali.ui.screens.stats.PlanUrgency
import com.jesuskrastev.bali.ui.screens.stats.examCountdownCopyOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HomePlanCopyTest {

    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun useSpanishTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    /** Local midnight [days] days after Wednesday 7 October 2026. */
    private fun day(days: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 7 + days)
        }.timeInMillis

    /** Wednesday 7 October 2026, 18:00. */
    private val now get() = day(0) + 18 * 60 * 60 * 1000L

    private fun plan(daysLeft: Int, isExamDate: Boolean = true) =
        PlanSummary(targetMillis = day(daysLeft), isExamDate = isExamDate, daysLeft = daysLeft)

    @Test
    fun `far from the onboarding promise the chip shows the days and the sheet the promise`() {
        val copy = homePlanCopyOf(plan(45, isExamDate = false), datePassed = false, studiedToday = false)

        assertThat(copy.hasDate).isTrue()
        assertThat(copy.chipLabel).isEqualTo("45 días")
        assertThat(copy.stage).isEqualTo("CUENTA ATRÁS")
        assertThat(copy.title).isEqualTo("Carnet antes del 21 de noviembre")
        assertThat(copy.subtitle).isEqualTo("Faltan 45 días")
        assertThat(copy.urgency.isClose).isFalse()
    }

    @Test
    fun `an exam date of the student's own is named with its weekday`() {
        val copy = homePlanCopyOf(plan(5), datePassed = false, studiedToday = false)

        assertThat(copy.title).isEqualTo("Examen el lunes, 12 de octubre")
    }

    @Test
    fun `the last week turns the chip urgent with the same stretch label as the statistics card`() {
        val copy = homePlanCopyOf(plan(5), datePassed = false, studiedToday = false)

        assertThat(copy.urgency).isEqualTo(PlanUrgency.FINAL_WEEK)
        assertThat(copy.urgency.isClose).isTrue()
        assertThat(copy.stage).isEqualTo("RECTA FINAL")
    }

    @Test
    fun `Home and Statistics call every stretch by the same name`() {
        val readiness = CalculateProgressStatsUseCase(CalculateReadinessUseCase())
            .invoke(emptyList(), emptyList(), User(), now).readiness

        for (days in listOf(0, 1, 5, 12, 21, 45)) {
            for (isExam in listOf(true, false)) {
                val summary = plan(days, isExam)
                val home = homePlanCopyOf(summary, datePassed = false, studiedToday = false)
                val stats = examCountdownCopyOf(summary, readiness, studiedToday = false)!!

                assertThat(home.stage).isEqualTo(stats.stage)
            }
        }
    }

    @Test
    fun `while today's session is pending the chip carries the dot and the sheet the goal`() {
        val copy = homePlanCopyOf(plan(12), datePassed = false, studiedToday = false)

        assertThat(copy.showsPendingDot).isTrue()
        assertThat(copy.goal?.state).isEqualTo(DailyGoalState.PENDING)
        assertThat(copy.goal?.title).isEqualTo("Objetivo de hoy: 1 sesión")
        assertThat(copy.goal?.detail).isEqualTo("Vale un test, un simulacro o un minijuego.")
    }

    @Test
    fun `once today is done the dot goes and the sheet counts the sessions left`() {
        val copy = homePlanCopyOf(plan(12), datePassed = false, studiedToday = true)

        assertThat(copy.showsPendingDot).isFalse()
        assertThat(copy.goal?.state).isEqualTo(DailyGoalState.DONE)
        assertThat(copy.goal?.title).isEqualTo("Hecho hoy")
        assertThat(copy.goal?.detail).isEqualTo("Te quedan 11 sesiones más hasta tu examen.")
    }

    @Test
    fun `one session left is said in the singular`() {
        val copy = homePlanCopyOf(plan(2, isExamDate = false), datePassed = false, studiedToday = true)

        assertThat(copy.goal?.detail).isEqualTo("Te queda 1 sesión más hasta tu fecha meta.")
    }

    @Test
    fun `the day before, the chip says tomorrow and the goal is a short review`() {
        val copy = homePlanCopyOf(plan(1), datePassed = false, studiedToday = false)

        assertThat(copy.chipLabel).isEqualTo("Mañana")
        assertThat(copy.subtitle).isEqualTo("Tu examen es mañana")
        assertThat(copy.goal?.detail).isEqualTo("Un repaso corto y a descansar: es tu último día de estudio.")
    }

    @Test
    fun `on exam day there is no goal to chase and no dot`() {
        val copy = homePlanCopyOf(plan(0), datePassed = false, studiedToday = false)

        assertThat(copy.chipLabel).isEqualTo("Hoy")
        assertThat(copy.subtitle).isEqualTo("Hoy es tu examen")
        assertThat(copy.goal?.state).isEqualTo(DailyGoalState.EXAM_DAY)
        assertThat(copy.showsPendingDot).isFalse()
    }

    @Test
    fun `on the promised day the goal still counts and the date can be changed`() {
        val copy = homePlanCopyOf(plan(0, isExamDate = false), datePassed = false, studiedToday = true)

        assertThat(copy.subtitle).isEqualTo("Tu fecha meta es hoy")
        assertThat(copy.goal?.state).isEqualTo(DailyGoalState.DONE)
        assertThat(copy.goal?.detail).isEqualTo("Si tu examen es otro día, cambia la fecha y seguimos.")
    }

    @Test
    fun `without a date the chip asks for the exam and the sheet has no goal`() {
        val copy = homePlanCopyOf(PlanSummary(), datePassed = false, studiedToday = false)

        assertThat(copy.hasDate).isFalse()
        assertThat(copy.chipLabel).isEqualTo("Tu examen")
        assertThat(copy.title).isEqualTo("¿Cuándo es tu examen?")
        assertThat(copy.goal).isNull()
        assertThat(copy.showsPendingDot).isFalse()
    }

    @Test
    fun `a date that has gone by asks for a new one instead of counting negative days`() {
        val copy = homePlanCopyOf(PlanSummary(), datePassed = true, studiedToday = false)

        assertThat(copy.title).isEqualTo("Tu fecha ya ha pasado")
        assertThat(copy.chipLabel).doesNotContain("-")
    }

    @Test
    fun `a screen reader hears the days left and whether today is done`() {
        val pending = homePlanCopyOf(plan(5), datePassed = false, studiedToday = false)
        val tomorrowDone = homePlanCopyOf(plan(1, isExamDate = false), datePassed = false, studiedToday = true)

        assertThat(pending.chipDescription).isEqualTo("Tu plan: faltan 5 días para tu examen. Objetivo de hoy pendiente.")
        assertThat(tomorrowDone.chipDescription).isEqualTo("Tu plan: tu fecha meta es mañana. Objetivo de hoy hecho.")
    }

    @Test
    fun `the chip stays hidden until the profile has loaded`() {
        val state = homePlanStateOf(user = null, now = now)

        assertThat(state.isLoaded).isFalse()
        assertThat(state.copy).isNull()
    }

    @Test
    fun `the profile's plan date and today's study feed the chip`() {
        val user = User(planTargetMillis = day(30), practiceDays = listOf(day(0)))

        val state = homePlanStateOf(user, now)

        assertThat(state.plan.daysLeft).isEqualTo(30)
        assertThat(state.studiedToday).isTrue()
        assertThat(state.copy?.chipLabel).isEqualTo("30 días")
    }

    @Test
    fun `a saved date that has gone by is told apart from no date at all`() {
        val passed = homePlanStateOf(User(examDateMillis = day(-3), planTargetMillis = day(-3)), now)
        val never = homePlanStateOf(User(), now)

        assertThat(passed.datePassed).isTrue()
        assertThat(passed.copy?.hasDate).isFalse()
        assertThat(never.datePassed).isFalse()
    }
}
