package com.jesuskrastev.bali.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.jesuskrastev.bali.data.local.room.dao.AnswerDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreUserDao
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnswerRepositoryImpl @Inject constructor(
    private val answerDao: AnswerDao,
    private val firestoreUserDao: FirestoreUserDao,
    private val authRepository: AuthRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val userId: String get() = auth.currentUser?.uid ?: ""

    suspend fun insert(answer: Answer) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.insertAnswer(userId, answer.toFirestore())
        } else {
            answerDao.insert(answer.toEntity())
        }
    }

    fun getRecentMistakes(): Flow<List<Answer>> = authRepository.isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            firestoreUserDao.getRecentMistakes(userId).map { list ->
                list.map { it.toDomain() }
            }
        } else {
            answerDao.getRecentMistakes().map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    fun getAll(): Flow<List<Answer>> = authRepository.isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            firestoreUserDao.getAnswers(userId).map { list ->
                list.map { it.toDomain() }
            }
        } else {
            answerDao.getAll().map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    suspend fun markAsCorrected(questionText: String) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.markAnswerAsCorrected(userId, questionText)
        } else {
            answerDao.markAsCorrected(questionText)
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        answerDao.clear()
    }
}