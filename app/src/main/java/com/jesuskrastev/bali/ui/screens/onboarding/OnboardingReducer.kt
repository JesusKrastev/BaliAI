package com.jesuskrastev.bali.ui.screens.onboarding

/**
 * OnboardingReducer handles the business logic for the onboarding flow.
 * This keeps the ViewModel focused on orchestration while business logic is testable and reusable.
 */
class OnboardingReducer {

    /**
     * Maps a step position to the value of the top progress bar.
     *
     * @param index zero-based position of the current step
     * @param totalSteps number of steps in the flow
     * @return progress between 0f and 1f
     */
    fun calculateProgress(index: Int, totalSteps: Int): Float = index.toFloat() / (totalSteps - 1)

    /**
     * Picks the step that "back" leads to.
     *
     * The "building your plan" screen is skipped on the way back: it advances on its own and
     * shows no controls, so landing on it again would strand the user there. For the same
     * reason going back is refused while it is on screen. Steps left out of this run (the test
     * result when the test was skipped) are jumped over as well.
     *
     * @param order the steps of the flow, in the order they are shown
     * @param current the step currently on screen
     * @param isSkipped steps left out of this run
     * @return the step to show, or null when the user cannot go back from [current]: it is
     *   the first one, the plan is being built, or it is not part of [order] at all
     */
    fun previousStep(
        order: List<OnboardingStep>,
        current: OnboardingStep,
        isSkipped: (OnboardingStep) -> Boolean = { false }
    ): OnboardingStep? {
        val index = order.indexOf(current)
        if (index <= 0 || current == OnboardingStep.Processing) return null
        return order.subList(0, index)
            .lastOrNull { it != OnboardingStep.Processing && !isSkipped(it) }
    }

    /**
     * Decides whether the bottom "continue" button is tappable.
     * Informational steps have nothing to answer, so they are always enabled.
     *
     * @param step the step currently on screen
     * @param data the answers collected so far
     * @return true when the user may move forward
     */
    fun shouldEnableNextButton(step: OnboardingStep, data: OnboardingData): Boolean = when (step) {
        OnboardingStep.Name -> {
            val name = data.name ?: ""
            name.isNotBlank() && name.all { it.isLetter() || it.isWhitespace() }
        }
        OnboardingStep.Province -> data.province != null
        is OnboardingStep.Informational -> true
        else -> false
    }

    /**
     * Builds the mascot line for a step. On informational screens this line *is* the
     * headline of the narrative, so the screen body never repeats it.
     * Text wrapped in pipes (`|like this|`) is rendered highlighted.
     *
     * @param step the step currently on screen
     * @param data the answers collected so far, used to personalise the copy
     * @param quizIndex the mini-test question on screen, counted from 0
     * @return the message to show in the speech bubble
     */
    fun updateMascotMessage(step: OnboardingStep, data: OnboardingData, quizIndex: Int = 0): String {
        OnboardingNarratives.forStep(step, data)?.let { return it.headline }

        val name = data.name ?: ""
        return when (step) {
            // StepIntro oculta la mascota: las capturas son la imagen.
            OnboardingStep.Intro -> ""

            // El porqué
            OnboardingStep.Motivation -> "Dime una cosa: ¿para qué quieres el carnet? 🎯"
            OnboardingStep.TheoryBlocker -> "¿Qué es lo que más se te complica |del teórico|? 🎯"

            // Diagnóstico
            OnboardingStep.Experience -> "¿Es tu |primer intento| con el carnet? Así clavo el plan 🎯"
            // StepComparison oculta la mascota y pone su propio título.
            OnboardingStep.Comparison -> ""
            OnboardingStep.Readiness -> "¿Cómo te ves ahora mismo para |el teórico|? 💪"
            OnboardingStep.Concern -> "¿Qué es lo que más te |preocupa| de cara al examen? 😰"

            // La prueba
            OnboardingStep.Quiz -> quizHeadline(quizIndex)
            OnboardingStep.QuizResult -> quizResultHeadline(data)

            // La solución: StepMethodComparison oculta la mascota y pone su propio título.
            OnboardingStep.MethodComparison -> ""

            // El plan
            OnboardingStep.Name -> "Vamos a montar tu plan. Antes, ¿|cómo te llamas|? 👋"
            OnboardingStep.ExamDate ->
                "¿Tienes ya fecha para |el teórico|? Si no, sin problema: te diré cuándo es buen momento para pedirla 📅"
            OnboardingStep.Province -> "¿En qué |provincia| te examinas? 📍"
            OnboardingStep.WeeklyStudy -> "¿Cuánto quieres estudiar |a la semana|? Mejor poco y constante ⚡"
            OnboardingStep.StudyTime -> "¿A qué hora te viene mejor |estudiar|? ⏰"
            OnboardingStep.Notifications -> notificationsHeadline(data)
            OnboardingStep.LearningPreference -> "¿Cómo te gusta |practicar|? 🧠"

            // Cierre
            // StepProcessing oculta la mascota y pone su propio título.
            OnboardingStep.Processing -> ""
            // StepPlanReveal oculta la mascota y pone su propio encabezado.
            OnboardingStep.PlanReveal -> ""
            OnboardingStep.SocialProof -> "|1.000 personas| ya lo consiguieron. |Ahora te toca a ti| 💯"
            OnboardingStep.Pact -> "Último paso, |$name|. Hagamos |un trato| ✊"

            else -> ""
        }
    }

    /**
     * The mascot line over each mini-test question.
     *
     * @param quizIndex the question on screen, counted from 0
     * @return the line
     */
    private fun quizHeadline(quizIndex: Int): String = when (quizIndex) {
        0 -> "Vale, vamos a probar justo eso. |${OnboardingQuiz.QUESTION_COUNT} preguntas|, sin presión 👀"
        OnboardingQuiz.QUESTION_COUNT - 1 -> "|La última|. Léela con calma 🤔"
        else -> "Vamos con |la siguiente| 👇"
    }

    /**
     * The mascot line over the mini-test result, in the tone of the score.
     *
     * @param data the answers collected so far, including the test
     * @return the line
     */
    private fun quizResultHeadline(data: OnboardingData): String {
        val total = data.quizAnswers.size
        return when (data.quizScore()) {
            total -> "¡|Lo has clavado|! 🎯"
            total - 1 -> "¡|Nada mal|! Vamos a pulir el resto 👏"
            else -> "Tranquilidad: |para eso estamos| 🙂"
        }
    }

    /**
     * Headline of the reminders offer, naming the hour picked on the previous screen so the
     * question is about that reminder, not about notifications in general.
     *
     * @param data the answers collected so far
     * @return the message to show in the speech bubble
     */
    private fun notificationsHeadline(data: OnboardingData): String = data.studySlot()
        ?.let { "¿Te aviso a las |${OnboardingConfig.reminderTimeLabel(it)}| para que no se te pase? 🔔" }
        ?: "¿Te aviso para que |no se te pase|? 🔔"
}
