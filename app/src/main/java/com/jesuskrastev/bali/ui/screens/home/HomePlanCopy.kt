package com.jesuskrastev.bali.ui.screens.home

import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.ui.screens.stats.PlanSummary
import com.jesuskrastev.bali.ui.screens.stats.PlanUrgency
import com.jesuskrastev.bali.ui.screens.stats.planDayText
import com.jesuskrastev.bali.ui.screens.stats.planPromiseText
import com.jesuskrastev.bali.ui.screens.stats.planSubjectOf
import com.jesuskrastev.bali.ui.screens.stats.planSummaryOf
import com.jesuskrastev.bali.ui.screens.stats.plural
import com.jesuskrastev.bali.ui.screens.stats.stageLabelOf
import com.jesuskrastev.bali.ui.screens.stats.urgency

/**
 * Where today's study stands on Home's plan sheet.
 */
enum class DailyGoalState {
    /** Today still has no test, mock exam or mini-game. */
    PENDING,

    /** Today already counts as a study day. */
    DONE,

    /** It is the day of the student's own exam: there is nothing left to study. */
    EXAM_DAY
}

/**
 * Today's goal as the plan sheet words it.
 *
 * @property state where today's study stands, which picks the row's icon
 * @property title the goal or its outcome, e.g. "Objetivo de hoy: 1 sesión"
 * @property detail one sentence under it
 */
data class DailyGoalCopy(
    val state: DailyGoalState,
    val title: String,
    val detail: String
)

/**
 * What Home's plan chip and the sheet it opens say.
 *
 * @property hasDate whether there is a date ahead; without one the sheet asks for it
 * @property urgency how close the date is, which sets the chip's tone like the statistics card's
 * @property chipLabel the chip's text, kept short to fit beside the streak and the coins: "32 días",
 *   "Mañana", "Hoy" or "Tu examen"
 * @property chipDescription what a screen reader says for the chip
 * @property stage label at the top of the sheet: the statistics card's stretch, or "TU EXAMEN"
 * @property title the sheet's headline: the plan's date, or the question for one
 * @property subtitle the days left, or why a date is needed
 * @property goal today's goal, or null when there is no date ahead
 * @property showsPendingDot whether the chip carries the dot that says today's goal is pending
 */
data class HomePlanCopy(
    val hasDate: Boolean,
    val urgency: PlanUrgency,
    val chipLabel: String,
    val chipDescription: String,
    val stage: String,
    val title: String,
    val subtitle: String,
    val goal: DailyGoalCopy?,
    val showsPendingDot: Boolean
)

/**
 * Reads what the plan chip needs from the user's profile: the same date the statistics countdown
 * shows ([planSummaryOf]), whether a saved date has already gone by, and whether today is done.
 *
 * @param user the profile, or null while it loads
 * @param now current time in millis
 * @return the chip's state; not loaded while [user] is null, so the chip does not flash a wrong state
 */
internal fun homePlanStateOf(user: User?, now: Long): HomePlanUiState {
    if (user == null) return HomePlanUiState()
    val plan = planSummaryOf(user.examDateMillis, user.planTargetMillis, now)
    val datePassed = plan.targetMillis == null && (user.examDateMillis != null || user.planTargetMillis != null)
    val studiedToday = DailyStreak.of(user).hasPracticedOn(now)
    return HomePlanUiState(
        isLoaded = true,
        plan = plan,
        datePassed = datePassed,
        studiedToday = studiedToday,
        copy = homePlanCopyOf(plan, datePassed, studiedToday)
    )
}

/**
 * Writes the plan chip's and the plan sheet's text. The stretch label and the date come from the
 * same functions as the statistics countdown, so Home and Statistics never disagree; the days left
 * are never negative, because a date that has gone by is treated as no date and asked for again.
 *
 * @param plan the date being counted down to, or an empty summary when there is none ahead
 * @param datePassed whether a date was saved but has already gone by
 * @param studiedToday whether today already has a study session
 * @return the texts to show
 */
