package com.jesuskrastev.bali.ui.screens.auth

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.ChestReward
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.FIRST_STEPS_BONUS_COINS
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.domain.model.StudySchedule
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.repository.*
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

class FakeUserRepository(
    hasCompletedOnboarding: Boolean = true,
    private val userExists: Boolean = true
) : UserRepository {
    private val _user = MutableStateFlow<User?>(User(name = "Jesus", coins = 500, level = 1, xp = 0))
    private val _hasCompletedOnboarding = MutableStateFlow(hasCompletedOnboarding)

    /** Every profile handed to [uploadAll], i.e. every brand-new account created. */
    val uploadedUsers = mutableListOf<User>()

    override fun get(): Flow<User?> = _user

    override fun exists(userId: String?): Flow<Boolean> = flowOf(userExists)

    override fun hasCompletedOnboarding(): Flow<Boolean> = _hasCompletedOnboarding

    override suspend fun insert(user: User) {
        _user.value = user
        // Mirrors UserRepositoryImpl, where hasCompletedOnboarding() reflects whether a
        // local row exists: inserting the profile is what completes onboarding.
        _hasCompletedOnboarding.value = true
    }

    override suspend fun updateStreak(streak: DailyStreak) {
        _user.update {
            it?.copy(
                currentStreak = streak.current,
                highestStreak = streak.highest,
                streakFreezes = streak.freezes,
                lastPracticeTimestamp = streak.lastPracticeMillis,
                lostStreak = streak.lostStreak,
                lostStreakDayMillis = streak.lostStreakDayMillis,
                practiceDays = streak.practiceDays,
                frozenDays = streak.frozenDays
            )
        }
    }

    override suspend fun updateXp(xp: Int, level: Int) {
        _user.update { it?.copy(xp = xp, level = level) }
    }

    override suspend fun incrementCoins(amount: Int) {
        _user.update { it?.copy(coins = it.coins + amount) }
    }

    override suspend fun decrementCoinsIfEnough(amount: Int): Boolean {
        val current = _user.value?.coins ?: return false
        if (current < amount) return false
        _user.update { it?.copy(coins = current - amount) }
        return true
    }

    /** Test-only helper to set the balance directly, bypassing the real increment/decrement contract. */
    fun setCoinsForTest(amount: Int) {
        _user.update { it?.copy(coins = amount) }
    }

    override suspend fun updateStreakFreezes(count: Int) {
        _user.update { it?.copy(streakFreezes = count) }
    }

    /** Buys [item] from the fake inventory when the test profile can afford [cost]. */
    override suspend fun purchaseInventoryItem(item: ShopInventoryItem, cost: Int): Boolean {
        val user = _user.value ?: return false
        if (user.coins < cost) return false
        _user.value = user.copy(
            coins = user.coins - cost,
            hints = user.hints + if (item == ShopInventoryItem.HINT) 1 else 0,
            fiftyFifties = user.fiftyFifties + if (item == ShopInventoryItem.FIFTY_FIFTY) 1 else 0,
            doubleXpBoosts = user.doubleXpBoosts + if (item == ShopInventoryItem.DOUBLE_XP) 1 else 0,
            doubleCoinBoosts = user.doubleCoinBoosts + if (item == ShopInventoryItem.DOUBLE_COINS) 1 else 0
        )
        return true
    }

    /** Opens a fake surprise chest by charging [cost] and granting its [reward]. */
    override suspend fun openSurpriseChest(cost: Int, reward: ChestReward): Boolean {
        val user = _user.value ?: return false
        if (user.coins < cost) return false
        _user.value = when (reward) {
            is ChestReward.Coins -> user.copy(coins = user.coins - cost + reward.amount)
            is ChestReward.Inventory -> user.copy(
                coins = user.coins - cost,
                hints = user.hints + if (reward.item == ShopInventoryItem.HINT) reward.quantity else 0,
                fiftyFifties = user.fiftyFifties +
                    if (reward.item == ShopInventoryItem.FIFTY_FIFTY) reward.quantity else 0,
                doubleXpBoosts = user.doubleXpBoosts +
                    if (reward.item == ShopInventoryItem.DOUBLE_XP) reward.quantity else 0,
                doubleCoinBoosts = user.doubleCoinBoosts +
                    if (reward.item == ShopInventoryItem.DOUBLE_COINS) reward.quantity else 0
            )
        }
        return true
    }

    /** Consumes one fake inventory [item] when it is owned. */
    override suspend fun consumeInventoryItem(item: ShopInventoryItem): Boolean {
        val user = _user.value ?: return false
        val canConsume = when (item) {
            ShopInventoryItem.HINT -> user.hints > 0
            ShopInventoryItem.FIFTY_FIFTY -> user.fiftyFifties > 0
            ShopInventoryItem.DOUBLE_XP -> user.doubleXpBoosts > 0
            ShopInventoryItem.DOUBLE_COINS -> user.doubleCoinBoosts > 0
        }
        if (!canConsume) return false
        _user.value = user.copy(
            hints = user.hints - if (item == ShopInventoryItem.HINT) 1 else 0,
            fiftyFifties = user.fiftyFifties - if (item == ShopInventoryItem.FIFTY_FIFTY) 1 else 0,
            doubleXpBoosts = user.doubleXpBoosts - if (item == ShopInventoryItem.DOUBLE_XP) 1 else 0,
            doubleCoinBoosts = user.doubleCoinBoosts - if (item == ShopInventoryItem.DOUBLE_COINS) 1 else 0
        )
        return true
    }

    /** Places a fake streak bet when the profile can pay and has none running. */
    override suspend fun placeStreakBet(cost: Int, target: Int): Boolean {
        val user = _user.value ?: return false
        if (user.coins < cost || user.streakBetTarget > 0) return false
        _user.value = user.copy(coins = user.coins - cost, streakBetTarget = target)
        return true
    }

    /** Pays [payout] if the fake profile has a streak bet, and clears it. */
    override suspend fun claimStreakBet(payout: Int): Boolean {
        val user = _user.value ?: return false
        if (user.streakBetTarget <= 0) return false
        _user.value = user.copy(coins = user.coins + payout, streakBetTarget = 0)
        return true
    }

    /** Forgets the fake profile's streak bet without paying it. */
    override suspend fun clearStreakBet() {
        _user.update { it?.copy(streakBetTarget = 0) }
    }


    /** Mirrors the Firestore transaction: pays once, only while enrolled and not dismissed. */
    override suspend fun completeFirstStep(task: FirstStepTask): Int {
        val user = _user.value ?: return 0
        val progress = user.firstSteps
        if (!progress.isActive || progress.isDone(task)) return 0

        val completed = progress.completed + task
        val paid = task.coins + if (completed.containsAll(FirstStepTask.entries)) FIRST_STEPS_BONUS_COINS else 0
        _user.value = user.copy(coins = user.coins + paid, firstSteps = progress.copy(completed = completed))
        return paid
    }

    override suspend fun dismissFirstSteps() {
        _user.update { it?.copy(firstSteps = it.firstSteps.copy(dismissed = true)) }
    }

    /** Test-only helper that puts the account in the first-steps window with [completed] already done. */
    fun enrollInFirstStepsForTest(completed: Set<FirstStepTask> = emptySet()) {
        _user.update { it?.copy(firstSteps = FirstStepsProgress.startingAt(1_000L).copy(completed = completed)) }
    }

    override suspend fun updateExamDate(examDateMillis: Long) {
        _user.update { it?.copy(examDateMillis = examDateMillis) }
    }

    /** Test-only helper to set the saved exam and plan dates directly. */
    fun setPlanDatesForTest(examDateMillis: Long?, planTargetMillis: Long?) {
        _user.update { it?.copy(examDateMillis = examDateMillis, planTargetMillis = planTargetMillis) }
    }

    override suspend fun uploadAll(userId: String, user: User, results: List<TestResult>, answers: List<Answer>): Result<Unit> {
        uploadedUsers.add(user)
        return Result.success(Unit)
    }

    override suspend fun updateFcmToken(token: String) {}

    override suspend fun clear() {
        _user.value = null
    }

    override suspend fun getSchemaVersion(): Int = 1
}

