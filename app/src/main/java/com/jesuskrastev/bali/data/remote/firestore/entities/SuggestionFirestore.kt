package com.jesuskrastev.bali.data.remote.firestore.entities

import com.google.firebase.firestore.DocumentId

data class SuggestionFirestore(
    @DocumentId val id: String = "",
    val text: String = "",
    val userId: String? = null,
    val email: String? = null,
    val timestamp: Long = 0,
    val date: String = ""
)