package com.jesuskrastev.bali.ui.screens.home

import com.jesuskrastev.bali.domain.model.LessonNode

enum class StreakStatus {
    COMPLETED,
    FROZEN,
    FAILED,
    TODAY,
    FUTURE
}

data class DailyStreakState(
    val dayOfWeek: String,
    val dayOfMonth: Int,
    val status: StreakStatus,
    val isToday: Boolean
)

/**
 * UI state for Home.
 *
 * @property plan the date the plan card counts down to, see [planSummaryOf]
 * @property streak current daily streak momentum, from zero to seven
 * @property practicedToday whether today already counts for the streak; the flame is grey until it does
 * @property weekSessions days practised so far this week, counted from [practiceDays] like the
 *   streak screens do, since the stored counter can still hold last week's number
 * @property weeklyGoal sessions per week the student aims for (Remote Config, same for everyone)
 */
data class HomeUiState(
    val userName: String = "Futuro Conductor",
    val userEmail: String? = null,
    val profilePictureUrl: String? = null,
    val plan: PlanSummary = PlanSummary(),
    val streak: Int = 0,
    val practicedToday: Boolean = false,
    val weekSessions: Int = 0,
    val weeklyGoal: Int = 3,
    val weekProgressPercent: Int = 0,
    val readinessPercent: Int = 5,
    val avgScore: Int = 0,
    val totalTests: Int = 0,
    val practiceDays: List<Long> = emptyList(),
    val xpLevel: Int = 1,
    val coinsCount: Int = 0,
    val mistakesCount: Int = 0,
    val streakFreezes: Int = 0,
    val highestStreak: Int = 0,
    val difficultTopics: List<String> = emptyList(),
    val dailyTip: String = "",
    val weeklyStreak: List<DailyStreakState> = emptyList(),
    val lastPracticeTimestamp: Long = 0L,
    val pathNodes: List<LessonNode> = emptyList(),
    val isPathLoading: Boolean = false,
    val pathError: String? = null
)
