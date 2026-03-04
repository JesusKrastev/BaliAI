package com.jesuskrastev.bali.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.data.repository.AnswerRepositoryImpl
import com.jesuskrastev.bali.data.repository.TestResultRepositoryImpl
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
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
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val testResultRepository: TestResultRepositoryImpl,
    private val answerRepository: AnswerRepositoryImpl,
    private val decrementCoinsUseCase: DecrementCoinsUseCase,
    private val authRepository: AuthRepository,
    private val pathRepository: PathRepository,
    private val generateNextPathNodesUseCase: GenerateNextPathNodesUseCase,
    private val generateInitialPathUseCase: GenerateInitialPathUseCase,
    private val analyticsTracker: FirebaseAnalyticsTracker,
    private val dateTimeHelper: DateTimeHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _isPathLoading = MutableStateFlow(false)
    private val _pathError = MutableStateFlow<String?>(null)

    private val _showEnergyDialog = MutableStateFlow(false)
    private val _showNoCoinsDialog = MutableStateFlow(false)
    private val _dailyTip = MutableStateFlow("")

    init {
        loadDailyTip()
        observeAndAutoGeneratePath()
    }

    private fun observeAndAutoGeneratePath() {
        viewModelScope.launch {
            authRepository.isLoggedIn
                .flatMapLatest { pathRepository.getPathNodes(auth.currentUser?.uid ?: "") }
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
        _showEnergyDialog,
        _showNoCoinsDialog,
        _dailyTip,
        authRepository.isLoggedIn,
        authRepository.isLoggedIn.flatMapLatest { pathRepository.getPathNodes(auth.currentUser?.uid ?: "") },
        _isPathLoading,
        _pathError
    ) { flows ->
        val user = flows[0] as? User
        val totalTests = flows[1] as Int
        val mistakes = flows[2] as List<*>
        val avgScore = flows[3] as? Double ?: 0.0
        val showEnergyDialog = flows[4] as Boolean
        val showNoCoinsDialog = flows[5] as Boolean
        val dailyTip = flows[6] as String
        val isLoggedIn = flows[7] as Boolean
        @Suppress("UNCHECKED_CAST")
        val pathNodes = flows[8] as List<LessonNode>
        val isPathLoading = flows[9] as Boolean
        val pathError = flows[10] as? String

        val profilePictureUrl = authRepository.currentUserPhotoUrl()
        val userEmail = authRepository.currentUserEmail()

        if (user == null) {
            HomeUiState(
                dailyTip = dailyTip, 
                isLoggedIn = isLoggedIn,
                profilePictureUrl = profilePictureUrl,
                userEmail = userEmail
            )
        } else {
            HomeUiState(
                userName = user.name ?: "Futuro Conductor",
                profilePictureUrl = profilePictureUrl,
                userEmail = userEmail,
                streak = user.currentStreak,
                avgScore = avgScore.toInt(),
                totalTests = totalTests,
                practiceDays = user.practiceDays,
                xpLevel = user.level,
                energyCount = user.energy,
                lastEnergyUpdateTimestamp = user.lastEnergyUpdateTimestamp,
                mistakesCount = mistakes.size,
                coinsCount = user.coins,
                streakFreezes = user.streakFreezes,
                showEnergyDialog = showEnergyDialog,
                showNoCoinsDialog = showNoCoinsDialog,
                dailyTip = dailyTip,
                isLoggedIn = isLoggedIn,
                weeklyStreak = generateWeeklyStreak(user.currentStreak, user.streakFreezes, user.practiceDays),
                lastPracticeTimestamp = user.lastPracticeTimestamp,
                pathNodes = pathNodes,
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
        }
    }

    fun startExam(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = decrementCoinsUseCase(100)
            if (success) {
                analyticsTracker.coinsSpent(100, "dgt_simulacro")
                analyticsTracker.dgtSimulacroUnlocked()
                onSuccess()
            } else {
                _showNoCoinsDialog.value = true
            }
        }
    }

    fun showEnergyDialog() {
        _showEnergyDialog.value = true
    }

    fun dismissEnergyDialog() {
        _showEnergyDialog.value = false
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

    private fun generateWeeklyStreak(streak: Int, freezes: Int, practiceDays: List<Long>): List<DailyStreakState> {
        val today = Calendar.getInstance()
        val currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
        // Lunes = 0, Domingo = 6
        val offset = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - 2

        val startOfWeek = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -offset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
        val result = mutableListOf<DailyStreakState>()

        for (i in 0..6) {
            val day = Calendar.getInstance().apply {
                timeInMillis = startOfWeek.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
            }
            val isToday = i == offset
            val isFuture = i > offset
            val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
            
            // Current day's start timestamp
            val dayStartMillis = day.timeInMillis
            val hasPracticed = practiceDays.contains(dayStartMillis)

            val status = if (isFuture) {
                StreakStatus.FUTURE
            } else if (isToday) {
                if (hasPracticed) StreakStatus.COMPLETED else StreakStatus.TODAY
            } else {
                if (hasPracticed) {
                    StreakStatus.COMPLETED
                } else {
                    // TODO: Advanced freezer logic can be mapped here later
                    StreakStatus.FAILED
                }
            }
            
            result.add(DailyStreakState(weekDays[i], dayOfMonth, status, isToday))
        }
        return result
    }
}
