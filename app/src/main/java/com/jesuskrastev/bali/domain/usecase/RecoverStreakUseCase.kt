package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Buys back the streak the user lost by missing yesterday, for [DailyStreak.RECOVERY_COST_COINS].
 * The rules for what comes back and until when live in [DailyStreak.recoveredAt].
 */
open class RecoverStreakUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val decrementCoinsUseCase: DecrementCoinsUseCase
) {
    /**
     * Charges the coins and restores the streak, or does nothing when there is no streak to
     * recover or the balance falls short. Nothing is charged unless the streak is restored.
     *
     * @return the streak count after recovering, or null when nothing was recovered
     */
    open suspend operator fun invoke(): Int? {
        val user = userRepository.get().first() ?: return null
        val now = System.currentTimeMillis()
        val settled = DailyStreak.of(user).settledAt(now)
        if (settled.recoverableStreakAt(now) == 0) return null

        val recovered = settled.recoveredAt(now)
        if (!decrementCoinsUseCase(DailyStreak.RECOVERY_COST_COINS)) return null

        userRepository.updateStreak(recovered)
        return recovered.current
    }
}
