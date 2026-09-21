package com.jesuskrastev.bali.ui.screens.mistakes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.ui.screens.test.ErrorView
import com.jesuskrastev.bali.ui.screens.test.LoadingView
import com.jesuskrastev.bali.ui.screens.test.TestContentView
import com.jesuskrastev.bali.ui.screens.test.TestUiState
import com.jesuskrastev.bali.ui.screens.test.TestSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(
    onBackClick: () -> Unit,
    onFinishTest: (TestSummary) -> Unit,
    viewModel: MistakesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading && uiState.questions.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (uiState.sessionStreak >= 2 && uiState.isAnswerChecked) {
                                Text(
                                    text = "${uiState.sessionStreak} SEGUIDAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            LinearProgressIndicator(
                                progress = { (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
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
                    onRetry = { viewModel.onEvent(MistakesEvent.Retry) }
                )
                uiState.questions.isEmpty() -> ErrorView(
                    message = "No se pudieron generar las preguntas de repaso.",
                    onRetry = { viewModel.onEvent(MistakesEvent.Retry) }
                )
                else -> {
                    TestContentView(
                        uiState = TestUiState(
                            category = "Repaso de Fallos",
                            questions = uiState.questions,
                            currentQuestionIndex = uiState.currentQuestionIndex,
                            selectedAnswers = uiState.selectedAnswers,
                            isAnswerChecked = uiState.isAnswerChecked,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            sessionStreak = uiState.sessionStreak
                        ),
                        onOptionSelect = { viewModel.onEvent(MistakesEvent.SelectOption(it)) },
                        onCheckClick = { viewModel.onEvent(MistakesEvent.CheckAnswer) },
                        onNextClick = {
                            if (uiState.currentQuestionIndex == uiState.questions.size - 1) {
                                viewModel.onEvent(MistakesEvent.FinishReview { result ->
                                    onFinishTest(result)
                                })
                            } else {
                                viewModel.onEvent(MistakesEvent.NextQuestion)
                            }
                        }
                    )
                }
            }
        }
    }
}
