package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.XpEarned
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import com.jesuskrastev.bali.domain.util.LevelCalculator

/**
 * Use case responsible for calculating and applying XP rewards after a user completes a test.
 *
 * It factors in the test mode, accuracy, speed, and current streak to determine
 * base XP and various bonuses. It also checks if the accumulated XP results in a level-up,
 * updating the user's progress in the repository.
 */
class IncrementXpUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    fun calculateBaseXp(mode: TestMode, accuracy: Int): Int {
        return when (mode) {
            TestMode.PRACTICE -> when {
                accuracy >= 95 -> 15
                accuracy >= 90 -> 12
                accuracy >= 80 -> 8
                accuracy >= 70 -> 5
                else -> 3
            }
            TestMode.CHALLENGE -> when {
                accuracy >= 95 -> 25
                accuracy >= 90 -> 20
                accuracy >= 80 -> 15
                accuracy >= 70 -> 10
                else -> 5
            }
            TestMode.EXAM -> when {
                accuracy >= 95 -> 40
                accuracy >= 90 -> 35
                accuracy >= 80 -> 25
                accuracy >= 70 -> 15
                else -> 8
            }
        }
    }

    fun calculateSpeedBonus(durationSeconds: Int, totalQuestions: Int, accuracy: Int): Int? {
        if (accuracy < 70 || totalQuestions == 0 || durationSeconds == 0) return null

        val avgTime = durationSeconds / totalQuestions
        return when {
            avgTime <= 15 -> 10
            avgTime <= 20 -> 5
            avgTime <= 30 -> 3
            else -> null
        }
    }

    fun calculateStreakBonus(streakDays: Int): Int? {
        return when {
            streakDays >= 30 -> 20
            streakDays >= 14 -> 10
            streakDays >= 7 -> 5
            streakDays >= 3 -> 2
            else -> null
        }
    }

    fun calculatePerfectionBonus(accuracy: Int, totalQuestions: Int): Int? {
        return if (accuracy == 100 && totalQuestions >= 5) 15 else null
    }

    suspend operator fun invoke(
        mode: TestMode,
        correctAnswers: Int,
        totalQuestions: Int,
        durationSeconds: Int,
        isRepeat: Boolean = false
    ): XpEarned {
        val user = userRepository.get().first()
            ?: throw IllegalStateException("No user found to update XP")

        val accuracy = if (totalQuestions > 0) ((correctAnswers.toFloat() / totalQuestions) * 100).toInt() else 0

        val baseXp = if (isRepeat) {
            (calculateBaseXp(mode, accuracy) * 0.3).toInt()
        } else {
            calculateBaseXp(mode, accuracy)
        }
        val speedBonus = if (isRepeat) null else calculateSpeedBonus(durationSeconds, totalQuestions, accuracy)
        val perfectionBonus = if (isRepeat) null else calculatePerfectionBonus(accuracy, totalQuestions)
        val streakBonus = calculateStreakBonus(user.currentStreak)

        val totalXpGained = baseXp + (speedBonus ?: 0) + (perfectionBonus ?: 0) + (streakBonus ?: 0)

        val newTotalXp = user.xp + totalXpGained
        val newLevel = LevelCalculator.calculateLevel(newTotalXp)
        val hasLeveledUp = newLevel > user.level

        if (totalXpGained > 0) {
            userRepository.updateXp(
                xp = newTotalXp,
                level = newLevel
            )
        }

        return XpEarned(
            xpGained = totalXpGained,
            levelUp = hasLeveledUp,
            baseXp = baseXp,
            bonusPerfection = perfectionBonus,
            bonusFast = speedBonus,
            bonusStreak = streakBonus
        )
    }
}