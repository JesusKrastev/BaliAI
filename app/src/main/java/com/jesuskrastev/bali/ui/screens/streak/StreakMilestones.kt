package com.jesuskrastev.bali.ui.screens.streak

/** Days in a year, for the yearly milestones. */
private const val DAYS_PER_YEAR = 365

/** After the first year, every this many days is also a milestone. */
private const val MILESTONE_STEP_AFTER_A_YEAR = 100

/**
 * Streak lengths, in days, that get the big milestone celebration instead of the daily confetti.
 * Dense at the start, where most people preparing the DGT theory test are (a few weeks of study),
 * and sparse later so a milestone stays rare. After [DAYS_PER_YEAR], every hundred days and every
 * full year count too (see [isStreakMilestone]).
 */
val STREAK_MILESTONES = listOf(3, 7, 14, 30, 50, 100, 200, DAYS_PER_YEAR)

/**
 * Whether a streak length is a milestone.
 *
 * @param days the streak including today
 * @return true for the lengths in [STREAK_MILESTONES] and, past a year, for every multiple of
 *   one hundred days or of a full year
 */
fun isStreakMilestone(days: Int): Boolean =
    days in STREAK_MILESTONES ||
        (days > DAYS_PER_YEAR && (days % MILESTONE_STEP_AFTER_A_YEAR == 0 || days % DAYS_PER_YEAR == 0))

/**
 * The next milestone after a streak length.
 *
 * @param days the streak including today
 * @return the smallest milestone strictly greater than [days]
 */
fun nextStreakMilestone(days: Int): Int {
    var candidate = maxOf(days, 0) + 1
    while (!isStreakMilestone(candidate)) candidate++
    return candidate
}

/**
 * Headline for a milestone, in round words where Spanish has them.
 *
 * @param days a milestone streak length
 * @return for example "¡Una semana seguida!" for 7 or "¡50 días seguidos!" for 50
 */
fun streakMilestoneTitle(days: Int): String = when {
    days == 7 -> "¡Una semana seguida!"
    days == 14 -> "¡Dos semanas seguidas!"
    days == 30 -> "¡Un mes seguido!"
    days == DAYS_PER_YEAR -> "¡Un año seguido!"
    days > DAYS_PER_YEAR && days % DAYS_PER_YEAR == 0 -> "¡${days / DAYS_PER_YEAR} años seguidos!"
    else -> "¡$days días seguidos!"
}

/**
 * Line under a milestone headline: whether it is a record, and the next milestone to aim for.
 *
 * @param days a milestone streak length
 * @param highest the longest streak ever, which already includes today
 * @return for example "Es tu mejor racha. Próximo hito: 14 días."
 */
fun streakMilestoneSubtitle(days: Int, highest: Int): String {
    val next = "Próximo hito: ${nextStreakMilestone(days)} días."
    return if (days >= highest) "Es tu mejor racha. $next" else next
}
