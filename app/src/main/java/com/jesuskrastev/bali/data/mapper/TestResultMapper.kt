package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.TestResultEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.TestResultFirestore
import com.jesuskrastev.bali.domain.model.TestResult
import java.util.UUID

fun TestResultEntity.toDomain(): TestResult = TestResult(
    id = id,
    category = category,
    score = score,
    total = total,
    date = timestamp.toDate(),
    isPassed = isPassed
)

fun TestResult.toEntity(): TestResultEntity =
    TestResultEntity(
        id = UUID.randomUUID().toString(),
        category = category,
        score = score,
        total = total,
        timestamp = date.toTimestamp(),
        isPassed = isPassed
    )

fun TestResult.toFirestore(): TestResultFirestore = TestResultFirestore(
    id = id,
    category = category,
    score = score,
    total = total,
    dateMillis = date.time,
    isPassed = isPassed
)

fun TestResultFirestore.toDomain(): TestResult = TestResult(
    id = id,
    category = category,
    score = score,
    total = total,
    date = dateMillis.toDate(),
    isPassed = isPassed
)