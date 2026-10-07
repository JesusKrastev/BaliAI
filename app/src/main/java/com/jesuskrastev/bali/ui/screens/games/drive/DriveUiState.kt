package com.jesuskrastev.bali.ui.screens.games.drive

import com.jesuskrastev.bali.ui.screens.games.GameRewards

/** Screen phases are independent of the engine countdown and driving phases. */
enum class DriveScreenPhase { LOADING, PLAYING, RESULTS }

/** Rewards are never represented as zero while pending or failed. */
enum class DriveRewardStatus { PENDING, COMPLETE, FAILED }

/**
 * Screen state of Bali Drive.
 *
 * @param coached whether the run teaches the controls (the player has not finished the lesson yet)
 * @param runId increases with every run; the screen rebuilds the simulation when it changes
 * @param runSeed seed of the current run's route
 * @param bestScore personal best before the current run
 * @param showHints whether the current run announces each situation (first runs only)
 * @param result the finished run, null while driving
 */
data class DriveUiState(
    val phase: DriveScreenPhase = DriveScreenPhase.LOADING,
    val coached: Boolean = false,
    val error: Boolean = false,
    val runId: Int = 0,
    val runSeed: Long = 0L,
    val bestScore: Int = 0,
    val showHints: Boolean = true,
    val result: DriveResult? = null,
)

/**
 * A finished run and what it earned.
 *
 * @param previousBest personal best before this run
 * @param rewards XP and coins, null until the reward use cases answer
 */
data class DriveResult(val summary: DriveSummary, val previousBest: Int, val rewards: GameRewards? = null, val rewardStatus: DriveRewardStatus = DriveRewardStatus.PENDING) {
    /** True when this run beat the stored best (the first finished run always does). */
    val isNewRecord: Boolean get() = summary.score > previousBest
}
