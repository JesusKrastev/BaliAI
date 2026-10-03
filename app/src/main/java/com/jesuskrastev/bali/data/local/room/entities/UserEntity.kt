package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "",
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
    // Left over from the discarded streak speedometer (D-019), which already shipped to internal
    // testing. Kept so their database matches this schema; nothing reads or writes it.
    @ColumnInfo(defaultValue = "0") val lastStreakSettledDayMillis: Long = 0,
    @ColumnInfo(defaultValue = "0") val lostStreak: Int = 0,
    @ColumnInfo(defaultValue = "0") val lostStreakDayMillis: Long = 0,
    val practiceDays: List<Long> = emptyList(),
    // Only the old weekly streak used these two. Kept so the table does not have to be rebuilt.
    val weekSessions: Int = 0,
    val currentWeekStart: Long = 0L,
    @ColumnInfo(defaultValue = "[]") val frozenDays: List<Long> = emptyList(),
    @ColumnInfo(defaultValue = "0") val hints: Int = 0,
    @ColumnInfo(defaultValue = "0") val fiftyFifties: Int = 0,
    @ColumnInfo(defaultValue = "0") val doubleXpBoosts: Int = 0,
    @ColumnInfo(defaultValue = "0") val doubleCoinBoosts: Int = 0,
    @ColumnInfo(defaultValue = "0") val streakBetTarget: Int = 0,
    @ColumnInfo(defaultValue = "[]") val claimedRankRewards: List<String> = emptyList()
)
