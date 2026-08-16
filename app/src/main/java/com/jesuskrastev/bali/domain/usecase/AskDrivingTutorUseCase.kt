package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.ChatRole
import com.jesuskrastev.bali.domain.repository.AiTutorRepository
import com.jesuskrastev.bali.domain.repository.ChatRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Sends a student question to the AI tutor and stores both turns of the exchange.
 *
 * The question is persisted *before* the model is called so the message the student
 * just typed survives a failed request, an app kill or a lost connection: retrying
 * re-asks it instead of losing it.
 */
open class AskDrivingTutorUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiTutorRepository: AiTutorRepository,
    private val userRepository: UserRepository
) {

    /**
     * Asks the tutor [question] and appends the reply to the conversation.
     *
     * @param question the student's message; blank input is rejected.
     * @param persistQuestion false when the question is already in the transcript —
     *   the case when retrying a turn whose answer failed — so it isn't stored twice.
     * @return the tutor's reply on success, or the failure that stopped it. The student
     *   message stays persisted either way.
     */
    open suspend operator fun invoke(
        question: String,
        persistQuestion: Boolean = true
    ): Result<ChatMessage> {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("La pregunta está vacía"))

        // Captured before storing the new turn so the model never sees the question twice.
        val history = chatRepository.observeHistory().first().takeLast(MAX_HISTORY_TURNS)

        if (persistQuestion) {
            chatRepository.save(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = trimmed,
                    role = ChatRole.USER
                )
            )
        }

        return runCatching {
            val student = userRepository.get().first()
            val answer = aiTutorRepository.ask(trimmed, history, student)
            ChatMessage(
                id = UUID.randomUUID().toString(),
                content = answer,
                role = ChatRole.ASSISTANT
            ).also { chatRepository.save(it) }
        }
    }

    companion object {
        /**
         * How many previous turns travel with each question. Enough for follow-ups
         * ("¿y de noche?") without letting an old conversation inflate every prompt.
         */
        const val MAX_HISTORY_TURNS = 12
    }
}
