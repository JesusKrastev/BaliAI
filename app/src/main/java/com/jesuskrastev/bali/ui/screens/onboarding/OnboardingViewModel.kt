package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class OnboardingData(
    val name: String? = null,
    val licenseType: String? = null,
    val experience: String? = null,
    val reasons: Set<String> = emptySet(),
    val examDate: Long? = null,
    val dailyGoal: String? = null,
    val learningPreference: String? = null,
    val difficultTopics: Set<String> = emptySet(),
    val concern: String? = null,
    val studyTime: String? = null,
)

sealed class OnboardingStep {
    data object Name : OnboardingStep()
    data object License : OnboardingStep()
    data object Experience : OnboardingStep()
    data object DialogueExperience : OnboardingStep()
    data object Reasons : OnboardingStep()
    data object ExamDate : OnboardingStep()
    data object DailyGoal : OnboardingStep()
    data object LearningPreference : OnboardingStep()
    data object DifficultTopics : OnboardingStep()
    data object DialogueDifficultTopics : OnboardingStep()
    data object Concern : OnboardingStep()
    data object StudyTime : OnboardingStep()
    data object Notifications : OnboardingStep()
    data object Processing : OnboardingStep()
    data object Comparison : OnboardingStep()
    data object Pact : OnboardingStep()
    data object Completed : OnboardingStep()
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Name,
    val data: OnboardingData = OnboardingData(),
    val progress: Float = 0f,
    val processingProgress: Float = 0f,
    val mascotMessage: String = "",
    val canGoBack: Boolean = false,
    val canGoNext: Boolean = false,
    val isProcessingFinished: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val analyticsTracker: FirebaseAnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val stepsOrder = listOf(
        OnboardingStep.Name, OnboardingStep.License, OnboardingStep.Experience, OnboardingStep.DialogueExperience,
        OnboardingStep.Reasons, OnboardingStep.ExamDate, OnboardingStep.DailyGoal,
        OnboardingStep.LearningPreference, OnboardingStep.DifficultTopics,
        OnboardingStep.DialogueDifficultTopics, OnboardingStep.Concern, OnboardingStep.StudyTime,
        OnboardingStep.Notifications, OnboardingStep.Processing, OnboardingStep.Comparison,
        OnboardingStep.Pact
    )

