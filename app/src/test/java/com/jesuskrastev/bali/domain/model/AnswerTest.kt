package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.util.QuestionId
import org.junit.Test
import java.util.Date

class AnswerTest {

    private fun answer(questionId: String = "") = Answer(
        testId = "t",
        questionText = "¿Qué es la prioridad?",
        selectedOption = 0,
        isCorrect = true,
        date = Date(0),
        questionId = questionId
    )

    @Test
    fun `an answer saved without an id works out the id from its text`() {
        assertThat(answer().resolvedQuestionId).isEqualTo(QuestionId.of("¿Qué es la prioridad?"))
    }

    @Test
    fun `an answer with a saved id keeps it`() {
        assertThat(answer(questionId = "q_saved").resolvedQuestionId).isEqualTo("q_saved")
    }

    @Test
    fun `new fields default to unknown so old call sites still compile`() {
        val old = answer()

        assertThat(old.topic).isNull()
        assertThat(old.mode).isNull()
    }

    @Test
    fun `every mode survives its tag`() {
        AnswerMode.entries.forEach { assertThat(AnswerMode.fromTag(it.tag)).isEqualTo(it) }
    }

    @Test
    fun `a blank or unknown mode tag reads as unknown`() {
        assertThat(AnswerMode.fromTag(null)).isNull()
        assertThat(AnswerMode.fromTag("")).isNull()
        assertThat(AnswerMode.fromTag("FUTURE_MODE")).isNull()
    }

    @Test
    fun `a path node type becomes its mode`() {
        assertThat(AnswerMode.fromNodeType(NodeType.LESSON.name)).isEqualTo(AnswerMode.LESSON)
        assertThat(AnswerMode.fromNodeType(NodeType.REVIEW.name)).isEqualTo(AnswerMode.REVIEW)
        assertThat(AnswerMode.fromNodeType(NodeType.EXAM.name)).isEqualTo(AnswerMode.EXAM)
    }

    @Test
    fun `no node, an unknown type or the official exam tag fall back to practice`() {
        assertThat(AnswerMode.fromNodeType(null)).isEqualTo(AnswerMode.PRACTICE)
        assertThat(AnswerMode.fromNodeType("SOMETHING_NEW")).isEqualTo(AnswerMode.PRACTICE)
        assertThat(AnswerMode.fromNodeType(AnswerMode.OFFICIAL_EXAM.tag)).isEqualTo(AnswerMode.PRACTICE)
    }

    @Test
    fun `every topic survives its tag and an unknown one reads as no topic`() {
        DrivingTopic.entries.forEach { assertThat(DrivingTopic.fromTag(it.name)).isEqualTo(it) }
        assertThat(DrivingTopic.fromTag(null)).isNull()
        assertThat(DrivingTopic.fromTag("NOT_A_TOPIC")).isNull()
    }
}
