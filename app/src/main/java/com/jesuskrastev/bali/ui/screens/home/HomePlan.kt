package com.jesuskrastev.bali.ui.screens.home

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * What Home's plan card shows: the day the student is working towards and how far away it is.
 *
 * @property targetMillis the day the card counts down to, or null when there is none ahead,
 *   in which case the card asks for the exam date instead
 * @property isExamDate true when [targetMillis] is an exam date of the student's own ("Examen
 *   el…"), false when it is the date the onboarding plan promised ("Carnet antes del…")
 * @property daysLeft calendar days from today to [targetMillis]; 0 on the day itself
 */
data class PlanSummary(
    val targetMillis: Long? = null,
    val isExamDate: Boolean = false,
    val daysLeft: Int = 0
)

/** Days before the date from which the plan card switches to its final-week look. */
internal const val PLAN_FINAL_WEEK_DAYS = 7

/** How close the plan's date is; the plan card escalates its look as the date approaches. */
enum class PlanUrgency {
    /** No date ahead: the card asks for one. */
    NO_DATE,

    /** More than [PLAN_FINAL_WEEK_DAYS] days left. */
    ON_TRACK,

    /** Between 1 and [PLAN_FINAL_WEEK_DAYS] days left. */
    FINAL_WEEK,

    /** The date is today. */
    TODAY;

    /** Whether the date is close enough for the card to switch to its red, final-stretch look. */
    val isClose: Boolean get() = this == FINAL_WEEK || this == TODAY
}

/**
 * Classifies how close this plan's date is.
 *
 * @return [PlanUrgency.NO_DATE] without a date, otherwise the band [PlanSummary.daysLeft] falls in
 */
internal fun PlanSummary.urgency(): PlanUrgency = when {
    targetMillis == null -> PlanUrgency.NO_DATE
    daysLeft == 0 -> PlanUrgency.TODAY
    daysLeft <= PLAN_FINAL_WEEK_DAYS -> PlanUrgency.FINAL_WEEK
    else -> PlanUrgency.ON_TRACK
}

/**
 * Where the student stands against this week's session goal, as the plan card reports it.
 *
 * @property sessions days practised so far this week
 * @property goal sessions per week the student aims for, at least 1
 * @property daysLeft days of this week still open for a session: today if it has no session
 *   yet, plus the days after it
 * @property practicedToday whether today already has a session
 */
data class WeekPace(
    val sessions: Int,
    val goal: Int,
    val daysLeft: Int,
    val practicedToday: Boolean
) {
    /** Sessions still needed to reach [goal]; 0 once it is reached. */
    val missing: Int get() = (goal - sessions).coerceAtLeast(0)
}

/**
 * Works out this week's pace from the Monday-to-Sunday strip Home already builds.
 *
 * @param week the seven days of the current week, as built by `StreakUiHelper`
 * @param sessions days practised so far this week
 * @param weeklyGoal sessions per week the student aims for; anything below 1 counts as 1
 * @return the pace the plan card reports
 */
internal fun weekPaceOf(week: List<DailyStreakState>, sessions: Int, weeklyGoal: Int): WeekPace =
    WeekPace(
        sessions = sessions,
        goal = weeklyGoal.coerceAtLeast(1),
        daysLeft = week.count { it.status == StreakStatus.TODAY || it.status == StreakStatus.FUTURE },
        practicedToday = week.any { it.isToday && it.status == StreakStatus.COMPLETED }
    )

/**
 * Builds the line under the plan card's week strip: what is still missing and how many days
 * are left to do it in, so falling behind shows before the week is lost.
 *
 * @param pace this week's pace
 * @return the sentence to show
 */
internal fun weekPaceMessage(pace: WeekPace): String {
    val missing = pace.missing
    val days = pace.daysLeft
    val sessionsText = if (missing == 1) "1 sesión" else "$missing sesiones"
    val missingText = if (missing == 1) "Te falta $sessionsText" else "Te faltan $sessionsText"
    val daysText = if (days == 1) "queda 1 día" else "quedan $days días"
    return when {
        missing == 0 -> "¡Objetivo de la semana cumplido!"
        days == 0 -> "Esta semana te has quedado a $sessionsText. El lunes, otra oportunidad."
        missing > days -> "$missingText y solo $daysText"
        missing == days -> "$missingText y $daysText: ${if (days == 1) "que no se te pase" else "no te saltes ninguno"}"
        else -> "$missingText y $daysText"
    }
}

/**
 * Picks the day Home's plan card counts down to.
 *
 * For a student who had booked their exam, onboarding saves the same estimate as both the exam
 * date and the plan date, and the card keeps the onboarding promise ("Carnet antes del…").
 * Once the exam date differs from the plan date, it is the student's own (they set it from
 * the card, or they onboarded before the plan date was saved) and it takes over, even once it
 * has passed: the card then asks for a new date rather than falling back to an older promise.
 *
 * @param examDateMillis the saved exam date, or null when there is none
 * @param planTargetMillis the date the onboarding plan promised, or null when it wasn't saved
 * @param now current time in millis
 * @return the date to count down to, or an empty summary when there is none ahead
 */
internal fun planSummaryOf(examDateMillis: Long?, planTargetMillis: Long?, now: Long): PlanSummary {
    if (examDateMillis != null && examDateMillis != planTargetMillis) {
        val daysLeft = calendarDaysBetween(now, examDateMillis)
        return if (daysLeft >= 0) PlanSummary(examDateMillis, isExamDate = true, daysLeft) else PlanSummary()
    }
    if (planTargetMillis != null) {
        val daysLeft = calendarDaysBetween(now, planTargetMillis)
        if (daysLeft >= 0) return PlanSummary(planTargetMillis, isExamDate = false, daysLeft)
    }
    return PlanSummary()
}

/**
 * Counts the calendar days between two instants in the device's time zone. Rounds rather than
 * truncates, so a 23-hour day at the spring daylight-saving change still counts as a whole day.
 *
 * @param from the earlier instant, in millis
 * @param to the later instant, in millis
 * @return days from [from]'s day to [to]'s day; negative when [to] is on an earlier day
 */
internal fun calendarDaysBetween(from: Long, to: Long): Int {
    val difference = startOfLocalDay(to) - startOfLocalDay(from)
    return (difference.toDouble() / TimeUnit.DAYS.toMillis(1)).roundToInt()
}

/**
 * Converts the date chosen in a Material date picker, which is midnight UTC of that day, into
 * local midnight of the same day, so it counts down to the right day in any time zone.
 *
 * @param pickerMillis the picker's selection, midnight UTC of the chosen day
 * @return local midnight of that same calendar day
 */
internal fun localDayFromPickerMillis(pickerMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerMillis }
    return Calendar.getInstance()
        .apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }
        .timeInMillis
}

/**
 * The inverse of [localDayFromPickerMillis]: turns a local instant into the value a Material
 * date picker uses for that calendar day, to preselect it or to bound the selectable days.
 *
 * @param localMillis any instant on the day, in the device's time zone
 * @return midnight UTC of that calendar day
 */
internal fun pickerMillisFromLocalDay(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        .apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        .timeInMillis
}

/**
 * Returns local midnight of the day [millis] falls on.
 *
 * @param millis any instant, in millis
 * @return local midnight of that day
 */
private fun startOfLocalDay(millis: Long): Long =
    Calendar.getInstance()
        .apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis
