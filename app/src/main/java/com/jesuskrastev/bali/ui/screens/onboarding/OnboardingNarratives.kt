package com.jesuskrastev.bali.ui.screens.onboarding

import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.onboarding.steps.NarrativeScene
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
 * The pain comes once the user has named what blocks them, how far along they are and what worries
 * them, right before the mini-test; the gain comes once the plan's answers are in, so it can speak
 * to them by name on the day their plan aims at (2026-10-03: both used to come too early, the pain
 * after two answers).
 *
 * The arc used to be eight screens (empathy, three losses, the method, three gains) and PostHog
 * showed only the first two were read: from the third on, most people passed each screen in one or
 * two seconds. What those two said survives here as one screen early in the flow, right after the
 * user names what blocks them; the gain closes the diagnosis, just before the plan is built.
 *
 * Each animation shows what its line says: job offers stamped "sin carnet" for a job that will not
 * wait and the same offer stamped "carnet B ✓" for the gain (hand-drawn, see `NarrativeScene`), the
 * clock for waiting on others, the bus for the stop in the rain; someone driving for independence, the door opening for
 * independence, the beach for getting away. Pain and gain never share a picture for the same answer. No line carries a figure that cannot be sourced.
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
        OnboardingStep.Gain -> gain(data)
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
                emoji = "💼",
                animation = R.raw.waiting,
                scene = NarrativeScene.JobOffersLost
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

    /**
     * The life the licence unlocks, for the reason the user gave, told to them by name on the day
     * their plan aims at. It comes once the plan's answers are in, so the date is theirs.
     */
    private fun gain(data: OnboardingData, now: Long = System.currentTimeMillis()): Narrative {
        val day = SPANISH_DAY.format(Date(OnboardingConfig.planTargetMillis(data.examDate, data.weeklyStudy, now)))
        val name = data.name?.trim()?.takeIf { it.isNotEmpty() }
        val headline = if (name != null) "$name, imagina el |$day|" else "Imagina el |$day|"
        return when (data.motivation) {
            OnboardingConfig.MOTIVATION_WORK -> narrative(
                headline = "$headline 💼",
                body = "Carnet en la mano. Esa oferta, esos turnos, esa entrevista lejos: " +
                    "|esta vez dices que sí|.",
                emoji = "💼",
                animation = R.raw.door_open,
                scene = NarrativeScene.JobOfferWon
            )
            OnboardingConfig.MOTIVATION_FREEDOM -> narrative(
                headline = "$headline 🌍",
                body = "Carnet en la mano. Esa escapada, ese viaje con amigos, esa playa lejos: " +
                    "|ya no es un quizá, es un plan|.",
                emoji = "🌍",
                animation = R.raw.experiences
            )
            else -> narrative(
                headline = "$headline 🕊️",
                body = "Carnet en la mano. Sales cuando quieres y vuelves cuando quieres, " +
                    "|sin pedirle nada a nadie|.",
                emoji = "🚗",
                animation = R.raw.freedom
            )
        }
    }

    private val SPANISH_DAY = SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))

    /**
     * Builds a [Narrative].
     *
     * @param headline the mascot line
     * @param body the line under the visual
     * @param emoji fallback visual
     * @param animation Lottie shown instead of [emoji]
     * @return the screen copy
     */
    private fun narrative(
        headline: String,
        body: String,
        emoji: String,
        animation: Int,
        scene: NarrativeScene? = null
    ) = Narrative(headline, NarrativeContent(emoji = emoji, body = body, animation = animation, scene = scene))
}
