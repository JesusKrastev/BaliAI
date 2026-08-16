package com.jesuskrastev.bali.ui.screens.auth

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.*
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

class FakeUserRepository : UserRepository {
    private val _user = MutableStateFlow<User?>(User(name = "Jesus", coins = 500, level = 1, xp = 0))
    
    override fun get(): Flow<User?> = _user

    override fun exists(userId: String?): Flow<Boolean> = flowOf(true)

    override fun hasCompletedOnboarding(): Flow<Boolean> = flowOf(true)

    override suspend fun insert(user: User) {
        _user.value = user
    }

    override suspend fun resetStreak() {
        _user.update { it?.copy(currentStreak = 0) }
    }

    override suspend fun updateStreak(streak: Int, timestamp: Long, practiceDays: List<Long>) {
        _user.update { it?.copy(currentStreak = streak, lastPracticeTimestamp = timestamp, practiceDays = practiceDays) }
    }

    override suspend fun updateWeeklyProgress(weekSessions: Int, currentWeekStart: Long, lastPracticeTimestamp: Long, practiceDays: List<Long>) {
        _user.update { it?.copy(weekSessions = weekSessions, lastPracticeTimestamp = lastPracticeTimestamp, practiceDays = practiceDays) }
    }

    override suspend fun updateXp(xp: Int, level: Int) {
        _user.update { it?.copy(xp = xp, level = level) }
    }

    override suspend fun updateCoins(coins: Int) {
        _user.update { it?.copy(coins = coins) }
    }

    override suspend fun updateStreakFreezes(count: Int) {
        _user.update { it?.copy(streakFreezes = count) }
    }

    override suspend fun updateHighestStreak(highestStreak: Int) {
        _user.update { it?.copy(highestStreak = highestStreak) }
    }

    override suspend fun uploadAll(userId: String, user: User, results: List<TestResult>, answers: List<Answer>): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun updateFcmToken(token: String) {}

    override suspend fun clear() {
        _user.value = null
    }

    override suspend fun getSchemaVersion(): Int = 1
}

class FakeTestResultRepository : TestResultRepository {
    override fun getRecent(): Flow<List<TestResult>> = flowOf(emptyList())
    override fun get(): Flow<List<TestResult>> = flowOf(emptyList())
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

class FakeAuthRepository : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = flowOf(true)
    override val currentUserFlow: Flow<String?> = flowOf("user_123")
    override suspend fun getGoogleIdTokenAndEmail(context: android.content.Context): Result<Pair<String, String>> {
        return Result.success("token" to "test@example.com")
    }
    override suspend fun signInWithGoogleCredential(idToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun existsInAuth(email: String): Boolean = true
    override suspend fun signOut(context: android.content.Context) {}
    override suspend fun currentUser(): String? = "user_123"
    override suspend fun currentUserEmail(): String? = "test@example.com"
    override suspend fun currentUserPhotoUrl(): String? = null
}

class FakeAnalyticsTracker(
    firebase: com.google.firebase.analytics.FirebaseAnalytics,
    mixpanel: com.mixpanel.android.mpmetrics.MixpanelAPI
) : AnalyticsTracker(firebase, mixpanel) {
    val identifiedUsers = mutableListOf<Pair<String, String?>>()
    val signUpEvents = mutableListOf<String>()
    val loginEvents = mutableListOf<String>()
    val onboardingSteps = mutableListOf<String>()

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

    fun clear() {
        identifiedUsers.clear()
        signUpEvents.clear()
        loginEvents.clear()
        onboardingSteps.clear()
    }
}

class FakeSuggestionsRepository : SuggestionsRepository {
    override suspend fun sendSuggestion(suggestion: com.jesuskrastev.bali.domain.model.Suggestion): Result<Unit> {
        return Result.success(Unit)
    }
}
