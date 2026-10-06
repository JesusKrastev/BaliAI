package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.GameRecord
import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import javax.inject.Inject

/** Record and tutorial preference captured for the same signed-in user. */
data class PreparedGame(val record: GameRecord, val userId: String?, val tutorialCompleted: Boolean)

/** Prepares the local game context without exposing preferences or authentication to the UI. */
class PrepareGameUseCase @Inject constructor(private val records: GameRecordRepository, private val auth: AuthRepository) {
    /** Returns [gameId]'s record and tutorial completion for the current user. */
    suspend operator fun invoke(gameId: String): PreparedGame {
        val userId = auth.currentUser()
        return PreparedGame(records.record(gameId), userId, records.tutorialCompleted(gameId, userId))
    }
}
