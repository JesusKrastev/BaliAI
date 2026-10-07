package com.jesuskrastev.bali.ui.screens.stats

import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.MockExam
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.ReadinessResult
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What the readiness card says for one verdict.
 *
 * @property status short label in the pill
 * @property title headline
 * @property body one or two sentences explaining it
 * @property nextStep the single thing the user should do next
 */
data class ReadinessCopy(
    val status: String,
    val title: String,
    val body: String,
    val nextStep: String
)

/**
 * Writes the readiness card's text. Kept apart from the composables so every wording, plural and
 * edge case can be unit-tested, and so no sentence can promise a result: the chance shown is an
 * estimate from past mock exams, never a guarantee.
 *
 * @param readiness the verdict to describe
 * @return the texts to show
 */
fun readinessCopyOf(readiness: ReadinessResult): ReadinessCopy {
    val percent = readiness.passProbability?.let { percentOf(it) }
    return when (readiness.level) {
        ReadinessLevel.NOT_ENOUGH_DATA -> ReadinessCopy(
            status = "SIN DATOS SUFICIENTES",
            title = "Aún no podemos decirlo",
            body = if (readiness.mocksTaken == 0) {
                "Necesitamos ${plural(readiness.mocksMissing, "simulacro")} para calcular tus opciones reales de aprobar."
            } else {
                "Llevas ${plural(readiness.mocksTaken, "simulacro")}. Con ${CalculateReadinessUseCase.MIN_MOCKS} podemos estimar tus opciones de aprobar."
            },
            nextStep = "Haz ${plural(readiness.mocksMissing, "simulacro")} más para ver tu estimación."
        )

        ReadinessLevel.NOT_YET -> ReadinessCopy(
            status = "TODAVÍA NO",
            title = "Todavía no es seguro",
            body = "Con tus simulacros, aprobarías aproximadamente $percent de cada 100 exámenes.",
            nextStep = "Repasa tus temas más flojos y haz otro simulacro."
        )

        ReadinessLevel.ALMOST -> ReadinessCopy(
            status = "CASI LISTO",
            title = "Vas muy bien encaminado",
            body = "Con tus simulacros, aprobarías aproximadamente $percent de cada 100 exámenes.",
            nextStep = "Encadena simulacros con 3 fallos o menos para afianzarlo."
        )

        ReadinessLevel.READY -> ReadinessCopy(
            status = "LISTO",
            title = "Estás para presentarte",
            body = "Tus últimos simulacros apuntan a un aprobado: aprobarías aproximadamente $percent de cada 100 exámenes.",
            nextStep = "Mantén el ritmo hasta el día del examen: un repaso diario basta."
        )
    }
}

/**
 * Describes how the latest mock exams compare with the goal.
 *
 * @param readiness the verdict holding the latest mock exams
 * @return a sentence such as "Has aprobado 3 de tus últimos 5 simulacros", worded for zero or one
 *   mock exam so it never reads "últimos 1 simulacro"
 */
fun recentSummaryOf(readiness: ReadinessResult): String = when (readiness.recent.size) {
    0 -> "Todavía no has hecho ningún simulacro."
    1 -> if (readiness.passedInRecent == 1) {
        "Has hecho 1 simulacro y lo has aprobado."
    } else {
        "Has hecho 1 simulacro y no has llegado al aprobado."
    }
    else -> "Has aprobado ${readiness.passedInRecent} de tus últimos ${readiness.recent.size} simulacros."
}

/**
 * Describes the target for the latest mock exams.
 *
 * @return the goal, e.g. "Objetivo antes del examen: ${CalculateReadinessUseCase.RECENT_GOAL} de ${CalculateReadinessUseCase.RECENT_WINDOW}"
 */
fun recentGoalText(): String =
    "Objetivo antes del examen: ${CalculateReadinessUseCase.RECENT_GOAL} de ${CalculateReadinessUseCase.RECENT_WINDOW}"

/**
 * Says how far the latest mock exams are from the goal, beside the goal's segmented bar.
 *
 * @param readiness the verdict holding the latest mock exams
 * @return "Cumplido" once the goal is met, otherwise the passes still missing, e.g. "Faltan 2"
 */
