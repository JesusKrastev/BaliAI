package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.UserFirestore
import com.jesuskrastev.bali.domain.model.User

fun UserEntity.toDomain(): User = User(
    name = name,
    licenseType = licenseType,
    experience = experience,
    reasons = reasons,
    examDateMillis = examDateMillis,
    dailyGoal = dailyGoal,
    learningPreference = learningPreference,
    difficultTopics = difficultTopics,
    concern = concern,
    studyTime = studyTime,
    lastPracticeTimestamp = lastPracticeTimestamp,
    lastEnergyUpdateTimestamp = lastEnergyUpdateTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    energy = energy,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays
)

fun User.toEntity(): UserEntity = UserEntity(
    name = name,
    licenseType = licenseType,
    experience = experience,
    reasons = reasons,
    examDateMillis = examDateMillis,
    dailyGoal = dailyGoal,
    learningPreference = learningPreference,
    difficultTopics = difficultTopics,
    concern = concern,
    studyTime = studyTime,
    lastPracticeTimestamp = lastPracticeTimestamp,
    lastEnergyUpdateTimestamp = lastEnergyUpdateTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    energy = energy,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays
)

fun User.toFirestore(): UserFirestore = UserFirestore(
    id = id,
    name = name.orEmpty(),
    licenseType = licenseType.orEmpty(),
    experience = experience.orEmpty(),
    reasons = reasons,
    examDateMillis = examDateMillis ?: 0L,
    dailyGoal = dailyGoal.orEmpty(),
    learningPreference = learningPreference.orEmpty(),
    difficultTopics = difficultTopics,
    concern = concern.orEmpty(),
    studyTime = studyTime.orEmpty(),
    lastPracticeTimestamp = lastPracticeTimestamp,
    lastEnergyUpdateTimestamp = lastEnergyUpdateTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    energy = energy,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays
)

fun UserFirestore.toDomain(): User = User(
    id = id,
    name = name.ifEmpty { null },
    licenseType = licenseType.ifEmpty { null },
    experience = experience.ifEmpty { null },
    reasons = reasons,
    examDateMillis = examDateMillis.takeIf { it != 0L },
    dailyGoal = dailyGoal.ifEmpty { null },
    learningPreference = learningPreference.ifEmpty { null },
    difficultTopics = difficultTopics,
    concern = concern.ifEmpty { null },
    studyTime = studyTime.ifEmpty { null },
    lastPracticeTimestamp = lastPracticeTimestamp,
    lastEnergyUpdateTimestamp = lastEnergyUpdateTimestamp,
    currentStreak = currentStreak,
    xp = xp,
    level = level,
    energy = energy,
    coins = coins,
    streakFreezes = streakFreezes,
    highestStreak = highestStreak,
    practiceDays = practiceDays
)