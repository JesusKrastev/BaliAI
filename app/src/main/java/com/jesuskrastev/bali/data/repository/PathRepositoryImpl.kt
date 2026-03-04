package com.jesuskrastev.bali.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.jesuskrastev.bali.data.local.room.dao.LessonNodeDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreLessonNodeDao
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.PathRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PathRepositoryImpl @Inject constructor(
    private val localDao: LessonNodeDao,
    private val remoteDao: FirestoreLessonNodeDao,
    private val authRepository: AuthRepository
) : PathRepository {

    private val auth = FirebaseAuth.getInstance()
    private val userId: String get() = auth.currentUser?.uid ?: ""

    override fun getPathNodes(userId: String): Flow<List<LessonNode>> = authRepository.isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            remoteDao.getPathNodes(userId).map { list -> list.map { it.toDomain() } }
        } else {
            localDao.getAllNodes().map { list -> list.map { it.toDomain() } }
        }
    }

    override suspend fun saveGeneratedNodes(userId: String, nodes: List<LessonNode>) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            remoteDao.insertNodes(userId, nodes.map { it.toFirestore() })
        } else {
            localDao.insertNodes(nodes.map { it.toEntity() })
        }
    }

    override suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int?) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
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
        // Find maximum orderIndex. For simplicity, reading from local DB if local.
        // Reading all from flow and finding max.
        val nodes = getPathNodes(userId).first()
        nodes.maxOfOrNull { it.orderIndex } ?: -1
    }
}
