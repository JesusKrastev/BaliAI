package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.annotation.RawRes
import com.jesuskrastev.bali.domain.model.StudyRhythm
import com.jesuskrastev.bali.domain.model.StudySlot
import com.jesuskrastev.bali.ui.screens.stats.calendarDaysBetween
import java.text.Normalizer
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Content of a purely informational onboarding screen: a single idea presented as
 * one big visual plus a short line. The headline itself is delivered by the mascot
 * bubble, so it is intentionally not part of this model.
 *
 * @param emoji large symbol shown at the top, used when [animation] is null
 * @param body short, punchy line that lands the idea
 * @param animation optional Lottie raw resource rendered instead of [emoji]
 * @param scene optional hand-drawn scene, rendered instead of [animation] and [emoji]
 */
data class NarrativeContent(
    val emoji: String,
    val body: String,
    @RawRes val animation: Int? = null,
    val scene: com.jesuskrastev.bali.ui.screens.onboarding.steps.NarrativeScene? = null
)

/**
 * A review shown as social proof.
 *
 * @param name who wrote it
 * @param title the one-line verdict
 * @param body the review itself, rendered between quotes by the screens
 */
data class Testimonial(val name: String, val title: String, val body: String)

/**
 * One answer to "¿Cómo te gusta practicar?".
 *
 * @property key stable value reported to analytics
 * @property emoji the big symbol of the tile
 * @property label two words at most, read at a glance
 * @property hint one short line under the label
 */
data class LearningStyle(val key: String, val emoji: String, val label: String, val hint: String)

/**
 * Strips the leading emoji from an option label, so the answer can be shown as plain text
 * or reported to analytics without dragging its decoration along.
 *
 * @param option the option label as defined in [OnboardingConfig]
 * @return the label without its leading emoji
 */
fun optionLabel(option: String): String =
    option.substringAfter(' ', missingDelimiterValue = option).trim()

/**
 * Formats the time left until the exam in the largest sensible unit.
 *
 * @param days whole days left until the exam
 * @return a human-readable countdown such as "3 semanas" or "5 días"
 */
fun examCountdownLabel(days: Long): String = when {
    days >= 14 -> "${days / 7} semanas"
    days >= 7 -> "1 semana"
    days == 1L -> "1 día"
    days <= 0L -> "menos de un día"
    else -> "$days días"
}

/**
 * OnboardingConfig holds all the static data for the onboarding flow.
 * Separating data from logic improves maintainability and testability.
 */
object OnboardingConfig {

    /** The user has never sat the theory exam. */
    const val EXPERIENCE_FIRST_TIME = "🎓 Sí, es mi primera vez"

    /** The user has already failed the theory exam at least once. */
    const val EXPERIENCE_RETRY = "🔁 No, ya he suspendido antes"

    /**
     * Kept to two options on purpose: a binary question is answered in a single tap and
     * it is the only distinction the rest of the flow actually branches on.
     */
    val experiences = listOf(EXPERIENCE_FIRST_TIME, EXPERIENCE_RETRY)

    /** The user has no idea where to start. */
    const val BLOCKER_NO_START = "😵‍💫 No sé ni por dónde empezar"

    /** The user studies but cannot see any progress. */
    const val BLOCKER_NO_PROGRESS = "📉 Estudio pero no veo que avance"

    /** The user is missing a method to follow. */
    const val BLOCKER_NO_METHOD = "🧭 Me falta un método claro"

    val theoryBlockers = listOf(BLOCKER_NO_START, BLOCKER_NO_PROGRESS, BLOCKER_NO_METHOD)

    /** The user fears turning up to the exam without really being ready. */
    const val CONCERN_NOT_READY = "😨 Llegar sin estar realmente preparado"

    /** The user fears the exam will not look like what they studied. */
    const val CONCERN_EXAM_MISMATCH = "🤨 Que el examen no se parezca a lo que he estudiado"

    /** The user fears failing over a detail they misread. */
    const val CONCERN_SILLY_MISTAKES = "🤦 Fallar por detalles tontos"

    /**
     * What the user is afraid of on exam day. It picks the questions of the mini-test that
     * follows, so the first taste of the app tests exactly that fear.
     */
    val concerns = listOf(CONCERN_NOT_READY, CONCERN_EXAM_MISMATCH, CONCERN_SILLY_MISTAKES)

