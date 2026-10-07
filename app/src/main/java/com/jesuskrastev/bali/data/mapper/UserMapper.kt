package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.UserFirestore
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.domain.model.User

/** Converts the local Room [UserEntity] into the framework-free [User] domain model. */
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
    lostStreak = lostStreak,
    lostStreakDayMillis = lostStreakDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays,
    hints = hints,
    fiftyFifties = fiftyFifties,
    doubleXpBoosts = doubleXpBoosts,
    doubleCoinBoosts = doubleCoinBoosts,
    activeStreakBet = activeStreakBet,
    streakBetTarget = streakBetTarget,
    claimedRankRewards = claimedRankRewards
)

/** Converts this [User] into the Room entity stored for offline access. */
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
    lostStreak = lostStreak,
    lostStreakDayMillis = lostStreakDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays,
    hints = hints,
    fiftyFifties = fiftyFifties,
    doubleXpBoosts = doubleXpBoosts,
    doubleCoinBoosts = doubleCoinBoosts,
    activeStreakBet = activeStreakBet,
    streakBetTarget = streakBetTarget,
    claimedRankRewards = claimedRankRewards
)

/** Converts this [User] into its Firestore document representation. */
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
    lostStreak = lostStreak,
    lostStreakDayMillis = lostStreakDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays,
    hints = hints,
    fiftyFifties = fiftyFifties,
    doubleXpBoosts = doubleXpBoosts,
    doubleCoinBoosts = doubleCoinBoosts,
    activeStreakBet = activeStreakBet,
    streakBetTarget = streakBetTarget,
    firstStepsStartedAt = firstSteps.startedAtMillis,
    firstStepsDone = firstSteps.completed.map { it.id },
    firstStepsDismissed = firstSteps.dismissed,
    claimedRankRewards = claimedRankRewards
)

/** Converts a Firestore profile document into the framework-free [User] domain model. */
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
    lostStreak = lostStreak,
    lostStreakDayMillis = lostStreakDayMillis,
    practiceDays = practiceDays,
    frozenDays = frozenDays,
    hints = hints,
    fiftyFifties = fiftyFifties,
    doubleXpBoosts = doubleXpBoosts,
    doubleCoinBoosts = doubleCoinBoosts,
    activeStreakBet = activeStreakBet,
    streakBetTarget = streakBetTarget,
    claimedRankRewards = claimedRankRewards,
    firstSteps = FirstStepsProgress(
        startedAtMillis = firstStepsStartedAt,
        // Ids this build does not know (written by a newer version) are ignored, not crashed on.
        completed = firstStepsDone.mapNotNull(FirstStepTask::fromId).toSet(),
        dismissed = firstStepsDismissed
    )
)
