package com.jesuskrastev.bali.domain.usecase

import android.util.Log
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.model.LevelInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.pow

class CalculateLevelUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl
) {

    private object Rules {
        const val FIRST_LEVEL_XP = 10
        const val BASE_XP = 25
        const val GROWTH_FACTOR = 1.4
    }

    /**
     * Calcula el XP necesario para alcanzar un nivel específico
     */
    fun xpForLevel(level: Int): Int {
        return when (level) {
            0 -> 0 // Nivel inicial
            1 -> Rules.FIRST_LEVEL_XP // Primer nivel muy accesible
            else -> (Rules.BASE_XP * Rules.GROWTH_FACTOR.pow(level - 2)).toInt()
        }
    }

    /**
     * Calcula el XP total acumulado necesario para estar en un nivel
     */
    fun totalXpForLevel(level: Int): Int {
        if (level <= 0) return 0

        var total = 0
        for (i in 1..level) {
            total += xpForLevel(i)
        }
        return total
    }

    /**
     * Calcula el nivel actual basado en el XP total
     */
    fun calculateLevel(totalXp: Int): Int {
        if (totalXp < Rules.FIRST_LEVEL_XP) return 0

        var level = 0
        var accumulatedXp = 0

        while (true) {
            val xpNeeded = xpForLevel(level + 1)
            if (accumulatedXp + xpNeeded > totalXp) {
                break
            }
            accumulatedXp += xpNeeded
            level++
        }

        return level
    }

    /**
     * Calcula el progreso dentro del nivel actual (0.0 a 1.0)
     */
    fun calculateLevelProgress(totalXp: Int): Float {
        val currentLevel = calculateLevel(totalXp)
        val xpAtStartOfLevel = totalXpForLevel(currentLevel)
        val xpInCurrentLevel = totalXp - xpAtStartOfLevel
        val xpNeededForNextLevel = xpForLevel(currentLevel + 1)

        if (xpNeededForNextLevel == 0) return 0f

        return (xpInCurrentLevel.toFloat() / xpNeededForNextLevel).coerceIn(0f, 1f)
    }

    /**
     * XP necesario para el siguiente nivel
     */
    fun xpForNextLevel(currentLevel: Int): Int {
        return xpForLevel(currentLevel + 1)
    }

    /**
     * XP restante para alcanzar el siguiente nivel
     */
    fun xpRemainingForNextLevel(totalXp: Int): Int {
        val currentLevel = calculateLevel(totalXp)
        val xpAtStartOfLevel = totalXpForLevel(currentLevel)
        val xpInCurrentLevel = totalXp - xpAtStartOfLevel
        val xpNeededForNextLevel = xpForLevel(currentLevel + 1)

        return xpNeededForNextLevel - xpInCurrentLevel
    }

    suspend operator fun invoke(): LevelInfo {
        val totalXp = userRepository.get().first()?.xp ?: 0
        val level = calculateLevel(totalXp)
        val progress = calculateLevelProgress(totalXp)
        val xpAtStartOfLevel = totalXpForLevel(level)
        val xpInCurrentLevel = totalXp - xpAtStartOfLevel
        val xpNeeded = xpForLevel(level + 1)
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