package com.jesuskrastev.bali.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke as FoundationBorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.review.InAppReviewEffect
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliDarkBackground
import com.jesuskrastev.bali.ui.theme.BaliDarkGray
import com.jesuskrastev.bali.ui.theme.BaliDarkSurface
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Delay before animations start, so the screen renders first. */
private const val INTRO_DELAY_MS = 120L

/** Duration of the XP count-up animation in milliseconds. */
private const val XP_COUNT_UP_MS = 1_200L

/** Duration of the accuracy ring fill animation in milliseconds. */
private const val RING_FILL_MS = 1_000L

/** How long after the accuracy ring finishes before XP starts counting up. */
private const val XP_DELAY_AFTER_RING_MS = 200L

/** Returns the accent colour that best communicates the session performance. */
private fun accentForAccuracy(accuracy: Int): Color = when {
    accuracy >= 70 -> BaliAccentGreen
    accuracy >= 40 -> BaliAccentYellow
    else -> BaliAccentRed
}

/** One-line motivational tag based on accuracy, styled for an arcade end-screen. */
private fun tagForAccuracy(accuracy: Int, score: Int, total: Int): String = when {
    accuracy == 100 -> "¡PERFECTO! $score/$total 🎯"
    accuracy >= 80 -> "¡EXCELENTE! $score/$total 🔥"
    accuracy >= 60 -> "¡BUEN JUEGO! $score/$total 💪"
    accuracy >= 40 -> "PUEDES MÁS $score/$total 📈"
    else -> "¡SIGUE INTENTÁNDOLO! $score/$total 🧠"
}

/**
 * Full-screen arcade-style result screen for the DGT mini-games.
 *
 * Shows a dark background with a radial glow whose colour reflects performance, a large
 * animated accuracy ring, a count-up XP display, neon stat chips, and two clearly-labelled
 * action buttons. The level-up overlay from [ResultCelebrations] is reused above the scaffold
 * when the session earned a new level.
 *
 * @param xpGained total XP credited to the user this session
 * @param baseXp base XP before bonuses
 * @param bonusPerfection optional perfection bonus
 * @param bonusFast optional speed bonus
 * @param bonusStreak optional streak bonus
 * @param leveledUp whether this session triggered a level-up
 * @param newLevel the level reached, or 0 if unknown
 * @param score correct rounds out of [ROUNDS_PER_SESSION]
 * @param accuracy percentage of correct rounds (0-100)
 * @param durationSeconds total session time
 * @param onContinueClick called when the user chooses to leave the arcade
 * @param onPlayAgainClick called when the user wants an immediate rematch
 */
