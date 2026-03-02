package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class IncrementStreakUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val dateTimeHelper: DateTimeHelper
) {
    suspend operator fun invoke(): Int {
        val user = userRepository.get().first() ?: return -1
        val currentTimestamp = System.currentTimeMillis()

        val hasAlreadyPracticedToday = dateTimeHelper.isSameDay(
            timestamp1 = user.lastPracticeTimestamp,
            timestamp2 = currentTimestamp
        )

        if (hasAlreadyPracticedToday) return -1

        val updatedStreak = user.currentStreak + 1
        
        val startOfToday = dateTimeHelper.getStartOfDay(currentTimestamp)
        val updatedPracticeDays = user.practiceDays + startOfToday

        userRepository.updateStreak(
            streak = updatedStreak,
            timestamp = currentTimestamp,
            practiceDays = updatedPracticeDays
        )
        return updatedStreak
    }
}