package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answers")
data class AnswerEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val questionText: String,
    val selectedOption: Int,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
