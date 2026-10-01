package com.jesuskrastev.bali.domain.model

import java.util.Calendar
import java.util.TimeZone

/**
 * The daily streak: consecutive days with at least one test, mock exam or mini-game.
 *
 * Days are the user's local calendar days, stored as the millis of their local midnight (the
 * same values [User.practiceDays] always held). The app is the only one that computes the
 * streak; no server job touches it.
 *
 * Rules:
 * - Studying extends the streak by one, once per day.
 * - A day without study is covered by a streak freeze while any are left: the freeze is spent
 *   and the day is recorded in [frozenDays], so it can be shown.
 * - With more missed days than freezes the streak goes back to 0, and the freezes are kept: a
 *   freeze that cannot save the streak is not spent.
 *
 * @property current consecutive days, counting today once the user has studied today
 * @property highest the longest streak ever reached
 * @property freezes streak freezes still available
 * @property lastPracticeMillis when the user last started a study day, 0 if never
 * @property practiceDays local midnights of the days with study, the last [HISTORY_DAYS] only
 * @property frozenDays local midnights of the days a freeze covered, the last [HISTORY_DAYS] only
 */
data class DailyStreak(
    val current: Int,
    val highest: Int,
    val freezes: Int,
    val lastPracticeMillis: Long,
    val practiceDays: List<Long>,
    val frozenDays: List<Long>
) {

    /**
     * Tells whether the user has already studied on the day of [nowMillis].
     *
     * @param nowMillis the moment to check
     * @return true when that day is one of [practiceDays]
     */
    fun hasPracticedOn(nowMillis: Long): Boolean =
        practiceDays.any { epochDay(it) == epochDay(nowMillis) }

    /**
     * Applies the days missed since the last covered day: each is covered by a freeze while
     * there are enough of them, otherwise the streak is lost. Also drops history older than
     * [HISTORY_DAYS].
     *
     * @param nowMillis the current time
     * @return the streak as it stands today, before any study today is counted
     */
    fun settledAt(nowMillis: Long): DailyStreak {
        val today = epochDay(nowMillis)
        val lastCovered = (practiceDays + frozenDays).maxOfOrNull(::epochDay)
        val trimmed = trimmedTo(today)
        if (current == 0) return trimmed
        if (lastCovered == null) return trimmed.copy(current = 0)

        val missedDays = today - lastCovered - 1
        return when {
            missedDays <= 0 -> trimmed
            missedDays <= freezes -> trimmed.copy(
                freezes = freezes - missedDays.toInt(),
                frozenDays = trimmed.frozenDays +
                    (lastCovered + 1 until today).map(::startOfDayMillis)
            )
            else -> trimmed.copy(current = 0)
        }
    }

    /**
     * Counts a study session. Only the first one of the day extends the streak.
     *
     * @param nowMillis when the session ended
     * @return the streak after the session; equal to [settledAt] if the user had already
     *   studied that day
     */
    fun practicedAt(nowMillis: Long): DailyStreak {
        val settled = settledAt(nowMillis)
        if (settled.hasPracticedOn(nowMillis)) return settled

        val extended = settled.current + 1
        return settled.copy(
            current = extended,
            highest = maxOf(settled.highest, extended),
            lastPracticeMillis = nowMillis,
            practiceDays = settled.practiceDays + startOfDayMillis(epochDay(nowMillis))
        )
    }

    /**
     * Drops the days that fall out of the kept history.
     *
     * @param today the current epoch day
     * @return a copy with only the last [HISTORY_DAYS] days in each list
     */
    private fun trimmedTo(today: Long): DailyStreak {
        val oldestKept = today - HISTORY_DAYS + 1
        return copy(
            practiceDays = practiceDays.filter { epochDay(it) >= oldestKept }.distinct().sorted(),
            frozenDays = frozenDays.filter { epochDay(it) >= oldestKept }.distinct().sorted()
        )
    }

    companion object {
        /** Days of study and freeze history kept: enough for a month view. */
        const val HISTORY_DAYS = 35

        /** Freezes a user can hold at once. */
        const val MAX_FREEZES = 2

        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

        /**
         * Reads the streak off a profile.
         *
         * @param user the profile
         * @return the profile's streak fields
         */
        fun of(user: User): DailyStreak = DailyStreak(
            current = user.currentStreak,
            highest = user.highestStreak,
            freezes = user.streakFreezes,
            lastPracticeMillis = user.lastPracticeTimestamp,
            practiceDays = user.practiceDays,
            frozenDays = user.frozenDays
        )

        /**
         * Rebuilds a streak from study history alone: the run of consecutive study days ending
         * today, or yesterday if the user has not studied yet today. Used when moving from the
         * old weekly streak, whose count cannot be turned into days.
         *
         * @param practiceDays local midnights of the days with study
         * @param nowMillis the current time
         * @return the number of consecutive days, 0 when yesterday and today had no study
         */
        fun fromHistory(practiceDays: List<Long>, nowMillis: Long): Int {
            val studied = practiceDays.map(::epochDay).toSet()
            val today = epochDay(nowMillis)
            var day = if (today in studied) today else today - 1
            var count = 0
            while (day in studied) {
                count++
                day--
            }
            return count
        }

        /**
         * Numbers the local calendar day of [millis], so days can be compared and counted
         * without daylight-saving days (23 or 25 hours long) throwing the count off.
         *
         * @param millis any moment
         * @return days since 1970-01-01 of that moment's local date
         */
        fun epochDay(millis: Long): Long {
            val local = Calendar.getInstance().apply { timeInMillis = millis }
            val utc = Calendar.getInstance(UTC).apply {
                clear()
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            }
            return Math.floorDiv(utc.timeInMillis, DAY_MILLIS)
        }

        /**
         * Turns an epoch day back into the stored form.
         *
         * @param epochDay days since 1970-01-01
         * @return the millis of that day's local midnight
         */
        fun startOfDayMillis(epochDay: Long): Long {
            val utc = Calendar.getInstance(UTC).apply { timeInMillis = epochDay * DAY_MILLIS }
            return Calendar.getInstance().apply {
                clear()
                set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis
        }
    }
}
