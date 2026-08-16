package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.User

/**
 * Contract for asking the AI tutor a question about the Spanish DGT theory exam.
 *
 * Kept separate from [ChatRepository] on purpose: this one talks to the model and owns
 * no state, while [ChatRepository] owns the transcript and never calls the network.
 */
interface AiTutorRepository {
    /**
     * Answers a student question about traffic signs, rules or exam procedure.
     *
     * @param question the student's latest message.
     * @param history previous turns, oldest first, used so the tutor can resolve
     *   follow-ups like "¿y si es de noche?".
     * @param student the student's profile, used to tailor the answer to their licence
     *   and level; null when no profile exists yet.
     * @return the tutor's reply as lightweight markdown.
     * @throws Exception when the model is unreachable or answers with no usable text.
     */
    suspend fun ask(question: String, history: List<ChatMessage>, student: User?): String
}
