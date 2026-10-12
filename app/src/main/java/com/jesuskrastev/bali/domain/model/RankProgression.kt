package com.jesuskrastev.bali.domain.model

/** A driving themed rank earned from total accumulated XP. */
data class RankTier(val id: String, val name: String, val requiredXp: Int)

/**
 * A guaranteed prize collected once at [requiredXp], using the user's existing inventory.
 * [id] remains stable across releases; quantities are additive and never activate boosts early.
 */
data class RankReward(
    val id: String,
    val requiredXp: Int,
    val coins: Int = 0,
    val hints: Int = 0,
    val fiftyFifties: Int = 0,
    val doubleXpBoosts: Int = 0,
    val doubleCoinBoosts: Int = 0
) {
    init {
        require(requiredXp >= 0)
        require(listOf(coins, hints, fiftyFifties, doubleXpBoosts, doubleCoinBoosts).all { it >= 0 })
        require(itemKinds > 0) { "A rank reward must grant something" }
    }

    /** Number of distinct prizes in this reward, including coins. */
    val itemKinds: Int
        get() = listOf(coins, hints, fiftyFifties, doubleXpBoosts, doubleCoinBoosts).count { it > 0 }

    /** Whether this reward adds consumables to the inventory. */
    val hasInventory: Boolean
        get() = hints + fiftyFifties + doubleXpBoosts + doubleCoinBoosts > 0
}

/** Stable catalogue: existing rank thresholds and coin prizes stay unchanged for saved profiles. */
object RankProgression {
    val ranks = listOf(
        RankTier("aprendiz", "Aprendiz", 0),
        RankTier("novato", "Novato", 40),
        RankTier("conductor", "Conductor", 100),
        RankTier("agil", "Conductor ágil", 200),
        RankTier("explorador", "Explorador", 300),
        RankTier("aventurero", "Aventurero", 500),
        RankTier("piloto", "Piloto", 700),
        RankTier("especialista", "Especialista", 1000),
        RankTier("experto", "Experto", 1400),
        RankTier("maestro", "Maestro", 2500),
        RankTier("leyenda", "Leyenda", 4000)
    )

    val rewards = listOf(
        RankReward("hint_40", 40, hints = 1),
        RankReward("xp_50", 50, 20),
        RankReward("fifty_75", 75, fiftyFifties = 1),
        RankReward("xp_100", 100, 30),
        RankReward("hints_150", 150, hints = 2),
        RankReward("xp_200", 200, 35),
        RankReward("double_xp_250", 250, doubleXpBoosts = 1),
        RankReward("xp_300", 300, 50),
        RankReward("double_coins_400", 400, doubleCoinBoosts = 1),
        RankReward("xp_500", 500, 55),
        RankReward("practice_pack_600", 600, hints = 2, fiftyFifties = 1),
        RankReward("xp_700", 700, 75),
        RankReward("double_xp_850", 850, doubleXpBoosts = 1),
        RankReward("xp_1000", 1000, 80),
        RankReward("fifties_1200", 1200, fiftyFifties = 2),
        RankReward("xp_1400", 1400, 100),
        RankReward("double_coins_1700", 1700, doubleCoinBoosts = 2),
        RankReward("xp_1900", 1900, 110),
        RankReward("practice_pack_2200", 2200, hints = 3, fiftyFifties = 2),
        RankReward("xp_2500", 2500, 150),
        RankReward("double_xp_2800", 2800, doubleXpBoosts = 2),
        RankReward("xp_3200", 3200, 160),
        RankReward("boost_pack_3600", 3600, doubleXpBoosts = 2, doubleCoinBoosts = 2),
        RankReward("xp_4000", 4000, 250),
        RankReward("legend_pack_4500", 4500, hints = 3, fiftyFifties = 3),
        RankReward("xp_5000", 5000, 300),
        RankReward("boost_pack_5700", 5700, doubleXpBoosts = 3, doubleCoinBoosts = 3),
        RankReward("xp_6500", 6500, 400)
    )

    /** Returns the highest rank unlocked by accumulated [xp]. */
    fun rankFor(xp: Int): RankTier = ranks.lastOrNull { xp >= it.requiredXp } ?: ranks.first()

    /** Returns the prize with stable [id], or null when the id is unknown. */
    fun rewardFor(id: String): RankReward? = rewards.firstOrNull { it.id == id }
}
