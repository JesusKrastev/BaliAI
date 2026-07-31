package com.jesuskrastev.bali.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.jesuskrastev.bali.data.local.room.dao.UserDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.local.room.Converters
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreUserDao
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.UserRepository
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
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestoreUserDao: FirestoreUserDao,
    private val authRepository: AuthRepository
) : UserRepository {

    override fun get(): Flow<User?> = authRepository.currentUserFlow.flatMapLatest { userId ->
        if (userId != null) {
            firestoreUserDao.getUser(userId).map { it?.toDomain() }
        } else {
            userDao.get().map { it?.toDomain() }
        }
    }

    override suspend fun getSchemaVersion(): Int = withContext(Dispatchers.IO) {
        authRepository.currentUser()?.let { userId ->
            firestoreUserDao.getSchemaVersion(userId)
        } ?: 0 // Return a default or throw an error if not logged in
    }

    /**
     * Executes [remoteAction] if the user is authenticated, otherwise [localAction].
     * Both actions run on [Dispatchers.IO].
     */
    private suspend inline fun <T> withAuthRouting(
        actionRemote: suspend (String) -> T,
        actionLocal: suspend () -> T
    ): T {
        val userId = authRepository.currentUser()
        return if (userId != null) {
            actionRemote(userId)
        } else {
            actionLocal()
        }
    }

    /**
     * Writes [fields] to Firestore if logged in, or runs [localAction] otherwise.
     */
    private suspend fun updateField(
        fields: Map<String, Any>,
        localAction: suspend () -> Unit
    ) = withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.updateFields(userId, fields) },
        actionLocal = localAction
    )

    /**
     * Persists onboarding answers locally only (Room). Onboarding responses are
     * intentionally never written to Firestore, regardless of auth state.
     */
    override suspend fun insert(user: User) = withContext(Dispatchers.IO) {
        userDao.insert(user.toEntity())
    }

    override suspend fun resetStreak() = updateField(
        fields = mapOf("currentStreak" to 0),
        localAction = { userDao.resetStreak() }
    )

    override suspend fun updateStreak(streak: Int, timestamp: Long, practiceDays: List<Long>) = updateField(
        fields = mapOf(
            "currentStreak" to streak, 
            "lastPracticeTimestamp" to timestamp,
            "practiceDays" to practiceDays
        ),
        localAction = {
            val practiceDaysStr = Converters().fromLongList(practiceDays)
            userDao.updateStreak(streak, timestamp, practiceDaysStr)
        }
    )

    override suspend fun updateWeeklyProgress(
        weekSessions: Int,
        currentWeekStart: Long,
        lastPracticeTimestamp: Long,
        practiceDays: List<Long>
    ) = updateField(
        fields = mapOf(
            "weekSessions" to weekSessions,
            "currentWeekStart" to currentWeekStart,
            "lastPracticeTimestamp" to lastPracticeTimestamp,
            "practiceDays" to practiceDays
        ),
        localAction = {
            val practiceDaysStr = Converters().fromLongList(practiceDays)
            userDao.updateWeeklyProgress(weekSessions, currentWeekStart, lastPracticeTimestamp, practiceDaysStr)
        }
    )

    override suspend fun updateXp(xp: Int, level: Int) = updateField(
        fields = mapOf("xp" to xp, "level" to level),
        localAction = { userDao.updateXp(xp, level) }
    )

    override suspend fun updateCoins(coins: Int) = updateField(
        fields = mapOf("coins" to coins),
        localAction = { userDao.updateCoins(coins) }
    )

    override suspend fun updateStreakFreezes(count: Int) = updateField(
        fields = mapOf("streakFreezes" to count),
        localAction = { userDao.updateStreakFreezes(count) }
    )

    override suspend fun updateHighestStreak(highestStreak: Int) = updateField(
        fields = mapOf("highestStreak" to highestStreak),
        localAction = { userDao.updateHighestStreak(highestStreak) }
    )

    override fun exists(userId: String?): Flow<Boolean> = authRepository.currentUserFlow.flatMapLatest { currentUserId ->
        val targetId = userId ?: currentUserId
        if (targetId != null) {
            firestoreUserDao.exists(targetId)
        } else {
            userDao.exists()
        }
    }

    override suspend fun uploadAll(
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

    override suspend fun updateFcmToken(token: String) = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUser() ?: return@withContext
        firestoreUserDao.updateFields(userId, mapOf("fcmToken" to token))
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        userDao.clear()
    }

    override fun hasCompletedOnboarding(): Flow<Boolean> = userDao.exists()
}