@Composable
fun GameResultScreen(
    xpGained: Int,
    baseXp: Int,
    bonusPerfection: Int? = null,
    bonusFast: Int? = null,
    bonusStreak: Int? = null,
    leveledUp: Boolean = false,
    newLevel: Int = 0,
    score: Int,
    accuracy: Int,
    durationSeconds: Int,
    onContinueClick: () -> Unit,
    onPlayAgainClick: () -> Unit,
) {
    val accent = remember(accuracy) { accentForAccuracy(accuracy) }
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60

    // ── Intro animation state ────────────────────────────────────────────────
    var showHeader by remember { mutableStateOf(false) }
    var showRing by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    var displayedXp by remember { mutableIntStateOf(0) }

    val ringProgress: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }

    // Saved so a rotation doesn't replay the level-up overlay.
    var levelUpSeen by rememberSaveable { mutableStateOf(!leveledUp) }
    var levelUpVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(INTRO_DELAY_MS)
        showHeader = true
        delay(250)
        showRing = true
        // Fill the ring while XP is still at 0.
        launch { ringProgress.animateTo(accuracy / 100f, tween(RING_FILL_MS.toInt(), easing = FastOutSlowInEasing)) }
        delay(RING_FILL_MS + XP_DELAY_AFTER_RING_MS)
        // Count XP up.
        val steps = 40
        val stepMs = XP_COUNT_UP_MS / steps
        repeat(steps) { i ->
            displayedXp = (xpGained * (i + 1) / steps)
            delay(stepMs)
        }
        displayedXp = xpGained
        showStats = true
        delay(200)
        showButtons = true
        // Level-up overlay fires after the screen is fully revealed.
        if (!levelUpSeen) {
            delay(400)
            levelUpVisible = true
        }
    }

    InAppReviewEffect(accuracy = accuracy, enabled = levelUpSeen)

    // ── Confetti (accuracy ≥ 70 %) ──────────────────────────────────────────
    val showConfetti = accuracy >= 70
    val confettiComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val confettiProgress by animateLottieCompositionAsState(
        composition = confettiComposition,
        isPlaying = showConfetti,
        iterations = LottieConstants.IterateForever,
    )

    Box(modifier = Modifier.fillMaxSize().background(BaliDarkBackground)) {
        // ── Radial background glow ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.18f), Color.Transparent),
                        radius = 900f,
                    )
                )
        )

        if (showConfetti) {
            LottieAnimation(
                composition = confettiComposition,
                progress = { confettiProgress },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // ── Main content ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Header tag line
            AnimatedVisibility(
                visible = showHeader,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                ) + fadeIn(),
            ) {
                Text(
                    text = tagForAccuracy(accuracy, score, ROUNDS_PER_SESSION),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Accuracy ring + XP count-up in the centre
            AnimatedVisibility(
                visible = showRing,
                enter = fadeIn(tween(400)),
            ) {
                ArcadeAccuracyRing(
                    progress = ringProgress.value,
                    accent = accent,
                    xpDisplayed = displayedXp,
                    ringSize = 220.dp,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Stat chips
            AnimatedVisibility(
                visible = showStats,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ) + fadeIn(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ArcadeStatChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.CheckCircle,
                            label = "Aciertos",
                            value = "$accuracy%",
                            color = BaliAccentGreen,
                        )
                        ArcadeStatChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Timer,
                            label = "Tiempo",
                            value = String.format("%02d:%02d", minutes, seconds),
                            color = BaliPrimary,
                        )
                        ArcadeStatChip(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Bolt,
                            label = "XP ganado",
                            value = "+$xpGained",
                            color = BaliAccentYellow,
                        )
                    }

                    // XP breakdown chips (bonuses only if present)
                    if (bonusPerfection != null || bonusFast != null || bonusStreak != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            bonusPerfection?.let {
                                XpBonusChip(emoji = "🎯", label = "Perfecto", value = it)
                            }
                            bonusFast?.let {
                                XpBonusChip(emoji = "⚡", label = "Velocidad", value = it)
                            }
                            bonusStreak?.let {
                                XpBonusChip(emoji = "🔥", label = "Racha", value = it)
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom action buttons ────────────────────────────────────────────
        AnimatedVisibility(
            visible = showButtons,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            ) + fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Primary: play again — the most expected action in an arcade game
                Button(
                    onClick = onPlayAgainClick,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                ) {
                    Text(
                        "🎮  JUGAR OTRA VEZ",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = if (accent == BaliAccentYellow) BaliDarkBackground else Color.White,
                    )
                }
                // Secondary: leave the arcade
                OutlinedButton(
                    onClick = onContinueClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f)),
                    border = FoundationBorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                ) {
                    Text(
                        "CONTINUAR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }

        // ── Level-up overlay (reused from test result system) ────────────────
        AnimatedVisibility(
            visible = levelUpVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            com.jesuskrastev.bali.ui.screens.test.LevelUpOverlay(
                newLevel = newLevel,
                onDismiss = {
                    levelUpVisible = false
                    levelUpSeen = true
                },
            )
        }
    }
}

/**
 * Large circular ring that animates from 0 to [progress], drawn on a dark track, with the
 * current [xpDisplayed] shown in the centre in arcade-style typography.
 *
 * @param progress fill fraction from 0f to 1f
 * @param accent ring and text accent colour
 * @param xpDisplayed current count-up XP value to show in the centre
 * @param ringSize outer diameter of the ring
 */
@Composable
private fun ArcadeAccuracyRing(
    progress: Float,
    accent: Color,
    xpDisplayed: Int,
    ringSize: Dp = 200.dp,
) {
    val pulse = rememberInfiniteTransition(label = "ring_pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow_alpha",
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
        // Glow layer (slightly larger ring)
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = 14.dp.toPx()
            val inset = strokePx / 2 + 4.dp.toPx()
            drawArc(
                color = accent.copy(alpha = glowAlpha * progress.coerceAtLeast(0.02f)),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = strokePx + 8.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Main ring
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = 14.dp.toPx()
            val inset = strokePx / 2
            // Track
            drawArc(
                color = BaliDarkGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            // Fill
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }

        // Centre text: XP count-up
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+$xpDisplayed",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = accent,
                letterSpacing = (-1).sp,
            )
            Text(
                text = "XP",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 3.sp,
            )
        }
    }
}

/**
 * Compact dark chip displaying a single result stat with an icon, a numeric value and a label.
 *
 * @param modifier layout modifier for the chip
 * @param icon Material icon to display at the top of the chip
 * @param label descriptive label shown below [value]
 * @param value formatted value string (e.g. "80%", "+120", "01:23")
 * @param color accent colour applied to the icon and value text
 */
@Composable
private fun ArcadeStatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = BaliDarkSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

/**
 * Small pill showing an XP bonus with its emoji and value.
 *
 * @param emoji decorative emoji character representing the bonus type
 * @param label bonus category name
 * @param value bonus XP amount
 */
@Composable
private fun XpBonusChip(emoji: String, label: String, value: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BaliDarkGray,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(
                text = "$label  +$value XP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BaliAccentYellow,
            )
        }
    }
}
