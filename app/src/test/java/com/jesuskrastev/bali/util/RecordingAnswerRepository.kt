package com.jesuskrastev.bali.util

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [AnswerRepository] that keeps every inserted [Answer] so a test can look at exactly what a
 * ViewModel saved. The list is thread-safe because ViewModels insert from `Dispatchers.IO`.
 */
class RecordingAnswerRepository : AnswerRepository {
    val inserted = CopyOnWriteArrayList<Answer>()

    override suspend fun insert(answer: Answer) {
        inserted += answer
    }

    override fun getRecentMistakes(): Flow<List<Answer>> = flowOf(emptyList())
    override fun getAll(): Flow<List<Answer>> = flowOf(emptyList())
    override suspend fun markAsCorrected(questionText: String) {}
    override suspend fun clear() {}
}
