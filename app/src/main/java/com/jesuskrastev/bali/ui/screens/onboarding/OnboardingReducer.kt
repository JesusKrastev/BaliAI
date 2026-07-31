package com.jesuskrastev.bali.ui.screens.onboarding

/**
 * OnboardingReducer handles the business logic for the onboarding flow.
 * This keeps the ViewModel focused on orchestration while business logic is testable and reusable.
 */
class OnboardingReducer {

    fun calculateProgress(index: Int, totalSteps: Int): Float = index.toFloat() / (totalSteps - 1)

    fun shouldEnableNextButton(step: OnboardingStep, data: OnboardingData): Boolean = when (step) {
        OnboardingStep.Name -> {
            val name = data.name ?: ""
            name.isNotBlank() && name.all { it.isLetter() || it.isWhitespace() }
        }
        OnboardingStep.DifficultTopics -> data.difficultTopics.isNotEmpty()
        is OnboardingStep.DialogueExperience,
        is OnboardingStep.DialogueDifficultTopics,
        OnboardingStep.MethodComparison,
        OnboardingStep.Notifications,
        OnboardingStep.SocialProof,
        OnboardingStep.Comparison,
        OnboardingStep.LossAversion -> true
        else -> false
    }

    fun updateMascotMessage(step: OnboardingStep, data: OnboardingData): String {
        return when (step) {
            OnboardingStep.Name -> "¡Hola! Soy |Bali| 👋 Tu |copiloto| para aprobar. ¿Cómo te llamas?"
            OnboardingStep.License -> "|${data.name ?: ""}|, ¿qué carnet vas a por ello?"
            OnboardingStep.Experience -> "¿De dónde partimos? Así |clavo el plan| 🎯"
            OnboardingStep.DialogueExperience -> getExperienceReaction(data.experience)
            OnboardingStep.TheoryBlocker -> "¿Qué es lo que más se te complica |del teórico|? 🎯"
            OnboardingStep.ExamDate -> "¿Cuándo es el examen? Sin fecha no hay |plan de ataque| 📅"
            OnboardingStep.MethodComparison -> "Con |Bali| aprendes |toda| la teórica. Sin nosotros |olvidas la mitad| 🤯"
            OnboardingStep.DailyGoal -> "¿Cuánto tiempo al día? Poco y constante |bate| a mucho y esporádico ⚡"
            OnboardingStep.LearningPreference -> "¿Cómo aprendes mejor? Tu |método favorito| manda 🧠"
            OnboardingStep.DifficultTopics -> "¿Qué temas se te atragantan? Los |atacamos primero| 💪"
            OnboardingStep.DialogueDifficultTopics -> "Listo. Estos temas |no te van a coger de sorpresa| 🎯"
            OnboardingStep.Concern -> "¿Qué es lo que más te preocupa de cara al examen? Lo |dejamos resuelto| 😤"
            OnboardingStep.StudyTime -> "¿Cuándo tienes la mente más fresca? Ponemos las clases |difíciles ahí| 🌟"
            OnboardingStep.Notifications -> "¿Te aviso para que |no pierdas el ritmo|? Solo 1 push al día 🔔"
            OnboardingStep.SocialProof -> "|9.000 personas| ya aprobaron. |Tú eres el siguiente| 💯"
            OnboardingStep.Processing -> "Analizando tu perfil... tu |plan personalizado| ya viene 🚀"
            OnboardingStep.Comparison -> "Los datos de tu perfil confirman que con |Bali| tu éxito es |cuestión de semanas| 🚀"
            OnboardingStep.LossAversion -> "|${data.name ?: ""}|, esto es |solo tuyo|. No lo desperdicies ⚡"
            OnboardingStep.Pact -> "Último paso, |${data.name ?: ""}|. Hagamos |un trato| ✊"
            else -> ""
        }
    }

    private fun getExperienceReaction(experience: String?): String = when (experience) {
        "\uD83C\uDF93 Empiezo de cero absoluto" -> "Perfecto. |Empezar de cero| es tu ventaja: sin malos hábitos 🚀"
        "\uD83D\uDCD6 Ya tengo algunas nociones básicas" -> "Genial, |saltamos| directamente a lo que importa ⚡"
        "\uD83D\uDD01 He suspendido y quiero repetirlo" -> "Esta vez |es diferente|. Ya sabes dónde están las trampas 💪"
        "\uD83E\uDEA7 Ya tengo otro carnet" -> "Con experiencia tienes |la mitad ganada|. A rematar 😎"
        else -> ""
    }
}
