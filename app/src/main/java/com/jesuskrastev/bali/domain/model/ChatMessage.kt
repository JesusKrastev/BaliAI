package com.jesuskrastev.bali.domain.model

/**
 * Who wrote a message in the AI tutor conversation.
 *
 * The names double as the values persisted in Room and Firestore, so renaming a
 * constant would orphan existing history — add a new one instead.
 */
enum class ChatRole {
    /** Written by the student. */
    USER,

    /** Written by Bali, the Gemini-backed tutor. */
    ASSISTANT
}

/**
 * A single turn of the conversation between the student and the AI tutor.
 *
 * @property id stable identifier, generated on the client so the optimistic UI and the
 *   persisted row refer to the same message.
 * @property content the message text. For assistant turns this is lightweight markdown
 *   (bold with `**`, bullet lists) that the UI renders inline.
 * @property role who wrote it.
 * @property timestampMillis creation time in epoch milliseconds; the conversation is
 *   always ordered by this field.
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val role: ChatRole,
    val timestampMillis: Long = System.currentTimeMillis()
)
