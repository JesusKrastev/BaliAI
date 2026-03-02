package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "",
    val name: String? = null,
    val licenseType: String? = null,
    val experience: String? = null,
    val reasons: String = "",
    val examDateMillis: Long? = null,
    val dailyGoal: String? = null,
    val learningPreference: String? = null,
    val difficultTopics: String = "",
    val concern: String? = null,
    val studyTime: String? = null,
    val lastPracticeTimestamp: Long = 0,
    val lastEnergyUpdateTimestamp: Long = 0,
    val currentStreak: Int = 0,
    val xp: Int = 0,
    val level: Int = 0,
    val energy: Int = 5,
    val coins: Int = 0,
    val streakFreezes: Int = 0,
    val practiceDays: List<Long> = emptyList()
)
