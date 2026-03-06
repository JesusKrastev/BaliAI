package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * @deprecated The streak reset logic is now handled by a Firebase Cloud Function
 * running on a weekly schedule.
 */
@Deprecated("Replaced by weekly Cloud Function evaluateWeeklyStreaks")
class ResetStreakUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val dateTimeHelper: DateTimeHelper
) {
    suspend operator fun invoke(): Int {
        return 0
    }
}