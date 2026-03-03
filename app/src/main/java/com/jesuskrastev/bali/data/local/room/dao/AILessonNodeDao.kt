package com.jesuskrastev.bali.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jesuskrastev.bali.data.local.room.entities.AILessonNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AILessonNodeDao {

    @Query("SELECT * FROM ai_lesson_nodes ORDER BY orderIndex ASC")
    fun getAllNodes(): Flow<List<AILessonNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<AILessonNodeEntity>)

    @Query("UPDATE ai_lesson_nodes SET status = :status, scorePercentage = :score WHERE id = :nodeId")
    suspend fun completeNode(nodeId: String, status: String, score: Int)

    @Query("UPDATE ai_lesson_nodes SET status = :status WHERE id = :nodeId")
    suspend fun updateNodeStatus(nodeId: String, status: String)

    @Query("SELECT * FROM ai_lesson_nodes WHERE id = :nodeId LIMIT 1")
    suspend fun getNodeById(nodeId: String): AILessonNodeEntity?

    @Query("SELECT MAX(orderIndex) FROM ai_lesson_nodes")
    suspend fun getMaxOrderIndex(): Int?

    @Query("DELETE FROM ai_lesson_nodes")
    suspend fun clearAllNodes()
}
