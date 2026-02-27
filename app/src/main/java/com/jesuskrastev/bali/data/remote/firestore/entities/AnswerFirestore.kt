package com.jesuskrastev.bali.data.remote.firestore.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

data class AnswerFirestore(
    @DocumentId val id: String = "",
    val testId: String = "",
    val questionText: String = "",
    val selectedOption: Int = 0,
    val isCorrect: Boolean = false,
    val dateMillis: Long = 0L
)