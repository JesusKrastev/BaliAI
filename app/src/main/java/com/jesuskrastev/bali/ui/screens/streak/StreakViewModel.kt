package com.jesuskrastev.bali.ui.screens.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.util.StreakUiHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * What the streak screens show.
 *
 * @property isLoading true until the profile has loaded
 * @property currentStreak consecutive days with study, as of today
 * @property highestStreak the longest streak ever reached
 * @property practicedToday whether today already counts
 * @property streakFreezes freezes still available
 * @property week the current week, Monday first
 */
data class StreakUiState(
    val isLoading: Boolean = true,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val practicedToday: Boolean = false,
    val streakFreezes: Int = 0,
    val week: List<DailyStreakState> = emptyList()
)

/**
 * Builds the streak screens' state from a profile. The streak is settled to [nowMillis] first,
 * so a streak lost since the last visit never shows as alive, even before it is saved.
 *
 * @param user the profile
 * @param nowMillis the current time
 * @return the state to render
 */
fun streakUiStateOf(user: User, nowMillis: Long): StreakUiState {
    val streak = DailyStreak.of(user).settledAt(nowMillis)
    return StreakUiState(
        isLoading = false,
        currentStreak = streak.current,
        highestStreak = streak.highest,
        practicedToday = streak.hasPracticedOn(nowMillis),
        streakFreezes = streak.freezes,
        week = StreakUiHelper.generateWeeklyStreak(streak.practiceDays, streak.frozenDays, nowMillis)
    )
}

/** Feeds both the streak page and the celebration shown after the first session of the day. */
@HiltViewModel
class StreakViewModel @Inject constructor(
    userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<StreakUiState> = userRepository.get()
        .filterNotNull()
        .map { streakUiStateOf(it, System.currentTimeMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreakUiState())
}
