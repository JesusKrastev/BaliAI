package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.AnswerFirestore
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.AnswerMode
import com.jesuskrastev.bali.domain.model.DrivingTopic
import java.util.UUID

fun Answer.toEntity(): AnswerEntity =
    AnswerEntity(
        id = UUID.randomUUID().toString(),
        testId = testId,
        questionText = questionText,
        selectedOption = selectedOption,
        isCorrect = isCorrect,
        timestamp = date.toTimestamp(),
        questionId = questionId.ifBlank { null },
        topic = topic?.name,
        mode = mode?.tag
)

fun AnswerEntity.toDomain(): Answer =
    Answer(
        id = id,
        testId = testId,
        questionText = questionText,
        selectedOption = selectedOption,
        isCorrect = isCorrect,
        date = timestamp.toDate(),
        questionId = questionId.orEmpty(),
        topic = DrivingTopic.fromTag(topic),
        mode = AnswerMode.fromTag(mode)
    )

fun Answer.toFirestore(): AnswerFirestore = AnswerFirestore(
    id = id,
    testId = testId,
    questionText = questionText,
    selectedOption = selectedOption,
    isCorrect = isCorrect,
    dateMillis = date.time,
    questionId = questionId,
    topic = topic?.name,
    mode = mode?.tag
)

fun AnswerFirestore.toDomain(): Answer = Answer(
    id = id,
    testId = testId,
    questionText = questionText,
    selectedOption = selectedOption,
    isCorrect = isCorrect,
    date = dateMillis.toDate(),
    questionId = questionId,
    topic = DrivingTopic.fromTag(topic),
    mode = AnswerMode.fromTag(mode)
)
