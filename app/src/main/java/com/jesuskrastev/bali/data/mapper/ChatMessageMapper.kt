package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.ChatMessageEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.ChatMessageFirestore
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.ChatRole

/**
 * Reads a persisted role name back into [ChatRole], falling back to [ChatRole.ASSISTANT]
 * for anything unrecognised. A row written by a newer build should still render as a
 * message rather than crash the conversation.
 */
private fun String.toChatRole(): ChatRole =
    ChatRole.entries.firstOrNull { it.name == this } ?: ChatRole.ASSISTANT

fun ChatMessage.toEntity(): ChatMessageEntity =
    ChatMessageEntity(
        id = id,
        content = content,
        role = role.name,
        timestamp = timestampMillis
    )

fun ChatMessageEntity.toDomain(): ChatMessage =
    ChatMessage(
        id = id,
        content = content,
        role = role.toChatRole(),
        timestampMillis = timestamp
    )

fun ChatMessage.toFirestore(): ChatMessageFirestore =
    ChatMessageFirestore(
        id = id,
        content = content,
        role = role.name,
        timestampMillis = timestampMillis
    )

fun ChatMessageFirestore.toDomain(): ChatMessage =
    ChatMessage(
        id = id,
        content = content,
        role = role.toChatRole(),
        timestampMillis = timestampMillis
    )
