package com.jesuskrastev.bali.ui.screens.onboarding

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.ui.screens.onboarding.components.MascotHeader
import com.onesignal.OneSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OnboardingScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: OnboardingViewModel,
    onComplete: (OnboardingData) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == OnboardingStep.Completed) {
            onComplete(uiState.data)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { OnboardingTopBar(uiState.progress, visible = uiState.currentStep != OnboardingStep.Processing && uiState.currentStep != OnboardingStep.Completed) },
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
    viewModel: OnboardingViewModel
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
                        OneSignal.Notifications.requestPermission(false)
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

        OnboardingStepContent(uiState, viewModel)
    }
}

@Composable
private fun OnboardingStepContent(state: OnboardingUiState, viewModel: OnboardingViewModel) {
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
            OnboardingStep.License -> StepSelectorList(OnboardingConfig.licenses) { viewModel.onEvent(OnboardingEvent.SelectLicense(it)) }
            OnboardingStep.Experience -> StepSelectorList(OnboardingConfig.experiences) { viewModel.onEvent(OnboardingEvent.SelectExperience(it)) }
            OnboardingStep.Reasons -> StepMultiSelectorList(OnboardingConfig.reasons, state.data.reasons) { viewModel.onEvent(OnboardingEvent.ToggleReason(it)) }
            OnboardingStep.ExamDate -> StepExamDate(state.data.examDate, viewModel)
            OnboardingStep.DailyGoal -> StepSelectorList(OnboardingConfig.dailyGoals) { viewModel.onEvent(OnboardingEvent.SelectDailyGoal(it)) }
            OnboardingStep.LearningPreference -> StepSelectorList(OnboardingConfig.learningPreferences) { viewModel.onEvent(OnboardingEvent.SelectLearningPreference(it)) }
            OnboardingStep.DifficultTopics -> StepMultiSelectorList(OnboardingConfig.difficultTopics, state.data.difficultTopics) { viewModel.onEvent(OnboardingEvent.ToggleDifficultTopic(it)) }
            OnboardingStep.Concern -> StepSelectorList(OnboardingConfig.concerns) { viewModel.onEvent(OnboardingEvent.SelectConcern(it)) }
            OnboardingStep.StudyTime -> StepSelectorList(OnboardingConfig.studyTimes) { viewModel.onEvent(OnboardingEvent.SelectStudyTime(it)) }
            OnboardingStep.Notifications -> StepNotifications(viewModel)
            OnboardingStep.Processing -> StepProcessing(
                progress = state.processingProgress
            )
            OnboardingStep.Comparison -> StepComparison()
            OnboardingStep.Pact -> StepPact { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
            is OnboardingStep.DialogueExperience, is OnboardingStep.DialogueDifficultTopics -> Box(Modifier.fillMaxSize())
            else -> Unit
        }
    }
}

