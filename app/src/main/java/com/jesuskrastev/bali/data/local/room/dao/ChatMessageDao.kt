package com.jesuskrastev.bali.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jesuskrastev.bali.data.local.room.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    /** Stores a message, replacing any earlier row with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    /** Emits the whole conversation oldest first, re-emitting whenever it changes. */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAll(): Flow<List<ChatMessageEntity>>

    /** Deletes every stored message. */
    @Query("DELETE FROM chat_messages")
    suspend fun clear()
}
