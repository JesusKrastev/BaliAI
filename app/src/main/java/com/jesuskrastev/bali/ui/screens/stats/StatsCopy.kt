package com.jesuskrastev.bali.ui.screens.stats

import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.ReadinessResult
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase
import kotlin.math.abs

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
 * Describes the target for the latest mock exams, adjusted when fewer than the window exist.
 *
 * @return the goal, e.g. "Objetivo antes del examen: ${CalculateReadinessUseCase.RECENT_GOAL} de ${CalculateReadinessUseCase.RECENT_WINDOW}"
 */
fun recentGoalText(): String =
    "Objetivo antes del examen: ${CalculateReadinessUseCase.RECENT_GOAL} de ${CalculateReadinessUseCase.RECENT_WINDOW}"

/**
 * Words the distance to the exam date.
 *
 * @param daysToExam days until the exam, negative once it has passed
 * @return "Tu examen es hoy", "Tu examen es mañana", "Faltan 12 días para tu examen" or
 *   "Tu examen fue hace 3 días"
 */
fun examCountdownText(daysToExam: Int): String = when {
    daysToExam == 0 -> "Tu examen es hoy"
    daysToExam == 1 -> "Tu examen es mañana"
    daysToExam > 1 -> "Faltan $daysToExam días para tu examen"
    daysToExam == -1 -> "Tu examen fue ayer"
    else -> "Tu examen fue hace ${abs(daysToExam)} días"
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
    val text = String.format(java.util.Locale.US, "%.1f", value)
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
