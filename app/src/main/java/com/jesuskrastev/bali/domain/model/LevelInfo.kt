package com.jesuskrastev.bali.domain.model

data class LevelInfo(
    val level: Int,
    val progress: Float,
    val xpInCurrentLevel: Int,
    val xpNeededForLevel: Int,
    val xpRemainingForNextLevel: Int
)