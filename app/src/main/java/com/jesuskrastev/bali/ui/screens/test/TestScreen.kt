package com.jesuskrastev.bali.ui.screens.test

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen

/**
 * Title content shared by the quiz top bars (practice, mistakes review, exam): an optional
 * "N SEGUIDAS" streak label above a rounded progress bar, followed by any [footer] content.
 *
 * @param currentIndex zero-based index of the question being answered
 * @param totalQuestions number of questions in the session; must be greater than zero
 * @param sessionStreak consecutive correct answers in this session
 * @param isAnswerChecked true once the current answer has been checked; the streak label only
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (sessionStreak >= 2 && isAnswerChecked) {
            Text(
                text = "$sessionStreak SEGUIDAS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
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
                        onUseHint = { viewModel.onEvent(TestEvent.UseHint) },
                        onUseFiftyFifty = { viewModel.onEvent(TestEvent.UseFiftyFifty) },
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
                }
            }
        }
    }
}

/** Full-screen loading state shown while the AI prepares the questions: a bobbing mascot and a spinner. */
@Composable
fun LoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val translateY by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bali),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = translateY
                    }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Estoy preparando un test para ti...",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Solo tomará unos segundos",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.7f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.5.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
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
 * @param onUseHint consumes a hint to reveal the explanation before answering
 * @param onUseFiftyFifty consumes a 50/50 aid to hide incorrect options
 * @param onNextClick invoked when the "Siguiente" / "Finalizar práctica" button is tapped
 */
@Composable
fun TestContentView(
    uiState: TestUiState,
    onOptionSelect: (Int) -> Unit,
    onCheckClick: () -> Unit,
    onUseHint: () -> Unit,
    onUseFiftyFifty: () -> Unit,
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

            if (!uiState.isAnswerChecked) {
                item {
                    PracticeAids(
                        hints = uiState.hints,
                        fiftyFifties = uiState.fiftyFifties,
                        isHintVisible = uiState.isHintVisible,
                        onUseHint = onUseHint,
                        onUseFiftyFifty = onUseFiftyFifty
                    )
                }
            }

            if (uiState.isHintVisible) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "Pista: ${currentQuestion.explanation}",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            itemsIndexed(currentQuestion.options, key = { index, _ -> index }) { optionIndex, option ->
                if (optionIndex in uiState.eliminatedOptionIndices) return@itemsIndexed
                val isSelected = uiState.selectedAnswers[uiState.currentQuestionIndex] == optionIndex
                val isCorrect = currentQuestion.correctAnswerIndex == optionIndex

                OptionCard(
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

/** Shows the consumable practice aids that are valid before the current answer is checked. */
@Composable
private fun PracticeAids(
    hints: Int,
    fiftyFifties: Int,
    isHintVisible: Boolean,
    onUseHint: () -> Unit,
    onUseFiftyFifty: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onUseHint,
            enabled = hints > 0 && !isHintVisible,
            modifier = Modifier.weight(1f)
        ) {
            Text("Pista ($hints)")
        }
        OutlinedButton(
            onClick = onUseFiftyFifty,
            enabled = fiftyFifties > 0,
            modifier = Modifier.weight(1f)
        ) {
            Text("50/50 ($fiftyFifties)")
        }
    }
}

/**
 * Selectable answer option that reflects the check result once the answer has been verified.
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
    val borderColor = when {
        isCorrect == true -> BaliAccentGreen
        wasSelectedAndIncorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = when {
        isCorrect == true -> BaliAccentGreen.copy(alpha = 0.1f)
        wasSelectedAndIncorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(width = if (isSelected || isCorrect != null) 2.dp else 1.dp, color = borderColor),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected || isCorrect == true) borderColor else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .background(
                        color = if (isSelected || isCorrect == true) borderColor else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected || isCorrect == true) {
                    Icon(
                        imageVector = if (wasSelectedAndIncorrect) Icons.Rounded.Close else Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
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
