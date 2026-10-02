package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Brings the stored streak up to today when the app opens: days missed since the last visit
 * spend freezes or end the streak. Without it, someone who stopped studying would still see
 * their old streak until their next session.
 */
class SettleStreakUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /** Saves the settled streak, only when something changed. */
    suspend operator fun invoke() {
        val user = userRepository.get().first() ?: return
        val streak = DailyStreak.of(user)
        val settled = streak.settledAt(System.currentTimeMillis())
        if (settled != streak) userRepository.updateStreak(settled)
    }
}
