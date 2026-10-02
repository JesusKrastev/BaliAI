package com.jesuskrastev.bali.ui.screens.stats

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.MockExam
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.ReadinessResult
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatsCopyTest {

    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun useSpanishTime() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun readiness(
        level: ReadinessLevel,
        probability: Float? = null,
        taken: Int = 0,
        recent: List<MockExam> = emptyList()
    ) = ReadinessResult(
        level = level,
        passProbability = probability,
        mocksTaken = taken,
        mocksMissing = (3 - taken).coerceAtLeast(0),
        history = recent,
        recent = recent,
        passedInRecent = recent.count { it.passed },
        averageScore = null,
        bestScore = null,
        trend = null
    )

    @Test
    fun `without mock exams the card asks for three of them`() {
        val copy = readinessCopyOf(readiness(ReadinessLevel.NOT_ENOUGH_DATA))

        assertThat(copy.body).contains("3 simulacros")
        assertThat(copy.nextStep).contains("3 simulacros")
    }

    @Test
    fun `with two mock exams it asks for one more in the singular`() {
        val copy = readinessCopyOf(readiness(ReadinessLevel.NOT_ENOUGH_DATA, taken = 2))

        assertThat(copy.body).contains("2 simulacros")
        assertThat(copy.nextStep).contains("1 simulacro más")
        assertThat(copy.nextStep).doesNotContain("simulacros")
    }

    @Test
    fun `a verdict states the chance without promising the result`() {
        listOf(ReadinessLevel.NOT_YET, ReadinessLevel.ALMOST, ReadinessLevel.READY).forEach { level ->
            val copy = readinessCopyOf(readiness(level, probability = 0.87f, taken = 4))

            assertThat(copy.body).contains("87 de cada 100")
            assertThat(copy.body.lowercase()).doesNotContain("garantiz")
            assertThat(copy.body.lowercase()).doesNotContain("aprobarás")
        }
    }

    @Test
    fun `every level has its own status and next step`() {
        val copies = ReadinessLevel.entries.map {
            readinessCopyOf(readiness(it, probability = 0.5f, taken = 3))
        }

        assertThat(copies.map { it.status }.toSet()).hasSize(4)
        assertThat(copies.map { it.nextStep }.toSet()).hasSize(4)
    }

    @Test
    fun `the recent summary reads in singular and plural`() {
        val one = listOf(MockExam(0, 28, 30, true))
        val two = listOf(MockExam(0, 28, 30, true), MockExam(1, 20, 30, false))

        assertThat(recentSummaryOf(readiness(ReadinessLevel.NOT_ENOUGH_DATA)))
            .isEqualTo("Todavía no has hecho ningún simulacro.")
        assertThat(recentSummaryOf(readiness(ReadinessLevel.NOT_ENOUGH_DATA, recent = one)))
            .isEqualTo("Has hecho 1 simulacro y lo has aprobado.")
        assertThat(recentSummaryOf(readiness(ReadinessLevel.NOT_ENOUGH_DATA, recent = listOf(MockExam(0, 20, 30, false)))))
            .isEqualTo("Has hecho 1 simulacro y no has llegado al aprobado.")
        assertThat(recentSummaryOf(readiness(ReadinessLevel.NOT_ENOUGH_DATA, recent = two)))
            .isEqualTo("Has aprobado 1 de tus últimos 2 simulacros.")
    }

    /** A plan whose date is [daysLeft] days after Wednesday 7 October 2026. */
    private fun plan(daysLeft: Int, isExamDate: Boolean = true): PlanSummary {
        val date = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 7 + daysLeft)
        }.timeInMillis
        return PlanSummary(date, isExamDate, daysLeft)
    }

    private val notStarted = readiness(ReadinessLevel.NOT_ENOUGH_DATA)

    private fun countdown(
        daysLeft: Int,
        studiedToday: Boolean = false,
        isExamDate: Boolean = true,
        readiness: ReadinessResult = notStarted
    ) = examCountdownCopyOf(plan(daysLeft, isExamDate), readiness, studiedToday)!!

    @Test
    fun `without a date there is no countdown to word`() {
        assertThat(examCountdownCopyOf(PlanSummary(), notStarted, studiedToday = false)).isNull()
    }

    @Test
    fun `the countdown names the figure, what it counts and the date`() {
        val exam = countdown(9)
        assertThat(exam.number).isEqualTo("9")
        assertThat(exam.unit).isEqualTo("DÍAS PARA TU EXAMEN")
        assertThat(exam.date).isEqualTo("Viernes, 16 de octubre")

        val promise = countdown(9, isExamDate = false)
        assertThat(promise.unit).isEqualTo("DÍAS PARA TU FECHA META")
        assertThat(promise.date).isEqualTo("Carnet antes del 16 de octubre")
    }

    @Test
    fun `the stretch label and the headline escalate with the days left`() {
        assertThat(countdown(45).stage).isEqualTo("CUENTA ATRÁS")
        assertThat(countdown(45).headline).isEqualTo("Tienes tiempo, pero se acaba.")
        assertThat(countdown(21).stage).isEqualTo("ÚLTIMO MES")
        assertThat(countdown(21).headline).isEqualTo("Queda menos de un mes.")
        assertThat(countdown(10).stage).isEqualTo("ÚLTIMAS 2 SEMANAS")
        assertThat(countdown(10).headline).isEqualTo("Quedan dos semanas o menos.")
        assertThat(countdown(5).stage).isEqualTo("RECTA FINAL")
        assertThat(countdown(5).headline).isEqualTo("Última semana: no hay días de sobra.")
    }

    @Test
    fun `tomorrow and today replace the figure with a word`() {
        val tomorrow = countdown(1)
        assertThat(tomorrow.number).isEqualTo("MAÑANA")
        assertThat(tomorrow.unit).isEqualTo("ES TU EXAMEN")
        assertThat(tomorrow.headline).isEqualTo("Mañana te examinas.")

        val today = countdown(0)
        assertThat(today.number).isEqualTo("HOY")
        assertThat(today.stage).isEqualTo("HA LLEGADO EL DÍA")
        assertThat(today.headline).isEqualTo("Hoy es tu examen.")

        assertThat(countdown(0, isExamDate = false).unit).isEqualTo("ES TU FECHA META")
        assertThat(countdown(0, isExamDate = false).headline).isEqualTo("Hoy vence tu fecha meta.")
    }

    @Test
    fun `the detail counts the sessions left and whether today is done`() {
        assertThat(countdown(14).detail)
            .startsWith("Tienes 14 sesiones por delante y la de hoy aún no está hecha.")
        assertThat(countdown(14, studiedToday = true).detail)
            .startsWith("Hoy ya has estudiado: te quedan 13 sesiones más.")
        assertThat(countdown(2, studiedToday = true).detail)
            .startsWith("Hoy ya has estudiado: te queda 1 sesión más.")
    }

    @Test
    fun `the last day tells the student to rest rather than to cram`() {
        assertThat(countdown(1, studiedToday = false).detail)
            .isEqualTo("Haz un repaso corto hoy y descansa: es tu último día de estudio.")
        assertThat(countdown(1, studiedToday = true).detail)
            .isEqualTo("Hoy ya has estudiado: descansa y llega con la cabeza fría.")
    }

    @Test
    fun `missing mock exams are added while there is no verdict and left out once there is one`() {
        val one = readiness(ReadinessLevel.NOT_ENOUGH_DATA, taken = 2)
        assertThat(countdown(14, readiness = one).detail).endsWith("Y te falta 1 simulacro para saber si estás listo.")
        assertThat(countdown(14, readiness = notStarted).detail).endsWith("Y te faltan 3 simulacros para saber si estás listo.")

        val verdict = readiness(ReadinessLevel.ALMOST, probability = 0.8f, taken = 5)
        assertThat(countdown(14, readiness = verdict).detail).doesNotContain("simulacro")
    }

    @Test
    fun `percentages never round a partial value to zero or one hundred`() {
        assertThat(percentOf(0f)).isEqualTo(0)
        assertThat(percentOf(1f)).isEqualTo(100)
        assertThat(percentOf(0.001f)).isEqualTo(1)
        assertThat(percentOf(0.999f)).isEqualTo(99)
        assertThat(percentOf(0.874f)).isEqualTo(87)
        assertThat(percentText(0.5f)).isEqualTo("50 %")
    }

    @Test
    fun `decimals use a comma and drop a trailing zero`() {
        assertThat(formatDecimal(25.4f)).isEqualTo("25,4")
        assertThat(formatDecimal(27f)).isEqualTo("27")
    }

    @Test
    fun `the trend shows an arrow and its sign`() {
        assertThat(trendText(1.3f)).isEqualTo("▲ +1,3")
        assertThat(trendText(-2f)).isEqualTo("▼ -2")
        assertThat(trendText(0f)).isEqualTo("= 0")
    }

    @Test
    fun `plural adds an s unless told otherwise`() {
        assertThat(plural(1, "pregunta")).isEqualTo("1 pregunta")
        assertThat(plural(5, "pregunta")).isEqualTo("5 preguntas")
        assertThat(plural(2, "sesión", "sesiones")).isEqualTo("2 sesiones")
    }
}
