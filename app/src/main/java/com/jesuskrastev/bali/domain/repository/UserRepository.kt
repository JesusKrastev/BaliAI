package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.ChestReward
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.model.StreakBet
import kotlinx.coroutines.flow.Flow

/**
 * Contract for user data access. Abstracts the local (Room) and remote (Firestore)
 * data sources behind a single interface used by the domain layer.
 */
interface UserRepository {
    /** Atomically grants [reward] when earned and unclaimed; returns true only for a new claim. */
    suspend fun claimRankReward(reward: com.jesuskrastev.bali.domain.model.RankReward): Boolean
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

    /**
     * Marks [task] as done on the account and pays its coins, all as one indivisible write: if
     * the task is already done, the bar was dismissed or the account was never enrolled, nothing
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

    /** Hides the first-steps bar for good; tasks stop paying once it is dismissed. */
    suspend fun dismissFirstSteps()

    suspend fun updateStreakFreezes(count: Int)

    /**
     * Atomically charges [cost] and adds [item] to the user's inventory.
     *
     * @return true when the balance and the item's ownership limit allow the purchase.
     */
    suspend fun purchaseInventoryItem(item: ShopInventoryItem, cost: Int): Boolean

    /**
     * Atomically spends [cost] and grants the random coin [reward] from a surprise chest.
     *
     * @return true when the chest was opened; false when the balance is insufficient.
     */
    suspend fun openSurpriseChest(cost: Int, reward: Int): Boolean

    /**
     * Atomically spends [cost] and grants [reward] from a surprise chest.
     *
     * @return true when the chest was opened; false when the balance is insufficient.
     */
    suspend fun openSurpriseChest(cost: Int, reward: ChestReward): Boolean

    /**
     * Consumes one owned [item].
     *
     * @return true when an item was available and consumed.
     */
    suspend fun consumeInventoryItem(item: ShopInventoryItem): Boolean

    /**
     * Pays the active streak wager once and clears it.
     *
     * @return true if a wager existed and was paid.
     */
    suspend fun claimStreakBet(): Boolean

    /**
     * Atomically charges [cost] and records the streak bet's [target] ([StreakBet]).
     *
     * @return true when the bet was placed; false for an insufficient balance or a bet already active.
     */
    suspend fun placeStreakBet(cost: Int, target: Int): Boolean

    /**
     * Pays [payout] for a won streak bet and clears it, once.
     *
     * @return true if a bet existed and was paid.
     */
    suspend fun claimStreakBet(payout: Int): Boolean

    /** Forgets the streak bet without paying it: a lost bet does not return its stake. */
    suspend fun clearStreakBet()

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
