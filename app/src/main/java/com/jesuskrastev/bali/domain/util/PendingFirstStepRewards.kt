package com.jesuskrastev.bali.domain.util

import com.jesuskrastev.bali.domain.model.FirstStepReward
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewards earned on other screens (a test, the chat, a mini-game) that Home has yet to celebrate.
 *
 * Process-wide rather than per-ViewModel on purpose: finishing a test rebuilds Home, so anything
 * held by its ViewModel would be gone by the time the student lands back on it. It is in-memory
 * only — the coins themselves are already persisted, so losing a pending celebration to a process
 * death costs a bit of confetti, never the money.
 */
@Singleton
class PendingFirstStepRewards @Inject constructor() {

    private val queue = MutableStateFlow<List<FirstStepReward>>(emptyList())

    /** Emits the oldest reward still waiting to be celebrated, or null when there is none. */
    val next: Flow<FirstStepReward?> = queue.map { it.firstOrNull() }

    /**
     * Queues [reward] behind any already waiting.
     *
     * @param reward the coins to celebrate once Home is on screen.
     */
    fun publish(reward: FirstStepReward) = queue.update { it + reward }

    /** Drops the reward currently at the head of the queue, once Home has celebrated it. */
    fun consume() = queue.update { it.drop(1) }
}
