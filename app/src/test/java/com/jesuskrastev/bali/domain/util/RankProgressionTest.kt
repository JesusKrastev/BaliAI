package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RankProgressionTest {
    /** Keeps reward ids unique and thresholds ordered so claims stay stable across releases. */
    @Test
    fun catalogueHasStableOrderedMilestones() {
        assertThat(RankProgression.ranks.map { it.requiredXp }).isInOrder()
        assertThat(RankProgression.rewards.map { it.requiredXp }).isInOrder()
        assertThat(RankProgression.rewards.map { it.id }.toSet().size)
            .isEqualTo(RankProgression.rewards.size)
        assertThat(RankProgression.rewards.all { it.coins > 0 }).isTrue()
    }

    /** Rank changes exactly when XP reaches its new threshold. */
    @Test
    fun rankBoundaryUsesAccumulatedXp() {
        assertThat(RankProgression.rankFor(299).id).isEqualTo("conductor")
        assertThat(RankProgression.rankFor(300).id).isEqualTo("explorador")
        assertThat(RankProgression.rankFor(100_000).id).isEqualTo("leyenda")
    }
}
