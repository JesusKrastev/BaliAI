package com.jesuskrastev.bali.domain.model

data class XpEarned(
    val xpGained: Int = 0,
    val levelUp: Boolean = false,
    val baseXp: Int = 0,
    val bonusPerfection: Int? = null,
    val bonusFast: Int? = null,
    val bonusStreak: Int? = null
)