package com.jesuskrastev.bali.ui.screens.streak

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus
import com.jesuskrastev.bali.ui.theme.BaliBackgroundGradient

/**
 * Celebration screen shown right after a lesson or test is finished.
 *
 * It stacks a full-screen confetti animation over a hero with the happy Bali mascot, the weekly
 * progress towards the user's goal and the day-by-day streak tracker.
 *
 * @param viewModel Supplies the weekly streak, goal and current streak state.
 * @param newWeekSessions Sessions completed this week including the one just finished.
 * @param onContinueClick Invoked when the user taps the continue button.
 */
@Composable
fun LessonStreakScreen(
    viewModel: LessonStreakViewModel,
    newWeekSessions: Int,
    onContinueClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val weeklyGoal = uiState.weeklyGoal.coerceAtLeast(1)
    val isGoalReached = newWeekSessions >= weeklyGoal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = BaliBackgroundGradient())
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Button(
                    onClick = onContinueClick,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "CONTINUAR",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    CelebrationHeadline(
                        newWeekSessions = newWeekSessions,
                        isGoalReached = isGoalReached
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CelebrationHero(currentStreak = uiState.currentStreak)

                    Spacer(modifier = Modifier.height(8.dp))

                    RevealOnAppear(delayMillis = 150) {
                        WeeklyProgressCard(
                            sessions = newWeekSessions,
                            goal = weeklyGoal,
                            currentStreak = uiState.currentStreak
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RevealOnAppear(delayMillis = 300) {
                        WeeklyTracker(
                            weeklyStreak = uiState.weeklyStreak,
                            streakFreezes = uiState.streakFreezes
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        ConfettiOverlay()
    }
}

/**
 * Full-screen looping confetti animation rendered above the rest of the content.
 *
 * Drawn last so the particles fall in front of the mascot; it never intercepts touches.
 */
@Composable
private fun ConfettiOverlay() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

/**
 * Eyebrow + title pair announcing what the user just achieved.
 *
 * @param newWeekSessions Sessions completed this week, used to detect the very first one.
 * @param isGoalReached Whether the weekly goal has just been met or exceeded.
 */
@Composable
private fun CelebrationHeadline(
    newWeekSessions: Int,
    isGoalReached: Boolean
) {
    val eyebrow = when {
        newWeekSessions == 1 -> "PRIMERA SESIÓN"
        isGoalReached -> "SEMANA COMPLETADA"
        else -> "SESIÓN COMPLETADA"
    }
    val title = when {
        newWeekSessions == 1 -> "¡Has empezado!"
        isGoalReached -> "¡Objetivo cumplido!"
        else -> "¡Buen trabajo!"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Hero block with a breathing radial glow, the happy Bali mascot and a streak badge.
 *
 * @param currentStreak Weeks the user has kept the streak alive; the badge is hidden when zero.
 */
@Composable
private fun CelebrationHero(currentStreak: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.6f, animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(400))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(breathe)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.happy_bali),
                    contentDescription = "Bali celebrando la sesión completada",
                    modifier = Modifier
                        .size(170.dp)
                        .scale(breathe)
                )
            }

            if (currentStreak > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                StreakBadge(currentStreak = currentStreak)
            }
        }
    }
}

/**
 * Pill showing the number of consecutive weeks the streak has been kept.
 *
 * @param currentStreak Weeks in a row; expected to be greater than zero.
 */
@Composable
private fun StreakBadge(currentStreak: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (currentStreak == 1) "1 semana de racha" else "$currentStreak semanas de racha",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Card with the weekly session count, an animated progress bar and a motivational line.
 *
 * @param sessions Sessions completed this week.
 * @param goal Weekly session goal; always at least 1.
 * @param currentStreak Weeks in a row, used in the copy when the goal is reached.
 */
@Composable
private fun WeeklyProgressCard(
    sessions: Int,
    goal: Int,
    currentStreak: Int
) {
    val fraction = (sessions.toFloat() / goal).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(900, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val remaining = (goal - sessions).coerceAtLeast(0)

    val message = when {
        remaining == 0 && currentStreak > 0 -> "Tu racha sigue viva. Nos vemos la semana que viene."
        remaining == 0 -> "Has cumplido tu objetivo semanal. ¡Sigue así!"
        remaining == 1 -> "Solo te queda 1 sesión para completar la semana."
        else -> "Te quedan $remaining sesiones para completar la semana."
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$sessions",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " / $goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = "sesiones esta semana",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Day-by-day tracker for the current week, plus the remaining streak freezes.
 *
 * @param weeklyStreak One entry per day of the week, in display order.
 * @param streakFreezes Freezes the user still has available; the row is hidden when zero.
 */
@Composable
private fun WeeklyTracker(
    weeklyStreak: List<DailyStreakState>,
    streakFreezes: Int
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weeklyStreak.forEach { day ->
                    StreakDayItem(state = day)
                }
            }

            if (streakFreezes > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AcUnit,
                        contentDescription = null,
                        tint = FrozenDayColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (streakFreezes == 1) {
                            "Te queda 1 congelador de racha"
                        } else {
                            "Te quedan $streakFreezes congeladores de racha"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Blue used for days saved by a streak freeze. */
private val FrozenDayColor = Color(0xFF2196F3)

/**
 * Single day cell of the weekly tracker.
 *
 * @param state Status and label of the day to render.
 */
@Composable
private fun StreakDayItem(state: DailyStreakState) {
    val isCompleted = state.status == StreakStatus.COMPLETED
    val isFrozen = state.status == StreakStatus.FROZEN
    val isToday = state.isToday

    val circleColor = when (state.status) {
        StreakStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        StreakStatus.FROZEN -> FrozenDayColor
        StreakStatus.TODAY -> MaterialTheme.colorScheme.surfaceVariant
        StreakStatus.FAILED, StreakStatus.FUTURE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = state.dayOfWeek,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            }
        )

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(circleColor)
                .then(
                    if (isToday && !isCompleted) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )

                isFrozen -> Icon(
                    imageVector = Icons.Rounded.AcUnit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                state.status == StreakStatus.TODAY -> Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Fades and slides its content up shortly after first composition, for a staggered reveal.
 *
 * @param delayMillis Delay before the entrance animation starts.
 * @param content Content to reveal.
 */
@Composable
private fun RevealOnAppear(
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400, delayMillis = delayMillis)) +
                slideInVertically(
                    animationSpec = tween(400, delayMillis = delayMillis, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 3 }
                )
    ) {
        content()
    }
}
