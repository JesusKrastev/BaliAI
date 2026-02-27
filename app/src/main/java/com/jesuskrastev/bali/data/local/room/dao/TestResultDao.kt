package com.jesuskrastev.bali.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jesuskrastev.bali.data.local.room.entities.TestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC LIMIT 10")
    fun getRecent(): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun get(): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results")
    suspend fun getAll(): List<TestResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TestResultEntity): Long

    @Query("SELECT COUNT(*) FROM test_results")
    fun count(): Flow<Int>

    @Query("SELECT AVG(score * 100.0 / total) FROM test_results")
    fun getAverageScore(): Flow<Double?>

    @Query("DELETE FROM test_results")
    suspend fun clear()
}
