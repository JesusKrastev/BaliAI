package com.jesuskrastev.bali.ui.screens.test

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliFlameColors
import com.jesuskrastev.bali.ui.theme.BaliPrimaryDark
import kotlinx.coroutines.launch

/** Peak scale of the run label's pulse when the run reaches [ComboTier.HOT]. */
private const val COMBO_PULSE_HOT = 1.25f

/** Peak scale of the run label's pulse when the run reaches [ComboTier.ON_FIRE]. */
private const val COMBO_PULSE_ON_FIRE = 1.4f

/**
 * Title content shared by the quiz top bars (practice, mistakes review, exam): an optional
 * "N SEGUIDAS" run label above a rounded progress bar, followed by any [footer] content. The
 * label grows with the run (see [ComboTier]) and pulses once on the answer that reaches five and
 * the one that reaches ten.
 *
 * @param currentIndex zero-based index of the question being answered
 * @param totalQuestions number of questions in the session; must be greater than zero
 * @param sessionStreak consecutive correct answers in this session
 * @param isAnswerChecked true once the current answer has been checked; the run label only
 *   shows then, and only from two correct answers in a row
 * @param footer extra content drawn under the progress bar (for example the exam timer)
 */
@Composable
fun QuizProgressTitle(
    currentIndex: Int,
    totalQuestions: Int,
    sessionStreak: Int,
    isAnswerChecked: Boolean,
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    val tier = ComboTier.of(sessionStreak)
    val pulse = remember { Animatable(1f) }
    // The question whose answer last pulsed the label, saved so a rotation does not replay it.
    var pulsedQuestion by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(isAnswerChecked, currentIndex) {
        if (isAnswerChecked && isComboThreshold(sessionStreak) && pulsedQuestion != currentIndex) {
            pulsedQuestion = currentIndex
            val peak = if (tier == ComboTier.ON_FIRE) COMBO_PULSE_ON_FIRE else COMBO_PULSE_HOT
            pulse.animateTo(peak, tween(durationMillis = 140, easing = FastOutSlowInEasing))
            pulse.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (isAnswerChecked && tier != ComboTier.HIDDEN) {
            ComboLabel(run = sessionStreak, tier = tier, scale = { pulse.value })
            Spacer(modifier = Modifier.height(4.dp))
        }
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        footer()
    }
}

/**
 * The "N SEGUIDAS" label in the size and colour of its tier: plain orange for a short run, then
 * bigger with the app's flame, and in the flame's gradient from ten on. Its height stays within
 * 20dp so the exam's top bar, which also holds the timer, still fits.
 *
 * @param run consecutive correct answers, at least two
 * @param tier the tier of [run]; never [ComboTier.HIDDEN]
 * @param scale current pulse scale, read at draw time so the pulse does not recompose
 */
@Composable
private fun ComboLabel(run: Int, tier: ComboTier, scale: () -> Float) {
    val (style, flameSize) = when (tier) {
        ComboTier.ON_FIRE -> TextStyle(
            brush = Brush.horizontalGradient(BaliFlameColors),
            fontSize = 16.sp,
            lineHeight = 20.sp
        ) to 20.dp
        ComboTier.HOT -> TextStyle(color = BaliPrimaryDark, fontSize = 13.sp, lineHeight = 18.sp) to 16.dp
        else -> MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary) to null
    }
    Row(
        modifier = Modifier
            .padding(start = 8.dp)
            .graphicsLayer {
                val pulseScale = scale()
                scaleX = pulseScale
                scaleY = pulseScale
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (flameSize != null) {
            Image(
                painter = painterResource(id = R.drawable.streak_icon),
                contentDescription = null,
                modifier = Modifier.size(flameSize)
            )
        }
        Text(
            text = "$run SEGUIDAS",
            style = style,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Free-practice quiz screen: a close button and progress bar on top, and the question flow
 * (loading, error and content states) below.
 *
 * @param onBackClick closes the quiz
 * @param onFinishTest receives the summary once the last question is answered
 * @param viewModel owner of the quiz state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    onBackClick: () -> Unit,
    onFinishTest: (TestSummary) -> Unit,
    viewModel: TestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading && uiState.questions.isNotEmpty()) {
                        QuizProgressTitle(
                            currentIndex = uiState.currentQuestionIndex,
                            totalQuestions = uiState.questions.size,
                            sessionStreak = uiState.sessionStreak,
                            isAnswerChecked = uiState.isAnswerChecked
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (!uiState.isLoading && uiState.questions.isNotEmpty()) {
                        PracticeAidChips(
                            hints = uiState.hints,
                            fiftyFifties = uiState.fiftyFifties,
                            isHintVisible = uiState.isHintVisible,
                            isFiftyFiftyUsed = uiState.eliminatedOptionIndices.isNotEmpty(),
                            isAnswerChecked = uiState.isAnswerChecked,
                            onUseHint = { viewModel.onEvent(TestEvent.UseHint) },
                            onUseFiftyFifty = { viewModel.onEvent(TestEvent.UseFiftyFifty) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(
                    message = uiState.error ?: "Ocurrió un error inesperado",
                    onRetry = { viewModel.onEvent(TestEvent.Retry) }
                )
                uiState.questions.isEmpty() -> ErrorView(
                    message = "No se pudieron generar las preguntas. Por favor, inténtalo de nuevo.",
                    onRetry = { viewModel.onEvent(TestEvent.Retry) }
                )
                else -> {
                    TestContentView(
                        uiState = uiState,
                        onOptionSelect = { viewModel.onEvent(TestEvent.SelectOption(it)) },
                        onCheckClick = { viewModel.onEvent(TestEvent.CheckAnswer) },
                        onNextClick = {
                            if (uiState.currentQuestionIndex == uiState.questions.size - 1) {
                                viewModel.onEvent(TestEvent.FinishTest { result ->
                                    onFinishTest(result)
                                })
                            } else {
                                viewModel.onEvent(TestEvent.NextQuestion)
                            }
                        }
                    )
                    StreakCheer(
                        currentIndex = uiState.currentQuestionIndex,
                        sessionStreak = uiState.sessionStreak,
                        isAnswerChecked = uiState.isAnswerChecked,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = CHEER_ABOVE_BUTTON_PADDING)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen error state with a retry button.
 *
 * @param message explanation shown under the "¡Ups! Algo salió mal" title
 * @param onRetry invoked when the retry button is tapped
 */
@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¡Ups! Algo salió mal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reintentar")
        }
    }
}

/**
 * One question of the quiz: its text, optional image, answer options and, once checked, the
 * explanation, with the check/next button pinned at the bottom.
 *
 * @param uiState quiz state; must contain at least one question
 * @param onOptionSelect invoked with the index of the tapped option while the answer is unchecked
 * @param onCheckClick invoked when the "Comprobar" button is tapped
 * @param onNextClick invoked when the "Siguiente" / "Finalizar práctica" button is tapped
 */
@Composable
fun TestContentView(
    uiState: TestUiState,
    onOptionSelect: (Int) -> Unit,
    onCheckClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val currentQuestion = uiState.questions[uiState.currentQuestionIndex]

    // Efecto para hacer scroll automático a la explicación cuando aparece
    LaunchedEffect(uiState.isAnswerChecked) {
        if (uiState.isAnswerChecked) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        } else {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Question Content
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = currentQuestion.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
            }

            if (currentQuestion.imageUrl != null) {
                item {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentQuestion.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagen de la pregunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            // Once checked, the explanation card below says the same thing, so the hint steps aside.
            if (uiState.isHintVisible && !uiState.isAnswerChecked) {
                item(key = HINT_ITEM_KEY) {
                    HintCard(
                        explanation = currentQuestion.explanation,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Options the 50/50 removed fade out and the rest slide together, instead of jumping.
            val visibleOptions = currentQuestion.options.withIndex()
                .filter { it.index !in uiState.eliminatedOptionIndices }
            items(visibleOptions, key = { it.index }) { (optionIndex, option) ->
                val isSelected = uiState.selectedAnswers[uiState.currentQuestionIndex] == optionIndex
                val isCorrect = currentQuestion.correctAnswerIndex == optionIndex

                OptionCard(
                    modifier = Modifier.animateItem(),
                    text = option,
                    isSelected = isSelected,
                    isCorrect = if (uiState.isAnswerChecked) isCorrect else null,
                    wasSelectedAndIncorrect = if (uiState.isAnswerChecked) isSelected && !isCorrect else false,
                    onClick = { if (!uiState.isAnswerChecked) onOptionSelect(optionIndex) }
                )
            }

            if (uiState.isAnswerChecked) {
                item {
                    ExplanationCard(
                        isCorrect = uiState.selectedAnswers[uiState.currentQuestionIndex] == currentQuestion.correctAnswerIndex,
                        explanation = currentQuestion.explanation
                    )
                }
            }
        }

        // Navigation Buttons (Fixed at bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            if (!uiState.isAnswerChecked) {
                Button(
                    onClick = onCheckClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = uiState.selectedAnswers.containsKey(uiState.currentQuestionIndex)
                ) {
                    Text("Comprobar")
                }
            } else {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val isLast = uiState.currentQuestionIndex == uiState.questions.size - 1
                    Text(if (isLast) "Finalizar práctica" else "Siguiente")
                }
            }
        }
    }
}

/** Peak scale of the hop a correct answer makes when it is checked. */
private const val CORRECT_HOP_SCALE = 1.04f

/** Extra green the card flashes with when a correct answer is checked, fading back in 600 ms. */
private const val CORRECT_FLASH_ALPHA = 0.22f

/** Lazy-list key of the hint card, a string so it never collides with the options' index keys. */
private const val HINT_ITEM_KEY = "hint"

/** Gap between the bottom of the quiz content and Bali's cheer: clears the pinned button. */
private val CHEER_ABOVE_BUTTON_PADDING = 92.dp

/** Opacity of an aid chip that cannot be used right now (answer already checked). */
private const val AID_DISABLED_ALPHA = 0.4f

/**
 * The practice aids as two compact chips in the top bar, each with its shop picture and how many
 * the user owns, so they are at hand without pushing the question down. A chip only shows while
 * the user owns that aid or is using it on this question; an aid in use stays highlighted until
 * the next question, and both dim once the answer is checked.
 *
 * @param hints hints the user owns
 * @param fiftyFifties 50/50 aids the user owns
 * @param isHintVisible true when a hint is already revealed for this question
 * @param isFiftyFiftyUsed true when a 50/50 already removed options from this question
 * @param isAnswerChecked true once the current answer has been checked
 * @param onUseHint spends a hint to reveal the explanation before answering
 * @param onUseFiftyFifty spends a 50/50 to remove incorrect options
 */
@Composable
fun PracticeAidChips(
    hints: Int,
    fiftyFifties: Int,
    isHintVisible: Boolean,
    isFiftyFiftyUsed: Boolean,
    isAnswerChecked: Boolean,
    onUseHint: () -> Unit,
    onUseFiftyFifty: () -> Unit
) {
    Row(
        modifier = Modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hints > 0 || isHintVisible) {
            AidChip(
                imageRes = R.drawable.shop_hint,
                name = "pista",
                count = hints,
                isInUse = isHintVisible,
                enabled = hints > 0 && !isHintVisible && !isAnswerChecked,
                isDimmed = isAnswerChecked,
                onClick = onUseHint
            )
        }
        if (fiftyFifties > 0 || isFiftyFiftyUsed) {
            AidChip(
                imageRes = R.drawable.shop_fifty_fifty,
                name = "50/50",
                count = fiftyFifties,
                isInUse = isFiftyFiftyUsed,
                enabled = fiftyFifties > 0 && !isFiftyFiftyUsed && !isAnswerChecked,
                isDimmed = isAnswerChecked,
                onClick = onUseFiftyFifty
            )
        }
    }
}

/**
 * One aid chip: the aid's picture and the number owned. Visually 32dp tall, but its touch target
 * is the full 48dp the clickable [Surface] guarantees.
 *
 * @param imageRes the aid's shop picture
 * @param name the aid's name as TalkBack reads it
 * @param count how many the user owns
 * @param isInUse true while the aid is active on this question; the chip is highlighted
 * @param enabled whether tapping spends one now
 * @param isDimmed true to fade the chip because aids no longer apply to this question
 * @param onClick spends one
 */
@Composable
private fun AidChip(
    @DrawableRes imageRes: Int,
    name: String,
    count: Int,
    isInUse: Boolean,
    enabled: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    val description = if (isInUse) "$name en uso" else "Usar $name, te quedan $count"
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (isInUse) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isInUse) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .graphicsLayer { alpha = if (isDimmed) AID_DISABLED_ALPHA else 1f }
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier.height(32.dp).padding(start = 4.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                },
                label = "aidCount"
            ) { shown ->
                Text(
                    text = shown.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isInUse) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The revealed hint: the bulb from the shop beside the question's explanation.
 *
 * @param explanation the text the hint reveals
 * @param modifier layout modifier, used for the list's appear animation
 */
@Composable
private fun HintCard(explanation: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BaliAccentYellow.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, BaliAccentYellow.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.shop_hint),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "Pista",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Selectable answer option that reflects the check result once the answer has been verified.
 *
 * When the user's own choice turns out correct, the card makes a short hop, flashes green and
 * pops its tick, at the moment the right-answer sound plays. Only that flip celebrates: a card
 * that first appears already answered (after a rotation or when scrolled back into view) stays
 * still, and a wrong answer gets no animation at all.
 *
 * @param modifier layout modifier applied to the card
 * @param text answer text
 * @param isSelected true when this option is the user's current choice
 * @param isCorrect null before checking; afterwards whether this option is the correct one
 * @param wasSelectedAndIncorrect true when the user picked this option and it was wrong
 * @param onClick invoked when the card is tapped
 */
@Composable
fun OptionCard(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    wasSelectedAndIncorrect: Boolean,
    onClick: () -> Unit
) {
    val celebrate = isSelected && isCorrect == true
    val hop = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val tickScale = remember { Animatable(1f) }
    val wasCelebrating = remember { mutableStateOf(celebrate) }

    LaunchedEffect(celebrate) {
        val flippedToCorrect = celebrate && !wasCelebrating.value
        wasCelebrating.value = celebrate
        if (!flippedToCorrect) return@LaunchedEffect
        launch {
            hop.animateTo(CORRECT_HOP_SCALE, tween(durationMillis = 110, easing = FastOutSlowInEasing))
            hop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
        launch {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(durationMillis = 600))
        }
        launch {
            tickScale.snapTo(0.4f)
            tickScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
    }

    val borderColor = when {
        isCorrect == true -> BaliAccentGreen
        wasSelectedAndIncorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = when {
        isCorrect == true -> BaliAccentGreen.copy(alpha = 0.1f + CORRECT_FLASH_ALPHA * flash.value)
        wasSelectedAndIncorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = hop.value
                scaleY = hop.value
            },
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(width = if (isSelected || isCorrect != null) 2.dp else 1.dp, color = borderColor),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isMarked = isSelected || isCorrect == true
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (isMarked) borderColor else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .background(
                        color = if (isMarked) borderColor else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isMarked) {
                    Icon(
                        imageVector = if (wasSelectedAndIncorrect) Icons.Rounded.Close else Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                scaleX = tickScale.value
                                scaleY = tickScale.value
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (wasSelectedAndIncorrect) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Feedback card shown after checking an answer: correct/incorrect header plus the explanation.
 *
 * @param isCorrect whether the user's answer was correct
 * @param explanation AI-provided explanation of the correct answer
 */
@Composable
fun ExplanationCard(isCorrect: Boolean, explanation: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isCorrect) BaliAccentGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (isCorrect) BaliAccentGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (isCorrect) BaliAccentGreen else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isCorrect) "¡Correcto!" else "No es correcto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) BaliAccentGreen else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }
}
