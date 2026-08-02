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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OnboardingScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: OnboardingViewModel,
    onComplete: (OnboardingData) -> Unit
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
                visible = uiState.currentStep != OnboardingStep.Processing
            )
        },
        bottomBar = { OnboardingBottomBar(uiState, viewModel) }
    ) { paddingValues ->
        OnboardingBody(
            uiState = uiState,
            viewModel = viewModel,
            paddingValues = paddingValues,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

/**
 * Top progress bar of the flow.
 *
 * @param progress completion ratio between 0f and 1f
 * @param visible whether the bar should be rendered at all
 */
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

/**
 * Bottom "continue" button, shown only on steps that do not advance on their own.
 *
 * @param uiState current onboarding state
 * @param viewModel receiver of the navigation event
 */
@Composable
private fun OnboardingBottomBar(
    uiState: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    if (!shouldShowBottomButton(uiState.currentStep)) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = { viewModel.onEvent(OnboardingEvent.GoToNextStep) },
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
    paddingValues: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Some steps own their full-height layout and read better without the bubble.
        if (shouldShowMascot(uiState.currentStep)) {
            MascotHeader(
                message = uiState.mascotMessage,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        OnboardingStepContent(uiState, viewModel)
    }
}

@Composable
private fun OnboardingStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
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
            .padding(horizontal = if (state.currentStep == OnboardingStep.Processing) 0.dp else 24.dp)
    ) { step ->
        // Every screen of the emotional arc shares the same one-idea layout.
        val narrative = OnboardingConfig.narratives[step]
        if (narrative != null) {
            StepNarrative(content = narrative)
            return@AnimatedContent
        }

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
            OnboardingStep.Motivation -> {
                StepSelectorList(OnboardingConfig.motivations, viewModel) { motivation, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectMotivation(motivation))
                }
            }
            OnboardingStep.FutureImpact -> {
                StepSelectorList(OnboardingConfig.futureImpacts, viewModel) { impact, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectFutureImpact(impact))
                }
            }
            OnboardingStep.MethodComparison -> StepMethodComparison()
            OnboardingStep.ExamDate -> StepExamDate(state.data.examDate, viewModel)
            OnboardingStep.DifficultTopics -> {
                StepMultiSelectorList(OnboardingConfig.difficultTopics, state.data.difficultTopics, viewModel) {
                    viewModel.onEvent(OnboardingEvent.ToggleDifficultTopic(it))
                }
            }
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
            OnboardingStep.Processing -> StepProcessing(
                progress = state.processingProgress,
                data = state.data
            )
            OnboardingStep.Comparison -> StepComparison(state.data)
            OnboardingStep.PlanReveal -> StepPlanReveal(state.data)
            OnboardingStep.SocialProof -> StepSocialProof(
                onRateAppClicked = { viewModel.onEvent(OnboardingEvent.RateAppClicked) }
            )
            OnboardingStep.Pact -> StepPact { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
            // Dialogue steps are carried entirely by the mascot bubble above.
            OnboardingStep.DialogueExperience, OnboardingStep.DialogueDifficultTopics -> Box(Modifier.fillMaxSize())
            else -> Unit
        }
    }
}

/**
 * Decides whether the mascot bubble is rendered above the step content.
 *
 * @param step the step currently on screen
 * @return false for steps that own their full-height layout and carry their own title
 */
private fun shouldShowMascot(step: OnboardingStep): Boolean = when (step) {
    OnboardingStep.MethodComparison,
    OnboardingStep.Comparison,
    OnboardingStep.Processing -> false
    else -> true
}

/**
 * Decides whether the step needs the bottom button. Informational steps always do;
 * selection steps advance on tap and therefore do not.
 *
 * @param step the step currently on screen
 * @return true when the bottom button should be rendered
 */
private fun shouldShowBottomButton(step: OnboardingStep): Boolean =
    step is OnboardingStep.Informational ||
        step == OnboardingStep.Name ||
        step == OnboardingStep.DifficultTopics

/**
 * Copy for the bottom button. On the narrative screens the label doubles as a
 * micro-commitment, so the user agrees with the argument before moving on.
 *
 * @param step the step currently on screen
 * @return the button label
 */
private fun getButtonText(step: OnboardingStep): String = when (step) {
    OnboardingStep.Name -> "Empezar mi plan 🚀"
    OnboardingStep.Empathy -> "Sí, es justo eso →"
    OnboardingStep.LossTime -> "Es verdad 😔"
    OnboardingStep.LossOpportunity -> "No quiero eso →"
    OnboardingStep.LossAutonomy -> "Se acabó 😤"
    OnboardingStep.MethodComparison -> "Ese es mi camino 🕊️"
    OnboardingStep.GainFreedom -> "Eso quiero 🕊️"
    OnboardingStep.GainExperiences -> "Me lo estoy imaginando 🏖️"
    OnboardingStep.GainLevelUp -> "Ese es mi siguiente paso 🚀"
    OnboardingStep.DifficultTopics -> "Estos son mis retos →"
    OnboardingStep.Comparison -> "Quiero este método 💪"
    OnboardingStep.PlanReveal -> "Este es mi plan 🎯"
    OnboardingStep.SocialProof -> "Yo también puedo →"
    else -> "Continuar →"
}