class FakeTestResultRepository(private val results: List<TestResult> = emptyList()) : TestResultRepository {
    override fun getRecent(): Flow<List<TestResult>> = flowOf(emptyList())
    override fun get(): Flow<List<TestResult>> = flowOf(results)
    override suspend fun insert(result: TestResult): String = "test_id"
    override fun count(): Flow<Int> = flowOf(0)
    override fun getAverageScore(): Flow<Double?> = flowOf(0.0)
    override suspend fun clear() {}
}

class FakeAnswerRepository : AnswerRepository {
    override suspend fun insert(answer: Answer) {}
    override fun getRecentMistakes(): Flow<List<Answer>> = flowOf(emptyList())
    override fun getAll(): Flow<List<Answer>> = flowOf(emptyList())
    override suspend fun clear() {}
    override suspend fun markAsCorrected(questionText: String) {}
}

class FakePathRepository : PathRepository {
    override fun getPathNodes(userId: String): Flow<List<com.jesuskrastev.bali.domain.model.LessonNode>> = flowOf(emptyList())
    override suspend fun saveGeneratedNodes(userId: String, nodes: List<com.jesuskrastev.bali.domain.model.LessonNode>) {}
    override suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int?) {}
    override suspend fun getLastUnlockedNodeOrder(userId: String): Int = 0
}

