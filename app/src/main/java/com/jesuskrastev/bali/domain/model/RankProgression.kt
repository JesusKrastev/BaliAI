package com.jesuskrastev.bali.domain.model

/** A driving themed rank earned from total accumulated XP. */
data class RankTier(val id: String, val name: String, val requiredXp: Int)

/** A coin prize that must be collected after its XP threshold is reached. */
data class RankReward(val id: String, val requiredXp: Int, val coins: Int)

/** Stable rank and reward catalogue; ids must remain stable for saved claims. */
object RankProgression {
    val ranks = listOf(
        RankTier("aprendiz", "Aprendiz", 0),
        RankTier("conductor", "Conductor", 100),
        RankTier("explorador", "Explorador", 300),
        RankTier("piloto", "Piloto", 700),
        RankTier("experto", "Experto", 1400),
        RankTier("maestro", "Maestro", 2500),
        RankTier("leyenda", "Leyenda", 4000)
    )

    val rewards = listOf(
        RankReward("xp_50", 50, 20),
        RankReward("xp_100", 100, 30),
        RankReward("xp_200", 200, 35),
        RankReward("xp_300", 300, 50),
        RankReward("xp_500", 500, 55),
        RankReward("xp_700", 700, 75),
        RankReward("xp_1000", 1000, 80),
        RankReward("xp_1400", 1400, 100),
        RankReward("xp_1900", 1900, 110),
        RankReward("xp_2500", 2500, 150),
        RankReward("xp_3200", 3200, 160),
        RankReward("xp_4000", 4000, 250),
        RankReward("xp_5000", 5000, 300),
        RankReward("xp_6500", 6500, 400)
    )

    /** Returns the highest rank unlocked by [xp]. */
    fun rankFor(xp: Int): RankTier = ranks.lastOrNull { xp >= it.requiredXp } ?: ranks.first()

    /** Returns the prize with stable [id], or null when the id is unknown. */
    fun rewardFor(id: String): RankReward? = rewards.firstOrNull { it.id == id }
}
