package com.jesuskrastev.bali.domain.exam

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.path.LessonQuestionBank.StaticQuestion
import org.junit.Test

class ScopedReviewPromptTest {

    private val student = ScopedExamPrompt.Student(
        license = "B (Coche)", level = 2, experience = "Ninguna",
        difficultTopics = "Prioridad", totalTests = 3, daysToExam = null
    )

    private val sources = listOf(
        ScopedReviewComposer.Source(
            1, "Señales de peligro",
            StaticQuestion("¿Qué indica esta señal?", listOf("Stop", "Ceda", "Peligro"), 2, "Es peligro.", "https://img/p1.png")
        ),
        ScopedReviewComposer.Source(
            2, "Alcohol",
            StaticQuestion("¿Tasa máxima general?", listOf("0,5 g/l", "0,8 g/l", "0,3 g/l"), 0, "El límite general es 0,5 g/l.")
        )
    )

    private val prompt = ScopedReviewPrompt.build("Repaso — Señales I", sources, student)

    @Test
    fun `each source is numbered so the rewrite can quote it back`() {
        assertThat(prompt).contains("[1] (Señales de peligro)")
        assertThat(prompt).contains("[2] (Alcohol)")
        assertThat(prompt).contains("sourceIndex")
    }

    @Test
    fun `a source with a picture is flagged and the rewrite is told the picture stays`() {
        assertThat(prompt).contains("[lleva imagen]")
        assertThat(prompt).contains("esa MISMA imagen se mostrará")
    }

    @Test
    fun `Gemini is told never to write picture links`() {
        assertThat(prompt).contains("Nunca escribas URLs")
        assertThat(prompt.lowercase()).doesNotContain("wikimedia")
        assertThat(prompt).doesNotContain("https://img/p1.png")
    }

    @Test
    fun `the rewrite must keep the concept and the right answer but change the wording`() {
        assertThat(prompt).contains("MISMO significado de respuesta correcta")
        assertThat(prompt).contains("No copies el texto original")
    }

    @Test
    fun `it asks for exactly one question per source`() {
        assertThat(prompt).contains("EXACTAMENTE 2 preguntas")
    }
}
