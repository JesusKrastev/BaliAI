package com.jesuskrastev.bali.ui.screens.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.jesuskrastev.bali.ui.util.StreakUiHelper
import com.jesuskrastev.bali.data.remote.RemoteConfigProvider
import javax.inject.Inject

data class MainStreakUiState(
    val isLoading: Boolean = true,
    val weeklyStreak: List<DailyStreakState> = emptyList(),
    val streakFreezes: Int = 0,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val completionPercentage: Int = 0,
    val encouragingMessage: String = "",
    val weekSessions: Int = 0,
    val weeklyGoal: Int = 5
)

@HiltViewModel
class StreakViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val remoteConfigProvider: RemoteConfigProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainStreakUiState())
    val uiState: StateFlow<MainStreakUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            userRepository.get().collectLatest { user ->
                if (user != null) {
                    val weeklyStreak = StreakUiHelper.generateWeeklyStreak(user.practiceDays)
                    // Calculate actual sessions from weeklyStreak instead of relying on weekSessions DB field
                    val actualSessionsThisWeek = weeklyStreak.count { it.status == StreakStatus.COMPLETED }
                    _uiState.value = MainStreakUiState(
                        isLoading = false,
                        weeklyStreak = weeklyStreak,
                        streakFreezes = user.streakFreezes,
                        currentStreak = user.currentStreak,
                        highestStreak = user.highestStreak,
                        completionPercentage = calculateCompletionPercentage(weeklyStreak),
                        encouragingMessage = generateEncouragingMessage(user.currentStreak, user.highestStreak),
                        weekSessions = actualSessionsThisWeek,
                        weeklyGoal = remoteConfigProvider.getWeeklyGoal()
                    )
                }
            }
        }
    }

    private fun calculateCompletionPercentage(weeklyStreak: List<DailyStreakState>): Int {
        val completedDays = weeklyStreak.count { it.status == StreakStatus.COMPLETED }
        return (completedDays * 100) / 7
    }

    private fun generateEncouragingMessage(currentStreak: Int, highestStreak: Int): String {
        return if (currentStreak >= highestStreak) {
            "¡Increíble! Estás estableciendo un nuevo récord personal."
        } else {
            val remaining = highestStreak - currentStreak
            "Estás a solo $remaining días de batir tu récord personal de $highestStreak días."
        }
    }
}
