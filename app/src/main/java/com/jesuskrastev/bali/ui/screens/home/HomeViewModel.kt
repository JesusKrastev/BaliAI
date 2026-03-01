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
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val analyticsTracker: FirebaseAnalyticsTracker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _showEnergyDialog = MutableStateFlow(false)
    private val _showNoCoinsDialog = MutableStateFlow(false)
    private val _dailyTip = MutableStateFlow("")

    init {
        loadDailyTip()
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
        authRepository.isLoggedIn
    ) { flows ->
        val user = flows[0] as? User
        val totalTests = flows[1] as Int
        val mistakes = flows[2] as List<*>
        val avgScore = flows[3] as? Double ?: 0.0
        val showEnergyDialog = flows[4] as Boolean
        val showNoCoinsDialog = flows[5] as Boolean
        val dailyTip = flows[6] as String
        val isLoggedIn = flows[7] as Boolean

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
                xpLevel = user.level,
                xpProgress = (user.xp % 100) / 100f,
                energyCount = user.energy,
                lastEnergyUpdateTimestamp = user.lastEnergyUpdateTimestamp,
                mistakesCount = mistakes.size,
                coinsCount = user.coins,
                streakFreezes = user.streakFreezes,
                showEnergyDialog = showEnergyDialog,
                showNoCoinsDialog = showNoCoinsDialog,
                dailyTip = dailyTip,
                isLoggedIn = isLoggedIn
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
}
