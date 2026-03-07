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
import com.jesuskrastev.bali.ui.util.StreakUiHelper
import com.jesuskrastev.bali.data.remote.RemoteConfigProvider
import javax.inject.Inject

data class StreakUiState(
    val isLoading: Boolean = true,
    val weeklyStreak: List<DailyStreakState> = emptyList(),
    val streakFreezes: Int = 0,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val weekSessions: Int = 0,
    val weeklyGoal: Int = 5
)

@HiltViewModel
class LessonStreakViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val remoteConfigProvider: RemoteConfigProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState: StateFlow<StreakUiState> = _uiState.asStateFlow()

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
                    _uiState.value = StreakUiState(
                        isLoading = false,
                        weeklyStreak = weeklyStreak,
                        streakFreezes = user.streakFreezes,
                        currentStreak = user.currentStreak,
                        highestStreak = user.highestStreak,
                        weekSessions = actualSessionsThisWeek,
                        weeklyGoal = remoteConfigProvider.getWeeklyGoal()
                    )
                }
            }
        }
    }
}
