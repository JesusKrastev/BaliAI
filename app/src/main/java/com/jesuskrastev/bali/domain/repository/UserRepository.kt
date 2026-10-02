package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.DailyStreak
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

    /**
     * Saves every field of the daily streak in one write, so the count, the freezes and the
     * day history never disagree.
     *
     * @param streak the streak to store
     */
    suspend fun updateStreak(streak: DailyStreak)

    suspend fun updateXp(xp: Int, level: Int)

    /** Atomically adds [amount] coins to the user's balance. */
    suspend fun incrementCoins(amount: Int)

    /**
     * Atomically spends [amount] coins from the user's balance if there's enough, in one
     * indivisible read-check-write so two concurrent spends can't both succeed off the
     * same starting balance.
     *
     * @return true if the balance was sufficient and the coins were spent, false otherwise.
     */
    suspend fun decrementCoinsIfEnough(amount: Int): Boolean

    suspend fun updateStreakFreezes(count: Int)

    /**
     * Saves the real exam date the user set, replacing the onboarding estimate.
     *
     * @param examDateMillis local midnight of the exam day
     */
    suspend fun updateExamDate(examDateMillis: Long)

    suspend fun uploadAll(userId: String, user: User, results: List<TestResult>, answers: List<Answer>): Result<Unit>
    suspend fun updateFcmToken(token: String)
    suspend fun clear()
    suspend fun getSchemaVersion(): Int
}
