package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.data.local.room.dao.TestResultDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreUserDao
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestResultRepositoryImpl @Inject constructor(
    private val testResultDao: TestResultDao,
    private val firestoreUserDao: FirestoreUserDao,
    private val authRepository: AuthRepository
) : TestResultRepository {

    override fun getRecent(): Flow<List<TestResult>> = authRepository.currentUserFlow.flatMapLatest { userId ->
        if (userId != null) {
            firestoreUserDao.getTestResults(userId).map { results -> 
                results.take(10).map { it.toDomain() } 
            }
        } else {
            testResultDao.getRecent().map { entities -> entities.map { it.toDomain() } }
        }
    }

    override fun get(): Flow<List<TestResult>> = authRepository.currentUserFlow.flatMapLatest { userId ->
        if (userId != null) {
            firestoreUserDao.getTestResults(userId).map { results -> 
                results.map { it.toDomain() } 
            }
        } else {
            testResultDao.get().map { entities -> entities.map { it.toDomain() } }
        }
    }


    /**
     * Executes [remoteAction] if the user is authenticated, otherwise [localAction].
     * Both actions run on [Dispatchers.IO].
     */
    private suspend inline fun <T> withAuthRouting(
        crossinline actionRemote: suspend (String) -> T,
        crossinline actionLocal: suspend () -> T
    ): T = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUser()
        if (userId != null) {
            actionRemote(userId)
        } else {
            actionLocal()
        }
    }

    override suspend fun insert(result: TestResult): String = withAuthRouting(
        actionRemote = { userId ->
            firestoreUserDao.insertTestResult(userId, result.toFirestore())
            result.id
        },
        actionLocal = {
            val resultEntity = result.toEntity()
            testResultDao.insert(resultEntity)
            resultEntity.id
        }
    )

    override fun count(): Flow<Int> = get().map { it.size }

    override fun getAverageScore(): Flow<Double?> = get().map { results ->
        if (results.isEmpty()) null else results.map { it.score }.average()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        testResultDao.clear()
    }
}
