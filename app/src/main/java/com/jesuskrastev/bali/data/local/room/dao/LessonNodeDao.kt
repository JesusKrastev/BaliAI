package com.jesuskrastev.bali.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jesuskrastev.bali.data.local.room.entities.LessonNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonNodeDao {

    @Query("SELECT * FROM lesson_nodes ORDER BY orderIndex ASC")
    fun getAllNodes(): Flow<List<LessonNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<LessonNodeEntity>)

    @Query("UPDATE lesson_nodes SET status = :status, scorePercentage = :score WHERE id = :nodeId")
    suspend fun completeNode(nodeId: String, status: String, score: Int)

    @Query("UPDATE lesson_nodes SET status = :status WHERE id = :nodeId")
    suspend fun updateNodeStatus(nodeId: String, status: String)

    @Query("SELECT * FROM lesson_nodes WHERE id = :nodeId LIMIT 1")
    suspend fun getNodeById(nodeId: String): LessonNodeEntity?

    @Query("SELECT MAX(orderIndex) FROM lesson_nodes")
    suspend fun getMaxOrderIndex(): Int?

    @Query("DELETE FROM lesson_nodes")
    suspend fun clearAllNodes()
}
