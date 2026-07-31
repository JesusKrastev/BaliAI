package com.jesuskrastev.bali.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun get(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preferences: UserEntity)

    @Query("UPDATE users SET currentStreak = :streak, lastPracticeTimestamp = :timestamp, practiceDays = :practiceDaysStr")
    suspend fun updateStreak(streak: Int, timestamp: Long, practiceDaysStr: String)

    @Query("UPDATE users SET weekSessions = :weekSessions, currentWeekStart = :currentWeekStart, lastPracticeTimestamp = :timestamp, practiceDays = :practiceDaysStr")
    suspend fun updateWeeklyProgress(weekSessions: Int, currentWeekStart: Long, timestamp: Long, practiceDaysStr: String)

    @Query("UPDATE users SET xp = :xp, level = :level")
    suspend fun updateXp(xp: Int, level: Int)

    @Query("UPDATE users SET currentStreak = 0")
    suspend fun resetStreak()

    @Query("UPDATE users SET coins = :coins")
    suspend fun updateCoins(coins: Int)

    @Query("UPDATE users SET streakFreezes = :count")
    suspend fun updateStreakFreezes(count: Int)

    @Query("UPDATE users SET highestStreak = :highestStreak")
    suspend fun updateHighestStreak(highestStreak: Int)

    @Query("SELECT COUNT(*) > 0 FROM users")
    fun exists(): Flow<Boolean>

    @Query("DELETE FROM users")
    suspend fun clear()
}
