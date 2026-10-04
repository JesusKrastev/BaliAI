package com.jesuskrastev.bali.ui.screens.onboarding

import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.ui.screens.onboarding.steps.NarrativeScene
import com.jesuskrastev.bali.ui.screens.onboarding.steps.SolvedRow

/**
 * What one screen of the emotional block says: the mascot's headline and the visual with its line.
 *
 * @property headline the mascot bubble; `|` pairs mark the words to highlight
 * @property content the visual and the body line under it
 */
data class Narrative(val headline: String, val content: NarrativeContent)

/**
 * Copy of the three screens that hand the user's own answers back to them: their problem, what it
 * risks on exam day, and Bali as the answer to both.
 *
 * They come together, right after the mini-test result, because by then the user has said what
 * blocks them, whether it is their first attempt, what worries them, and has just tested that worry:
 * everything a screen needs to talk about *their* problem rather than a generic one. This follows the
 * empathy framework Headway presented on RevenueCat's blog (identify the feeling, acknowledge it with
 * a screen built from the answers, show the app as the answer to it, then the paywall).
 *
 * The headline mirrors the answer in the user's own words, the body says in one line what that feels
 * like, and the scene shows it, so someone skimming at two seconds a screen (PostHog, 13 sep–4 oct:
 * the old arc's screens got 1.1–3.9 s each) still gets the idea from the picture. The licence's "why"
 * only closes the last screen: the problem is how they study, not who drives them around. No line
 * carries a figure that cannot be sourced.
 */
object OnboardingNarratives {

    /**
     * Picks the copy of an emotional-block screen for the answers collected so far.
     *
     * @param step the step on screen
     * @param data the answers collected so far
     * @return the screen's copy, or null when [step] is not part of the block
     */
    fun forStep(step: OnboardingStep, data: OnboardingData): Narrative? = when (step) {
        OnboardingStep.Problem -> problem(data)
        OnboardingStep.Risk -> risk(data)
        OnboardingStep.Solution -> solution(data)
        else -> null
    }

    /** What blocks the user, in their words, and a second attempt named when it is one. */
    private fun problem(data: OnboardingData): Narrative {
        val retry = data.experience == OnboardingConfig.EXPERIENCE_RETRY
        return when (data.theoryBlocker) {
            OnboardingConfig.BLOCKER_NO_PROGRESS -> narrative(
                headline = if (retry) "Ya suspendiste una vez y |estudias sin ver avance| 💛"
                else "Me has dicho que |estudias, pero no ves avance| 💛",
                body = "Test tras test, se te escapan las mismas preguntas. |Mucho esfuerzo, poco avance|.",
                emoji = "📉",
                scene = NarrativeScene.SameMistake
            )
            OnboardingConfig.BLOCKER_NO_METHOD -> narrative(
                headline = if (retry) "Ya suspendiste una vez y |te falta un método| 💛"
                else "Me has dicho que |te falta un método claro| 💛",
                body = "Un test hoy, nada en tres días, otro al azar. Sin orden, |lo de hoy se olvida mañana|.",
                emoji = "🧭",
                scene = NarrativeScene.ScatteredWeek
            )
            else -> narrative(
                headline = if (retry) "Ya suspendiste una vez y |no sabes por dónde retomarlo| 💛"
                else "Me has dicho que |no sabes por dónde empezar| 💛",
                body = "Abres el temario y todo parece igual de urgente. Así, lo normal es |dejarlo para mañana|.",
                emoji = "😵‍💫",
                scene = NarrativeScene.TopicPile
            )
        }
    }

