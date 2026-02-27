package com.jesuskrastev.bali.ui.features.test

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    onBackClick: () -> Unit,
    onFinishTest: (Int, Int) -> Unit,
    viewModel: TestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.category, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (!uiState.isLoading && uiState.questions.isNotEmpty()) {
                        AnimatedContent(
                            targetState = uiState.currentQuestionIndex,
                            transitionSpec = {
                                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                            },
                            label = "counter"
                        ) { index ->
                            Text(
                                text = "${index + 1}/${uiState.questions.size}",
                                modifier = Modifier.padding(end = 16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "screen_state"
            ) { state ->
                when {
                    state.isLoading -> LoadingView()
                    state.error != null -> ErrorView(
                        message = state.error ?: "Ocurrió un error inesperado",
                        onRetry = { viewModel.retry() }
                    )
                    state.questions.isEmpty() -> ErrorView(
                        message = "No se pudieron generar las preguntas. Por favor, inténtalo de nuevo.",
                        onRetry = { viewModel.retry() }
                    )
                    else -> {
                        TestContentView(
                            uiState = state,
                            onOptionSelect = { viewModel.selectOption(it) },
                            onPreviousClick = { viewModel.previousQuestion() },
                            onNextClick = {
                                if (state.currentQuestionIndex == state.questions.size - 1) {
                                    val result = viewModel.calculateResult()
                                    onFinishTest(result.score, result.total)
                                } else {
                                    viewModel.nextQuestion()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round
            )
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Bali está diseñando tu práctica...",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Text(
            "Preparando preguntas específicas para ti",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

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

@Composable
fun TestContentView(
    uiState: TestUiState,
    onOptionSelect: (Int) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Animated Progress Bar
        val animatedProgress by animateFloatAsState(
            targetValue = (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
            label = "progress"
        )
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            strokeCap = StrokeCap.Round
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Question Content with Slide Animation
        AnimatedContent(
            targetState = uiState.currentQuestionIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "question_slide",
            modifier = Modifier.weight(1f)
        ) { index ->
            val currentQuestion = uiState.questions[index]
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                            model = currentQuestion.imageUrl,
                            contentDescription = "Imagen de la pregunta",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                itemsIndexed(currentQuestion.options) { optionIndex, option ->
                    val isSelected = uiState.selectedAnswers[index] == optionIndex
                    OptionCard(
                        text = option,
                        isSelected = isSelected,
                        onClick = { onOptionSelect(optionIndex) }
                    )
                }
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (uiState.currentQuestionIndex > 0) {
                OutlinedButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.height(56.dp).weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Anterior")
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Button(
                onClick = onNextClick,
                modifier = Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.selectedAnswers.containsKey(uiState.currentQuestionIndex)
            ) {
                Text(if (uiState.currentQuestionIndex == uiState.questions.size - 1) "Finalizar" else "Siguiente")
            }
        }
    }
}

@Composable
fun OptionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "color"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    scaleIn() togetherWith scaleOut()
                },
                label = "check_anim"
            ) { selected ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