    /**
     * Self-assessed starting point. Binary on purpose: it sets the tone of the plan without
     * asking the user to score themselves on a scale they cannot calibrate. Worded so it reads
     * the same whoever answers it.
     */
    val readinessLevels = listOf(
        "💪 Tengo una base, pero quiero ir con todo bien atado",
        "🚀 Aún me falta mucho, necesito darle caña"
    )

    /** The licence as a step towards not depending on anyone. */
    const val MOTIVATION_INDEPENDENCE = "🕊️ Ser más independiente"

    /** The licence as a job requirement. */
    const val MOTIVATION_WORK = "💼 Tener mejores oportunidades de trabajo"

    /** The licence as freedom of movement. */
    const val MOTIVATION_FREEDOM = "🌍 Moverme con libertad"

    val motivations = listOf(MOTIVATION_INDEPENDENCE, MOTIVATION_WORK, MOTIVATION_FREEDOM)

    /**
     * Sorts the exam date into the buckets the funnel was segmented by before the date picker,
     * so analytics can still compare the groups.
     *
     * @param examDate the exam day picked, or null when the user has no date yet
     * @param now current time in millis
     * @return `imminent` (under 3 days), `soon` (up to 2 weeks), `later` or `unbooked`
     */
    fun examTimingTag(examDate: Long?, now: Long = System.currentTimeMillis()): String {
        if (examDate == null) return "unbooked"
        val days = TimeUnit.MILLISECONDS.toDays(examDate - now)
        return when {
            days < 3 -> "imminent"
            days <= 14 -> "soon"
            else -> "later"
        }
    }

    /** Studying every single day. */
    const val WEEKLY_STUDY_DAILY = "🔥 Todos los días, quiero aprobar cuanto antes"

    /** Studying a few times a week. */
    const val WEEKLY_STUDY_OFTEN = "📖 Varias veces por semana"

    /** Studying whenever there is a gap. */
    const val WEEKLY_STUDY_WHENEVER = "🤷 Cuando pueda"

    /**
     * How often the user intends to study. Weekly rather than daily: committing to a
     * rhythm is easier to say yes to than committing to a number of minutes.
     */
    val weeklyStudyOptions = listOf(
        WEEKLY_STUDY_DAILY, WEEKLY_STUDY_OFTEN, WEEKLY_STUDY_WHENEVER
    )

    /**
     * Short form of a weekly study option, for the places that show it inside a sentence
     * or a compact card and cannot fit the full label.
     *
     * @param option one of [weeklyStudyOptions], or null if unanswered
     * @return a two or three word rhythm, or null when there is nothing to show
     */
    fun weeklyStudyShortLabel(option: String?): String? = when (option) {
        WEEKLY_STUDY_DAILY -> "Todos los días"
        WEEKLY_STUDY_OFTEN -> "Varias veces por semana"
        WEEKLY_STUDY_WHENEVER -> "Cuando puedas"
        else -> null
    }

    /**
     * The rhythm a weekly study answer commits to, as the reminders understand it.
     *
     * @param option one of [weeklyStudyOptions], or null if unanswered
     * @return the matching rhythm, or null when there is none
     */
    fun studyRhythmFor(option: String?): StudyRhythm? = when (option) {
        WEEKLY_STUDY_DAILY -> StudyRhythm.DAILY
        WEEKLY_STUDY_OFTEN -> StudyRhythm.OFTEN
        WEEKLY_STUDY_WHENEVER -> StudyRhythm.WHENEVER
        else -> null
    }

    /**
     * Formats the hour a study reminder goes out at.
     *
     * @param slot the part of the day the user picked
     * @return the hour as the app writes times, e.g. "21:00"
     */
    fun reminderTimeLabel(slot: StudySlot): String = "${slot.hour}:00"

    /**
     * When the user likes to study, keyed by the option label. The hour is part of the label
     * so the reminder offered on the next screen is no surprise, and it is built from
     * [StudySlot.hour] so the two can never disagree.
     */
    val studyTimes: Map<String, StudySlot> = linkedMapOf(
        "🌅 Por la mañana (${reminderTimeLabel(StudySlot.MORNING)})" to StudySlot.MORNING,
        "☀️ A mediodía (${reminderTimeLabel(StudySlot.NOON)})" to StudySlot.NOON,
        "🌆 Por la tarde (${reminderTimeLabel(StudySlot.AFTERNOON)})" to StudySlot.AFTERNOON,
        "🌙 Por la noche (${reminderTimeLabel(StudySlot.NIGHT)})" to StudySlot.NIGHT
    )

