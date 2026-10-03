package com.jesuskrastev.bali.ui.screens.ranks

import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.domain.model.RankReward
import com.jesuskrastev.bali.domain.model.RankTier

/** One stop on the rank road, in the order the user reaches them. */
sealed interface PathStop {
    /** XP needed to reach this stop. */
    val requiredXp: Int

    /** The rank this stop belongs to. */
    val rank: RankTier

    /**
     * A rank milestone.
     *
     * @property prize the coins paid for reaching the rank, or null for the starting rank
     * @property prizesOnTheWay the prizes between this rank and the next one
     */
    data class Rank(
        override val rank: RankTier,
        val prize: RankReward?,
        val prizesOnTheWay: List<RankReward>
    ) : PathStop {
        override val requiredXp: Int get() = rank.requiredXp
    }

    /**
     * A coin prize between two ranks.
     *
     * @property side 0 or 1, alternating, so the road zigzags from prize to prize
     */
    data class Prize(override val rank: RankTier, val reward: RankReward, val side: Int) : PathStop {
        override val requiredXp: Int get() = reward.requiredXp
    }
}

/** How a prize looks to the user right now. */
enum class PrizeState { Locked, Claimable, Claimed }

/**
 * Lays the catalogue out as a road: each rank, then the prizes earned on the way to the next one.
 * A prize whose XP equals a rank's is that rank's own prize and sits on the milestone.
 *
 * @param ranks ranks in ascending XP
 * @param rewards prizes in any order
 * @return the stops from the first rank to the last prize
 */
fun rankPath(
    ranks: List<RankTier> = RankProgression.ranks,
    rewards: List<RankReward> = RankProgression.rewards
): List<PathStop> {
    val sorted = rewards.sortedBy { it.requiredXp }
    var side = 0
    return ranks.flatMapIndexed { index, rank ->
        val nextXp = ranks.getOrNull(index + 1)?.requiredXp ?: Int.MAX_VALUE
        val onTheWay = sorted.filter { it.requiredXp > rank.requiredXp && it.requiredXp < nextXp }
        val milestone = PathStop.Rank(rank, sorted.firstOrNull { it.requiredXp == rank.requiredXp }, onTheWay)
        listOf(milestone) + onTheWay.map { PathStop.Prize(rank, it, side).also { side = 1 - side } }
    }
}

/**
 * The state of [reward] for a user with [xp] who already collected [claimedIds].
 *
 * @return [PrizeState.Claimed], [PrizeState.Claimable] or [PrizeState.Locked]
 */
fun prizeState(reward: RankReward, xp: Int, claimedIds: Set<String>): PrizeState = when {
    reward.id in claimedIds -> PrizeState.Claimed
    xp >= reward.requiredXp -> PrizeState.Claimable
    else -> PrizeState.Locked
}

/**
 * Everything the celebration of a collected prize says.
 *
 * @property reward the prize just collected
 * @property rank the rank this prize is the reward for reaching, or null for a prize on the way
 * @property coinsAfter the balance once the prize is added
 * @property stillClaimable prizes already reached and still waiting to be collected
 * @property next the next prize ahead on the road, or null when none is left
 * @property xpToNext XP still needed for [next], or 0 when there is none
 */
data class PrizeCelebration(
    val reward: RankReward,
    val rank: RankTier?,
    val coinsAfter: Int,
    val stillClaimable: Int,
    val next: RankReward?,
    val xpToNext: Int
)

/**
 * Builds the celebration for [reward], collected by a user with [xp].
 *
 * @param claimedIds the prizes collected, [reward] included
 * @param coinsAfter the balance with [reward] already added
 * @param ranks ranks in ascending XP
 * @param rewards the prize catalogue
 */
fun prizeCelebrationOf(
    reward: RankReward,
    xp: Int,
    claimedIds: Set<String>,
    coinsAfter: Int,
    ranks: List<RankTier> = RankProgression.ranks,
    rewards: List<RankReward> = RankProgression.rewards
): PrizeCelebration {
    val next = rewards.filter { it.requiredXp > xp }.minByOrNull { it.requiredXp }
    return PrizeCelebration(
        reward = reward,
        rank = ranks.firstOrNull { it.requiredXp == reward.requiredXp && it.requiredXp > 0 },
        coinsAfter = coinsAfter,
        stillClaimable = rewards.count { prizeState(it, xp, claimedIds) == PrizeState.Claimable },
        next = next,
        xpToNext = next?.let { it.requiredXp - xp } ?: 0
    )
}
