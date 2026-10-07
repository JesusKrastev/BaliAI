package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.data.local.room.dao.AnswerDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreUserDao
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
) : AnswerRepository {

    override suspend fun insert(answer: Answer) = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.insertAnswer(userId, answer.toFirestore()) },
        actionLocal = { answerDao.insert(answer.toEntity()) }
    )

    override fun getRecentMistakes(): Flow<List<Answer>> = authRepository.currentUserFlow.flatMapLatest { userId ->
        if (userId != null) {
            firestoreUserDao.getRecentMistakes(userId).map { list ->
                list.map { it.toDomain() }
            }
        } else {
            answerDao.getRecentMistakes().map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    override fun getAll(): Flow<List<Answer>> = authRepository.currentUserFlow.flatMapLatest { userId ->
        if (userId != null) {
            firestoreUserDao.getAnswers(userId).map { list ->
                list.map { it.toDomain() }
            }
        } else {
            answerDao.getAll().map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    override suspend fun markAsCorrected(questionText: String) = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.markAnswerAsCorrected(userId, questionText) },
        actionLocal = { answerDao.markAsCorrected(questionText) }
    )

    override suspend fun clear() = withContext(Dispatchers.IO) {
        answerDao.clear()
    }
}