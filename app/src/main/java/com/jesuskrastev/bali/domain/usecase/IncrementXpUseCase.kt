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
open class IncrementXpUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    fun calculateBaseXp(mode: TestMode, accuracy: Int): Int {
        return when (mode) {
            TestMode.PRACTICE -> when {
                accuracy >= 95 -> 20
                accuracy >= 90 -> 16
                accuracy >= 80 -> 12
                accuracy >= 70 -> 8
                else -> 4
            }
            TestMode.CHALLENGE -> when {
                accuracy >= 95 -> 22
                accuracy >= 90 -> 18
                accuracy >= 80 -> 14
                accuracy >= 70 -> 10
                else -> 5
            }
            TestMode.EXAM -> when {
                accuracy >= 95 -> 30
                accuracy >= 90 -> 25
                accuracy >= 80 -> 18
                accuracy >= 70 -> 12
                else -> 6
            }
        }
    }

    fun calculateSpeedBonus(durationSeconds: Int, totalQuestions: Int, accuracy: Int): Int? {
        if (accuracy < 70 || totalQuestions == 0 || durationSeconds == 0) return null

        val avgTime = durationSeconds / totalQuestions
        return when {
            avgTime <= 15 -> 5
            avgTime <= 20 -> 3
            avgTime <= 30 -> 1
            else -> null
        }
    }

    fun calculateStreakBonus(streakDays: Int): Int? {
        return when {
            streakDays >= 30 -> 10
            streakDays >= 14 -> 5
            streakDays >= 7 -> 3
            streakDays >= 5 -> 1
            else -> null
        }
    }

    fun calculatePerfectionBonus(accuracy: Int, totalQuestions: Int): Int? {
        return if (accuracy == 100 && totalQuestions >= 5) 5 else null
    }

    /**
     * Adds the experience a finished activity earned to the user, raising the level if it is reached.
     *
     * @param mode the kind of activity, which sets the base experience
     * @param correctAnswers questions answered correctly
     * @param totalQuestions questions in the activity
     * @param durationSeconds time the activity took, for the speed bonus
     * @param isRepeat true for an already completed lesson: 30 % of the base and no speed or
     *   perfection bonus
     * @return the experience earned, its breakdown and the resulting level
     */
    open suspend operator fun invoke(
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
            newLevel = newLevel,
            baseXp = baseXp,
            bonusPerfection = perfectionBonus,
            bonusFast = speedBonus,
            bonusStreak = streakBonus
        )
    }
}