package com.jesuskrastev.bali.data.remote.firestore.entities

// Status is stored as a String to keep it fully compatible with Firestore and serialization.
data class AILessonNodeFirestore(
    val id: String = "",
    val orderIndex: Int = 0,
    val title: String = "",
    val description: String = "",
    val status: String = "LOCKED",
    val scorePercentage: Int? = null
)
