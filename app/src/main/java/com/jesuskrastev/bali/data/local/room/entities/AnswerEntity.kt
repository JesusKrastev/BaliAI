package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One answer row. [questionId], [topic] and [mode] are nullable with no column default: they are
 * null on rows saved before they existed, and `ALTER TABLE … ADD COLUMN` can add them as-is.
 */
@Entity(tableName = "answers")
data class AnswerEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val questionText: String,
    val selectedOption: Int,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val questionId: String? = null,
    val topic: String? = null,
    val mode: String? = null
)
