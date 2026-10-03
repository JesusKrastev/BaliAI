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
 * Copy of the two screens left of the emotional arc: the pain and the gain.
 *
 * The arc used to be eight screens (empathy, three losses, the method, three gains) and PostHog
 * showed only the first two were read: from the third on, most people passed each screen in one or
 * two seconds. What those two said survives here as one screen early in the flow, right after the
 * user names what blocks them; the gain closes the diagnosis, just before the plan is built.
 *
 * Each animation shows what its line says: the clock for waiting on others or on a job that will
 * not wait, the bus for the stop in the rain; someone driving for independence, the door opening for
 * work, the beach for getting away. Pain and gain never share a picture for the same answer. No line carries a figure that cannot be sourced.
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
        OnboardingStep.Pain -> pain(data)
        OnboardingStep.Gain -> gain(data.motivation)
        else -> null
    }

    /**
     * Names what blocks the user in the headline and what not having the licence costs them in
     * the body, in the terms of why they want it.
     */
    private fun pain(data: OnboardingData): Narrative {
        val headline = when (data.theoryBlocker) {
            OnboardingConfig.BLOCKER_NO_START -> "Normal no saber |por dónde empezar| 💛"
            OnboardingConfig.BLOCKER_NO_PROGRESS -> "Estudiar sin ver avance |desanima a cualquiera| 💛"
            OnboardingConfig.BLOCKER_NO_METHOD -> "Hacer tests a lo loco |no es un método| 💛"
            else -> "|Te entiendo|. No te pasa solo a ti 💛"
        }
        return when (data.motivation) {
            OnboardingConfig.MOTIVATION_INDEPENDENCE -> narrative(
                headline = headline,
                body = "Y mientras tanto, |dependes de otros|: pedir que te lleven, cuadrar horarios, " +
                    "esperar a que alguien pueda.",
                emoji = "⏳",
                animation = R.raw.waiting
            )
            OnboardingConfig.MOTIVATION_WORK -> narrative(
                headline = headline,
                body = "Y mientras tanto, |el trabajo no espera|: ofertas que piden carnet y turnos " +
                    "a los que no llegas en transporte.",
                emoji = "⏳",
                animation = R.raw.waiting
            )
            else -> narrative(
                headline = headline,
                body = "Y mientras tanto, cada minuto esperando en la parada, con frío o con lluvia, " +
                    "es |tiempo que no vuelve|.",
                emoji = "🚌",
                animation = R.raw.bus
            )
        }
    }

    /** The life the licence unlocks, for the reason the user gave. */
    private fun gain(motivation: String?): Narrative = when (motivation) {
        OnboardingConfig.MOTIVATION_WORK -> narrative(
            headline = "|Abre puertas| en el trabajo 💼",
            body = "Di que sí a ese trabajo, a esos turnos o a esa entrevista lejos: " +
                "|el carnet deja de ser un freno|.",
            emoji = "💼",
            animation = R.raw.door_open
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
            emoji = "🚗",
            animation = R.raw.freedom
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