class FakeAuthRepository(
    isLoggedIn: Boolean = true,
    private val currentUserId: String? = "user_123"
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = flowOf(isLoggedIn)
    override val currentUserFlow: Flow<String?> = flowOf(currentUserId)
    override suspend fun getGoogleIdTokenAndEmail(context: android.content.Context): Result<Pair<String, String>> {
        return Result.success("token" to "test@example.com")
    }
    override suspend fun signInWithGoogleCredential(idToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun existsInAuth(email: String): Boolean = true
    override suspend fun signOut(context: android.content.Context) {}
    override suspend fun currentUser(): String? = currentUserId
    override val currentUserEmailFlow: Flow<String?> = flowOf("test@example.com")
    override val currentUserPhotoUrlFlow: Flow<String?> = flowOf(null)
}

data class GameCompletedEvent(val gameId: String, val score: Int, val totalRounds: Int, val durationSeconds: Int)

class FakeAnalyticsTracker(
    firebase: com.google.firebase.analytics.FirebaseAnalytics,
    mixpanel: com.mixpanel.android.mpmetrics.MixpanelAPI,
    posthog: com.posthog.PostHogInterface
) : AnalyticsTracker(firebase, mixpanel, posthog) {
    val identifiedUsers = mutableListOf<Pair<String, String?>>()
    val signUpEvents = mutableListOf<String>()
    val loginEvents = mutableListOf<String>()
    val onboardingSteps = mutableListOf<String>()
    val gameStartedEvents = mutableListOf<String>()
    val gameCompletedEvents = mutableListOf<GameCompletedEvent>()
    val gameAbandonedEvents = mutableListOf<Pair<String, Int>>()
    val firstStepsShownEvents = mutableListOf<Int>()
    val firstStepRewards = mutableListOf<FirstStepReward>()
    val firstStepsDismissedEvents = mutableListOf<Int>()
    var firstStepsExamClicks = 0
        private set
    val notificationsAnswers = mutableListOf<Pair<String, String?>>()
    /** Every mini-test answer reported, as question id and whether it was right. */
    val quizAnswers = mutableListOf<Pair<String, Boolean>>()
    var quizSkips = 0
        private set
    /** Every exam date reported, as days until it and the screen it was set from. */
    val examDates = mutableListOf<Pair<Int, String>>()
    /** The answers profile sent when the onboarding content was finished. */
    var completedProfile: Map<String, String>? = null
        private set

    override fun notificationsPermissionAnswered(result: String, studySlot: String?) {
        notificationsAnswers.add(result to studySlot)
    }
    override fun onboardingQuizAnswered(
        questionId: String,
        topic: String,
        isCorrect: Boolean,
        position: Int,
        seconds: Int,
        concern: String?
    ) {
        quizAnswers.add(questionId to isCorrect)
    }
    override fun onboardingQuizSkipped() { quizSkips++ }
    override fun examDateSet(daysUntil: Int, hadPlanDate: Boolean, source: String) {
        examDates.add(daysUntil to source)
    }
    override fun onboardingFlowCompleted(profile: Map<String, String>) { completedProfile = profile }
    override fun identifyUser(userId: String, email: String?) { identifiedUsers.add(userId to email) }
    override fun resetUser() {}
    override fun signUp(method: String) { signUpEvents.add(method) }
    override fun login(method: String) { loginEvents.add(method) }
    override fun logout() {}
    override fun onboardingStarted() {}
    override fun onboardingStepReached(eventName: String) { onboardingSteps.add(eventName) }
    override fun onboardingCompleted() {}
    override fun onboardingAbandoned(lastStep: String, stepIndex: Int) {}
    override fun paywallShown(source: String) {}
    override fun paywallPurchased(source: String) {}
    override fun paywallClosed(source: String) {}
    override fun paywallBackgrounded(source: String) {}
    override fun paywallResumed(source: String) {}
    override fun gameStarted(gameId: String) { gameStartedEvents.add(gameId) }
    override fun gameCompleted(gameId: String, score: Int, totalRounds: Int, durationSeconds: Int) {
        gameCompletedEvents.add(GameCompletedEvent(gameId, score, totalRounds, durationSeconds))
    }
    override fun gameAbandoned(gameId: String, roundIndex: Int) { gameAbandonedEvents.add(gameId to roundIndex) }
    override fun firstStepsShown(tasksDone: Int) { firstStepsShownEvents.add(tasksDone) }
    override fun firstStepRewarded(reward: FirstStepReward) { firstStepRewards.add(reward) }
    override fun firstStepsExamClicked() { firstStepsExamClicks++ }
    override fun firstStepsDismissed(tasksDone: Int) { firstStepsDismissedEvents.add(tasksDone) }

    fun clear() {
        identifiedUsers.clear()
        signUpEvents.clear()
        loginEvents.clear()
        onboardingSteps.clear()
        gameStartedEvents.clear()
        gameCompletedEvents.clear()
        gameAbandonedEvents.clear()
        firstStepsShownEvents.clear()
        firstStepRewards.clear()
        firstStepsDismissedEvents.clear()
        firstStepsExamClicks = 0
        notificationsAnswers.clear()
        quizAnswers.clear()
        quizSkips = 0
        examDates.clear()
        completedProfile = null
    }
}

/**
 * [NotificationsRepository] that records every call instead of reaching OneSignal.
 *
 * @param grantsPermission what the system dialog answers when permission is requested
 */
class FakeNotificationsRepository(private val grantsPermission: Boolean = true) : NotificationsRepository {
    private val _studySchedule = MutableStateFlow<StudySchedule?>(null)
    override val studySchedule: Flow<StudySchedule?> = _studySchedule

    var permissionRequests = 0
        private set
    var optedOut = false
        private set

    /** Every [identify] call in order; null entries are sign-outs. */
    val identifiedUsers = mutableListOf<String?>()

    /** Every [updateTags] call in order. */
    val sentTags = mutableListOf<Map<String, String?>>()

    /** [identify] and [updateTags] calls interleaved, as `identify:<id>` and `tags`. */
    val calls = mutableListOf<String>()

    /** The latest saved study moment, or null if none was saved. */
    val savedSchedule: StudySchedule? get() = _studySchedule.value

    override suspend fun saveStudySchedule(schedule: StudySchedule) { _studySchedule.value = schedule }
    override suspend fun requestPermission(): Boolean {
        permissionRequests++
        return grantsPermission
    }
    override fun optOut() { optedOut = true }
    override fun identify(userId: String?) {
        identifiedUsers.add(userId)
        calls.add("identify:$userId")
    }
    override fun updateTags(tags: Map<String, String?>) {
        sentTags.add(tags)
        calls.add("tags")
    }
}

class FakeSuggestionsRepository : SuggestionsRepository {
    override suspend fun sendSuggestion(suggestion: com.jesuskrastev.bali.domain.model.Suggestion): Result<Unit> {
        return Result.success(Unit)
    }
}
