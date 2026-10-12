package com.jesuskrastev.bali.domain.exam

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.path.LessonQuestionBank.StaticQuestion
import org.junit.Test

class ScopedExamPromptTest {

    private val student = ScopedExamPrompt.Student(
        license = "B (Coche)", level = 3, experience = "Ninguna",
        difficultTopics = "Prioridad", totalTests = 12, daysToExam = null
    )

    private val sources = listOf(
        ExamLesson(
            nodeId = "node_0_1_lesson",
            title = "Alcohol: efectos, límites y sanciones",
            questions = listOf(
                StaticQuestion("¿Tasa máxima general?", listOf("0,5 g/l", "0,8 g/l", "0,3 g/l"), 0, "El límite general es 0,5 g/l."),
                StaticQuestion("¿Qué indica esta señal?", listOf("Stop", "Ceda", "Peligro"), 1, "Es un ceda.", imageUrl = "https://img/x.png")
            )
        )
    )

    private fun prompt(daysToExam: Long? = null) = ScopedExamPrompt.build(
        scopeTitle = "Examen — El Conductor",
        sources = sources,
        student = student.copy(daysToExam = daysToExam),
        questionCount = 22
    )

    @Test
    fun `the prompt carries the lesson titles and the right answers as study material`() {
        val text = prompt()

        assertThat(text).contains("Alcohol: efectos, límites y sanciones")
        assertThat(text).contains("¿Tasa máxima general?")
        assertThat(text).contains("Respuesta correcta: 0,5 g/l")
    }

    @Test
    fun `the prompt limits the exam to the material`() {
        assertThat(prompt()).contains("SOLO ha estudiado el MATERIAL DE ESTUDIO")
    }

    @Test
    fun `the prompt forbids pictures and no longer teaches Gemini how to build image links`() {
        val text = prompt()

        assertThat(text).contains("NO USES IMÁGENES")
        assertThat(text.lowercase()).doesNotContain("wikimedia")
        assertThat(text).doesNotContain("FilePath")
    }

    @Test
    fun `a source that came with a picture says so`() {
        assertThat(prompt()).contains("[en el original acompañaba una imagen]")
    }

    @Test
    fun `the prompt asks for the number of questions given`() {
        assertThat(prompt()).contains("EXACTAMENTE 22 preguntas")
    }

    @Test
    fun `the prompt flags urgency only when the exam is close`() {
        assertThat(prompt(daysToExam = 5)).contains("El examen real es en 5 días")
        assertThat(prompt(daysToExam = 90)).doesNotContain("El examen real es en")
        assertThat(prompt(daysToExam = null)).doesNotContain("El examen real es en")
    }
}
