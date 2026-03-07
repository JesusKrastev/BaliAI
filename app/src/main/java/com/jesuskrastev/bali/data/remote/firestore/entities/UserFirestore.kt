package com.jesuskrastev.bali.data.remote.firestore.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

data class UserFirestore(
    @DocumentId val id: String = "",
    val name: String = "",
    val licenseType: String = "",
    val experience: String = "",
    val reasons: String = "",
    val examDateMillis: Long = 0L,
    val dailyGoal: String = "",
    val learningPreference: String = "",
    val difficultTopics: String = "",
    val concern: String = "",
    val studyTime: String = "",
    val lastPracticeTimestamp: Long = 0L,
    val lastEnergyUpdateTimestamp: Long = 0L,
    val currentStreak: Int = 0,
    val xp: Int = 0,
    val level: Int = 0,
    val energy: Int = 5,
    val coins: Int = 0,
    val streakFreezes: Int = 0,
    val highestStreak: Int = 0,
    val practiceDays: List<Long> = emptyList(),
    val weekSessions: Int = 0,
    val currentWeekStart: Long = 0L,
    val lessonProgress: Map<String, LessonProgressFirestore> = emptyMap()
)