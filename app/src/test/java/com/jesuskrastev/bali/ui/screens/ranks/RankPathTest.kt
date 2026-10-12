package com.jesuskrastev.bali.ui.screens.ranks

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.RankProgression
import org.junit.Test

class RankPathTest {

    private val path = rankPath()

    @Test
    fun `every prize appears exactly once on the road`() {
        val onRoad = path.flatMap {
            when (it) {
                is PathStop.Rank -> listOfNotNull(it.prize)
                is PathStop.Prize -> listOf(it.reward)
            }
        }
        assertThat(onRoad.map { it.id }).containsExactlyElementsIn(RankProgression.rewards.map { it.id })
    }

    @Test
    fun `stops are in the order the user reaches them`() {
        assertThat(path.map { it.requiredXp }).isInOrder()
    }

    /** A rank owns its threshold prize, followed only by rewards before the next rank. */
    @Test
    fun `a prize at a rank threshold is that rank's own prize`() {
        val conductor = path.filterIsInstance<PathStop.Rank>().first { it.rank.id == "conductor" }

        assertThat(conductor.prize?.id).isEqualTo("xp_100")
        assertThat(conductor.prizesOnTheWay.map { it.id }).containsExactly("hints_150")
    }

    @Test
    fun `prizes between ranks alternate sides so the road zigzags`() {
        val sides = path.filterIsInstance<PathStop.Prize>().map { it.side }

        assertThat(sides.zipWithNext().all { (a, b) -> a != b }).isTrue()
    }

    @Test
    fun `a prize is locked, claimable or claimed`() {
        val reward = RankProgression.rewardFor("xp_300")!!

        assertThat(prizeState(reward, 299, emptySet())).isEqualTo(PrizeState.Locked)
        assertThat(prizeState(reward, 300, emptySet())).isEqualTo(PrizeState.Claimable)
        assertThat(prizeState(reward, 900, setOf("xp_300"))).isEqualTo(PrizeState.Claimed)
    }
}