    /**
     * Why a reminder is worth saying yes to, built on the rhythm the user committed to two
     * screens earlier. Text wrapped in pipes is rendered highlighted.
     *
     * @param weeklyStudy one of [weeklyStudyOptions], or null if unanswered
     * @return the body line of the notifications screen
     */
    fun notificationsPitch(weeklyStudy: String?): String = when (weeklyStudy) {
        WEEKLY_STUDY_DAILY -> "Un aviso al día, a tu hora. |10 preguntas| y sigues con tu plan."
        WEEKLY_STUDY_OFTEN -> "Has dicho que estudiarás |varias veces por semana|. Un aviso a tu hora y no pierdes la racha."
        else -> "Un aviso a tu hora para que |no se te pase|. Nunca de madrugada."
    }

    /** Weeks the plan needs when there is no booked exam to aim at, by study rhythm. */
    private const val PLAN_WEEKS_DAILY = 3
    private const val PLAN_WEEKS_OFTEN = 5
    private const val PLAN_WEEKS_WHENEVER = 8

    /**
     * Works out the day the plan aims to have the theory exam passed by: the one shown in the
     * plan reveal ("Puedes aprobar el teórico antes del…") and the one saved for Home's plan card,
     * so both always say the same thing.
     *
     * An exam date from today on is the honest answer. Without one the date is derived from the
     * rhythm the user committed to, which keeps the promise personal instead of inventing a
     * deadline.
     *
     * @param examDate the exam day the user picked (local midnight), or null when they have none
     * @param weeklyStudy one of [weeklyStudyOptions], or null if unanswered
     * @param now current time in millis, the day the plan starts
     * @return the target day in millis
     */
    fun planTargetMillis(examDate: Long?, weeklyStudy: String?, now: Long = System.currentTimeMillis()): Long {
        examDate?.takeIf { calendarDaysBetween(now, it) >= 0 }?.let { return it }

        val weeks = when (weeklyStudy) {
            WEEKLY_STUDY_DAILY -> PLAN_WEEKS_DAILY
            WEEKLY_STUDY_OFTEN -> PLAN_WEEKS_OFTEN
            else -> PLAN_WEEKS_WHENEVER
        }
        return Calendar.getInstance()
            .apply {
                timeInMillis = now
                add(Calendar.WEEK_OF_YEAR, weeks)
            }
            .timeInMillis
    }

    /** Short sessions of ten questions. */
    val STYLE_QUICK_TESTS = LearningStyle("quick_tests", "⚡", "Tests cortos", "10 preguntas y listo")

    /** Full exam simulations. */
    val STYLE_MOCK_EXAMS = LearningStyle("mock_exams", "🎓", "Simulacros", "Como el examen real")

    /** The mini-games. */
    val STYLE_GAMES = LearningStyle("games", "🎮", "Jugando", "Señales y normas en minijuegos")

    /** Understanding the why of every answer. */
    val STYLE_EXPLANATIONS = LearningStyle("explanations", "💡", "Entendiendo", "Cada fallo bien explicado")

    /**
     * How the user likes to practise. Four styles that are really different from each other —
     * each one is a part of the app — so the answer says something, and short enough to read at
     * a glance in a two-by-two grid.
     */
    val learningStyles = listOf(STYLE_QUICK_TESTS, STYLE_MOCK_EXAMS, STYLE_GAMES, STYLE_EXPLANATIONS)

    /**
     * Looks a learning style up by its key.
     *
     * @param key a [LearningStyle.key], or null when unanswered
     * @return the style, or null when [key] is unknown
     */
    fun learningStyle(key: String?): LearningStyle? = learningStyles.firstOrNull { it.key == key }

