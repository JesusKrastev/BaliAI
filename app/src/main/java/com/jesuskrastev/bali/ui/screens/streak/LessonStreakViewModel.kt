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
import java.util.Calendar
import javax.inject.Inject

data class StreakUiState(
    val isLoading: Boolean = true,
    val weeklyStreak: List<DailyStreakState> = emptyList(),
    val streakFreezes: Int = 0,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0
)

@HiltViewModel
class LessonStreakViewModel @Inject constructor(
    private val userRepository: UserRepository
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
                    _uiState.value = StreakUiState(
                        isLoading = false,
                        weeklyStreak = generateWeeklyStreak(user.practiceDays),
                        streakFreezes = user.streakFreezes,
                        currentStreak = user.currentStreak,
                        highestStreak = user.highestStreak
                    )
                }
            }
        }
    }

    private fun generateWeeklyStreak(practiceDays: List<Long>): List<DailyStreakState> {
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
                    StreakStatus.FAILED
                }
            }
            
            result.add(DailyStreakState(weekDays[i], dayOfMonth, status, isToday))
        }
        return result
    }
}
