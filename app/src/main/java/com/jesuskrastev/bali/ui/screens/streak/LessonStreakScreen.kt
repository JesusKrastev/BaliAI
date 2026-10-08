package com.jesuskrastev.bali.ui.screens.streak

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.onboarding.components.SpeechBubbleShape

/**
 * Celebration shown after the first session of the day, the one that extends the streak.
 *
 * @param viewModel supplies the streak, already including today
 * @param onContinueClick invoked by the continue button
 */
@Composable
fun LessonStreakScreen(
    viewModel: StreakViewModel,
    onContinueClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LessonStreakContent(uiState = uiState, onContinueClick = onContinueClick)
}

/**
 * Stateless body of [LessonStreakScreen]: the flame with the new count, the week, and the
 * mascot telling the user when to come back. On a milestone day (see [isStreakMilestone]) the
 * flame and the confetti give way to the bigger [MilestoneFlame] celebration, and the confetti
 * plays once instead of looping.
 *
 * @param uiState the streak to celebrate
 * @param onContinueClick invoked by the continue button
 */
@Composable
fun LessonStreakContent(
    uiState: StreakUiState,
    onContinueClick: () -> Unit
) {
    val isMilestone = !uiState.isLoading && isStreakMilestone(uiState.currentStreak)
    // Saved, so a rotation shows the finished milestone instead of replaying it.
    var milestonePlayed by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Button(
                    onClick = onContinueClick,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Continuar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                if (isMilestone) {
                    MilestoneFlame(
                        days = uiState.currentStreak,
                        played = milestonePlayed,
                        onPlayed = { milestonePlayed = true }
                    )
                } else {
                    CelebrationFlame(days = uiState.currentStreak)
                }
                Spacer(modifier = Modifier.height(20.dp))
                CelebrationHeadline(current = uiState.currentStreak, highest = uiState.highestStreak)
                Spacer(modifier = Modifier.height(24.dp))

                RevealOnAppear(delayMillis = 200) {
                    StreakCard {
                        StreakWeek(uiState.week)
                        if (uiState.streakFreezes > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FreezesLeft(freezes = uiState.streakFreezes)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                RevealOnAppear(delayMillis = 400) {
                    MascotTip(text = "Con un test al día basta para que no se apague la llama 🔥")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // A milestone has its own, bigger celebration, so the confetti plays once and stops.
        ConfettiOverlay(loop = !isMilestone)
    }
}

/**
 * The flame on a breathing glow, with the new day count under it.
 *
 * @param days the streak including today
 */
@Composable
private fun CelebrationFlame(days: Int) {
    val breathe by rememberInfiniteTransition(label = "flame").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.5f, animationSpec = tween(500, easing = FastOutSlowInEasing)) +
            fadeIn(animationSpec = tween(400))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(breathe)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                StreakFlame(lit = true, size = 120.dp, modifier = Modifier.scale(breathe))
            }
            Text(
                text = days.toString(),
                fontSize = 72.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${daysWord(days)} seguidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Title and subtitle for the new count; a milestone gets its own wording.
 *
 * @param current the streak including today
 * @param highest the longest streak ever, which already includes today
 */
@Composable
private fun CelebrationHeadline(current: Int, highest: Int) {
    val (title, subtitle) = when {
        isStreakMilestone(current) -> streakMilestoneTitle(current) to streakMilestoneSubtitle(current, highest)
        current <= 1 -> "¡Racha iniciada!" to "Vuelve mañana y suma el segundo día."
        current >= highest -> "¡Tu mejor racha!" to "Nunca habías estudiado $current días seguidos."
        else -> "¡Sigues en racha!" to "Mañana, a por el día ${current + 1}."
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

/**
 * One line with the freezes still available.
 *
 * @param freezes freezes left, more than zero
 */
@Composable
private fun FreezesLeft(freezes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.streak_freezer),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (freezes == 1) "Te queda 1 congelador" else "Te quedan $freezes congeladores",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The happy mascot with a speech bubble, as in the onboarding.
 *
 * @param text what the mascot says
 */
@Composable
private fun MascotTip(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.happy_bali),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Surface(
            shape = SpeechBubbleShape(),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            shadowElevation = 2.dp,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
            )
        }
    }
}

/**
 * Full-screen confetti, drawn above everything and never intercepting touches.
 *
 * @param loop true for the daily celebration, which keeps raining; false to play it once
 */
@Composable
private fun ConfettiOverlay(loop: Boolean) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (loop) LottieConstants.IterateForever else 1
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

/**
 * Fades and slides its content up shortly after it first appears, for a staggered entrance.
 *
 * @param delayMillis wait before the entrance starts
 * @param content what to reveal
 */
@Composable
private fun RevealOnAppear(delayMillis: Int, content: @Composable () -> Unit) {
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
