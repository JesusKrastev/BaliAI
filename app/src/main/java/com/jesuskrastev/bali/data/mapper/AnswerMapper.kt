package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.AnswerFirestore
import com.jesuskrastev.bali.domain.model.Answer
import java.util.UUID

fun Answer.toEntity(): AnswerEntity =
    AnswerEntity(
        id = UUID.randomUUID().toString(),
        testId = testId,
        questionText = questionText,
        selectedOption = selectedOption,
        isCorrect = isCorrect,
        timestamp = date.toTimestamp()
)

fun AnswerEntity.toDomain(): Answer =
    Answer(
        id = id,
        testId = testId,
        questionText = questionText,
        selectedOption = selectedOption,
        isCorrect = isCorrect,
        date = timestamp.toDate()
    )

fun Answer.toFirestore(): AnswerFirestore = AnswerFirestore(
    id = id,
    testId = testId,
    questionText = questionText,
    selectedOption = selectedOption,
    isCorrect = isCorrect,
    dateMillis = date.time
)

fun AnswerFirestore.toDomain(): Answer = Answer(
    id = id,
    testId = testId,
    questionText = questionText,
    selectedOption = selectedOption,
    isCorrect = isCorrect,
    date = dateMillis.toDate()
)