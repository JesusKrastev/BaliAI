package com.jesuskrastev.bali.ui.screens.chat

import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.AiTutorRepository
import com.jesuskrastev.bali.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.mockito.kotlin.mock

/** In-memory transcript so tests can assert what actually got persisted. */
class FakeChatRepository(
    initialMessages: List<ChatMessage> = emptyList()
) : ChatRepository {

    private val messages = MutableStateFlow(initialMessages)

    /** Number of times [clear] was called. */
    var clearCount: Int = 0
        private set

    /** Everything persisted so far, oldest first. */
    val saved: List<ChatMessage> get() = messages.value

    override fun observeHistory(): Flow<List<ChatMessage>> = messages

    override suspend fun save(message: ChatMessage) {
        messages.update { current -> current.filterNot { it.id == message.id } + message }
    }

    override suspend fun clear() {
        clearCount++
        messages.value = emptyList()
    }
}

/**
 * Tutor stub that either answers with a canned reply or fails.
 *
 * @param answer what [ask] returns when it succeeds
 * @param failure when non-null, thrown instead of answering
 */
class FakeAiTutorRepository(
    var answer: String = "La señal R-1 obliga a ceder el paso.",
    var failure: Throwable? = null
) : AiTutorRepository {

    /** Every question received, in order. */
    val questions = mutableListOf<String>()

    /** History passed alongside the most recent question. */
    var lastHistory: List<ChatMessage> = emptyList()
        private set

    override suspend fun ask(
        question: String,
        history: List<ChatMessage>,
        student: User?
    ): String {
        questions.add(question)
        lastHistory = history
        failure?.let { throw it }
        return answer
    }
}

/** Records chat analytics without touching Firebase or PostHog. */
class RecordingChatAnalytics : AnalyticsTracker(mock(), mock()) {

    val opened = mutableListOf<Boolean>()
    val sent = mutableListOf<Triple<Int, Boolean, Int>>()
    val failed = mutableListOf<String>()
    val firstStepRewards = mutableListOf<FirstStepReward>()
    var clearedCount: Int = 0
        private set

    override fun chatOpened(hasHistory: Boolean) {
        opened.add(hasHistory)
    }

    override fun chatMessageSent(questionLength: Int, fromSuggestion: Boolean, turnIndex: Int) {
        sent.add(Triple(questionLength, fromSuggestion, turnIndex))
    }

    override fun chatMessageFailed(reason: String) {
        failed.add(reason)
    }

    override fun chatCleared() {
        clearedCount++
    }

    override fun firstStepRewarded(reward: FirstStepReward) {
        firstStepRewards.add(reward)
    }
}
