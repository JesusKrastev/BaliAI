package com.jesuskrastev.bali.data.remote.firestore.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

data class TestResultFirestore(
    @DocumentId val id: String = "",
    val category: String = "",
    val score: Int = 0,
    val total: Int = 0,
    val dateMillis: Long = 0L,
    val isPassed: Boolean = false
)