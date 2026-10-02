package com.jesuskrastev.bali.ui.screens.stats

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.MockExam
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.ReadinessResult
import org.junit.Test

class StatsCopyTest {

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

    @Test
    fun `the exam countdown covers the day, tomorrow, the future and the past`() {
        assertThat(examCountdownText(0)).isEqualTo("Tu examen es hoy")
        assertThat(examCountdownText(1)).isEqualTo("Tu examen es mañana")
        assertThat(examCountdownText(12)).isEqualTo("Faltan 12 días para tu examen")
        assertThat(examCountdownText(-1)).isEqualTo("Tu examen fue ayer")
        assertThat(examCountdownText(-4)).isEqualTo("Tu examen fue hace 4 días")
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
