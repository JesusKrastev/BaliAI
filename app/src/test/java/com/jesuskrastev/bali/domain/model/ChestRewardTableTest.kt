package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.ui.screens.store.ShopCatalog
import org.junit.Test

class ChestRewardTableTest {

    /** Verifies every documented probability boundary maps to the intended reward. */
    @Test
    fun `probability boundaries map to the documented rewards`() {
        assertThat(ChestRewardTable.rewardForRoll(0)).isEqualTo(ChestReward.Coins(20))
        assertThat(ChestRewardTable.rewardForRoll(29)).isEqualTo(ChestReward.Coins(20))
        assertThat(ChestRewardTable.rewardForRoll(30)).isEqualTo(ChestReward.Coins(40))
        assertThat(ChestRewardTable.rewardForRoll(54)).isEqualTo(ChestReward.Coins(40))
        assertThat(ChestRewardTable.rewardForRoll(55)).isEqualTo(ChestReward.Coins(80))
        assertThat(ChestRewardTable.rewardForRoll(64)).isEqualTo(ChestReward.Coins(80))
        assertThat(ChestRewardTable.rewardForRoll(65))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.HINT))
        assertThat(ChestRewardTable.rewardForRoll(78))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.HINT))
        assertThat(ChestRewardTable.rewardForRoll(79))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.FIFTY_FIFTY))
        assertThat(ChestRewardTable.rewardForRoll(88))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.FIFTY_FIFTY))
        assertThat(ChestRewardTable.rewardForRoll(89))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.DOUBLE_XP))
        assertThat(ChestRewardTable.rewardForRoll(94))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.DOUBLE_XP))
        assertThat(ChestRewardTable.rewardForRoll(95))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.DOUBLE_COINS))
        assertThat(ChestRewardTable.rewardForRoll(99))
            .isEqualTo(ChestReward.Inventory(ShopInventoryItem.DOUBLE_COINS))
    }

    /** Verifies the table covers exactly one hundred possible rolls with the promised weights. */
    @Test
    fun `the reward table exposes exact item probabilities`() {
        val rewards = (0 until ChestRewardTable.TOTAL_WEIGHT).map(ChestRewardTable::rewardForRoll)

        assertThat(rewards.filterIsInstance<ChestReward.Coins>()).hasSize(65)
        assertThat(rewards.count { it == ChestReward.Inventory(ShopInventoryItem.HINT) }).isEqualTo(14)
        assertThat(rewards.count { it == ChestReward.Inventory(ShopInventoryItem.FIFTY_FIFTY) }).isEqualTo(10)
        assertThat(rewards.count { it == ChestReward.Inventory(ShopInventoryItem.DOUBLE_XP) }).isEqualTo(6)
        assertThat(rewards.count { it == ChestReward.Inventory(ShopInventoryItem.DOUBLE_COINS) }).isEqualTo(5)
    }

    /** Verifies the chest remains a coin sink when inventory prizes are valued at shop prices. */
    @Test
    fun `the expected reward value stays below the chest price`() {
        val catalogValueByItem = mapOf(
            ShopInventoryItem.HINT to ShopCatalog.HINT_COST,
            ShopInventoryItem.FIFTY_FIFTY to ShopCatalog.FIFTY_FIFTY_COST,
            ShopInventoryItem.DOUBLE_XP to ShopCatalog.DOUBLE_XP_COST,
            ShopInventoryItem.DOUBLE_COINS to ShopCatalog.DOUBLE_COINS_COST,
        )
        val totalRewardValue = (0 until ChestRewardTable.TOTAL_WEIGHT).sumOf { roll ->
            when (val reward = ChestRewardTable.rewardForRoll(roll)) {
                is ChestReward.Coins -> reward.amount
                is ChestReward.Inventory -> catalogValueByItem.getValue(reward.item) * reward.quantity
            }
        }
        val expectedRewardValue = totalRewardValue.toDouble() / ChestRewardTable.TOTAL_WEIGHT

        assertThat(expectedRewardValue).isLessThan(ShopCatalog.SURPRISE_CHEST_COST.toDouble())
    }

    /** Verifies invalid deterministic rolls fail instead of silently skewing the table. */
    @Test(expected = IllegalArgumentException::class)
    fun `a roll outside the probability table is rejected`() {
        ChestRewardTable.rewardForRoll(100)
    }
}
