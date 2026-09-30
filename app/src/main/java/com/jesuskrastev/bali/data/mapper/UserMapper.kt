package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.UserFirestore
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.domain.model.User

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    licenseType = licenseType,
    experience = experience,
    examDateMillis = examDateMillis,
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays,
    weekSessions = weekSessions,
    currentWeekStart = currentWeekStart
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    licenseType = licenseType,
    experience = experience,
    examDateMillis = examDateMillis,
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays,
    weekSessions = weekSessions,
    currentWeekStart = currentWeekStart
)

fun User.toFirestore(): UserFirestore = UserFirestore(
    id = id,
    name = name.orEmpty(),
    licenseType = licenseType.orEmpty(),
    experience = experience.orEmpty(),
    examDateMillis = examDateMillis ?: 0L,
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays,
    weekSessions = weekSessions,
    currentWeekStart = currentWeekStart,
    firstStepsStartedAt = firstSteps.startedAtMillis,
    firstStepsDone = firstSteps.completed.map { it.id },
    firstStepsDismissed = firstSteps.dismissed
)

fun UserFirestore.toDomain(): User = User(
    id = id,
    name = name.ifEmpty { null },
    licenseType = licenseType.ifEmpty { null },
    experience = experience.ifEmpty { null },
    examDateMillis = examDateMillis.takeIf { it != 0L },
    difficultTopics = difficultTopics,
    lastPracticeTimestamp = lastPracticeTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays,
    weekSessions = weekSessions,
    currentWeekStart = currentWeekStart,
    firstSteps = FirstStepsProgress(
        startedAtMillis = firstStepsStartedAt,
        // Ids this build does not know (written by a newer version) are ignored, not crashed on.
        completed = firstStepsDone.mapNotNull(FirstStepTask::fromId).toSet(),
        dismissed = firstStepsDismissed
    )
)
