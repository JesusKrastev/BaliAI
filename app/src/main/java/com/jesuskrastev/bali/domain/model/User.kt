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
 * @property currentStreak The user's current consecutive weeks of study.
 * @property xp The user's accumulated experience points.
 * @property level The user's current level based on their XP.
 * @property coins The user's current coin balance, spent in the shop on streak freezes.
 * @property streakFreezes The number of streak freezes the user currently owns.
 * @property weekSessions The number of sessions completed in the current week. Only
 *   refreshed when the user practises, so it can still hold last week's count: screens
 *   count this week's [practiceDays] instead.
 * @property currentWeekStart The timestamp of the start of the current week (Monday 00:00).
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
    val practiceDays: List<Long> = emptyList(),
    val weekSessions: Int = 0,
    val currentWeekStart: Long = 0L
)
