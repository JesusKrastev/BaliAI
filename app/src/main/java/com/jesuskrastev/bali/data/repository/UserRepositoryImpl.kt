package com.jesuskrastev.bali.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.jesuskrastev.bali.data.local.room.dao.UserDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreUserDao
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestoreUserDao: FirestoreUserDao,
    private val authRepository: AuthRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val userId: String get() = auth.currentUser?.uid ?: ""

    fun get(): Flow<User?> = authRepository.isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            firestoreUserDao.getUser(userId).map { it?.toDomain() }
        } else {
            userDao.get().map { it?.toDomain() }
        }
    }

    suspend fun insert(user: User) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateUser(userId, user)
        } else {
            userDao.insert(user.toEntity())
        }
    }

    suspend fun resetStreak() = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateFields(userId, mapOf("currentStreak" to 0))
        } else {
            userDao.resetStreak()
        }
    }

    suspend fun updateStreak(streak: Int, timestamp: Long) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateFields(userId, mapOf("currentStreak" to streak, "lastPracticeTimestamp" to timestamp))
        } else {
            userDao.updateStreak(streak, timestamp)
        }
    }

    suspend fun updateXp(xp: Int, level: Int) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateFields(userId, mapOf("xp" to xp, "level" to level))
        } else {
            userDao.updateXp(xp, level)
        }
    }

    suspend fun updateEnergy(energy: Int) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateFields(userId, mapOf("energy" to energy))
        } else {
            userDao.updateEnergy(energy)
        }
    }

    suspend fun updateEnergyAndTimestamp(energy: Int, timestamp: Long) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateFields(userId, mapOf("energy" to energy, "lastEnergyUpdateTimestamp" to timestamp))
        } else {
            userDao.updateEnergyAndTimestamp(energy, timestamp)
        }
    }

    suspend fun updateCoins(coins: Int) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateFields(userId, mapOf("coins" to coins))
        } else {
            userDao.updateCoins(coins)
        }
    }

    suspend fun updateStreakFreezes(count: Int) = withContext(Dispatchers.IO) {
        if (authRepository.isLoggedIn.first()) {
            firestoreUserDao.updateFields(userId, mapOf("streakFreezes" to count))
        } else {
            userDao.updateStreakFreezes(count)
        }
    }

    fun exists(userId: String? = null): Flow<Boolean> = authRepository.isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            firestoreUserDao.exists(userId ?: "")
        } else {
            userDao.exists()
        }
    }

    suspend fun uploadAll(
        userId: String,
        user: User,
        results: List<TestResult>,
        answers: List<Answer>
    ): Result<Unit> {
        return try {
            firestoreUserDao.uploadAll(
                userId,
                user.toFirestore(),
                results.map { it.toFirestore() },
                answers.map { it.toFirestore() }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        userDao.clear()
    }

    fun hasCompletedOnboarding(): Flow<Boolean> = userDao.exists()
}
