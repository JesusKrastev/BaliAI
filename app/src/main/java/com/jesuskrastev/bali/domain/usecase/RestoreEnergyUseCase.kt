package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RestoreEnergyUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val dateTimeHelper: DateTimeHelper
) {
    companion object {
        private const val MAX_ENERGY = 3
        private const val MINIMUM_DAYS_TO_RESTORE = 1
    }

    suspend operator fun invoke() {
        val user = userRepository.get().first() ?: return

        val shouldNotRestore = user.lastPracticeTimestamp == 0L
        if (shouldNotRestore) return

        val daysSinceLastPractice = dateTimeHelper.getDaysBetween(
            fromTimestamp = user.lastPracticeTimestamp,
            toTimestamp = System.currentTimeMillis()
        )

        val shouldRestoreEnergy = daysSinceLastPractice >= MINIMUM_DAYS_TO_RESTORE

        if (shouldRestoreEnergy) {
            userRepository.updateEnergy(MAX_ENERGY)
        }
    }
}