package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting the AI tutor conversation. Abstracts the local (Room) and
 * remote (Firestore) data sources so the conversation survives both offline use and a
 * device change once the user signs in.
 */
interface ChatRepository {
    /**
     * Emits the full conversation ordered from oldest to newest, re-emitting on every
     * change. Emits an empty list when the user has never written to the tutor.
     */
    fun observeHistory(): Flow<List<ChatMessage>>

    /**
     * Persists a single message.
     *
     * @param message the turn to store; its [ChatMessage.id] is kept as the row key so
     *   the message the UI already shows and the stored one stay the same entity.
     */
    suspend fun save(message: ChatMessage)

    /** Deletes the whole conversation for the active user. */
    suspend fun clear()
}
