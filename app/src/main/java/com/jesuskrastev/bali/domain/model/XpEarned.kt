package com.jesuskrastev.bali.domain.model

/**
 * Experience earned by one finished test, exam or mini-game, and what it did to the level.
 *
 * @property xpGained total experience earned, bonuses included
 * @property levelUp whether the experience took the user to a higher level
 * @property newLevel the user's level after adding [xpGained]; 0 when unknown
 * @property baseXp experience earned before bonuses
 * @property bonusPerfection bonus for a perfect result, or null when not earned
 * @property bonusFast bonus for finishing quickly, or null when not earned
 * @property bonusStreak bonus for the current streak, or null when not earned
 */
data class XpEarned(
    val xpGained: Int = 0,
    val levelUp: Boolean = false,
    val newLevel: Int = 0,
    val baseXp: Int = 0,
    val bonusPerfection: Int? = null,
    val bonusFast: Int? = null,
    val bonusStreak: Int? = null
)
