package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.StreakBet
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Counts a finished test, mock exam or mini-game towards the daily streak. Only the first
 * session of the day extends it; see [DailyStreak] for the rules. The same moment settles the
 * streak bet, if the user has one (see [StreakBet]).
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
        settleStreakBet(
            target = user.streakBetTarget,
            streakBeforeToday = streak.settledAt(now).current,
            streakNow = updated.current
        )
        return updated.current
    }

    /**
     * Pays the streak bet when today's study reached its target, or forgets it when the streak
     * was lost since it was placed; otherwise leaves it running. A failure here never blocks the
     * session: the bet is simply settled the next time a day is counted.
     *
     * @param target the stored bet target, 0 for no bet
     * @param streakBeforeToday the streak as it stood today before this session was counted
     * @param streakNow the streak after counting this session
     */
    private suspend fun settleStreakBet(target: Int, streakBeforeToday: Int, streakNow: Int) {
        try {
            when {
                StreakBet.isLost(target, streakBeforeToday) -> userRepository.clearStreakBet()
                StreakBet.isWon(target, streakNow) -> userRepository.claimStreakBet(StreakBet.PAYOUT_COINS)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Offline for a signed-in user: the bet stays as it is and is settled on the next study day.
        }
    }
}