fun recentGoalStatusOf(readiness: ReadinessResult): String {
    val missing = CalculateReadinessUseCase.RECENT_GOAL - readiness.passedInRecent
    return when {
        missing <= 0 -> "Cumplido"
        missing == 1 -> "Falta 1"
        else -> "Faltan $missing"
    }
}

/**
 * Counts the questions a mock exam got wrong, which is how the DGT exam is judged.
 *
 * @param exam the mock exam
 * @return e.g. "2 fallos" or "1 fallo"
 */
fun mistakesText(exam: MockExam): String = plural(exam.total - exam.score, "fallo")

/**
 * Spoken description of one slot of the latest mock exams.
 *
 * @param exam the mock exam, or null for a slot still to take
 * @return e.g. "Aprobado: 28 aciertos, 2 fallos"
 */
fun mockSlotDescriptionOf(exam: MockExam?): String = when {
    exam == null -> "Simulacro por hacer"
    exam.passed -> "Aprobado: ${plural(exam.score, "acierto")}, ${mistakesText(exam)}"
    else -> "Suspendido: ${plural(exam.score, "acierto")}, ${mistakesText(exam)}"
}

/**
 * Subtitle of the mock exam evolution chart.
 *
 * @param shown mock exams the chart draws
 * @param taken mock exams taken in total
 * @return e.g. "Aciertos de tus últimos 10 simulacros", or "Aciertos de tus 4 simulacros" when it draws all of them
 */
fun historySubtitleOf(shown: Int, taken: Int): String =
    if (taken > shown) "Aciertos de tus últimos $shown simulacros" else "Aciertos de tus ${plural(shown, "simulacro")}"

/**
 * Spoken description of the evolution chart, which is otherwise only drawn.
 *
 * @param exams the mock exams the chart draws, oldest first
 * @return e.g. "Aciertos de cada simulacro, del más antiguo al último: 25, 28 y 30. Se aprueba con 27."
 */
fun historyDescriptionOf(exams: List<MockExam>): String {
    val scores = exams.map { it.score.toString() }
    val list = if (scores.size < 2) scores.joinToString() else scores.dropLast(1).joinToString() + " y " + scores.last()
    return "Aciertos de cada simulacro, del más antiguo al último: $list. Se aprueba con ${ExamRules.PASS_SCORE}."
}

/**
 * Lowest score the evolution chart's axis shows: a round number at least five below the pass mark,
 * so the pass band never fills the chart, and low enough for the worst score.
 *
 * @param exams the mock exams the chart draws
 * @return a multiple of 5 from 0 up
 */
fun historyFloorOf(exams: List<MockExam>): Int {
    val lowest = minOf(exams.minOfOrNull { it.score } ?: ExamRules.PASS_SCORE, ExamRules.PASS_SCORE - 5)
    return (lowest.coerceAtLeast(0) / 5) * 5
}

/**
 * Writes a day short, for chart axes.
 *
 * @param millis any instant on the day
 * @return e.g. "5 oct"
 */
fun shortDayText(millis: Long): String =
    SimpleDateFormat("d MMM", SPANISH).format(Date(millis)).replace(".", "")

/**
 * The day a countdown points at, as a tear-off calendar page shows it.
 *
 * @property month the month, abbreviated and in capitals, e.g. "OCT"
 * @property day the day of the month, e.g. "19"
 * @property weekday the day of the week in capitals, e.g. "LUNES"
 */
data class CalendarPage(
    val month: String,
    val day: String,
    val weekday: String
)

/**
 * Splits a day into the three lines of a calendar page.
 *
 * @param millis any instant on the day
 * @return the page for that day
 */
fun calendarPageOf(millis: Long): CalendarPage {
    val date = Date(millis)
    return CalendarPage(
        month = SimpleDateFormat("MMM", SPANISH).format(date).replace(".", "").uppercase(SPANISH),
        day = SimpleDateFormat("d", SPANISH).format(date),
        weekday = SimpleDateFormat("EEEE", SPANISH).format(date).uppercase(SPANISH)
    )
}

