package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme

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
        OnboardingStep.Reasons -> data.reasons.isNotEmpty()
        OnboardingStep.DifficultTopics -> data.difficultTopics.isNotEmpty()
        is OnboardingStep.DialogueExperience,
        is OnboardingStep.DialogueDifficultTopics,
        OnboardingStep.Notifications,
        OnboardingStep.Comparison -> true
        else -> false
    }

    fun updateMascotMessage(step: OnboardingStep, data: OnboardingData): String {
        return when (step) {
            OnboardingStep.Name -> "¿Cómo te llamas?"
            OnboardingStep.License -> "Genial, ${data.name ?: ""}. ¿Qué carnet quieres sacarte?"
            OnboardingStep.Experience -> "¿En qué punto estás ahora mismo?"
            OnboardingStep.DialogueExperience -> getExperienceReaction(data.experience)
            OnboardingStep.Reasons -> getReasonReaction(data.reasons)
            OnboardingStep.ExamDate -> "¿Ya tienes fecha de examen?"
            OnboardingStep.DailyGoal -> "¿Cuánto tiempo puedes dedicarme al día?"
            OnboardingStep.LearningPreference -> "¿Cómo prefieres aprender?"
            OnboardingStep.DifficultTopics -> "¿Qué temas se te atragantan más?"
            OnboardingStep.DialogueDifficultTopics -> "Entendido. Vamos a machacarlo juntos 💪"
            OnboardingStep.Concern -> "¿Qué es lo que más miedo te da?"
            OnboardingStep.StudyTime -> "¿Cuándo te cunde más estudiar?"
            OnboardingStep.Notifications -> "Activa las notis. Yo cuido tu racha 🔥"
            OnboardingStep.Processing -> "Analizando tus datos... ¡Esto promete! 🤖"
            OnboardingStep.Comparison -> "Mira cómo vas a estudiar conmigo 👇"
            OnboardingStep.Pact -> "Casi listo, ${data.name ?: ""}. Solo falta tu compromiso..."
            else -> ""
        }
    }

    private fun getExperienceReaction(experience: String?): String = when (experience) {
        "🎓 Empiezo de cero absoluto" -> "Perfecto. Vamos a construirlo desde cero 🗽"
        "📖 Ya tengo algunas nociones básicas" -> "Bien. Aceleramos el ritmo entonces 🚀"
        "🔁 He suspendido y quiero repetirlo" -> "Esta vez lo clavamos. Te lo prometo 💪"
        "🧇 Ya tengo otro carnet" -> "¡Un experto! Esto será fácil para ti 😎"
        else -> ""
    }

    private fun getReasonReaction(reasons: Set<String>): String = when {
        reasons.size > 1 -> "Varias razones. Me gusta tu motivación 🔥"
        reasons.contains("💼 Trabajo") -> "¡A por ese trabajo! 💼"
        reasons.contains("🏠 Independencia") -> "Se acabó depender de los demás 🗽"
        reasons.contains("✈️ Viajes") -> "Carreteras esperándote 🗺️"
        reasons.contains("👨‍👩‍👧‍👦 Familia") -> "El chófer oficial en camino 🚗"
        reasons.contains("🛠️ Oportunidad académica") -> "Invirtiendo en tu futuro 📚"
        reasons.contains("🏎️ Disfrute personal") -> "¡Pura pasión por conducir! 🏎️"
        else -> "¿Por qué quieres el carnet?"
    }
}
