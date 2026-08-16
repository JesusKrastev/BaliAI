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
        OnboardingStep.DifficultTopics -> data.difficultTopics.isNotEmpty()
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
     * @return the message to show in the speech bubble
     */
    fun updateMascotMessage(step: OnboardingStep, data: OnboardingData): String {
        val name = data.name ?: ""
        return when (step) {
            // Diagnóstico
            OnboardingStep.Name -> "¡Hola! Soy |Bali| 👋 Tu |copiloto| para aprobar. ¿Cómo te llamas?"
            OnboardingStep.License -> "|$name|, ¿qué carnet quieres sacarte?"
            OnboardingStep.Experience -> "¿Es tu |primer intento| con el carnet? Así clavo el plan 🎯"
            OnboardingStep.DialogueExperience -> getExperienceReaction(data.experience)
            OnboardingStep.TheoryBlocker -> "¿Qué es lo que más se te complica |del teórico|? 🎯"

            // El porqué
            OnboardingStep.Motivation -> "Dime una cosa, |$name|: ¿para qué quieres el carnet? 🎯"
            OnboardingStep.FutureImpact -> "Imagina que |ya lo tienes|. ¿Cuánto cambiaría tu día a día?"
            OnboardingStep.Empathy -> "|Te entiendo|, $name. Y no eres el único 🫂"

            // Lo que cuesta no tenerlo
            OnboardingStep.LossTime -> "Mientras tanto, |pierdes tu tiempo| ⏳"
            OnboardingStep.LossOpportunity -> "Y también |pierdes oportunidades| 🚪"
            OnboardingStep.LossAutonomy -> "Pero sobre todo, |pierdes autonomía| ⛓️"

            // La solución: StepMethodComparison oculta la mascota y pone su propio título.
            OnboardingStep.MethodComparison -> ""

            // Lo que ganas
            OnboardingStep.GainFreedom -> "|Recupera tu libertad| 🕊️"
            OnboardingStep.GainExperiences -> "|Vive experiencias| que hoy dejas pasar 🏖️"
            OnboardingStep.GainLevelUp -> "|Sube de nivel| tu vida 📈"

            // El plan
            OnboardingStep.ExamDate -> "¿Cuándo es el examen? Sin fecha no hay |plan de ataque| 📅"
            OnboardingStep.DifficultTopics -> "¿Qué temas se te atragantan? Los |atacamos primero| 💪"
            OnboardingStep.DialogueDifficultTopics -> "Listo. Estos temas |no te van a coger de sorpresa| 🎯"
            OnboardingStep.DailyGoal -> "¿Cuánto tiempo al día? Mejor poco y constante que mucho y de vez en cuando ⚡"
            OnboardingStep.LearningPreference -> "¿Cómo prefieres practicar? Tu |método| manda 🧠"

            // Cierre
            // StepProcessing oculta la mascota y pone su propio título.
            OnboardingStep.Processing -> ""
            // StepComparison oculta la mascota y pone su propio título.
            OnboardingStep.Comparison -> ""
            OnboardingStep.PlanReveal -> "|$name|, este plan es |solo tuyo|. No lo desperdicies ⚡"
            OnboardingStep.SocialProof -> "|1.000 personas| ya lo consiguieron. |Tú eres el siguiente| 💯"
            OnboardingStep.Pact -> "Último paso, |$name|. Hagamos |un trato| ✊"

            else -> ""
        }
    }

    /**
     * Reacts to the user's starting point, mirroring their answer back at them.
     * For someone who already sat the exam the reaction primes the failure statistic
     * that the rest of the flow builds on.
     *
     * @param experience the selected experience label, or null if unanswered
     * @return the reaction line for the dialogue screen
     */
    private fun getExperienceReaction(experience: String?): String = when (experience) {
        OnboardingConfig.EXPERIENCE_FIRST_TIME ->
            "Perfecto. Ir |a la primera| es tu ventaja: lo hacemos bien desde el día uno 🚀"
        OnboardingConfig.EXPERIENCE_RETRY ->
            "Ya sabes lo que se siente. El |58% suspende a la primera|. Contigo va a ser distinto 💪"
        else -> ""
    }
}
