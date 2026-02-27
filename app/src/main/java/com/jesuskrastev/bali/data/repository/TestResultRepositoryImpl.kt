package com.jesuskrastev.bali.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.jesuskrastev.bali.data.local.room.dao.TestResultDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreUserDao
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TestResultRepositoryImpl @Inject constructor(
    private val testResultDao: TestResultDao,
    private val firestoreUserDao: FirestoreUserDao,
    private val authRepository: AuthRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val userId: String get() = auth.currentUser?.uid ?: ""

    fun getRecent(): Flow<List<TestResult>> = authRepository.isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            firestoreUserDao.getTestResults(userId).map { results -> 
                results.take(10).map { it.toDomain() } 
            }
        } else {
            testResultDao.getRecent().map { entities -> entities.map { it.toDomain() } }
        }
    }

    fun get(): Flow<List<TestResult>> = authRepository.isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            firestoreUserDao.getTestResults(userId).map { results -> 
                results.map { it.toDomain() } 
            }
        } else {
            testResultDao.get().map { entities -> entities.map { it.toDomain() } }
        }
    }

    fun getAll(): Flow<List<TestResult>> = get()

    suspend fun insert(result: TestResult): String = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.insertTestResult(userId, result.toFirestore())
        } else {
            val resultEntity = result.toEntity()
            testResultDao.insert(resultEntity)
            resultEntity.id
        }
    }

    fun count(): Flow<Int> = get().map { it.size }

    fun getAverageScore(): Flow<Double?> = get().map { results ->
        if (results.isEmpty()) null else results.map { it.score }.average()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        testResultDao.clear()
    }
}
