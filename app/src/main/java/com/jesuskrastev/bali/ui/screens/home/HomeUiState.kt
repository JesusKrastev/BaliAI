package com.jesuskrastev.bali.ui.screens.home

import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
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
data class HomeUiState(
    val userName: String = "Futuro Conductor",
    val userEmail: String? = null,
    val profilePictureUrl: String? = null,
    val examDateDays: Int? = null,
    val streak: Int = 0,
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
    val showNoCoinsDialog: Boolean = false,
    val weeklyStreak: List<DailyStreakState> = emptyList(),
    val lastPracticeTimestamp: Long = 0L,
    val pathNodes: List<LessonNode> = emptyList(),
    val isPathLoading: Boolean = false,
    val pathError: String? = null,
    /** Progress to show in the "Tus primeros pasos" card, or null when the card must stay hidden. */
    val firstSteps: FirstStepsProgress? = null,
    /** Coins just earned on another screen that Home has yet to celebrate, if any. */
    val firstStepReward: FirstStepReward? = null
)
