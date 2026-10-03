package com.jesuskrastev.bali.domain.model

import kotlin.random.Random

/** A reward granted atomically when the user opens a surprise chest. */
sealed interface ChestReward {
    /** Coins credited directly to the balance. */
    data class Coins(val amount: Int) : ChestReward

    /** Consumable shop items added to the user's inventory. */
    data class Inventory(val item: ShopInventoryItem, val quantity: Int = 1) : ChestReward
}

/**
 * Weighted surprise-chest reward table. Every integer roll in 0..99 maps to exactly one reward.
 */
object ChestRewardTable {
    const val TOTAL_WEIGHT = 100

    /**
     * Rolls one reward using [random].
     *
     * @param random source of randomness, replaceable in tests
     * @return the reward mapped from a uniformly distributed roll
     */
    fun roll(random: Random = Random): ChestReward = rewardForRoll(random.nextInt(TOTAL_WEIGHT))

    /**
     * Maps a deterministic [roll] to its reward, exposing exact probability boundaries for tests.
     *
     * @param roll integer from 0 through 99
     * @return the reward assigned to that part of the probability table
     */
    fun rewardForRoll(roll: Int): ChestReward {
        require(roll in 0 until TOTAL_WEIGHT) { "Chest roll must be between 0 and 99" }
        return when (roll) {
            in 0..29 -> ChestReward.Coins(20)
            in 30..54 -> ChestReward.Coins(40)
            in 55..64 -> ChestReward.Coins(80)
            in 65..78 -> ChestReward.Inventory(ShopInventoryItem.HINT)
            in 79..88 -> ChestReward.Inventory(ShopInventoryItem.FIFTY_FIFTY)
            in 89..94 -> ChestReward.Inventory(ShopInventoryItem.DOUBLE_XP)
            else -> ChestReward.Inventory(ShopInventoryItem.DOUBLE_COINS)
        }
    }
}