/**
 * What the countdown card says for one date.
 *
 * @property stage label at the top, which names the stretch the student is in
 * @property number the big figure: the days left, or "MAÑANA" / "HOY" in the last two days
 * @property unit what [number] counts, in capitals
 * @property date the date itself, worded as an exam date or as the onboarding promise
 * @property headline the urgency message
 * @property detail what the days left mean for today, in sessions and mock exams
 */
data class CountdownCopy(
    val stage: String,
    val number: String,
    val unit: String,
    val date: String,
    val headline: String,
    val detail: String
)

/**
 * Writes the countdown card's text. The urgency comes from real figures — the days left, the
 * sessions still possible, whether today is done and how many mock exams are missing — so no
 * sentence invents a deadline, and none promises a result.
 *
 * @param plan the date being counted down to
 * @param readiness the verdict, for the mock exams still missing
 * @param studiedToday whether today already has a study session
 * @return the texts to show, or null when [plan] has no date and the card asks for one instead
 */
fun examCountdownCopyOf(plan: PlanSummary, readiness: ReadinessResult, studiedToday: Boolean): CountdownCopy? {
    val target = plan.targetMillis ?: return null
    val days = plan.daysLeft
    val urgency = plan.urgency()
    val isExam = plan.isExamDate
    val date = if (isExam) {
        planDayText(target, withWeekday = true).replaceFirstChar { it.uppercase() }
    } else {
        planPromiseText(target)
    }
    val subject = planSubjectOf(isExam)

    return CountdownCopy(
        stage = stageLabelOf(urgency),
        number = when (urgency) {
            PlanUrgency.TODAY -> "HOY"
            PlanUrgency.TOMORROW -> "MAÑANA"
            else -> "$days"
        },
        unit = if (days <= 1) "ES ${subject.uppercase()}" else "DÍAS PARA ${subject.uppercase()}",
        date = date,
        headline = when (urgency) {
            PlanUrgency.NO_DATE, PlanUrgency.ON_TRACK -> "Tienes tiempo, pero se acaba."
            PlanUrgency.MONTH -> "Queda menos de un mes."
            PlanUrgency.TWO_WEEKS -> "Quedan dos semanas o menos."
            PlanUrgency.FINAL_WEEK -> "Última semana: no hay días de sobra."
            PlanUrgency.TOMORROW -> if (isExam) "Mañana te examinas." else "Mañana vence tu fecha meta."
            PlanUrgency.TODAY -> if (isExam) "Hoy es tu examen." else "Hoy vence tu fecha meta."
        },
        detail = when (urgency) {
            PlanUrgency.TODAY ->
                if (isExam) "Respira, lee cada pregunta entera y confía en lo que has practicado."
                else "Si tu examen es otro día, cámbiala para seguir con la cuenta atrás."
            PlanUrgency.TOMORROW ->
                if (studiedToday) "Hoy ya has estudiado: descansa y llega con la cabeza fría."
                else "Haz un repaso corto hoy y descansa: es tu último día de estudio."
            else -> sessionsLeftText(days, studiedToday) + mocksMissingText(readiness)
        }
    )
}

/**
 * Names the stretch a countdown is in. Home's plan chip and the statistics card both use it, so
 * the two screens never call the same day by different names.
 *
 * @param urgency how close the date is
 * @return the label in capitals, e.g. "RECTA FINAL"
 */
internal fun stageLabelOf(urgency: PlanUrgency): String = when (urgency) {
    PlanUrgency.NO_DATE, PlanUrgency.ON_TRACK -> "CUENTA ATRÁS"
    PlanUrgency.MONTH -> "ÚLTIMO MES"
    PlanUrgency.TWO_WEEKS -> "ÚLTIMAS 2 SEMANAS"
    PlanUrgency.FINAL_WEEK, PlanUrgency.TOMORROW -> "RECTA FINAL"
    PlanUrgency.TODAY -> "HA LLEGADO EL DÍA"
}

/**
 * Writes a plan's day in Spanish, in lower case.
 *
 * @param millis any instant on the day
 * @param withWeekday whether to start with the day of the week
 * @return e.g. "sábado, 14 de noviembre" or "14 de noviembre"
 */
