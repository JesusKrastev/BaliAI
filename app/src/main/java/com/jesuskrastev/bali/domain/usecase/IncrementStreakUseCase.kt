package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Counts a finished test, mock exam or mini-game towards the daily streak. Only the first
 * session of the day extends it; see [DailyStreak] for the rules.
 */
open class IncrementStreakUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Records the session and saves the streak.
     *
     * @return the streak in days when this was the first session of the day, so the caller can
     *   celebrate it; -1 when the user had already studied today or there is no profile
     */
    open suspend operator fun invoke(): Int {
        val user = userRepository.get().first() ?: return -1
        val now = System.currentTimeMillis()
        val streak = DailyStreak.of(user)
        if (streak.hasPracticedOn(now)) return -1

        val updated = streak.practicedAt(now)
        userRepository.updateStreak(updated)
        return updated.current
    }
}
