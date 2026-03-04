package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.LessonNode
import kotlinx.coroutines.flow.Flow

interface PathRepository {
    fun getPathNodes(userId: String): Flow<List<LessonNode>>
    
    suspend fun saveGeneratedNodes(userId: String, nodes: List<LessonNode>)
    
    suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int? = null)
    
    suspend fun getLastUnlockedNodeOrder(userId: String): Int
}
