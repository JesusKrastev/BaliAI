package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.annotation.RawRes
import com.jesuskrastev.bali.R
import java.text.Normalizer
import java.util.concurrent.TimeUnit

/**
 * Content of a purely informational onboarding screen: a single idea presented as
 * one big visual plus a short line. The headline itself is delivered by the mascot
 * bubble, so it is intentionally not part of this model.
 *
 * @param emoji large symbol shown at the top, used when [animation] is null
 * @param body short, punchy line that lands the idea
 * @param animation optional Lottie raw resource rendered instead of [emoji]
 */
data class NarrativeContent(
    val emoji: String,
    val body: String,
    @RawRes val animation: Int? = null
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

    /**
     * What the user is afraid of on exam day. Asked right after the study blocker so the
     * diagnosis covers both the problem today and the fear it feeds.
     */
    val concerns = listOf(
        "😨 Llegar sin estar realmente preparado",
        "🤨 Que el examen no se parezca a lo que he estudiado",
        "🤦 Fallar por detalles tontos"
    )

    /**
     * Self-assessed starting point. Binary on purpose: it sets the tone of the plan without
     * asking the user to score themselves on a scale they cannot calibrate.
     */
    val readinessLevels = listOf(
        "💪 Tengo una base, pero quiero llegar bien preparado",
        "🚀 No me siento listo, necesito darle caña"
    )

    /** The licence as a step towards not depending on anyone. */
    const val MOTIVATION_INDEPENDENCE = "🕊️ Ser más independiente"

    /** The licence as a job requirement. */
    const val MOTIVATION_WORK = "💼 Tener mejores oportunidades de trabajo"

    /** The licence as freedom of movement. */
    const val MOTIVATION_FREEDOM = "🌍 Moverme con libertad"

    val motivations = listOf(MOTIVATION_INDEPENDENCE, MOTIVATION_WORK, MOTIVATION_FREEDOM)

    val futureImpacts = listOf(
        "🚀 Me sentiría mucho más libre",
        "🗺️ Podría moverme cuando quisiera",
        "📈 Me ayudaría en el trabajo o los estudios"
    )

    /** The exam is days away. */
    const val EXAM_TIMING_IMMINENT = "🔥 En menos de 3 días"

    /** The exam falls within the next fortnight. */
    const val EXAM_TIMING_SOON = "📅 Esta semana o la siguiente"

    /** The exam is more than two weeks out. */
    const val EXAM_TIMING_LATER = "🗓️ En más de 2 semanas"

    /** No exam booked yet, so there is no date to schedule the plan against. */
    const val EXAM_TIMING_UNBOOKED = "🤷 Aún no lo he reservado"

    /**
     * How far away the exam is. Buckets instead of a calendar: a tap answers it, and the
     * plan only ever needs the order of magnitude, not the exact day.
     */
    val examTimings = listOf(
        EXAM_TIMING_IMMINENT, EXAM_TIMING_SOON, EXAM_TIMING_LATER, EXAM_TIMING_UNBOOKED
    )

    /**
     * Turns an exam timing bucket into the approximate date the plan is built around.
     *
     * @param timing one of [examTimings]
     * @return the estimated exam date in millis, or null when no exam is booked
     */
    fun examDateFor(timing: String): Long? {
        val days = when (timing) {
            EXAM_TIMING_IMMINENT -> 2L
            EXAM_TIMING_SOON -> 10L
            EXAM_TIMING_LATER -> 21L
            else -> return null
        }
        return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)
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

    val learningPreferences = listOf(
        "🎯 Tests adaptados a mis fallos",
        "🎲 Tests aleatorios, para estar listo ante todo",
        "🤖 Explicaciones con IA de cada error",
        "🎓 Simulacros de examen reales"
    )

    /**
     * The 52 Spanish provinces, in alphabetical order. Asked so the flow can promise the
     * user the exact question bank of the traffic office they will sit the exam in.
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

    /**
     * Copy for every informational screen of the emotional arc, keyed by its step.
     * The loss block agitates the cost of not having the licence; the gain block
     * pays it off with the life the licence unlocks.
     */
    val narratives: Map<OnboardingStep, NarrativeContent> = mapOf(
        OnboardingStep.Empathy to NarrativeContent(
            emoji = "🫂",
            body = "No tener carnet se siente como una |prisión sin barrotes|.",
            animation = R.raw.sad_face
        ),
        OnboardingStep.LossTime to NarrativeContent(
            emoji = "⏳",
            body = "Cada minuto esperando en la parada, con frío o con lluvia, es |tiempo de tu vida que no vuelve|.",
            animation = R.raw.waiting
        ),
        OnboardingStep.LossOpportunity to NarrativeContent(
            emoji = "🚪",
            body = "La mitad de las ofertas de trabajo piden carnet. Un |freno invisible| en tu carrera.",
            animation = R.raw.door_open
        ),
        OnboardingStep.LossAutonomy to NarrativeContent(
            emoji = "⛓️",
            body = "Sin carnet, |tu vida la deciden otros|: los horarios del transporte y los favores ajenos.",
            animation = R.raw.bus
        ),
        OnboardingStep.GainFreedom to NarrativeContent(
            emoji = "🕊️",
            body = "Sal cuando quieras y vuelve cuando quieras, |sin depender de nadie|.",
            animation = R.raw.freedom
        ),
        OnboardingStep.GainExperiences to NarrativeContent(
            emoji = "🏖️",
            body = "Esa escapada, ese viaje con amigos, esa playa lejos. Con el carnet, |todo eso pasa a ser un plan real|.",
            animation = R.raw.experiences
        ),
        OnboardingStep.GainLevelUp to NarrativeContent(
            emoji = "📈",
            body = "Accedes a trabajos y planes que antes te quedaban fuera. Lo que hoy es un freno |se convierte en tu siguiente paso|.",
            animation = R.raw.level_up
        )
    )

    /**
     * Copy for the screen that confirms the province, built at runtime because the line
     * names the province the user just picked.
     *
     * @param province the province selected on the previous step, or null if skipped
     * @return the single-idea content for the confirmation screen
     */
    fun provinceConfirmation(province: String?): NarrativeContent = NarrativeContent(
        emoji = "📋",
        body = province
            ?.let { "Practicas con las |mismas preguntas| que se usan en $it. Ni una de relleno." }
            ?: "Practicas con las |mismas preguntas| que usa la DGT. Ni una de relleno.",
        animation = R.raw.notebook
    )
}
