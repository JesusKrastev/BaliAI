package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contract for user data access. Abstracts the local (Room) and remote (Firestore)
 * data sources behind a single interface used by the domain layer.
 */
interface UserRepository {
    /** Emits the current user, or null if no user exists yet. */
    fun get(): Flow<User?>

    /** Returns true if a user record exists in the active data source. */
    fun exists(userId: String? = null): Flow<Boolean>

    /** Emits true once a user has completed onboarding (local check only). */
    fun hasCompletedOnboarding(): Flow<Boolean>

    suspend fun insert(user: User)
    suspend fun resetStreak()
    suspend fun updateStreak(streak: Int, timestamp: Long, practiceDays: List<Long>)
    suspend fun updateWeeklyProgress(weekSessions: Int, currentWeekStart: Long, lastPracticeTimestamp: Long, practiceDays: List<Long>)
    suspend fun updateXp(xp: Int, level: Int)
    suspend fun updateEnergy(energy: Int)
    suspend fun updateEnergyAndTimestamp(energy: Int, timestamp: Long)
    suspend fun updateCoins(coins: Int)
    suspend fun updateStreakFreezes(count: Int)
    suspend fun updateHighestStreak(highestStreak: Int)
    suspend fun uploadAll(userId: String, user: User, results: List<TestResult>, answers: List<Answer>): Result<Unit>
    suspend fun clear()
    suspend fun getSchemaVersion(): Int
}
