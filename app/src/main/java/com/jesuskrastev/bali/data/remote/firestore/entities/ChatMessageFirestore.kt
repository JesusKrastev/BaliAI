package com.jesuskrastev.bali.data.remote.firestore.entities

import com.google.firebase.firestore.DocumentId

/**
 * Firestore document for one turn of the AI tutor conversation, stored under
 * `users/{userId}/chat_messages/{messageId}`.
 *
 * The document id is the client-generated [com.jesuskrastev.bali.domain.model.ChatMessage.id],
 * which makes writes idempotent: a retried save overwrites its own document instead of
 * duplicating the turn.
 */
data class ChatMessageFirestore(
    @DocumentId val id: String = "",
    val content: String = "",
    val role: String = "",
    val timestampMillis: Long = 0L
)
