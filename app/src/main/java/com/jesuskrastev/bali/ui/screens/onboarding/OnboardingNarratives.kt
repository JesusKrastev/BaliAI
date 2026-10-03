package com.jesuskrastev.bali.ui.screens.onboarding

import com.jesuskrastev.bali.R

/**
 * What one screen of the emotional arc says: the mascot's headline and the visual with its line.
 *
 * @property headline the mascot bubble; `|` pairs mark the words to highlight
 * @property content the visual and the body line under it
 */
data class Narrative(val headline: String, val content: NarrativeContent)

/**
 * Copy of the emotional arc (empathy, the three losses, the three gains), written per answer.
 *
 * Every screen answers something the user said in the first questions, so the arc reads as being
 * about them: the empathy screen echoes what blocks them, the time lost follows why they want the
 * licence, the cost of failing depends on whether they already failed, the worst case is the fear
 * they picked, and the gains mirror all of it. Unanswered questions fall back to general copy.
 * No line carries a figure that cannot be sourced.
 */
object OnboardingNarratives {

    /**
     * Picks the copy of an emotional-arc screen for the answers collected so far.
     *
     * @param step the step on screen
     * @param data the answers collected so far
     * @return the screen's copy, or null when [step] is not part of the arc
     */
    fun forStep(step: OnboardingStep, data: OnboardingData): Narrative? = when (step) {
        OnboardingStep.Empathy -> empathy(data)
        OnboardingStep.LossTime -> lossTime(data.motivation)
        OnboardingStep.LossOpportunity -> lossOpportunity(data.experience)
        OnboardingStep.LossAutonomy -> lossAutonomy(data.concern)
        OnboardingStep.GainFreedom -> gainFreedom(data.motivation)
        OnboardingStep.GainExperiences -> gainExperiences(data.concern)
        OnboardingStep.GainLevelUp -> gainLevelUp(data.experience)
        else -> null
    }

    /** Echoes what blocks the user, or the frustration of having failed before. */
    private fun empathy(data: OnboardingData): Narrative {
        if (data.experience == OnboardingConfig.EXPERIENCE_RETRY) {
            return narrative(
                headline = "|Te entiendo|. Suspender da mucha rabia 🫂",
                body = "Lo bueno: ya sabes cómo es el examen por dentro. |Esta vez juegas con ventaja|.",
                emoji = "🫂",
                animation = R.raw.sad_face
            )
        }
        val body = when (data.theoryBlocker) {
            OnboardingConfig.BLOCKER_NO_START ->
                "Un manual enorme y tests sueltos por todas partes: normal no saber |por dónde empezar|."
            OnboardingConfig.BLOCKER_NO_PROGRESS ->
                "Estudiar sin ver avance |desanima a cualquiera|. Lo que falta es saber qué se te da mal."
            OnboardingConfig.BLOCKER_NO_METHOD ->
                "Hacer tests a lo loco no es un método. Lo que funciona es |practicar lo que fallas| hasta dominarlo."
            else -> "No tener carnet pesa más de lo que parece. |Vamos a cambiarlo|."
        }
        return narrative(
            headline = "|Te entiendo|. No te pasa solo a ti 🫂",
            body = body,
            emoji = "🫂",
            animation = R.raw.sad_face
        )
    }

    /** The time lost without a licence, in the terms of why the user wants one. */
    private fun lossTime(motivation: String?): Narrative = when (motivation) {
        OnboardingConfig.MOTIVATION_INDEPENDENCE -> narrative(
            headline = "Mientras tanto, |dependes de otros| ⏳",
            body = "Pedir que te lleven, cuadrar horarios, esperar a que alguien pueda. " +
                "|Cada semana sin carnet es otra semana igual|.",
            emoji = "⏳",
            animation = R.raw.waiting
        )
        OnboardingConfig.MOTIVATION_WORK -> narrative(
            headline = "Mientras tanto, |el trabajo no espera| ⏳",
            body = "Ofertas que piden carnet, turnos a los que no llegas en transporte. " +
                "|Cada semana sin carnet cuenta|.",
            emoji = "💼",
            animation = R.raw.door_open
        )
        else -> narrative(
            headline = "Mientras tanto, |pierdes tu tiempo| ⏳",
            body = "Cada minuto esperando en la parada, con frío o con lluvia, es " +
                "|tiempo de tu vida que no vuelve|.",
            emoji = "⏳",
            animation = R.raw.waiting
        )
    }

    /** What failing costs: a second fee for someone who already failed, a delay for everyone else. */
    private fun lossOpportunity(experience: String?): Narrative = when (experience) {
        OnboardingConfig.EXPERIENCE_RETRY -> narrative(
            headline = "Y otro suspenso |sale caro| 💸",
            body = "Si suspendes dos veces, toca volver a pagar la tasa de la DGT y esperar otra " +
                "fecha. |Mejor que esta sea la buena|.",
            emoji = "💸",
            animation = R.raw.door_open
        )
        else -> narrative(
            headline = "Y cada suspenso |lo retrasa todo| 🚪",
            body = "Otra fecha que esperar, más tiempo sin carnet y, al segundo suspenso, " +
                "|volver a pagar la tasa de la DGT|.",
            emoji = "🚪",
            animation = R.raw.door_open
        )
    }

