package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import javax.inject.Inject

/** Persists the tutorial only after the player has completed both controls. */
class CompleteGameTutorialUseCase @Inject constructor(private val records: GameRecordRepository) {
    /** Saves completion of [gameId] for the captured [userId]; returns Unit. */
    suspend operator fun invoke(gameId: String, userId: String?) = records.completeTutorial(gameId, userId)
}
