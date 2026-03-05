package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.Answer
import kotlinx.coroutines.flow.Flow

/**
 * Contract for answer data access. Abstracts the local and remote
 * data sources behind a single interface used by the domain layer.
 */
interface AnswerRepository {
    /** Inserts a new answer into the database. */
    suspend fun insert(answer: Answer)
    
    /** Emits a list of recent incorrect answers to be used in practice sessions. */
    fun getRecentMistakes(): Flow<List<Answer>>
    
    /** Emits the complete list of all answers submitted by the user. */
    fun getAll(): Flow<List<Answer>>
    
    /** Marks all previous incorrect answers for a specific question text as corrected. */
    suspend fun markAsCorrected(questionText: String)
    
    /** Removes all answers from the local database. */
    suspend fun clear()
}
