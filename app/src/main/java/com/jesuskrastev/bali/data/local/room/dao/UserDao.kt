package com.jesuskrastev.bali.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jesuskrastev.bali.data.local.room.Converters
import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import com.jesuskrastev.bali.domain.model.RankReward
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    /** Returns the current local profile once for an atomic reward claim. */
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getOnce(): UserEntity?

    /** Adds [coins] and writes [claimedJson] for [id] with [requiredXp]; returns updated row count. */
    @Query(
        "UPDATE users SET coins = coins + :coins, claimedRankRewards = :claimedJson " +
            "WHERE id = :id AND xp >= :requiredXp"
    )
    suspend fun saveRankClaim(id: String, requiredXp: Int, coins: Int, claimedJson: String): Int

    /** Returns whether [reward] was newly claimed, checking and updating in one Room transaction. */
    @Transaction
    suspend fun claimRankReward(reward: RankReward): Boolean {
        val user = getOnce() ?: return false
        if (user.xp < reward.requiredXp || reward.id in user.claimedRankRewards) return false
        return saveRankClaim(
            user.id,
            reward.requiredXp,
            reward.coins,
            Converters().fromStringList(user.claimedRankRewards + reward.id)
        ) == 1
    }

    @Query("SELECT * FROM users LIMIT 1")
    fun get(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preferences: UserEntity)

    /** Writes every daily-streak field at once; the day lists are JSON arrays of local midnights. */
    @Query(
        "UPDATE users SET currentStreak = :current, highestStreak = :highest, " +
            "streakFreezes = :freezes, lastPracticeTimestamp = :lastPracticeMillis, " +
            "lostStreak = :lostStreak, lostStreakDayMillis = :lostStreakDayMillis, " +
            "practiceDays = :practiceDaysJson, frozenDays = :frozenDaysJson"
    )
    suspend fun updateStreak(
        current: Int,
        highest: Int,
        freezes: Int,
        lastPracticeMillis: Long,
        lostStreak: Int,
        lostStreakDayMillis: Long,
        practiceDaysJson: String,
        frozenDaysJson: String
    )

    @Query("UPDATE users SET xp = :xp, level = :level")
    suspend fun updateXp(xp: Int, level: Int)

    /** Atomically adds [amount] to the local user's coin balance in one SQL statement. */
    @Query("UPDATE users SET coins = coins + :amount")
    suspend fun incrementCoins(amount: Int)

    /**
     * Atomically subtracts [amount] from the local user's coin balance, but only if the
     * row's current balance is at least [amount]. The condition is evaluated by SQLite
     * against the live row, not a value read earlier by the caller, so two overlapping
     * calls can't both succeed off the same starting balance.
     *
     * @return the number of rows updated: 1 if the balance was sufficient and the
     *   subtraction applied, 0 if it was insufficient and nothing changed.
     */
    @Query("UPDATE users SET coins = coins - :amount WHERE coins >= :amount")
    suspend fun decrementCoinsIfEnough(amount: Int): Int

    @Query("UPDATE users SET streakFreezes = :count")
    suspend fun updateStreakFreezes(count: Int)

    /**
     * Charges [cost] and adds [item] in one SQLite statement, preventing an interrupted
     * purchase from taking coins without granting its inventory item.
     *
     * @return 1 for a completed purchase, or 0 for insufficient coins.
     */
    @Query(
        "UPDATE users SET coins = coins - :cost, " +
            "hints = hints + CASE WHEN :item = 'HINT' THEN 1 ELSE 0 END, " +
            "fiftyFifties = fiftyFifties + CASE WHEN :item = 'FIFTY_FIFTY' THEN 1 ELSE 0 END, " +
            "doubleXpBoosts = doubleXpBoosts + CASE WHEN :item = 'DOUBLE_XP' THEN 1 ELSE 0 END, " +
            "doubleCoinBoosts = doubleCoinBoosts + CASE WHEN :item = 'DOUBLE_COINS' THEN 1 ELSE 0 END " +
            "WHERE coins >= :cost"
    )
    suspend fun purchaseInventoryItem(item: String, cost: Int): Int

    /**
     * Opens a surprise chest by charging [cost] and granting either coins or one inventory item
     * in the same SQLite statement.
     *
     * @return 1 if the chest opened, otherwise 0 when the balance was insufficient.
     */
    @Query(
        "UPDATE users SET coins = coins - :cost + :coinReward, " +
            "hints = hints + CASE WHEN :item = 'HINT' THEN :quantity ELSE 0 END, " +
            "fiftyFifties = fiftyFifties + CASE WHEN :item = 'FIFTY_FIFTY' THEN :quantity ELSE 0 END, " +
            "doubleXpBoosts = doubleXpBoosts + CASE WHEN :item = 'DOUBLE_XP' THEN :quantity ELSE 0 END, " +
            "doubleCoinBoosts = doubleCoinBoosts + CASE WHEN :item = 'DOUBLE_COINS' THEN :quantity ELSE 0 END " +
            "WHERE coins >= :cost"
    )
    suspend fun openSurpriseChest(cost: Int, coinReward: Int, item: String?, quantity: Int): Int

    /**
     * Removes exactly one consumable [item] only when it is available.
     *
     * @return 1 if an item was consumed, otherwise 0.
     */
    @Query(
        "UPDATE users SET " +
            "hints = hints - CASE WHEN :item = 'HINT' THEN 1 ELSE 0 END, " +
            "fiftyFifties = fiftyFifties - CASE WHEN :item = 'FIFTY_FIFTY' THEN 1 ELSE 0 END, " +
            "doubleXpBoosts = doubleXpBoosts - CASE WHEN :item = 'DOUBLE_XP' THEN 1 ELSE 0 END, " +
            "doubleCoinBoosts = doubleCoinBoosts - CASE WHEN :item = 'DOUBLE_COINS' THEN 1 ELSE 0 END " +
            "WHERE (:item = 'HINT' AND hints > 0) OR " +
            "(:item = 'FIFTY_FIFTY' AND fiftyFifties > 0) OR " +
            "(:item = 'DOUBLE_XP' AND doubleXpBoosts > 0) OR " +
            "(:item = 'DOUBLE_COINS' AND doubleCoinBoosts > 0)"
    )
    suspend fun consumeInventoryItem(item: String): Int

    /**
     * Charges [cost] and records the streak bet's [target] in one statement.
     *
     * @return 1 if the bet was placed, 0 for insufficient coins or a bet that is already active.
     */
    @Query(
        "UPDATE users SET coins = coins - :cost, streakBetTarget = :target " +
            "WHERE coins >= :cost AND streakBetTarget = 0"
    )
    suspend fun placeStreakBet(cost: Int, target: Int): Int

    /**
     * Pays the won streak bet one time and clears it.
     *
     * @return 1 if a bet was paid, otherwise 0.
     */
    @Query("UPDATE users SET coins = coins + :payout, streakBetTarget = 0 WHERE streakBetTarget > 0")
    suspend fun claimStreakBet(payout: Int): Int

    /** Forgets the streak bet without paying it: the stake of a lost bet is not returned. */
    @Query("UPDATE users SET streakBetTarget = 0")
    suspend fun clearStreakBet()

    /**
     * Replaces the exam date with the one the user picked on Home's plan card.
     *
     * @param examDateMillis local midnight of the exam day
     */
    @Query("UPDATE users SET examDateMillis = :examDateMillis")
    suspend fun updateExamDate(examDateMillis: Long)

    @Query("SELECT COUNT(*) > 0 FROM users")
    fun exists(): Flow<Boolean>

    @Query("DELETE FROM users")
    suspend fun clear()
}
