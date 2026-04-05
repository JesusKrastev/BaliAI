package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.data.local.room.dao.LessonNodeDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreLessonNodeDao
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.repository.PathRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PathRepositoryImpl @Inject constructor(
    private val localDao: LessonNodeDao,
    private val remoteDao: FirestoreLessonNodeDao
) : PathRepository {

    override fun getPathNodes(userId: String): Flow<List<LessonNode>> {
        return if (userId.isNotEmpty()) {
            remoteDao.getPathNodes(userId).map { list -> list.map { it.toDomain() } }
        } else {
            localDao.getAllNodes().map { list -> list.map { it.toDomain() } }
        }
    }

    override suspend fun saveGeneratedNodes(userId: String, nodes: List<LessonNode>) = withContext(Dispatchers.IO) {
        if (userId.isNotEmpty()) {
            remoteDao.insertNodes(userId, nodes.map { it.toFirestore() })
        } else {
            localDao.insertNodes(nodes.map { it.toEntity() })
        }
    }

    override suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int?) = withContext(Dispatchers.IO) {
        if (userId.isNotEmpty()) {
            remoteDao.updateNodeStatus(userId, nodeId, status, scorePercentage)
        } else {
            if (scorePercentage != null) {
                localDao.completeNode(nodeId, status, scorePercentage)
            } else {
                localDao.updateNodeStatus(nodeId, status)
            }
        }
    }

    override suspend fun getLastUnlockedNodeOrder(userId: String): Int = withContext(Dispatchers.IO) {
        val nodes = getPathNodes(userId).first()
        nodes.maxOfOrNull { it.orderIndex } ?: -1
    }
}
