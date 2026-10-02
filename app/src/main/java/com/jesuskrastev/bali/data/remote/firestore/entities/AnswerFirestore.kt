package com.jesuskrastev.bali.data.remote.firestore.entities

import com.google.firebase.firestore.DocumentId

/**
 * An answer document under `users/{uid}/answers`. [questionId], [topic] and [mode] are missing
 * from documents saved before they existed; Firestore then reads them as these defaults, so no
 * document has to be rewritten.
 */
data class AnswerFirestore(
    @DocumentId val id: String = "",
    val testId: String = "",
    val questionText: String = "",
    val selectedOption: Int = 0,
    val isCorrect: Boolean = false,
    val dateMillis: Long = 0L,
    val questionId: String = "",
    val topic: String? = null,
    val mode: String? = null
)
