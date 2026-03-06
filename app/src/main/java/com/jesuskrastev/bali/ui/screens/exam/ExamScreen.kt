package com.jesuskrastev.bali.ui.screens.exam

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.test.ErrorView
import com.jesuskrastev.bali.ui.screens.test.LoadingView
import com.jesuskrastev.bali.ui.screens.test.OptionCard
import com.jesuskrastev.bali.ui.screens.test.TestSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    onBackClick: () -> Unit,
    onFinishExam: (TestSummary) -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

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
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = formatTime(uiState.timeLeftSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.timeLeftSeconds < 300) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Salir")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ExamEvent.ToggleQuestionReview) }) {
                        Icon(Icons.Rounded.GridView, contentDescription = "Ver todas las preguntas")
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.questions.isNotEmpty()) {
                ExamBottomBar(
                    currentIndex = uiState.currentQuestionIndex,
                    totalCount = uiState.questions.size,
                    isAnswerChecked = uiState.isAnswerChecked,
                    onCheck = { viewModel.onEvent(ExamEvent.CheckAnswer) },
                    onPrevious = { viewModel.onEvent(ExamEvent.PreviousQuestion) },
                    onNext = { viewModel.onEvent(ExamEvent.NextQuestion) },
                    onFinish = {
                        viewModel.onEvent(ExamEvent.FinishExam { result ->
                            onFinishExam(result)
                        })
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(
                    message = uiState.error ?: "Error al cargar el examen",
                    onRetry = { viewModel.onEvent(ExamEvent.Retry) }
                )
                else -> {
                    ExamContent(
                        uiState = uiState,
                        onOptionSelect = { viewModel.onEvent(ExamEvent.SelectOption(it)) }
                    )
                }
            }

            if (uiState.showReviewGrid) {
                QuestionReviewGrid(
                    questionsCount = uiState.questions.size,
                    selectedAnswers = uiState.selectedAnswers,
                    currentIndex = uiState.currentQuestionIndex,
                    onQuestionClick = { viewModel.onEvent(ExamEvent.GoToQuestion(it)) },
                    onDismiss = { viewModel.onEvent(ExamEvent.ToggleQuestionReview) }
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Abandonar examen?") },
            text = { Text("Si sales ahora, perderás todo el progreso de este examen.") },
            confirmButton = {
                TextButton(onClick = onBackClick) { Text("SALIR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("CONTINUAR") }
            }
        )
    }
}

@Composable
fun ExamContent(
    uiState: ExamUiState,
    onOptionSelect: (Int) -> Unit
) {
    val currentQuestion = uiState.questions[uiState.currentQuestionIndex]
    val selectedOption = uiState.selectedAnswers[uiState.currentQuestionIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "Pregunta ${uiState.currentQuestionIndex + 1} de ${uiState.questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            itemsIndexed(currentQuestion.options) { index, option ->
                val isCorrect = currentQuestion.correctAnswerIndex == index
                
                OptionCard(
                    text = option,
                    isSelected = selectedOption == index,
                    isCorrect = if (uiState.isAnswerChecked) isCorrect else null,
                    wasSelectedAndIncorrect = if (uiState.isAnswerChecked) selectedOption == index && !isCorrect else false,
                    onClick = { if (!uiState.isAnswerChecked) onOptionSelect(index) }
                )
            }
        }
    }
}

@Composable
fun ExamBottomBar(
    currentIndex: Int,
    totalCount: Int,
    isAnswerChecked: Boolean,
    onCheck: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isAnswerChecked) {
                Button(
                    onClick = onCheck,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("COMPROBAR", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.ArrowBack, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anterior")
                }

                if (currentIndex == totalCount - 1) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("FINALIZAR", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Siguiente")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Rounded.ArrowForward, null)
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionReviewGrid(
    questionsCount: Int,
    selectedAnswers: Map<Int, Int>,
    currentIndex: Int,
    onQuestionClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Revisión de preguntas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, null)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(questionsCount) { index ->
                    val isAnswered = selectedAnswers.containsKey(index)
                    val isCurrent = currentIndex == index
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isAnswered -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { onQuestionClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.onPrimary
                                isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
