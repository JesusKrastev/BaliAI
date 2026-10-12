package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RankProgressionTest {
    /** Verifies every rank and reward has a unique id and a strictly increasing XP threshold. */
    @Test
    fun catalogueHasStableOrderedMilestones() {
        assertThat(RankProgression.ranks).hasSize(11)
        assertThat(RankProgression.rewards).hasSize(28)
        assertThat(RankProgression.ranks.map { it.requiredXp }.zipWithNext().all { (a, b) -> a < b }).isTrue()
        assertThat(RankProgression.rewards.map { it.requiredXp }.zipWithNext().all { (a, b) -> a < b }).isTrue()
        assertThat(RankProgression.rewards.map { it.id }.toSet()).hasSize(RankProgression.rewards.size)
        assertThat(RankProgression.ranks.map { it.id }.toSet()).hasSize(RankProgression.ranks.size)
    }

    /** Checks every boundary, including new early ranks and the final Lamborghini rank. */
    @Test
    fun rankBoundaryUsesAccumulatedXp() {
        RankProgression.ranks.drop(1).forEachIndexed { index, rank ->
            assertThat(RankProgression.rankFor(rank.requiredXp - 1)).isEqualTo(RankProgression.ranks[index])
            assertThat(RankProgression.rankFor(rank.requiredXp)).isEqualTo(rank)
        }
        assertThat(RankProgression.rankFor(-1).id).isEqualTo("aprendiz")
        assertThat(RankProgression.rankFor(100_000).id).isEqualTo("leyenda")
    }

    /** Ensures old claims and their advertised coin values remain valid after adding consumables. */
    @Test
    fun existingCoinPrizesAreUnchanged() {
        val original = mapOf(
            50 to 20, 100 to 30, 200 to 35, 300 to 50, 500 to 55, 700 to 75,
            1000 to 80, 1400 to 100, 1900 to 110, 2500 to 150, 3200 to 160,
            4000 to 250, 5000 to 300, 6500 to 400
        )
        original.forEach { (xp, coins) ->
            assertThat(RankProgression.rewardFor("xp_$xp")).isEqualTo(RankReward("xp_$xp", xp, coins))
        }
    }

    /** Checks all four usable consumables appear alongside coins and guaranteed multi-item packs. */
    @Test
    fun pathContainsVariedUsefulRewards() {
        val rewards = RankProgression.rewards
        assertThat(rewards.any { it.coins > 0 }).isTrue()
        assertThat(rewards.any { it.hints > 0 }).isTrue()
        assertThat(rewards.any { it.fiftyFifties > 0 }).isTrue()
        assertThat(rewards.any { it.doubleXpBoosts > 0 }).isTrue()
        assertThat(rewards.any { it.doubleCoinBoosts > 0 }).isTrue()
        assertThat(rewards.any { it.itemKinds > 1 }).isTrue()
        assertThat(rewards.first().requiredXp).isEqualTo(40)
    }

    /** Empty rewards cannot produce misleading claims or blank celebrations. */
    @Test(expected = IllegalArgumentException::class)
    fun emptyPrizeIsRejected() {
        RankReward("empty", 10)
    }

    /** Negative grants cannot remove inventory while claiming a prize. */
    @Test(expected = IllegalArgumentException::class)
    fun negativePrizeIsRejected() {
        RankReward("negative", 10, hints = -1)
    }
}
