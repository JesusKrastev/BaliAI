package com.jesuskrastev.bali.data.remote.firestore.entities

import com.google.firebase.firestore.DocumentId

data class UserFirestore(
    @DocumentId val id: String = "",
    val name: String = "",
    val licenseType: String = "",
    val experience: String = "",
    val examDateMillis: Long = 0L,
    val planTargetMillis: Long = 0L,
    val difficultTopics: String = "",
    val lastPracticeTimestamp: Long = 0L,
    val currentStreak: Int = 0,
    val xp: Int = 0,
    val level: Int = 0,
    val coins: Int = 0,
    val streakFreezes: Int = 0,
    val highestStreak: Int = 0,
    val lostStreak: Int = 0,
    val lostStreakDayMillis: Long = 0L,
    val practiceDays: List<Long> = emptyList(),
    val frozenDays: List<Long> = emptyList(),
    val lessonProgress: Map<String, LessonProgressFirestore> = emptyMap(),
    val fcmToken: String = ""
)