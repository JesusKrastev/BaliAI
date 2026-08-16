package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one turn of the AI tutor conversation.
 *
 * [role] stores the `ChatRole` name rather than its ordinal so reordering the enum
 * can never re-attribute stored messages to the wrong speaker.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val role: String,
    val timestamp: Long
)
