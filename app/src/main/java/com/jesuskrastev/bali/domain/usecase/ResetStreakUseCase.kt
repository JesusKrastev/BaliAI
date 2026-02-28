package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ResetStreakUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val dateTimeHelper: DateTimeHelper
) {
    companion object {
        private const val STREAK_TOLERANCE = 1
        private const val MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    suspend operator fun invoke(): Int {
        val user = userRepository.get().first() ?: return 0

        val shouldNotReset = user.lastPracticeTimestamp == 0L || user.currentStreak == 0
        if (shouldNotReset) return 0

        val daysSinceLastPractice = dateTimeHelper.getDaysBetween(
            fromTimestamp = user.lastPracticeTimestamp,
            toTimestamp = System.currentTimeMillis()
        )

        val hasStreakExpired = daysSinceLastPractice > STREAK_TOLERANCE

        if (hasStreakExpired) {
            val missedDays = (daysSinceLastPractice - STREAK_TOLERANCE).toInt()

            if (user.streakFreezes >= missedDays) {
                val updatedFreezes = user.streakFreezes - missedDays
                val updatedTimestamp = user.lastPracticeTimestamp + (missedDays * MILLISECONDS_PER_DAY)

                userRepository.updateStreakFreezes(updatedFreezes)
                userRepository.updateStreak(user.currentStreak, updatedTimestamp)
                return missedDays
            } else {
                userRepository.resetStreak()
            }
        }
        return 0
    }
}