    /** The worst case the user named: their own fear about exam day, made concrete. */
    private fun lossAutonomy(concern: String?): Narrative = when (concern) {
        OnboardingConfig.CONCERN_NOT_READY -> narrative(
            headline = "Y lo peor: |presentarte a ciegas| 😰",
            body = "Llegar al examen sin saber si estás a punto: " +
                "|esa duda pesa más que cualquier pregunta|.",
            emoji = "😰",
            animation = R.raw.notebook
        )
        OnboardingConfig.CONCERN_EXAM_MISMATCH -> narrative(
            headline = "Y lo peor: |que te pille por sorpresa| 😰",
            body = "Estudiar semanas y encontrarte preguntas " +
                "|redactadas como nunca las habías visto|.",
            emoji = "😰",
            animation = R.raw.notebook
        )
        OnboardingConfig.CONCERN_SILLY_MISTAKES -> narrative(
            headline = "Y lo peor: |caer en una trampa| 😰",
            body = "Saberte la norma y fallar por un \"siempre\", un \"nunca\" o una palabra " +
                "que |no leíste con calma|.",
            emoji = "😰",
            animation = R.raw.notebook
        )
        else -> narrative(
            headline = "Pero sobre todo, |pierdes autonomía| ⛓️",
            body = "Sin carnet, |tu vida la deciden otros|: los horarios del transporte y los favores ajenos.",
            emoji = "⛓️",
            animation = R.raw.bus
        )
    }

    /** The life the licence unlocks, for the reason the user gave. */
    private fun gainFreedom(motivation: String?): Narrative = when (motivation) {
        OnboardingConfig.MOTIVATION_WORK -> narrative(
            headline = "|Abre puertas| en el trabajo 💼",
            body = "Di que sí a ese trabajo, a esos turnos o a esa entrevista lejos: " +
                "|el carnet deja de ser un freno|.",
            emoji = "💼",
            animation = R.raw.car
        )
        OnboardingConfig.MOTIVATION_FREEDOM -> narrative(
            headline = "|Muévete con libertad| 🌍",
            body = "Esa escapada, ese viaje con amigos, esa playa lejos: " +
                "|todo pasa a ser un plan real|.",
            emoji = "🌍",
            animation = R.raw.experiences
        )
        else -> narrative(
            headline = "|Recupera tu independencia| 🕊️",
            body = "Sal cuando quieras y vuelve cuando quieras, |sin depender de nadie|.",
            emoji = "🕊️",
            animation = R.raw.freedom
        )
    }

    /** Exam day the way the user wishes it went: the opposite of the fear they picked. */
    private fun gainExperiences(concern: String?): Narrative = when (concern) {
        OnboardingConfig.CONCERN_NOT_READY -> narrative(
            headline = "Imagina llegar |con todo controlado| ✅",
            body = "Con tus simulacros aprobados antes del día: |sabrás que estás a punto| antes de entrar.",
            emoji = "✅",
            animation = R.raw.tips
        )
        OnboardingConfig.CONCERN_EXAM_MISMATCH -> narrative(
            headline = "Que el examen |no te pille por sorpresa| 🎯",
            body = "Habrás hecho simulacros como el real: " +
                "|30 preguntas, 30 minutos y máximo 3 fallos|.",
            emoji = "🎯",
            animation = R.raw.tips
        )
        OnboardingConfig.CONCERN_SILLY_MISTAKES -> narrative(
            headline = "|Ver la trampa| a la primera 🔍",
            body = "Leer cada pregunta con calma y |detectar el detalle que te quería pillar|.",
            emoji = "🔍",
            animation = R.raw.tips
        )
        else -> narrative(
            headline = "|Vive experiencias| que hoy dejas pasar 🏖️",
            body = "Esa escapada, ese viaje con amigos, esa playa lejos. Con el carnet, " +
                "|todo eso pasa a ser un plan real|.",
            emoji = "🏖️",
            animation = R.raw.experiences
        )
    }

    /** The step after the theory exam, for a first attempt or a second chance. */
    private fun gainLevelUp(experience: String?): Narrative = when (experience) {
        OnboardingConfig.EXPERIENCE_RETRY -> narrative(
            headline = "Esta vez sales |con el aprobado| 🎉",
            body = "Y el siguiente paso ya es el práctico: |el carnet, cada vez más cerca|.",
            emoji = "🎉",
            animation = R.raw.level_up
        )
        else -> narrative(
            headline = "|A la primera| y sin repetir 🚀",
            body = "Aprobar el teórico a la primera y pasar al práctico |sin perder ni una semana|.",
            emoji = "🚀",
            animation = R.raw.level_up
        )
    }

    /**
     * Builds a [Narrative].
     *
     * @param headline the mascot line
     * @param body the line under the visual
     * @param emoji fallback visual
     * @param animation Lottie shown instead of [emoji]
     * @return the screen copy
     */
    private fun narrative(headline: String, body: String, emoji: String, animation: Int) =
        Narrative(headline, NarrativeContent(emoji = emoji, body = body, animation = animation))
}