    /**
     * The 52 Spanish provinces, in alphabetical order. The theory exam is the same in all of
     * them, so the answer only makes the plan the user's own: it is never used to claim a
     * different question bank per province.
     */
    val provinces = listOf(
        "Álava", "Albacete", "Alicante", "Almería", "Asturias", "Ávila", "Badajoz",
        "Baleares", "Barcelona", "Burgos", "Cáceres", "Cádiz", "Cantabria", "Castellón",
        "Ceuta", "Ciudad Real", "Córdoba", "Cuenca", "Girona", "Granada", "Guadalajara",
        "Guipúzcoa", "Huelva", "Huesca", "Jaén", "La Coruña", "La Rioja", "Las Palmas",
        "León", "Lleida", "Lugo", "Madrid", "Málaga", "Melilla", "Murcia", "Navarra",
        "Ourense", "Palencia", "Pontevedra", "Salamanca", "Santa Cruz de Tenerife",
        "Segovia", "Sevilla", "Soria", "Tarragona", "Teruel", "Toledo", "Valencia",
        "Valladolid", "Vizcaya", "Zamora", "Zaragoza"
    )

    /**
     * How many people the app claims to have helped. Lives here because the number appears on
     * the welcome screen, the learning-curve screen, the social proof screen and the plan, and
     * a flow that says 1.000 on one screen and something else two screens later stops being
     * believed on both.
     */
    const val USERS_HELPED = "1.000"

    /** Store rating shown next to the stars, in the same voice everywhere. */
    const val STORE_RATING = "4.8 / 5 en Google Play"

    /**
     * Reviews used as social proof, shared by every screen that shows one. The social proof
     * screen takes the first [SOCIAL_PROOF_TESTIMONIALS] and the plan takes the rest, so the
     * two screens never quote the same person minutes apart.
     */
    val testimonials = listOf(
        Testimonial(
            name = "Sara M.",
            title = "Aprobé al primer intento 🎉",
            body = "Llevaba meses estudiando sola sin avanzar. Con Bali en 3 semanas lo clavé."
        ),
        Testimonial(
            name = "Carlos R.",
            title = "Lo recomiendo sin dudar ⭐",
            body = "Las explicaciones de la IA son mucho mejores que estudiar el manual. No me aburrí en ningún momento."
        ),
        Testimonial(
            name = "Ana G.",
            title = "El mejor dinero que he gastado ✨",
            body = "Tenía miedo a las preguntas trampa. Bali me enseñó exactamente cómo detectarlas."
        ),
        Testimonial(
            name = "Alejandro B.",
            title = "Igual que el examen de verdad 🎯",
            body = "Los test se parecen muchísimo a los de la DGT. Llegué al examen sin ninguna sorpresa."
        ),
        Testimonial(
            name = "Lucía P.",
            title = "Aprobé con un solo fallo 🙌",
            body = "Hacía un test cada mañana en el autobús. Cuando llegó el día ya me lo sabía de memoria."
        )
    )

    /** How many of [testimonials] belong to the social proof screen. */
    const val SOCIAL_PROOF_TESTIMONIALS = 3

    /** Combining marks left behind by NFD normalisation, i.e. the accents themselves. */
    private val DIACRITICS = Regex("\\p{Mn}+")

    /**
     * Lowercases and strips accents so two spellings of the same word compare equal.
     *
     * @param text the text to fold
     * @return the text without accents, lowercased and trimmed
     */
    fun foldForSearch(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()

    /**
     * Filters the province list by what the user typed.
     *
     * Matching ignores case and accents — nobody types "Almería" with the accent in a search
     * box — and looks anywhere in the name, so "coruña" still finds "La Coruña".
     *
     * @param query the current content of the search field
     * @return the provinces to offer, or all of them when the query is blank
     */
    fun provincesMatching(query: String): List<String> {
        val needle = foldForSearch(query)
        if (needle.isEmpty()) return provinces
        return provinces.filter { foldForSearch(it).contains(needle) }
    }

    /**
     * Groups the matching provinces under their initial, for the section headers of the picker.
     *
     * The initial is taken from the folded name so "Álava" and "Ávila" both land under A
     * instead of forming a section of their own.
     *
     * @param query the current content of the search field
     * @return the provinces keyed by initial, in alphabetical order
     */
    fun provincesByInitial(query: String): Map<Char, List<String>> =
        provincesMatching(query).groupBy { foldForSearch(it).first().uppercaseChar() }
}
