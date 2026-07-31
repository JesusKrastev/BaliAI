package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.ui.screens.onboarding.components.MascotHeader
import com.jesuskrastev.bali.ui.screens.onboarding.components.SmoothProgressBar
import com.jesuskrastev.bali.ui.screens.onboarding.steps.*
import com.jesuskrastev.bali.ui.screens.paywall.PaywallScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OnboardingScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: OnboardingViewModel,
    onComplete: (OnboardingData) -> Unit,
    notificationManager: NotificationManager = NotificationManager()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.currentStep == OnboardingStep.PaywallPending) {
        PaywallScreen(
            onDismissResult = {
                viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                onComplete(uiState.data)
            }
        )
        return
    }

    if (uiState.currentStep == OnboardingStep.Completed) {
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            OnboardingTopBar(
                progress = uiState.progress,
                visible = uiState.currentStep != OnboardingStep.Processing && uiState.currentStep != OnboardingStep.Completed
            )
        },
        bottomBar = { OnboardingBottomBar(uiState, viewModel, notificationManager) }
    ) { paddingValues ->
        OnboardingBody(
            uiState = uiState,
            viewModel = viewModel,
            notificationManager = notificationManager,
            paddingValues = paddingValues,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

@Composable
private fun OnboardingTopBar(progress: Float, visible: Boolean) {
    if (!visible) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SmoothProgressBar(progress = progress)
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    uiState: OnboardingUiState,
    viewModel: OnboardingViewModel,
    notificationManager: NotificationManager = NotificationManager()
) {
    if (!shouldShowBottomButton(uiState.currentStep)) return

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = {
                if (uiState.currentStep == OnboardingStep.Notifications) {
                    scope.launch {
                        notificationManager.requestNotificationPermission()
                        viewModel.onEvent(OnboardingEvent.GoToNextStep)
                    }
                } else {
                    viewModel.onEvent(OnboardingEvent.GoToNextStep)
                }
            },
            enabled = uiState.canGoNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = getButtonText(uiState.currentStep),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun OnboardingBody(
    uiState: OnboardingUiState,
    viewModel: OnboardingViewModel,
    notificationManager: NotificationManager,
    paddingValues: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (uiState.currentStep != OnboardingStep.Completed) {
            MascotHeader(
                message = uiState.mascotMessage,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        OnboardingStepContent(uiState, viewModel, notificationManager)
    }
}

@Composable
private fun OnboardingStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    notificationManager: NotificationManager
) {
    AnimatedContent(
        targetState = state.currentStep,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() togetherWith
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut()
        },
        label = "onboarding_step",
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (state.currentStep == OnboardingStep.Processing || state.currentStep == OnboardingStep.Completed) 0.dp else 24.dp)
    ) { step ->
        when (step) {
            OnboardingStep.Name -> StepName(state.data.name ?: "", viewModel)
            OnboardingStep.License -> {
                StepSelectorList(OnboardingConfig.licenses, viewModel) { license, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectLicense(license))
                }
            }
            OnboardingStep.Experience -> {
                StepSelectorList(OnboardingConfig.experiences, viewModel) { experience, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectExperience(experience))
                }
            }
            OnboardingStep.TheoryBlocker -> {
                StepSelectorList(OnboardingConfig.theoryBlockers, viewModel) { blocker, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(blocker))
                }
            }
            OnboardingStep.ExamDate -> StepExamDate(state.data.examDate, viewModel)
            OnboardingStep.MethodComparison -> StepMethodComparison()
            OnboardingStep.DailyGoal -> {
                StepSelectorList(OnboardingConfig.dailyGoals, viewModel) { goal, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectDailyGoal(goal))
                }
            }
            OnboardingStep.LearningPreference -> {
                StepSelectorList(OnboardingConfig.learningPreferences, viewModel) { preference, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectLearningPreference(preference))
                }
            }
            OnboardingStep.DifficultTopics -> {
                StepMultiSelectorList(OnboardingConfig.difficultTopics, state.data.difficultTopics, viewModel) {
                    viewModel.onEvent(OnboardingEvent.ToggleDifficultTopic(it))
                }
            }
            OnboardingStep.Concern -> {
                StepSelectorList(OnboardingConfig.concerns, viewModel) { concern, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectConcern(concern))
                }
            }
            OnboardingStep.StudyTime -> {
                StepSelectorList(OnboardingConfig.studyTimes, viewModel) { studyTime, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectStudyTime(studyTime))
                }
            }
            OnboardingStep.Notifications -> StepNotifications(viewModel, notificationManager)
            OnboardingStep.SocialProof -> StepSocialProof(
                onRateAppClicked = { viewModel.onEvent(OnboardingEvent.RateAppClicked) }
            )
            OnboardingStep.Processing -> StepProcessing(progress = state.processingProgress)
            OnboardingStep.Comparison -> StepComparison(state.data)
            OnboardingStep.LossAversion -> StepLossAversion(data = state.data)
            OnboardingStep.Pact -> StepPact { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
            is OnboardingStep.DialogueExperience, is OnboardingStep.DialogueDifficultTopics -> Box(Modifier.fillMaxSize())
            else -> Unit
        }
    }
}

// Utility Functions
private fun shouldShowBottomButton(step: OnboardingStep): Boolean = when (step) {
    OnboardingStep.Name, OnboardingStep.DialogueExperience,
    OnboardingStep.DifficultTopics, OnboardingStep.DialogueDifficultTopics,
    OnboardingStep.Notifications, OnboardingStep.SocialProof,
    OnboardingStep.Comparison, OnboardingStep.LossAversion, OnboardingStep.MethodComparison -> true
    else -> false
}

private fun getButtonText(step: OnboardingStep): String = when (step) {
    OnboardingStep.Name -> "Empezar mi plan 🚀"
    OnboardingStep.Notifications -> "Activar recordatorios 🔔"
    OnboardingStep.MethodComparison -> "Impresionante 🤯"
    OnboardingStep.SocialProof -> "Yo también puedo →"
    OnboardingStep.Comparison -> "Quiero este método 💪"
    OnboardingStep.LossAversion -> "Ver mi plan ahora 🎯"
    OnboardingStep.DifficultTopics -> "Estos son mis retos →"
    else -> "Continuar →"
}