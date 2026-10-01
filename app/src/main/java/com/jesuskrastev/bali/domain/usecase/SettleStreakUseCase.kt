package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Brings stored streak momentum up to today when the app opens. Missed days spend freezes first
 * and then lower the level one step at a time, so someone who stopped studying does not keep a
 * stale speedometer.
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
