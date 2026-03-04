package com.jesuskrastev.bali.data.remote.firestore.entities

// Status and nodeType are stored as Strings for Firestore compatibility.
data class LessonNodeFirestore(
    val id: String = "",
    val orderIndex: Int = 0,
    val title: String = "",
    val description: String = "",
    val status: String = "LOCKED",
    val scorePercentage: Int? = null,
    val sectionIndex: Int = 0,
    val sectionTitle: String = "",
    val unitIndex: Int = 0,
    val nodeType: String = "LESSON",
    val iconResName: String = "lesson_test"
)
