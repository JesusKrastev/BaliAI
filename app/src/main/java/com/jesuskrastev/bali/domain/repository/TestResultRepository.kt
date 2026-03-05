package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.TestResult
import kotlinx.coroutines.flow.Flow

/**
 * Contract for test result data access. Abstracts the local and remote
 * data sources behind a single interface used by the domain layer.
 */
interface TestResultRepository {
    /** Emits a list of the most recent test results (e.g., last 10). */
    fun getRecent(): Flow<List<TestResult>>
    
    /** Emits the complete list of all test results for the user. */
    fun get(): Flow<List<TestResult>>

    /** Inserts a new test result and returns its generated ID. */
    suspend fun insert(result: TestResult): String
    
    /** Emits the total count of test results. */
    fun count(): Flow<Int>
    
    /** Emits the user's historical average score across all tests, or null if no tests exist. */
    fun getAverageScore(): Flow<Double?>
    
    /** Removes all test results from the local database. */
    suspend fun clear()
}
