package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class IncrementStreakUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val dateTimeHelper: DateTimeHelper
) {
    suspend operator fun invoke() {
        val user = userRepository.get().first() ?: return
        val currentTimestamp = System.currentTimeMillis()

        val hasAlreadyPracticedToday = dateTimeHelper.isSameDay(
            timestamp1 = user.lastPracticeTimestamp,
            timestamp2 = currentTimestamp
        )

        if (hasAlreadyPracticedToday) return

        val updatedStreak = user.currentStreak + 1
        userRepository.updateStreak(
            streak = updatedStreak,
            timestamp = currentTimestamp
        )
    }
}