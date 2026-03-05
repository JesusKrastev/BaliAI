package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.LevelInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

import com.jesuskrastev.bali.domain.util.LevelCalculator

/**
 * Use case that retrieves the user's current XP and delegates to [LevelCalculator]
 * to compute detailed progression metrics safely encapsulated in a [LevelInfo] object.
 */
class CalculateLevelUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): LevelInfo {
        val totalXp = userRepository.get().first()?.xp ?: 0
        val level = LevelCalculator.calculateLevel(totalXp)
        val progress = LevelCalculator.calculateLevelProgress(totalXp)
        val xpAtStartOfLevel = LevelCalculator.totalXpForLevel(level)
        val xpInCurrentLevel = totalXp - xpAtStartOfLevel
        val xpNeeded = LevelCalculator.xpForLevel(level + 1)
        val xpRemaining = xpNeeded - xpInCurrentLevel

        return LevelInfo(
            level = level,
            progress = progress,
            xpInCurrentLevel = xpInCurrentLevel,
            xpNeededForLevel = xpNeeded,
            xpRemainingForNextLevel = xpRemaining
        )
    }
}