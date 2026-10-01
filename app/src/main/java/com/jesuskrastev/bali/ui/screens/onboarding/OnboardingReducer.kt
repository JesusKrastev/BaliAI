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
     * @return the message to show in the speech bubble
     */
    fun updateMascotMessage(step: OnboardingStep, data: OnboardingData): String {
        val name = data.name ?: ""
        return when (step) {
            // El porqué
            OnboardingStep.Motivation -> "Dime una cosa: ¿para qué quieres el carnet? 🎯"
            OnboardingStep.TheoryBlocker -> "¿Qué es lo que más se te complica |del teórico|? 🎯"

            // Diagnóstico
            OnboardingStep.Concern -> "¿Qué es lo que más te |preocupa| de cara al examen? 😰"
            OnboardingStep.Experience -> "¿Es tu |primer intento| con el carnet? Así clavo el plan 🎯"
            OnboardingStep.Readiness -> "¿Cómo te ves ahora mismo para |el teórico|? 💪"
            OnboardingStep.FutureImpact -> "Imagina que |ya lo tienes|. ¿Cuánto cambiaría tu día a día?"
            OnboardingStep.Empathy -> "|Te entiendo|. Y no eres el único 🫂"

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
            OnboardingStep.Name -> "Vamos a montar tu plan. Antes, ¿|cómo te llamas|? 👋"
            OnboardingStep.ExamDate -> "¿Cuándo es tu examen? Sin fecha no hay |plan de ataque| 📅"
            OnboardingStep.Province -> "¿En qué |provincia| te examinas? 📍"
            OnboardingStep.ProvinceConfirmed -> provinceConfirmationHeadline(data.province)
            OnboardingStep.WeeklyStudy -> "¿Cuánto quieres estudiar |a la semana|? Mejor poco y constante ⚡"
            OnboardingStep.StudyTime -> "¿A qué hora te viene mejor |estudiar|? ⏰"
            OnboardingStep.Notifications -> notificationsHeadline(data)
            OnboardingStep.LearningPreference -> "¿Cómo prefieres practicar? Tu |método| manda 🧠"

            // Cierre
            // StepProcessing oculta la mascota y pone su propio título.
            OnboardingStep.Processing -> ""
            // StepComparison oculta la mascota y pone su propio título.
            OnboardingStep.Comparison -> ""
            // StepPlanReveal oculta la mascota y pone su propio encabezado.
            OnboardingStep.PlanReveal -> ""
            OnboardingStep.SocialProof -> "|1.000 personas| ya lo consiguieron. |Tú eres el siguiente| 💯"
            OnboardingStep.Pact -> "Último paso, |$name|. Hagamos |un trato| ✊"

            else -> ""
        }
    }

    /**
     * Headline of the screen that pays off the province question by promising the
     * question bank of the office the user will actually sit the exam in.
     *
     * @param province the province the user selected, or null if it was skipped
     * @return the message to show in the speech bubble
     */
    private fun provinceConfirmationHeadline(province: String?): String = province
        ?.let { "Tenemos el temario que se usa en |$it| 📋" }
        ?: "Tenemos el |temario oficial| de la DGT 📋"

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
