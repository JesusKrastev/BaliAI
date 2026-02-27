package com.jesuskrastev.bali.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: AnswerEntity)

    @Query("SELECT * FROM answers WHERE isCorrect = 0 ORDER BY timestamp DESC LIMIT 20")
    fun getRecentMistakes(): Flow<List<AnswerEntity>>

    @Query("SELECT * FROM answers")
    fun getAll(): Flow<List<AnswerEntity>>

    @Query("UPDATE answers SET isCorrect = 1 WHERE questionText = :questionText AND isCorrect = 0")
    suspend fun markAsCorrected(questionText: String)

    @Query("DELETE FROM answers")
    suspend fun clear()
}