    /** What the user fears about exam day, with what the mini-test just showed when it applies. */
    private fun risk(data: OnboardingData): Narrative {
        val answered = data.quizAnswers.size
        val missed = answered - data.quizScore()
        return when (data.concern) {
            OnboardingConfig.CONCERN_EXAM_MISMATCH -> narrative(
                headline = "Y te preocupa que el examen |no se parezca a lo que estudias| 🤨",
                body = "Te aprendes unas preguntas y el día del examen salen otras, |con otra trampa|.",
                emoji = "🧩",
                scene = NarrativeScene.Mismatch
            )
            OnboardingConfig.CONCERN_SILLY_MISTAKES -> narrative(
                headline = "Y te da miedo |fallar por detalles tontos| 🤦",
                body = if (missed > 0) "Un «no», un «salvo», y la respuesta cambia. Hoy has picado en |$missed de $answered|."
                else "Un «no», un «salvo», y la respuesta cambia. |Basta un despiste para suspender|.",
                emoji = "🔍",
                scene = NarrativeScene.TrapWord
            )
            else -> narrative(
                headline = "Y lo que más te preocupa es |llegar al examen sin tenerlo dominado| 😨",
                body = if (answered > 0) "Hoy has acertado |${data.quizScore()} de $answered|. Sin saber si lo dominas, ir al examen es |jugártela|."
                else "Sin saber si lo dominas, ir al examen es |jugártela|.",
                emoji = "🪙",
                scene = NarrativeScene.CoinFlip
            )
        }
    }

    /**
     * Bali against each problem the user named, in the same words they were named in, then the
     * licence's "why" as the payoff. Every fix is something the app does today.
     */
    private fun solution(data: OnboardingData): Narrative {
        val rows = buildList {
            add(
                when (data.theoryBlocker) {
                    OnboardingConfig.BLOCKER_NO_PROGRESS -> SolvedRow("Estudio y no avanzo", "Ves cuánto te falta para aprobar")
                    OnboardingConfig.BLOCKER_NO_METHOD -> SolvedRow("Sin un método claro", "Un plan semana a semana")
                    else -> SolvedRow("No sé por dónde empezar", "Un camino ordenado, tema a tema")
                }
            )
            add(
                when (data.concern) {
                    OnboardingConfig.CONCERN_EXAM_MISMATCH ->
                        SolvedRow("Que el examen me pille por sorpresa", "Simulacros de ${ExamRules.QUESTION_COUNT} preguntas, como el real")
                    OnboardingConfig.CONCERN_SILLY_MISTAKES -> SolvedRow("Fallar por detalles tontos", "La IA te explica cada fallo")
                    else -> SolvedRow("Llegar sin tenerlo dominado", "Simulacros que te dicen si aprobarías")
                }
            )
            val failedTopics = data.failedQuizQuestions().map { it.topic }.distinct()
            if (failedTopics.isNotEmpty()) {
                add(SolvedRow("Fallaste: ${failedTopics.joinToString(" y ")}", "Lo reforzamos en tu plan"))
            }
        }
        val opener = if (data.experience == OnboardingConfig.EXPERIENCE_RETRY) "Y esta vez, cuando apruebes" else "Y cuando apruebes"
        val payoff = when (data.motivation) {
            OnboardingConfig.MOTIVATION_INDEPENDENCE -> "|no dependes de nadie| para moverte"
            OnboardingConfig.MOTIVATION_WORK -> "|el carnet deja de cerrarte puertas|"
            OnboardingConfig.MOTIVATION_FREEDOM -> "|vas donde quieras, cuando quieras|"
            else -> "|el carnet es tuyo|"
        }
        return narrative(
            headline = "Así le vamos a |dar la vuelta| 💪",
            body = "$opener, $payoff.",
            emoji = "✅",
            scene = NarrativeScene.Solved(rows)
        )
    }

    /**
     * Builds a [Narrative].
     *
     * @param headline the mascot line
     * @param body the line under the visual
     * @param emoji fallback visual
     * @param scene the hand-drawn scene shown above [body]
     * @return the screen copy
     */
    private fun narrative(headline: String, body: String, emoji: String, scene: NarrativeScene) =
        Narrative(headline, NarrativeContent(emoji = emoji, body = body, scene = scene))
}
