package com.jesuskrastev.bali.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jesuskrastev.bali.data.local.room.dao.UserDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.local.room.Converters
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreUserDao
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.ChestReward
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.FIRST_STEPS_BONUS_COINS
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.RankReward
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestoreUserDao: FirestoreUserDao,
    private val authRepository: AuthRepository,
    private val migrationManager: FirestoreMigrationManager
) : UserRepository {

    /** Returns whether [reward] was collected through the active Firestore or Room profile. */
    override suspend fun claimRankReward(reward: RankReward): Boolean = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.claimRankReward(userId, reward) },
        actionLocal = { userDao.claimRankReward(reward) }
    )

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
     * Writes [fields] to Firestore if logged in, or runs [localAction] otherwise.
     */
    private suspend fun updateField(
        fields: Map<String, Any>,
        localAction: suspend () -> Unit
    ) = authRepository.withAuthRouting(
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

    /** See [UserRepository.updateStreak]. */
    override suspend fun updateStreak(streak: DailyStreak) = updateField(
        fields = mapOf(
            "currentStreak" to streak.current,
            "highestStreak" to streak.highest,
            "streakFreezes" to streak.freezes,
            "lastPracticeTimestamp" to streak.lastPracticeMillis,
            "lostStreak" to streak.lostStreak,
            "lostStreakDayMillis" to streak.lostStreakDayMillis,
            "practiceDays" to streak.practiceDays,
            "frozenDays" to streak.frozenDays
        ),
        localAction = {
            val converters = Converters()
            userDao.updateStreak(
                current = streak.current,
                highest = streak.highest,
                freezes = streak.freezes,
                lastPracticeMillis = streak.lastPracticeMillis,
                lostStreak = streak.lostStreak,
                lostStreakDayMillis = streak.lostStreakDayMillis,
                practiceDaysJson = converters.fromLongList(streak.practiceDays),
                frozenDaysJson = converters.fromLongList(streak.frozenDays)
            )
        }
    )

    override suspend fun updateXp(xp: Int, level: Int) = updateField(
        fields = mapOf("xp" to xp, "level" to level),
        localAction = { userDao.updateXp(xp, level) }
    )

    /** See [UserRepository.incrementCoins]. */
    override suspend fun incrementCoins(amount: Int) = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.incrementCoins(userId, amount) },
        actionLocal = { userDao.incrementCoins(amount) }
    )

    /** See [UserRepository.decrementCoinsIfEnough]. */
    override suspend fun decrementCoinsIfEnough(amount: Int): Boolean = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.decrementCoinsIfEnough(userId, amount) },
        actionLocal = { userDao.decrementCoinsIfEnough(amount) == 1 }
    )

    /**
     * See [UserRepository.completeFirstStep]. First steps live on the Firestore account only, so a
     * signed-out user (who can't reach Home anyway) has nothing to record. A failed write is
     * reported to Crashlytics and swallowed: it must never break the test, chat or game the
     * student just finished.
     */
    override suspend fun completeFirstStep(task: FirstStepTask): Int = authRepository.withAuthRouting(
        actionRemote = { userId ->
            try {
                firestoreUserDao.completeFirstStep(
                    userId = userId,
                    taskId = task.id,
                    taskCoins = task.coins,
                    allTaskIds = FirstStepTask.entries.map { it.id },
                    bonusCoins = FIRST_STEPS_BONUS_COINS
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                FirebaseCrashlytics.getInstance().recordException(e)
                0
            }
        },
        actionLocal = { 0 }
    )

    /** See [UserRepository.dismissFirstSteps]. */
    override suspend fun dismissFirstSteps() = authRepository.withAuthRouting(
        actionRemote = { userId ->
            firestoreUserDao.updateFields(userId, mapOf(FirestoreUserDao.FIRST_STEPS_DISMISSED to true))
        },
        actionLocal = {}
    )

    override suspend fun updateStreakFreezes(count: Int) = updateField(
        fields = mapOf("streakFreezes" to count),
        localAction = { userDao.updateStreakFreezes(count) }
    )

    /**
     * Charges [cost] and persists the purchased [item] together, in the current data source.
     *
     * @return true when the inventory item was granted, false when it could not be purchased.
     */
    override suspend fun purchaseInventoryItem(item: ShopInventoryItem, cost: Int): Boolean =
        authRepository.withAuthRouting(
            actionRemote = { userId -> firestoreUserDao.purchaseInventoryItem(userId, item, cost) },
            actionLocal = { userDao.purchaseInventoryItem(item.name, cost) == 1 }
        )

    /**
     * Applies a surprise chest's [reward] while charging its [cost] in the same persistence write.
     *
     * @return true when the chest opened, false for an insufficient balance.
     */
    override suspend fun openSurpriseChest(cost: Int, reward: Int): Boolean =
        authRepository.withAuthRouting(
            actionRemote = { userId -> firestoreUserDao.openSurpriseChest(userId, cost, reward) },
            actionLocal = { userDao.openSurpriseChest(cost, reward) == 1 }
        )


    /**
     * Applies a surprise chest's [reward] while charging its [cost] in one persistence write.
     *
     * @return true when the chest opened, false for an insufficient balance.
     */
    override suspend fun openSurpriseChest(cost: Int, reward: ChestReward): Boolean =
        authRepository.withAuthRouting(
            actionRemote = { userId -> firestoreUserDao.openSurpriseChest(userId, cost, reward) },
            actionLocal = {
                val coins = (reward as? ChestReward.Coins)?.amount ?: 0
                val inventory = reward as? ChestReward.Inventory
                userDao.openSurpriseChest(
                    cost = cost,
                    coinReward = coins,
                    item = inventory?.item?.name,
                    quantity = inventory?.quantity ?: 0
                ) == 1
            }
        )

    /**
     * Decrements one inventory [item] only when it is still owned.
     *
     * @return true when a consumable was spent.
     */
    override suspend fun consumeInventoryItem(item: ShopInventoryItem): Boolean =
        authRepository.withAuthRouting(
            actionRemote = { userId -> firestoreUserDao.consumeInventoryItem(userId, item) },
            actionLocal = { userDao.consumeInventoryItem(item.name) == 1 }
        )

    /**
     * Pays the active streak wager in whichever store currently owns the user's profile.
     *
     * @return true when a wager was paid.
     */
    override suspend fun claimStreakBet(): Boolean =
        authRepository.withAuthRouting(
            actionRemote = { userId -> firestoreUserDao.claimStreakBetHead(userId, STREAK_BET_PAYOUT) },
            actionLocal = { userDao.claimStreakBetHead(STREAK_BET_PAYOUT) == 1 }
        )

    /** See [UserRepository.placeStreakBet]. */
    override suspend fun placeStreakBet(cost: Int, target: Int): Boolean = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.placeStreakBet(userId, cost, target) },
        actionLocal = { userDao.placeStreakBet(cost, target) == 1 }
    )

    /** See [UserRepository.claimStreakBet]. */
    override suspend fun claimStreakBet(payout: Int): Boolean = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.claimStreakBet(userId, payout) },
        actionLocal = { userDao.claimStreakBet(payout) == 1 }
    )

    /** See [UserRepository.clearStreakBet]. */
    override suspend fun clearStreakBet() = authRepository.withAuthRouting(
        actionRemote = { userId -> firestoreUserDao.clearStreakBet(userId) },
        actionLocal = { userDao.clearStreakBet() }
    )

    /** See [UserRepository.updateExamDate]. */
    override suspend fun updateExamDate(examDateMillis: Long) = updateField(
        fields = mapOf("examDateMillis" to examDateMillis),
        localAction = { userDao.updateExamDate(examDateMillis) }
    )

    override fun exists(userId: String?): Flow<Boolean> = authRepository.currentUserFlow.flatMapLatest { currentUserId ->
        val targetId = userId ?: currentUserId
        if (targetId != null) {
            firestoreUserDao.exists(targetId)
        } else {
            userDao.exists()
        }
    }

    /**
     * See [UserRepository.uploadAll]. The new document is written already stamped with the
     * current schema version: it has today's structure, so the migrations written for older
     * documents (the first ones delete results and reset xp) must never run on it.
     */
    override suspend fun uploadAll(
        userId: String,
        user: User,
        results: List<TestResult>,
        answers: List<Answer>
    ): Result<Unit> = try {
        firestoreUserDao.uploadAll(
            userId,
            user.toFirestore().copy(schemaVersion = migrationManager.getTargetSchemaVersion()),
            results.map { it.toFirestore() },
            answers.map { it.toFirestore() }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }

    override suspend fun updateFcmToken(token: String) = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUser() ?: return@withContext
        firestoreUserDao.updateFields(userId, mapOf("fcmToken" to token))
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        userDao.clear()
    }

    override fun hasCompletedOnboarding(): Flow<Boolean> = userDao.exists()

    private companion object {
        const val STREAK_BET_PAYOUT = 100
    }
}
