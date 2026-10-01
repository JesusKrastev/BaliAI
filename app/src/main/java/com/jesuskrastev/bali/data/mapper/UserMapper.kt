package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.UserFirestore
import com.jesuskrastev.bali.domain.model.User

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    licenseType = licenseType,
    experience = experience,
    examDateMillis = examDateMillis,
    planTargetMillis = planTargetMillis,
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    lastStreakSettledDayMillis = lastStreakSettledDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    licenseType = licenseType,
    experience = experience,
    examDateMillis = examDateMillis,
    planTargetMillis = planTargetMillis,
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    lastStreakSettledDayMillis = lastStreakSettledDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays
)

fun User.toFirestore(): UserFirestore = UserFirestore(
    id = id,
    name = name.orEmpty(),
    licenseType = licenseType.orEmpty(),
    experience = experience.orEmpty(),
    examDateMillis = examDateMillis ?: 0L,
    planTargetMillis = planTargetMillis ?: 0L,
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    lastStreakSettledDayMillis = lastStreakSettledDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays
)

fun UserFirestore.toDomain(): User = User(
    id = id,
    name = name.ifEmpty { null },
    licenseType = licenseType.ifEmpty { null },
    experience = experience.ifEmpty { null },
    examDateMillis = examDateMillis.takeIf { it != 0L },
    planTargetMillis = planTargetMillis.takeIf { it != 0L },
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    lastStreakSettledDayMillis = lastStreakSettledDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays
)
