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
