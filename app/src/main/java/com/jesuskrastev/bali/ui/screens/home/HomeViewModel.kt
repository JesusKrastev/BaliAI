package com.jesuskrastev.bali.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.PathRepository
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateInitialPathUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateNextPathNodesUseCase
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.ui.util.StreakUiHelper
import com.jesuskrastev.bali.data.remote.RemoteConfigProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val testResultRepository: TestResultRepository,
    private val answerRepository: AnswerRepository,
    private val decrementCoinsUseCase: DecrementCoinsUseCase,
    private val authRepository: AuthRepository,
    private val pathRepository: PathRepository,
    private val generateNextPathNodesUseCase: GenerateNextPathNodesUseCase,
    private val generateInitialPathUseCase: GenerateInitialPathUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val dateTimeHelper: DateTimeHelper,
    private val remoteConfigProvider: RemoteConfigProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isPathLoading = MutableStateFlow(false)
    private val _pathError = MutableStateFlow<String?>(null)

    private val _showNoCoinsDialog = MutableStateFlow(false)
    private val _dailyTip = MutableStateFlow("")

    private val _pathNodes: StateFlow<List<LessonNode>?> = authRepository.currentUserFlow
        .flatMapLatest { userId -> 
             pathRepository.getPathNodes(userId ?: "") 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadDailyTip()
        observeAndAutoGeneratePath()
        viewModelScope.launch {
            remoteConfigProvider.fetchAndActivate()
        }
    }

    private fun observeAndAutoGeneratePath() {
        viewModelScope.launch {
            _pathNodes
                .filterNotNull()
                .distinctUntilChanged { old, new -> old.isNotEmpty() == new.isNotEmpty() }
                .collect { nodes ->
                    if (nodes.isEmpty() && !_isPathLoading.value) {
                        generateInitialPath()
                    }
                }
        }
    }

    private fun generateInitialPath() {
        viewModelScope.launch {
            _isPathLoading.value = true
            _pathError.value = null
            try {
                generateInitialPathUseCase()
            } catch (e: Exception) {
                _pathError.value = e.localizedMessage
            } finally {
                _isPathLoading.value = false
            }
        }
    }

    private fun loadDailyTip() {
        try {
            val jsonString = context.resources.openRawResource(R.raw.tips).bufferedReader().use { it.readText() }
            val tips = Json.decodeFromString<List<String>>(jsonString)
            if (tips.isNotEmpty()) {
                _dailyTip.value = tips.random()
            }
        } catch (e: Exception) {
            _dailyTip.value = "Conduce con precaución y respeta las señales."
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.get(),
        testResultRepository.count(),
        answerRepository.getRecentMistakes(),
        testResultRepository.getAverageScore(),
        _showNoCoinsDialog,
        _dailyTip,
        _pathNodes,
        _isPathLoading,
        _pathError,
        authRepository.currentUserPhotoUrlFlow,
        authRepository.currentUserEmailFlow
    ) { flows ->
        val user = flows[0] as User?
        val totalTests = flows[1] as Int
        val mistakes = flows[2] as List<*>
        val avgScore = flows[3] as Double? ?: 0.0
        val showNoCoinsDialog = flows[4] as Boolean
        val dailyTip = flows[5] as String
        val pathNodes = flows[6] as List<*>?
        val isPathLoading = flows[7] as Boolean
        val pathError = flows[8] as String?
        val profilePictureUrl = flows[9] as String?
        val userEmail = flows[10] as String?

        @Suppress("UNCHECKED_CAST")
        val typedMistakes = mistakes as List<Answer>

        @Suppress("UNCHECKED_CAST")
        val typedPathNodes = (pathNodes ?: emptyList<LessonNode>()) as List<LessonNode>

        if (user == null) {
            HomeUiState(
                dailyTip = dailyTip,
                profilePictureUrl = profilePictureUrl,
                userEmail = userEmail
            )
        } else {
            HomeUiState(
                userName = user.name ?: "Futuro Conductor",
                profilePictureUrl = profilePictureUrl,
                userEmail = userEmail,
                streak = user.currentStreak,
                weekSessions = user.weekSessions,
                weeklyGoal = remoteConfigProvider.getWeeklyGoal(),
                weekProgressPercent = ((user.weekSessions.toFloat() / remoteConfigProvider.getWeeklyGoal().coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100),
                avgScore = avgScore.toInt(),
                totalTests = totalTests,
                practiceDays = user.practiceDays,
                xpLevel = user.level,
                mistakesCount = typedMistakes.size,
                coinsCount = user.coins,
                streakFreezes = user.streakFreezes,
                highestStreak = user.highestStreak,
                showNoCoinsDialog = showNoCoinsDialog,
                dailyTip = dailyTip,
                weeklyStreak = StreakUiHelper.generateWeeklyStreak(user.practiceDays),
                lastPracticeTimestamp = user.lastPracticeTimestamp,
                pathNodes = typedPathNodes,
                isPathLoading = isPathLoading,
                pathError = pathError
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
            analyticsTracker.logout()
            analyticsTracker.resetUser()
        }
    }

    fun startExam(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = decrementCoinsUseCase(100)
            if (success) {
                onSuccess()
            } else {
                _showNoCoinsDialog.value = true
            }
        }
    }

    fun dismissNoCoinsDialog() {
        _showNoCoinsDialog.value = false
    }

    fun generateNextPathNodesCount(count: Int = 5) {
        if (_isPathLoading.value) return
        viewModelScope.launch {
            _isPathLoading.value = true
            _pathError.value = null
            try {
                generateNextPathNodesUseCase(count)
            } catch (e: Exception) {
                _pathError.value = e.localizedMessage
            } finally {
                _isPathLoading.value = false
            }
        }
    }
}
