package com.jesuskrastev.bali.domain.path

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LessonQuestionBankTest {

    /**
     * The answer key used to mark "Ninguna" as right, so a student who knew the difference between
     * STOP and give way was scored as failing it.
     */
    @Test
    fun `stop versus give way marks the full stop answer as correct`() {
        val question = LessonQuestionBank.getQuestionsForNode(PRIORITY_SIGNS_NODE, count = Int.MAX_VALUE)
            .single { it.text == "¿Qué diferencia hay entre STOP y ceda el paso?" }

        assertThat(question.options[question.correctAnswerIndex])
            .isEqualTo("En el STOP hay que detenerse siempre; en ceda el paso, solo si vienen vehículos")
    }

    /** Every question of the node has three options and an answer key that points at one of them. */
    @Test
    fun `priority signs node has a valid answer key for every question`() {
        val questions = LessonQuestionBank.getQuestionsForNode(PRIORITY_SIGNS_NODE, count = Int.MAX_VALUE)

        assertThat(questions).isNotEmpty()
        questions.forEach { question ->
            assertThat(question.options).hasSize(3)
            assertThat(question.correctAnswerIndex).isIn(question.options.indices)
        }
    }

    private companion object {
        /** "Prioridad en cruces señalizados", where the STOP question lives. */
        const val PRIORITY_SIGNS_NODE = "node_5_1_lesson"
    }
}
