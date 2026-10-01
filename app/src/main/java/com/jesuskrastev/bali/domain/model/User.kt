package com.jesuskrastev.bali.domain.model

/**
 * Represents a user in the Bali App.
 *
 * This domain model encapsulates user-related data actually used by the app: profile
 * information consumed by AI prompt generation (license, experience, difficult topics,
 * exam date), the plan promised during onboarding, progress metrics (XP, level, coins), and
 * streak tracking. Onboarding answers that aren't read anywhere after onboarding (reasons,
 * daily goal, learning preference, concern, study time) are intentionally not persisted here.
 *
 * @property id The unique identifier of the user (typically from Firebase Auth).
 * @property name The user's display name.
 * @property licenseType The type of driving license the user is studying for.
 * @property examDateMillis The exam date: an estimate from the onboarding's timing answer
 *   until the user sets the real one from Home's plan card; null when no exam is booked.
 * @property planTargetMillis The day the onboarding plan promised the license by
 *   ("Puedes tener tu carnet antes del…"), saved so Home can keep showing it after payment;
 *   null for users who finished onboarding before it was saved.
 * @property lastPracticeTimestamp When the user last started a study day, 0 if never.
 * @property currentStreak Current streak momentum, from 0 to [DailyStreak.MAX_LEVEL].
 * @property xp The user's accumulated experience points.
 * @property level The user's current level based on their XP.
 * @property coins The user's current coin balance, spent in the shop on streak freezes.
 * @property streakFreezes The number of streak freezes the user currently owns.
 * @property highestStreak The highest streak momentum the user has reached.
 * @property lastStreakSettledDayMillis Local midnight of the last day whose absence was
 * processed, preventing an absence from reducing momentum more than once.
 * @property practiceDays Local midnights of the days with study, the last
 *   [DailyStreak.HISTORY_DAYS] only.
 * @property frozenDays Local midnights of the days a streak freeze covered, the last
 *   [DailyStreak.HISTORY_DAYS] only.
 */
data class User(
    val id: String = "",
    val name: String? = null,
    val licenseType: String? = null,
    val experience: String? = null,
    val examDateMillis: Long? = null,
    val planTargetMillis: Long? = null,
    val difficultTopics: String = "",
    val lastPracticeTimestamp: Long = 0,
    val currentStreak: Int = 0,
    val xp: Int = 0,
    val level: Int = 0,
    val coins: Int = 0,
    val streakFreezes: Int = 0,
    val highestStreak: Int = 0,
    val lastStreakSettledDayMillis: Long = 0,
    val practiceDays: List<Long> = emptyList(),
    val frozenDays: List<Long> = emptyList()
)