fun homePlanCopyOf(plan: PlanSummary, datePassed: Boolean, studiedToday: Boolean): HomePlanCopy {
    val target = plan.targetMillis ?: return noDateCopyOf(datePassed)
    val urgency = plan.urgency()
    val days = plan.daysLeft
    val isExam = plan.isExamDate
    val subject = planSubjectOf(isExam)
    val goal = dailyGoalOf(urgency, days, isExam, studiedToday)
    val daysText = when (urgency) {
        PlanUrgency.TODAY -> if (isExam) "Hoy es tu examen" else "Tu fecha meta es hoy"
        PlanUrgency.TOMORROW -> if (isExam) "Tu examen es mañana" else "Tu fecha meta es mañana"
        else -> "Faltan $days días"
    }
    val goalStatus = when (goal.state) {
        DailyGoalState.PENDING -> " Objetivo de hoy pendiente."
        DailyGoalState.DONE -> " Objetivo de hoy hecho."
        DailyGoalState.EXAM_DAY -> ""
    }

    return HomePlanCopy(
        hasDate = true,
        urgency = urgency,
        chipLabel = when (urgency) {
            PlanUrgency.TODAY -> "Hoy"
            PlanUrgency.TOMORROW -> "Mañana"
            else -> "$days días"
        },
        chipDescription = buildString {
            append("Tu plan: ")
            append(if (days >= 2) "faltan $days días para $subject" else daysText.replaceFirstChar { it.lowercase() })
            append('.')
            append(goalStatus)
        },
        stage = stageLabelOf(urgency),
        title = if (isExam) "Examen el ${planDayText(target, withWeekday = true)}" else planPromiseText(target),
        subtitle = daysText,
        goal = goal,
        showsPendingDot = goal.state == DailyGoalState.PENDING
    )
}

/**
 * Writes the copy for when there is no date ahead: either none was ever saved, or it went by.
 *
 * @param datePassed whether a saved date has already gone by
 * @return the texts that ask for the exam date
 */
private fun noDateCopyOf(datePassed: Boolean): HomePlanCopy = HomePlanCopy(
    hasDate = false,
    urgency = PlanUrgency.NO_DATE,
    chipLabel = "Tu examen",
    chipDescription = if (datePassed) {
        "Tu plan: tu fecha ya ha pasado. Pon la nueva."
    } else {
        "Tu plan: sin fecha de examen. Ponla para ver cuántos días te quedan."
    },
    stage = "TU EXAMEN",
    title = if (datePassed) "Tu fecha ya ha pasado" else "¿Cuándo es tu examen?",
    subtitle = if (datePassed) {
        "Si aún no te has examinado, pon la nueva fecha y seguimos con la cuenta atrás."
    } else {
        "Ponle fecha y verás aquí cuántos días te quedan."
    },
    goal = null,
    showsPendingDot = false
)

/**
 * Writes today's goal. With the daily streak, one study session a day is the goal: a test, a
 * mock exam or a mini-game is enough, the same rule that keeps the streak alive.
 *
 * @param urgency how close the date is
 * @param daysLeft days until the date
 * @param isExam whether the date is the student's own exam
 * @param studiedToday whether today already has a session
 * @return the goal, done, pending, or replaced by a word of encouragement on exam day
 */
private fun dailyGoalOf(urgency: PlanUrgency, daysLeft: Int, isExam: Boolean, studiedToday: Boolean): DailyGoalCopy {
    if (isExam && urgency == PlanUrgency.TODAY) {
        return DailyGoalCopy(
            state = DailyGoalState.EXAM_DAY,
            title = "Hoy toca examen",
            detail = "Respira, lee cada pregunta entera y confía en lo que has practicado."
        )
    }
    if (!studiedToday) {
        return DailyGoalCopy(
            state = DailyGoalState.PENDING,
            title = "Objetivo de hoy: 1 sesión",
            detail = if (urgency == PlanUrgency.TOMORROW) {
                "Un repaso corto y a descansar: es tu último día de estudio."
            } else {
                "Vale un test, un simulacro o un minijuego."
            }
        )
    }
    val sessionsLeft = daysLeft - 1
    return DailyGoalCopy(
        state = DailyGoalState.DONE,
        title = "Hecho hoy",
        detail = when {
            urgency == PlanUrgency.TOMORROW -> "Descansa y llega con la cabeza fría."
            sessionsLeft < 1 -> "Si tu examen es otro día, cambia la fecha y seguimos."
            else -> {
                val verb = if (sessionsLeft == 1) "Te queda" else "Te quedan"
                "$verb ${plural(sessionsLeft, "sesión", "sesiones")} más hasta ${planSubjectOf(isExam)}."
            }
        }
    )
}