    init {
        updateMascotMessage()
        analyticsTracker.onboardingStarted()
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.SetName -> updateData { it.copy(name = event.name) }
            is OnboardingEvent.SelectLicense -> selectStepItem { it.copy(licenseType = event.license) }
            is OnboardingEvent.SelectExperience -> selectStepItem { it.copy(experience = event.experience) }
            is OnboardingEvent.ToggleReason -> toggleReason(event.reason)
            is OnboardingEvent.SelectExamDate -> selectStepItem { it.copy(examDate = event.dateMillis) }
            is OnboardingEvent.SelectDailyGoal -> selectStepItem { it.copy(dailyGoal = event.goal) }
            is OnboardingEvent.SelectLearningPreference -> selectStepItem { it.copy(learningPreference = event.preference) }
            is OnboardingEvent.ToggleDifficultTopic -> toggleDifficultTopic(event.topic)
            is OnboardingEvent.SelectConcern -> selectStepItem { it.copy(concern = event.concern) }
            is OnboardingEvent.SelectStudyTime -> selectStepItem { it.copy(studyTime = event.time) }
            OnboardingEvent.GoToNextStep -> goToNextStep()
            OnboardingEvent.GoToPreviousStep -> goToPreviousStep()
        }
    }

    private fun selectStepItem(update: (OnboardingData) -> OnboardingData) {
        updateData(update)
        goToNextStep()
    }

    private fun updateData(update: (OnboardingData) -> OnboardingData) {
        _uiState.update { it.copy(data = update(it.data)) }
        updateNavigationState()
        updateMascotMessage()
    }

    private fun toggleReason(reason: String) {
        val current = _uiState.value.data.reasons
        val newSet = if (current.contains(reason)) current - reason else current + reason
        updateData { it.copy(reasons = newSet) }
    }

    private fun toggleDifficultTopic(topic: String) {
        val current = _uiState.value.data.difficultTopics
        val newSet = if (current.contains(topic)) current - topic else current + topic
        updateData { it.copy(difficultTopics = newSet) }
    }

    private fun goToNextStep() {
        val currentIndex = stepsOrder.indexOf(_uiState.value.currentStep)
        analyticsTracker.onboardingStepCompleted(_uiState.value.currentStep.javaClass.simpleName)
        
        if (currentIndex < stepsOrder.lastIndex) {
            val nextStep = stepsOrder[currentIndex + 1]
            if (nextStep == OnboardingStep.Processing) {
                startProcessing()
            } else {
                _uiState.update {
                    it.copy(
                        currentStep = nextStep,
                        progress = calculateProgress(currentIndex + 1)
                    )
                }
                updateMascotMessage()
                updateNavigationState()
            }
        } else {
            saveDataAndComplete()
        }
    }

    private fun saveDataAndComplete() {
        viewModelScope.launch {
            val data = _uiState.value.data
            val preferences = User(
                name = data.name,
                licenseType = data.licenseType,
                experience = data.experience,
                reasons = data.reasons.joinToString(","),
                examDateMillis = data.examDate,
                dailyGoal = data.dailyGoal,
                learningPreference = data.learningPreference,
                difficultTopics = data.difficultTopics.joinToString(","),
                concern = data.concern,
                studyTime = data.studyTime,
                lastPracticeTimestamp = 0,
                currentStreak = 0
            )
            userRepository.insert(preferences)
            analyticsTracker.onboardingCompleted()
            _uiState.update { it.copy(currentStep = OnboardingStep.Completed) }
        }
    }

    private fun goToPreviousStep() {
        val currentIndex = stepsOrder.indexOf(_uiState.value.currentStep)
        if (currentIndex > 0) {
            val prevStep = stepsOrder[currentIndex - 1]
            _uiState.update {
                it.copy(
                    currentStep = prevStep,
                    progress = calculateProgress(currentIndex - 1)
                )
            }
            updateMascotMessage()
            updateNavigationState()
        }
    }

    private fun calculateProgress(index: Int): Float = index.toFloat() / (stepsOrder.size - 1)

    private fun startProcessing() {
        _uiState.update {
            it.copy(
                currentStep = OnboardingStep.Processing,
                progress = 1f,
                isProcessingFinished = false,
                processingProgress = 0f
            )
        }
        viewModelScope.launch {
            val totalSteps = 100
            for (i in 1..totalSteps) {
                delay(Random.nextLong(20, 50))
                _uiState.update { it.copy(processingProgress = i / totalSteps.toFloat()) }
            }
            delay(300)
            _uiState.update { it.copy(isProcessingFinished = true) }
            delay(800)
            goToNextStep()
        }
    }

    private fun updateNavigationState() {
        val state = _uiState.value
        val canGoNext = when (state.currentStep) {
            OnboardingStep.Name -> {
                val name = state.data.name ?: ""
                name.isNotBlank() && name.all { it.isLetter() || it.isWhitespace() }
            }
            OnboardingStep.Reasons -> state.data.reasons.isNotEmpty()
            OnboardingStep.DifficultTopics -> state.data.difficultTopics.isNotEmpty()
            is OnboardingStep.DialogueExperience,
            is OnboardingStep.DialogueDifficultTopics,
            OnboardingStep.Notifications,
            OnboardingStep.Comparison -> true
            else -> false
        }
        _uiState.update {
            it.copy(
                canGoNext = canGoNext,
                canGoBack = stepsOrder.indexOf(state.currentStep) > 0
            )
        }
    }

    private fun updateMascotMessage() {
        val state = _uiState.value
        val message = when (state.currentStep) {
            OnboardingStep.Name -> "¿Cómo te llamas?"
            OnboardingStep.License -> "Genial, ${state.data.name ?: ""}. ¿Qué carnet quieres sacarte?"
            OnboardingStep.Experience -> "¿En qué punto estás ahora mismo?"
            OnboardingStep.DialogueExperience -> getExperienceReaction(OnboardingConfig.experiences.indexOf(state.data.experience))
            OnboardingStep.Reasons -> getReasonReaction(state.data.reasons)
            OnboardingStep.ExamDate -> "¿Ya tienes fecha de examen?"
            OnboardingStep.DailyGoal -> "¿Cuánto tiempo puedes dedicarme al día?"
            OnboardingStep.LearningPreference -> "¿Cómo prefieres aprender?"
            OnboardingStep.DifficultTopics -> "¿Qué temas se te atragantan más?"
            OnboardingStep.DialogueDifficultTopics -> "Entendido. Vamos a machacarlo juntos 💪"
            OnboardingStep.Concern -> "¿Qué es lo que más miedo te da?"
            OnboardingStep.StudyTime -> "¿Cuándo te cunde más estudiar?"
            OnboardingStep.Notifications -> "Activa las notis. Yo cuido tu racha 🔥"
            OnboardingStep.Processing -> "Déjame pensar... Creando tu plan 🤔"
            OnboardingStep.Comparison -> "Mira cómo vas a estudiar conmigo 👇"
            OnboardingStep.Pact -> "Casi listo, ${state.data.name ?: ""}. Solo falta tu compromiso..."
            else -> ""
        }
        _uiState.update { it.copy(mascotMessage = message) }
    }

    private fun getExperienceReaction(index: Int?) = when (index) {
        0 -> "Perfecto. Vamos a construirlo desde cero 🏗️"
        1 -> "Bien. Aceleramos el ritmo entonces 🚀"
        2 -> "Esta vez lo clavamos. Te lo prometo 💪"
        3 -> "¡Un experto! Esto será fácil para ti 😎"
        else -> ""
    }

    private fun getReasonReaction(reasons: Set<String>) = when {
        reasons.size > 1 -> "Varias razones. Me gusta tu motivación 🔥"
        reasons.contains("💼 Trabajo") -> "¡A por ese trabajo! 💼"
        reasons.contains("🏠 Independencia") -> "Se acabó depender de los demás 🗝️"
        reasons.contains("✈️ Viajes") -> "Carreteras esperándote 🗺️"
        reasons.contains("👨‍👩‍👧‍👦 Familia") -> "El chófer oficial en camino 🚗"
        reasons.contains("🛠️ Oportunidad académica") -> "Invirtiendo en tu futuro 📚"
        reasons.contains("🏎️ Disfrute personal") -> "¡Pura pasión por conducir! 🏎️"
        else -> "¿Por qué quieres el carnet?"
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.currentStep !is OnboardingStep.Completed) {
            analyticsTracker.onboardingAbandoned(_uiState.value.currentStep.javaClass.simpleName)
        }
    }
}
