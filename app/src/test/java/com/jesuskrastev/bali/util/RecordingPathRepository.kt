package com.jesuskrastev.bali.util

import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.repository.PathRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [PathRepository] that holds a path in memory and applies every status update to it, so a test
 * can read back both what was written and the path as the next read would see it.
 *
 * @param nodes the path the repository starts with
 */
class RecordingPathRepository(nodes: List<LessonNode> = emptyList()) : PathRepository {

    /** One call to [updateNodeStatus]. */
    data class NodeUpdate(val nodeId: String, val status: String, val scorePercentage: Int?)

    @Volatile
    var nodes: List<LessonNode> = nodes
        private set

    val updates = CopyOnWriteArrayList<NodeUpdate>()

    override fun getPathNodes(userId: String): Flow<List<LessonNode>> = flow { emit(nodes) }

    override suspend fun saveGeneratedNodes(userId: String, nodes: List<LessonNode>) {}

    override suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int?) {
        updates += NodeUpdate(nodeId, status, scorePercentage)
        nodes = nodes.map {
            if (it.id == nodeId) it.copy(status = NodeStatus.valueOf(status), scorePercentage = scorePercentage ?: it.scorePercentage) else it
        }
    }

    override suspend fun getLastUnlockedNodeOrder(userId: String): Int = 0
}
