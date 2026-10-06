package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import javax.inject.Inject

/** Records a finished run through the domain repository contract. */
class SubmitGameRunUseCase @Inject constructor(private val records: GameRecordRepository) {
    /** Records [score] for [gameId] and returns the previous personal best and run count. */
    suspend operator fun invoke(gameId: String, score: Int) = records.submitRun(gameId, score)
}
