package com.jesuskrastev.bali.domain.repository

/**
 * Remembers, on this device, each mini-game's best score and how many runs were played. The
 * personal best is what a score-chasing game like Bali Drive is replayed for; it is a device
 * preference rather than profile data (no Room or Firestore migration), so a new install simply
 * starts with no record.
 */
interface GameRecordRepository {

    /**
     * The stored record of a game.
     *
     * @param gameId `GameType.id` of the game
     * @return its best score and number of finished runs, zeros when it was never finished here
     */
    suspend fun record(gameId: String): GameRecord

    /**
     * Records a finished run, keeping the higher of [score] and the stored best.
     *
     * @param gameId `GameType.id` of the game
     * @param score points of the run just finished
     * @return the record before this run, so the caller can tell whether it was beaten
     */
    suspend fun submitRun(gameId: String, score: Int): GameRecord
}

/**
 * Best score and number of finished runs of one mini-game on this device.
 *
 * @param bestScore highest score reached, 0 when never finished
 * @param runsPlayed finished runs, used to stop showing first-run hints
 */
data class GameRecord(val bestScore: Int = 0, val runsPlayed: Int = 0)
