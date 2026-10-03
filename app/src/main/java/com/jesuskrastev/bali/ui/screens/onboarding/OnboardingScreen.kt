package com.jesuskrastev.bali.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.ui.screens.onboarding.components.MascotHeader
import com.jesuskrastev.bali.ui.screens.onboarding.components.SmoothProgressBar
import com.jesuskrastev.bali.ui.screens.onboarding.steps.*
import com.jesuskrastev.bali.ui.screens.paywall.PaywallScreen

/**
 * Renders onboarding and keeps its paywall blocking until RevenueCat confirms premium. A
 * decline — including of the paywall's one-time win-back offer — finishes the hosting activity
 * instead of returning to an earlier onboarding step, since there is no free-content step to
 * send the user back to.
 *
 * Before the paywall, the back arrow and the system back gesture step back one screen and keep
 * the answers. On the first screen the gesture closes the app as usual.
 *
 * @param sharedTransitionScope scope used by the mascot transition
 * @param animatedVisibilityScope visibility scope of the onboarding destination
 * @param viewModel owner of the onboarding state and profile persistence
 * @param onComplete invoked after a successful purchase and profile persistence
 */
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
        // No onboarding step to fall back to here either: a decline (the win-back offer
        // already shown and turned down) has to leave the app, not strand the user on a
        // paywall step that never advances and whose close button stops responding.
        val activity = LocalActivity.current
        PaywallScreen(
            onDismissResult = { hasPremium ->
                if (hasPremium) {
                    viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                } else {
                    activity?.finish()
                }
            }
        )
        return
    }

    if (uiState.currentStep == OnboardingStep.Completed) {
        LaunchedEffect(Unit) {
            onComplete(uiState.data)
        }
        return
    }

    // The system back gesture does what the arrow does. While the plan is being built it is
    // swallowed instead: letting it through would close the app and lose every answer.
    BackHandler(enabled = uiState.canGoBack || uiState.currentStep == OnboardingStep.Processing) {
        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
    }
    val focusManager = LocalFocusManager.current

    // AppNavigation's own Scaffold already reserves the status bar (top) and the
    // navigation bar (bottom) for this whole screen via NavHost's content padding, so this
    // inner Scaffold must not reserve system bar insets a second time here — that previously
    // doubled the top gap and left dead space under the progress bar, and pushed the
    // "Continuar" button up off the true bottom edge instead of sitting flush like a proper
    // bottom bar.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            OnboardingTopBar(
                progress = uiState.progress,
                visible = uiState.currentStep != OnboardingStep.Processing,
                canGoBack = uiState.canGoBack,
                onBack = {
                    // Puts the name screen's keyboard away before the screen slides out.
                    focusManager.clearFocus()
                    viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
                }
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
 * Top of the flow: the back arrow and the progress bar.
 *
 * The arrow's slot is kept even on the first screen, where there is nothing to go back to, so
 * the bar does not change width when the arrow appears.
 *
 * @param progress completion ratio between 0f and 1f
 * @param visible whether the bar should be rendered at all
 * @param canGoBack whether the arrow is shown
 * @param onBack invoked when the arrow is tapped
 */
