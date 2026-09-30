package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.FirstStepTask
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

    /**
     * Marks [task] as done on the account and pays its coins, all as one indivisible write: if
     * the task is already done, the card was dismissed or the account was never enrolled, nothing
     * changes and nothing is paid, so the same reward can never be collected twice (even from two
     * devices). Completing the last task also pays [com.jesuskrastev.bali.domain.model.FIRST_STEPS_BONUS_COINS].
     *
     * Account-scoped: with no signed-in user there is nothing to record and it returns 0.
     *
     * @param task the first step the student just completed.
     * @return the coins paid (task reward plus bonus when it was the last one), or 0 when nothing
     *   was paid or the write failed. A failure is reported, never thrown: the task simply stays
     *   pending and pays the next time the student does it.
     */
    suspend fun completeFirstStep(task: FirstStepTask): Int

    /** Hides the first-steps card for good; tasks stop paying once it is dismissed. */
    suspend fun dismissFirstSteps()

    suspend fun updateStreakFreezes(count: Int)
    suspend fun updateHighestStreak(highestStreak: Int)
    suspend fun uploadAll(userId: String, user: User, results: List<TestResult>, answers: List<Answer>): Result<Unit>
    suspend fun updateFcmToken(token: String)
    suspend fun clear()
    suspend fun getSchemaVersion(): Int
}