internal fun planDayText(millis: Long, withWeekday: Boolean): String =
    SimpleDateFormat(if (withWeekday) "EEEE, d 'de' MMMM" else "d 'de' MMMM", SPANISH).format(Date(millis))

/**
 * Words the date the onboarding plan promised, as the onboarding itself said it.
 *
 * @param millis the plan's date
 * @return e.g. "Carnet antes del 14 de noviembre"
 */
internal fun planPromiseText(millis: Long): String = "Carnet antes del ${planDayText(millis, withWeekday = false)}"

/**
 * Names what a plan's date is, for sentences such as "faltan 5 días para tu examen".
 *
 * @param isExamDate whether the date is the student's own exam date
 * @return "tu examen" or "tu fecha meta"
 */
internal fun planSubjectOf(isExamDate: Boolean): String = if (isExamDate) "tu examen" else "tu fecha meta"

/** Locale every date on the plan is written in. */
private val SPANISH = Locale("es", "ES")

/**
 * Says how many study sessions are still possible before the date, counting today until it is done.
 *
 * @param daysLeft days until the date, at least 2
 * @param studiedToday whether today already has a session
 * @return e.g. "Tienes 14 sesiones por delante y la de hoy aún no está hecha."
 */
private fun sessionsLeftText(daysLeft: Int, studiedToday: Boolean): String {
    if (!studiedToday) return "Tienes $daysLeft sesiones por delante y la de hoy aún no está hecha."
    val remaining = daysLeft - 1
    val verb = if (remaining == 1) "te queda" else "te quedan"
    return "Hoy ya has estudiado: $verb ${plural(remaining, "sesión", "sesiones")} más."
}

/**
 * Adds the mock exams still missing for a verdict, which is the other thing that needs time.
 *
 * @param readiness the verdict
 * @return a sentence starting with a space, or an empty string when the verdict is already shown
 */
private fun mocksMissingText(readiness: ReadinessResult): String {
    if (readiness.level != ReadinessLevel.NOT_ENOUGH_DATA || readiness.mocksMissing <= 0) return ""
    val verb = if (readiness.mocksMissing == 1) "te falta" else "te faltan"
    return " Y $verb ${plural(readiness.mocksMissing, "simulacro")} para saber si estás listo."
}

/**
 * Words a score trend between the latest mock exams and the ones before.
 *
 * @param trend change in mean correct answers
 * @return an arrow and the signed change, e.g. "▲ +1,3"
 */
fun trendText(trend: Float): String {
    val rounded = Math.round(trend * 10) / 10f
    val sign = when {
        rounded > 0f -> "▲ +"
        rounded < 0f -> "▼ "
        else -> "= "
    }
    return sign + formatDecimal(rounded)
}

/**
 * Formats a number with one decimal and a comma, the Spanish way, dropping a trailing ",0".
 *
 * @param value the number
 * @return e.g. "25,4" or "27"
 */
fun formatDecimal(value: Float): String {
    val text = String.format(Locale.US, "%.1f", value)
    return text.removeSuffix(".0").replace('.', ',')
}

/**
 * Turns a 0-1 share into a whole percentage text.
 *
 * @param share a share from 0 to 1
 * @return e.g. "87 %"
 */
fun percentText(share: Float): String = "${percentOf(share)} %"

/**
 * Rounds a 0-1 share to a whole percentage, never showing 0 or 100 for values that are neither,
 * so "100 %" always means exactly all and "0 %" exactly none.
 *
 * @param share a share from 0 to 1
 * @return a whole number from 0 to 100
 */
fun percentOf(share: Float): Int {
    val rounded = Math.round(share * 100)
    return when {
        share in 0.0001f..0.9999f -> rounded.coerceIn(1, 99)
        else -> rounded.coerceIn(0, 100)
    }
}

/**
 * Pairs a count with a noun in singular or plural.
 *
 * @param count how many
 * @param noun the singular noun
 * @param pluralNoun the plural noun, when it is not just [noun] plus "s"
 * @return e.g. "1 simulacro" or "3 simulacros"
 */
fun plural(count: Int, noun: String, pluralNoun: String = "${noun}s"): String =
    if (count == 1) "$count $noun" else "$count $pluralNoun"
