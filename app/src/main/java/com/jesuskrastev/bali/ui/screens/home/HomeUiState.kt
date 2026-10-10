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

/**
 * UI state for Home.
 *
 * @property streak consecutive days with study, as of today
 * @property practicedToday whether today already counts for the streak; the flame is grey until it does
 */
data class HomeUiState(
    val userName: String = "Futuro Conductor",
    val userEmail: String? = null,
    val profilePictureUrl: String? = null,
    val streak: Int = 0,
    val practicedToday: Boolean = false,
    val avgScore: Int = 0,
    val totalTests: Int = 0,
    val practiceDays: List<Long> = emptyList(),
    val xpLevel: Int = 1,
    val xp: Int = 0,
    val claimableRankRewards: Int = 0,
    val coinsCount: Int = 0,
    val mistakesCount: Int = 0,
    val streakFreezes: Int = 0,
    val highestStreak: Int = 0,
    val dailyTip: String = "",
    val lastPracticeTimestamp: Long = 0L,
    val pathNodes: List<LessonNode> = emptyList(),
    val isPathLoading: Boolean = false,
    val pathError: String? = null,
    /** Progress to show in the "Tus primeros pasos" bar, or null when the bar must stay hidden. */
    val firstSteps: FirstStepsProgress? = null,
    /** Lesson the "Haz tu primer test" step opens (see [firstStepTestNodeOf]), or null when there is none. */
    val firstStepTestNode: LessonNode? = null,

    /** Exam the first-steps closing action opens (see [firstStepExamNodeOf]), or null while none is unlocked. */
    val firstStepExamNode: LessonNode? = null,
    /** Coins just earned on another screen that Home has yet to celebrate, if any. */
    val firstStepReward: FirstStepReward? = null
)
