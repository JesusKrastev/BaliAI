package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey val id: String,
    val category: String,
    val score: Int,
    val total: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isPassed: Boolean
)
