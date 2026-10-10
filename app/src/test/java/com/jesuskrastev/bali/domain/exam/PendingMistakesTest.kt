package com.jesuskrastev.bali.domain.exam

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.util.QuestionId
import org.junit.Test
import java.util.Date

class PendingMistakesTest {

    private fun answer(text: String, correct: Boolean, at: Long) = Answer(
        testId = "t", questionText = text, selectedOption = 0, isCorrect = correct, date = Date(at)
    )

    @Test
    fun `a question last answered wrong is a pending mistake`() {
        val ids = PendingMistakes.idsOf(listOf(answer("Pregunta uno", correct = false, at = 1)))

        assertThat(ids).containsExactly(QuestionId.of("Pregunta uno"))
    }

    @Test
    fun `a mistake answered right afterwards no longer counts`() {
        val ids = PendingMistakes.idsOf(
            listOf(answer("Pregunta uno", false, at = 1), answer("Pregunta uno", true, at = 2))
        )

        assertThat(ids).isEmpty()
    }

    @Test
    fun `a question answered right and then wrong is pending again`() {
        val ids = PendingMistakes.idsOf(
            listOf(answer("Pregunta uno", true, at = 1), answer("Pregunta uno", false, at = 2))
        )

        assertThat(ids).containsExactly(QuestionId.of("Pregunta uno"))
    }

    @Test
    fun `the order the answers come in does not matter`() {
        val ids = PendingMistakes.idsOf(
            listOf(answer("Pregunta uno", true, at = 2), answer("Pregunta uno", false, at = 1))
        )

        assertThat(ids).isEmpty()
    }
}
