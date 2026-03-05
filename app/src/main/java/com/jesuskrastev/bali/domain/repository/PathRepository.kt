package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.LessonNode
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing the user's learning path layout and progress.
 *
 * It abstracts the data source (local Room DB vs remote Firestore) based on the user's
 * authentication context.
 */
interface PathRepository {
    /**
     * Retrieves the list of lesson nodes for a specific user.
     * Starts by checking if the user is authenticated; if so, pulls from remote, otherwise local.
     */
    fun getPathNodes(userId: String): Flow<List<LessonNode>>
    
    /**
     * Saves a list of generated lesson nodes to the appropriate data source.
     */
    suspend fun saveGeneratedNodes(userId: String, nodes: List<LessonNode>)
    
    /**
     * Updates the completion status and optional score of a specific node.
     */
    suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int? = null)
    
    /**
     * Returns the order index of the highest unlocked node to facilitate progress tracking.
     */
    suspend fun getLastUnlockedNodeOrder(userId: String): Int
}