@Composable
fun StepName(name: String, viewModel: OnboardingViewModel) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.onEvent(OnboardingEvent.SetName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu nombre") },
            placeholder = { Text("Ej. María") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (name.isNotBlank()) {
                        viewModel.onEvent(OnboardingEvent.GoToNextStep)
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Solo lo usaremos para personalizar tu experiencia.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StepSelectorList(options: List<String>, onSelect: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(options) { option ->
            SingleSelectionCard(text = option, isSelected = false, onClick = { onSelect(option) })
        }
    }
}

@Composable
fun StepMultiSelectorList(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(options) { option ->
            MultiSelectionCard(text = option, isSelected = selected.contains(option), onToggle = { onToggle(option) })
        }
    }
}

@Composable
fun SmoothProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepExamDate(selectedDate: Long?, viewModel: OnboardingViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateText = remember(selectedDate) {
        if (selectedDate != null) {
            SimpleDateFormat("dd 'de' MMMM yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(selectedDate))
        } else "Seleccionar fecha"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleSelectionCard(text = dateText, isSelected = selectedDate != null, onClick = { showDatePicker = true }, iconEmoji = "📅")
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { viewModel.onEvent(OnboardingEvent.SelectExamDate(null)) }) {
            Text("Todavía no tengo fecha", color = MaterialTheme.colorScheme.outline)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis > System.currentTimeMillis()
        })
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    viewModel.onEvent(OnboardingEvent.SelectExamDate(datePickerState.selectedDateMillis))
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun StepNotifications(viewModel: OnboardingViewModel) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable {
                    scope.launch {
                        OneSignal.Notifications.requestPermission(false)
                        viewModel.onEvent(OnboardingEvent.GoToNextStep)
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permitir notificaciones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Impulsa tu aprobado con recordatorios inteligentes.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(contentAlignment = Alignment.TopCenter) {
                    Button(
                        onClick = {
                            scope.launch {
                                OneSignal.Notifications.requestPermission(false)
                                viewModel.onEvent(OnboardingEvent.GoToNextStep)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Activar avisos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    PointingFingerEmoji()
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { viewModel.onEvent(OnboardingEvent.GoToNextStep) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ahora no", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SocialProofBadge()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PointingFingerEmoji() {
    val infiniteTransition = rememberInfiniteTransition(label = "finger_bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -55f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Text(
        text = "👇",
        fontSize = 40.sp,
        modifier = Modifier.offset(y = offsetY.dp)
    )
}

@Composable
private fun SocialProofBadge() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💡", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Dato: Los alumnos con notificaciones tienen un 85% más de probabilidad de aprobar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun StepProcessing(progress: Float) {
    val processingTasks = listOf(
        "Analizando tu perfil y experiencia previa...",
        "Identificando patrones en tus temas difíciles...",
        "Optimizando tu horario de estudio personalizado...",
        "Generando estrategia inteligente para tu carnet...",
        "Finalizando tu plan de estudio a medida..."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Construyendo tu plan...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        processingTasks.forEachIndexed { index, task ->
            val stepProgress = (index + 1).toFloat() / processingTasks.size
            val isCompleted = progress >= stepProgress
            val isActive = progress >= (index.toFloat() / processingTasks.size) && !isCompleted

            ProcessingTaskRow(
                text = task,
                isCompleted = isCompleted,
                isActive = isActive
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProcessingTaskRow(
    text: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive || isCompleted) 1f else 0.3f,
        animationSpec = tween(500),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) Color(0xFF4CAF50)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StepComparison() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Tu ventaja competitiva",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Sin Bali
            Column(modifier = Modifier.weight(1f)) {
                ComparisonHeader(title = "Sin Bali", color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                ComparisonHeaderItem(text = "Libros aburridos", icon = "📚", isNegative = true)
                ComparisonHeaderItem(text = "Dudas sin resolver", icon = "❓", isNegative = true)
                ComparisonHeaderItem(text = "Estudiar lo que no sale", icon = "📉", isNegative = true)
                ComparisonHeaderItem(text = "Soledad total", icon = "👤", isNegative = true)
            }

            // Con Bali
            Column(modifier = Modifier.weight(1f)) {
                ComparisonHeader(title = "Con Bali", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                ComparisonHeaderItem(text = "Plan Inteligente", icon = "🎯", isNegative = false)
                ComparisonHeaderItem(text = "Respuestas al instante", icon = "⚡", isNegative = false)
                ComparisonHeaderItem(text = "Foco en el examen", icon = "🔥", isNegative = false)
                ComparisonHeaderItem(text = "Copiloto 24/7", icon = "🤖", isNegative = false)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Bali analiza tu perfil para que apruebes en tiempo récord.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ComparisonHeader(title: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ComparisonHeaderItem(text: String, icon: String, isNegative: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isNegative) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StepPact(onComplete: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100),
        label = "pact_progress"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Me comprometo a usar Bali para asegurar mi aprobado",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(64.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val startTime = System.currentTimeMillis()
                            val duration = 2500L
                            val job = scope.launch {
                                while (progress < 1f) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                                    if (progress % 0.1f < 0.01f) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                    delay(16)
                                }
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onComplete()
                            }
                            try {
                                awaitRelease()
                            } finally {
                                job.cancel()
                                if (progress < 1f) progress = 0f
                            }
                        }
                    )
                }
        ) {
            // Background Circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    style = Stroke(width = 8.dp.toPx())
                )
            }
            
            // Progress Circle
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round,
                trackColor = Color.Transparent
            )

            // Fingerprint Icon
            Surface(
                shape = CircleShape,
                color = if (progress > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(120.dp),
                shadowElevation = if (progress > 0f) 8.dp else 2.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                        .scale(if (progress > 0f) 1.1f else 1f),
                    tint = if (progress > 0f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = if (progress > 0f) "Mantén pulsado..." else "Pulsa y mantén para firmar",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SingleSelectionCard(text: String, isSelected: Boolean, onClick: () -> Unit, iconEmoji: String? = null) {
    val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, label = "bg")
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), label = "border")
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (iconEmoji != null) { Text(text = iconEmoji, fontSize = 20.sp); Spacer(modifier = Modifier.width(8.dp)) }
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun MultiSelectionCard(text: String, isSelected: Boolean, onToggle: () -> Unit) {
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), label = "border")
    Surface(
        modifier = Modifier.fillMaxWidth().height(64.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggle() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

fun shouldShowBottomButton(step: OnboardingStep): Boolean = when (step) {
    OnboardingStep.Name, OnboardingStep.DialogueExperience, OnboardingStep.Reasons,
    OnboardingStep.DifficultTopics, OnboardingStep.DialogueDifficultTopics,
    OnboardingStep.Notifications, OnboardingStep.Comparison -> true
    else -> false
}

fun getButtonText(step: OnboardingStep): String = when (step) {
    OnboardingStep.Comparison -> "Entendido"
    OnboardingStep.Notifications -> "Activar recordatorios"
    else -> "Continuar"
}
