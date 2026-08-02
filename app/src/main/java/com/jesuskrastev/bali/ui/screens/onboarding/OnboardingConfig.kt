package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.annotation.RawRes
import com.jesuskrastev.bali.R

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
    val licenses = listOf(
        "🚗 Coche (B)", "🏍️ Moto (A2)", "🏍 Moto (A1)", "🛵 Ciclomotor (AM)"
    )

    /** The user has never sat the theory exam. */
    const val EXPERIENCE_FIRST_TIME = "🎓 Sí, es mi primera vez"

    /** The user has already failed the theory exam at least once. */
    const val EXPERIENCE_RETRY = "🔁 No, ya he suspendido antes"

    /**
     * Kept to two options on purpose: a binary question is answered in a single tap and
     * it is the only distinction the rest of the flow actually branches on.
     */
    val experiences = listOf(EXPERIENCE_FIRST_TIME, EXPERIENCE_RETRY)

    val theoryBlockers = listOf(
        "😵‍💫 No sé ni por dónde empezar",
        "📉 Estudio pero no veo que avance",
        "🧭 Me falta un método claro"
    )

    val motivations = listOf(
        "🕊️ Ser más independiente",
        "💼 Tener mejores oportunidades de trabajo",
        "🌍 Moverme con libertad"
    )

    val futureImpacts = listOf(
        "🚀 Me sentiría mucho más libre",
        "🗺️ Podría moverme cuando quisiera",
        "📈 Me ayudaría en el trabajo o los estudios"
    )

    val dailyGoals = listOf(
        "⚡ 15 min (Modo rápido)",
        "🕓 30 min (Recomendado)",
        "🔥 1 hora (Intensivo)"
    )

    val learningPreferences = listOf(
        "🎯 Tests adaptados a mis fallos",
        "🎲 Tests aleatorios, para estar listo ante todo",
        "🤖 Explicaciones con IA de cada error",
        "🎓 Simulacros de examen reales"
    )

    val difficultTopics = listOf(
        "🛑 Señales y marcas", "🏎️ Velocidades máximas", "🚦 Prioridades de paso",
        "📄 Documentación y multas", "🔧 Mecánica y luces", "🍻 Alcohol, drogas y fatiga"
    )

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
}
