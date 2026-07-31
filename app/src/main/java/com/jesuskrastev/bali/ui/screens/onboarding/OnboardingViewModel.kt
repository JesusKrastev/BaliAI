package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.UserRepository
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
    val theoryBlocker: String? = null,
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
    data object TheoryBlocker : OnboardingStep()
    data object ExamDate : OnboardingStep()
    data object MethodComparison : OnboardingStep()
    data object DailyGoal : OnboardingStep()
    data object LearningPreference : OnboardingStep()
    data object DifficultTopics : OnboardingStep()
    data object DialogueDifficultTopics : OnboardingStep()
    data object Concern : OnboardingStep()
    data object StudyTime : OnboardingStep()
    data object Notifications : OnboardingStep()
    data object SocialProof : OnboardingStep()
    data object Processing : OnboardingStep()
    data object Comparison : OnboardingStep()
    data object LossAversion : OnboardingStep()
    data object Pact : OnboardingStep()
    data object PaywallPending : OnboardingStep()
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
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val reducer = OnboardingReducer()

    private val stepsOrder = listOf(
        OnboardingStep.Name, OnboardingStep.License, OnboardingStep.Experience,
        OnboardingStep.DialogueExperience, OnboardingStep.TheoryBlocker, OnboardingStep.ExamDate,
        OnboardingStep.MethodComparison, OnboardingStep.DailyGoal, OnboardingStep.LearningPreference, OnboardingStep.DifficultTopics,
        OnboardingStep.DialogueDifficultTopics, OnboardingStep.Concern, OnboardingStep.StudyTime,
        OnboardingStep.Notifications, OnboardingStep.SocialProof, OnboardingStep.Processing,
        OnboardingStep.Comparison, OnboardingStep.LossAversion, OnboardingStep.Pact
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
            is OnboardingEvent.SelectTheoryBlocker -> selectStepItem { it.copy(theoryBlocker = event.blocker) }
            is OnboardingEvent.SelectExamDate -> selectStepItem { it.copy(examDate = event.dateMillis) }
            is OnboardingEvent.SelectDailyGoal -> selectStepItem { it.copy(dailyGoal = event.goal) }
            is OnboardingEvent.SelectLearningPreference -> selectStepItem { it.copy(learningPreference = event.preference) }
            is OnboardingEvent.ToggleDifficultTopic -> toggleDifficultTopic(event.topic)
            is OnboardingEvent.SelectConcern -> selectStepItem { it.copy(concern = event.concern) }
            is OnboardingEvent.SelectStudyTime -> selectStepItem { it.copy(studyTime = event.time) }
            OnboardingEvent.GoToNextStep -> goToNextStep()
            OnboardingEvent.GoToPreviousStep -> goToPreviousStep()
            OnboardingEvent.CompleteOnboarding -> completeOnboarding()
            OnboardingEvent.RateAppClicked -> analyticsTracker.onboardingRateAppClicked()
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

    private fun toggleDifficultTopic(topic: String) {
        val current = _uiState.value.data.difficultTopics
        val newSet = if (current.contains(topic)) current - topic else current + topic
        updateData { it.copy(difficultTopics = newSet) }
    }

    private fun trackStepCompleted(step: OnboardingStep) {
        when (step) {
            OnboardingStep.Name -> analyticsTracker.onboardingStepName()
            OnboardingStep.License -> analyticsTracker.onboardingStepLicense()
            OnboardingStep.Experience -> analyticsTracker.onboardingStepExperience()
            OnboardingStep.DialogueExperience -> analyticsTracker.onboardingStepDialogueExperience()
            OnboardingStep.TheoryBlocker -> analyticsTracker.onboardingStepReasons()
            OnboardingStep.ExamDate -> analyticsTracker.onboardingStepExamDate()
            OnboardingStep.MethodComparison -> analyticsTracker.onboardingStepMethodComparison()
            OnboardingStep.DailyGoal -> analyticsTracker.onboardingStepDailyGoal()
            OnboardingStep.LearningPreference -> analyticsTracker.onboardingStepLearningPreference()
            OnboardingStep.DifficultTopics -> analyticsTracker.onboardingStepDifficultTopics()
            OnboardingStep.DialogueDifficultTopics -> analyticsTracker.onboardingStepDialogueDifficultTopics()
            OnboardingStep.Concern -> analyticsTracker.onboardingStepConcern()
            OnboardingStep.StudyTime -> analyticsTracker.onboardingStepStudyTime()
            OnboardingStep.Notifications -> analyticsTracker.onboardingStepNotifications()
            OnboardingStep.SocialProof -> analyticsTracker.onboardingStepSocialProof()
            OnboardingStep.Processing -> analyticsTracker.onboardingStepProcessing()
            OnboardingStep.Comparison -> analyticsTracker.onboardingStepComparison()
            OnboardingStep.LossAversion -> analyticsTracker.onboardingStepLossAversion()
            OnboardingStep.Pact -> analyticsTracker.onboardingStepPact()
            else -> Unit
        }
    }

    private fun goToNextStep() {
        val currentIndex = stepsOrder.indexOf(_uiState.value.currentStep)
        trackStepCompleted(_uiState.value.currentStep)

        if (currentIndex < stepsOrder.lastIndex) {
            val nextStep = stepsOrder[currentIndex + 1]
            if (nextStep == OnboardingStep.Processing) {
                startProcessing()
            } else {
                _uiState.update {
                    it.copy(
                        currentStep = nextStep,
                        progress = reducer.calculateProgress(currentIndex + 1, stepsOrder.size)
                    )
                }
                updateMascotMessage()
                updateNavigationState()
            }
        } else {
            // End of onboarding steps → show paywall
            _uiState.update { it.copy(currentStep = OnboardingStep.PaywallPending) }
        }
    }

    private fun completeOnboarding() {
        saveDataAndComplete()
    }

    private fun saveDataAndComplete() {
        viewModelScope.launch {
            val data = _uiState.value.data
            val preferences = User(
                name = data.name,
                licenseType = data.licenseType,
                experience = data.experience,
                examDateMillis = data.examDate,
                difficultTopics = data.difficultTopics.joinToString(","),
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
                    progress = reducer.calculateProgress(currentIndex - 1, stepsOrder.size)
                )
            }
            updateMascotMessage()
            updateNavigationState()
        }
    }

    private fun startProcessing() {
        _uiState.update {
            it.copy(
                currentStep = OnboardingStep.Processing,
                progress = 1f,
                isProcessingFinished = false,
                processingProgress = 0f
            )
        }
        updateMascotMessage()
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
        val canGoNext = reducer.shouldEnableNextButton(state.currentStep, state.data)
        _uiState.update {
            it.copy(
                canGoNext = canGoNext,
                canGoBack = stepsOrder.indexOf(state.currentStep) > 0
            )
        }
    }

    private fun updateMascotMessage() {
        val state = _uiState.value
        val message = reducer.updateMascotMessage(state.currentStep, state.data)
        _uiState.update { it.copy(mascotMessage = message) }
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.currentStep !is OnboardingStep.Completed) {
            val stepIndex = stepsOrder.indexOf(_uiState.value.currentStep)
            analyticsTracker.onboardingAbandoned(_uiState.value.currentStep.javaClass.simpleName, stepIndex)
        }
    }
}