@Composable
private fun OnboardingTopBar(
    progress: Float,
    visible: Boolean,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    if (!visible) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Less than the screens' 24 dp margin: the button and the glyph carry their own
            // padding, which brings the visible arrow back onto that margin.
            .padding(start = 8.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            SmoothProgressBar(progress = progress)
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
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                maskMessage = uiState.data.name.let { name ->
                    !name.isNullOrBlank() && uiState.mascotMessage.contains(name, ignoreCase = true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        OnboardingStepContent(uiState, viewModel)
    }
}

/**
 * The screen of the current step. It slides in from the right when the user moves on and from
 * the left when they go back.
 *
 * @param state current onboarding state
 * @param viewModel receiver of the events the step raises
 */
@Composable
private fun OnboardingStepContent(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    // 1 brings the new screen in from the right (moving on), -1 from the left (going back).
    val direction = if (state.isMovingBack) -1 else 1
    AnimatedContent(
        targetState = state.currentStep,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it * direction }, animationSpec = tween(300)) + fadeIn() togetherWith
            slideOutHorizontally(targetOffsetX = { -it * direction }, animationSpec = tween(300)) + fadeOut()
        },
        label = "onboarding_step",
        modifier = Modifier
            .fillMaxSize()
            // StepProcessing centres its own full-bleed layout and applies its own padding, and the
            // exam date calendar needs the full width of a small phone.
            .padding(horizontal = if (state.currentStep in FULL_WIDTH_STEPS) 0.dp else 24.dp)
    ) { step ->
        // Every screen of the emotional arc shares the same one-idea layout, with copy written
        // for the answers given so far.
        val narrative = OnboardingNarratives.forStep(step, state.data)
        if (narrative != null) {
            StepNarrative(content = narrative.content)
            return@AnimatedContent
        }

        when (step) {
            OnboardingStep.Intro -> StepIntro(
                onPageShown = { viewModel.onEvent(OnboardingEvent.IntroCardShown(it)) },
                onFinish = { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
            )
            OnboardingStep.Motivation -> {
                StepSelectorList(OnboardingConfig.motivations, viewModel) { motivation, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectMotivation(motivation))
                }
            }
            OnboardingStep.TheoryBlocker -> {
                StepSelectorList(OnboardingConfig.theoryBlockers, viewModel) { blocker, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(blocker))
                }
            }
            OnboardingStep.Concern -> {
                StepSelectorList(OnboardingConfig.concerns, viewModel) { concern, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectConcern(concern))
                }
            }
            OnboardingStep.Experience -> {
                StepSelectorList(OnboardingConfig.experiences, viewModel) { experience, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectExperience(experience))
                }
            }
            OnboardingStep.Readiness -> {
                StepSelectorList(OnboardingConfig.readinessLevels, viewModel) { readiness, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectReadiness(readiness))
                }
            }
            OnboardingStep.Quiz -> {
                val questions = state.data.quizQuestions()
                questions.getOrNull(state.quizIndex)?.let { question ->
                    StepQuiz(
                        question = question,
                        index = state.quizIndex,
                        total = questions.size,
                        selectedIndex = state.data.quizAnswers.getOrNull(state.quizIndex)?.selectedIndex,
                        onAnswer = { viewModel.onEvent(OnboardingEvent.AnswerQuiz(it)) },
                        onNext = { viewModel.onEvent(OnboardingEvent.NextQuizQuestion) },
                        onSkip = { viewModel.onEvent(OnboardingEvent.SkipQuiz) }
                    )
                }
            }
            OnboardingStep.QuizResult -> StepQuizResult(state.data)
            OnboardingStep.MethodComparison -> StepMethodComparison(state.data)
            OnboardingStep.Name -> StepName(state.data.name ?: "", viewModel)
            OnboardingStep.ExamDate -> StepExamDate(
                examDate = state.data.examDate,
                onConfirm = { viewModel.onEvent(OnboardingEvent.SetExamDate(it)) },
                onNoDate = { viewModel.onEvent(OnboardingEvent.SetExamDate(null)) }
            )
            OnboardingStep.Province -> StepProvince(state.data.province) { province ->
                viewModel.onEvent(OnboardingEvent.SelectProvince(province))
            }
            OnboardingStep.WeeklyStudy -> {
                StepSelectorList(OnboardingConfig.weeklyStudyOptions, viewModel) { weeklyStudy, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectWeeklyStudy(weeklyStudy))
                }
            }
            OnboardingStep.StudyTime -> {
                StepSelectorList(OnboardingConfig.studyTimes.keys.toList(), viewModel) { studyTime, _ ->
                    viewModel.onEvent(OnboardingEvent.SelectStudyTime(studyTime))
                }
            }
            OnboardingStep.Notifications -> StepNotifications(
                pitch = OnboardingConfig.notificationsPitch(state.data.weeklyStudy),
                isRequesting = state.isRequestingNotifications,
                onAccept = { viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = true)) },
                onDecline = { viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = false)) }
            )
            OnboardingStep.LearningPreference -> StepLearningStyle(
                styles = OnboardingConfig.learningStyles,
                selectedKey = state.data.learningPreference,
                onSelect = { viewModel.onEvent(OnboardingEvent.SelectLearningPreference(it.key)) }
            )
            OnboardingStep.Processing -> StepProcessing(
                progress = state.processingProgress,
                data = state.data
            )
            OnboardingStep.Comparison -> StepComparison(state.data)
            OnboardingStep.PlanReveal -> StepPlanReveal(state.data)
            OnboardingStep.SocialProof -> StepSocialProof()
            OnboardingStep.Pact -> StepPact { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
            else -> Unit
        }
    }
}

/** Steps that lay out their own side margins. */
private val FULL_WIDTH_STEPS = setOf(OnboardingStep.Processing, OnboardingStep.ExamDate)

/**
 * Decides whether the mascot bubble is rendered above the step content.
 *
 * @param step the step currently on screen
 * @return false for steps that own their full-height layout and carry their own title
 */
private fun shouldShowMascot(step: OnboardingStep): Boolean = when (step) {
    OnboardingStep.Intro,
    OnboardingStep.MethodComparison,
    OnboardingStep.Comparison,
    OnboardingStep.Processing,
    OnboardingStep.PlanReveal -> false
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
        step == OnboardingStep.Province

/**
 * Copy for the bottom button. On the narrative screens the label doubles as a
 * micro-commitment, so the user agrees with the argument before moving on.
 *
 * @param step the step currently on screen
 * @return the button label
 */
private fun getButtonText(step: OnboardingStep): String = when (step) {
    OnboardingStep.Name -> "Empezar mi plan 🚀"
    OnboardingStep.Pain -> "Sí, es justo eso →"
    OnboardingStep.MethodComparison -> "Ese es mi camino 🚀"
    OnboardingStep.Gain -> "Eso quiero ✨"
    OnboardingStep.Comparison -> "A por ello 💪"
    OnboardingStep.QuizResult -> "Seguir →"
    OnboardingStep.PlanReveal -> "Este es mi plan 🎯"
    OnboardingStep.SocialProof -> "Yo también puedo →"
    else -> "Continuar →"
}
