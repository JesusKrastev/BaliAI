package com.jesuskrastev.bali.data.mapper

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.AnswerFirestore
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.AnswerMode
import com.jesuskrastev.bali.domain.model.DrivingTopic
import com.jesuskrastev.bali.domain.util.QuestionId
import org.junit.Test
import java.util.Date

class AnswerMapperTest {

    private val tagged = Answer(
        id = "a1",
        testId = "t1",
        questionText = "¿Qué es la prioridad?",
        selectedOption = 2,
        isCorrect = false,
        date = Date(1_000),
        questionId = QuestionId.of("¿Qué es la prioridad?"),
        topic = DrivingTopic.RIGHT_OF_WAY,
        mode = AnswerMode.REVIEW
    )

    @Test
    fun `an answer keeps its id, topic and mode through Room`() {
        val back = tagged.toEntity().toDomain()

        assertThat(back.questionId).isEqualTo(tagged.questionId)
        assertThat(back.topic).isEqualTo(DrivingTopic.RIGHT_OF_WAY)
        assertThat(back.mode).isEqualTo(AnswerMode.REVIEW)
    }

    @Test
    fun `an answer keeps its id, topic and mode through Firestore`() {
        val back = tagged.toFirestore().toDomain()

        assertThat(back.questionId).isEqualTo(tagged.questionId)
        assertThat(back.topic).isEqualTo(DrivingTopic.RIGHT_OF_WAY)
        assertThat(back.mode).isEqualTo(AnswerMode.REVIEW)
    }

    @Test
    fun `an official exam answer is stored with a mode and no topic`() {
        val exam = tagged.copy(topic = null, mode = AnswerMode.OFFICIAL_EXAM)

        val entity = exam.toEntity()

        assertThat(entity.topic).isNull()
        assertThat(entity.mode).isEqualTo("OFFICIAL_EXAM")
    }

    @Test
    fun `a row saved before the new columns reads as unknown, with the id worked out from the text`() {
        val oldRow = AnswerEntity(
            id = "old",
            testId = "t",
            questionText = "¿Qué es la prioridad?",
            selectedOption = 0,
            isCorrect = true,
            timestamp = 5
        )

        val answer = oldRow.toDomain()

        assertThat(answer.questionId).isEmpty()
        assertThat(answer.topic).isNull()
        assertThat(answer.mode).isNull()
        assertThat(answer.resolvedQuestionId).isEqualTo(QuestionId.of("¿Qué es la prioridad?"))
    }

    @Test
    fun `a Firestore document saved before the new fields reads as unknown`() {
        val oldDocument = AnswerFirestore(id = "old", testId = "t", questionText = "q", dateMillis = 5)

        val answer = oldDocument.toDomain()

        assertThat(answer.questionId).isEmpty()
        assertThat(answer.topic).isNull()
        assertThat(answer.mode).isNull()
    }

    @Test
    fun `a topic or mode written by a newer version reads as unknown instead of crashing`() {
        val future = AnswerFirestore(id = "f", topic = "A_NEW_TOPIC", mode = "A_NEW_MODE")

        val answer = future.toDomain()

        assertThat(answer.topic).isNull()
        assertThat(answer.mode).isNull()
    }

    @Test
    fun `a blank question id is stored as null`() {
        assertThat(tagged.copy(questionId = "").toEntity().questionId).isNull()
    }
}
