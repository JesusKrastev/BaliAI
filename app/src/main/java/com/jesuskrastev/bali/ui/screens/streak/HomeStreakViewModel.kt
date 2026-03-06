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

data class HomeStreakUiState(
    val isLoading: Boolean = true,
    val weeklyStreak: List<DailyStreakState> = emptyList(),
    val streakFreezes: Int = 0,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val completionPercentage: Int = 0,
    val encouragingMessage: String = ""
)

@HiltViewModel
class HomeStreakViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeStreakUiState())
    val uiState: StateFlow<HomeStreakUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            userRepository.get().collectLatest { user ->
                if (user != null) {
                    _uiState.value = HomeStreakUiState(
                        isLoading = false,
                        weeklyStreak = generateWeeklyStreak(user.practiceDays),
                        streakFreezes = user.streakFreezes,
                        currentStreak = user.currentStreak,
                        highestStreak = user.highestStreak,
                        completionPercentage = calculateCompletionPercentage(generateWeeklyStreak(user.practiceDays)),
                        encouragingMessage = generateEncouragingMessage(user.currentStreak, user.highestStreak)
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

        val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
        val weeklyStreak = mutableListOf<DailyStreakState>()

        for (i in 0..6) {
            val dayCalendar = startOfWeek.clone() as Calendar
            dayCalendar.add(Calendar.DAY_OF_YEAR, i)
            val isToday = dayCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                          dayCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
            val isFuture = dayCalendar.timeInMillis > today.timeInMillis

            val isPracticed = practiceDays.any { timestamp ->
                val practiceCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
                practiceCalendar.get(Calendar.DAY_OF_YEAR) == dayCalendar.get(Calendar.DAY_OF_YEAR) &&
                practiceCalendar.get(Calendar.YEAR) == dayCalendar.get(Calendar.YEAR)
            }

            val status = when {
                isPracticed -> StreakStatus.COMPLETED
                isFuture -> StreakStatus.FUTURE
                isToday -> StreakStatus.TODAY // Si es hoy y no se ha practicado
                else -> StreakStatus.FAILED // TODO: Implementar lógica de escudos (FROZEN) cuando sea necesario
            }

            weeklyStreak.add(DailyStreakState(
                dayOfWeek = daysOfWeek[i], 
                dayOfMonth = dayCalendar.get(Calendar.DAY_OF_MONTH),
                status = status, 
                isToday = isToday
            ))
        }

        return weeklyStreak
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
