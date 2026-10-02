package com.jesuskrastev.bali.ui.screens.test

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliDarkBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the result screen waits before the stamp comes down, so the screen is seen first. */
private const val STAMP_DELAY_MS = 350L

/** Resting tilt of the stamp, in degrees: slightly crooked, as a hand-pressed stamp lands. */
private const val STAMP_ANGLE = -8f

/** Extra time the level-up overlay stays after its animation ends, before closing by itself. */
private const val LEVEL_UP_LINGER_MS = 800L

/** Green ink of the stamp on a light background, dark enough to read on white. */
private val StampGreenOnLight = Color(0xFF15803D)

/**
 * The hero of a passed mock exam: an "APROBADO" stamp that slams onto the screen, with a haptic
 * knock and an ink ring on impact, and the score under it. It is the biggest celebration in the
 * app, kept for the result that matters most to someone preparing the DGT theory test.
 *
 * @param score correct answers
 * @param total questions in the exam; the score line is left out when it is 0
 * @param landed whether the stamp is already down; false plays the slam, true shows it at rest
 *   (e.g. after a rotation, so it is not replayed)
 * @param onLanded invoked at the moment of impact, once
 */
@Composable
internal fun PassedExamStamp(score: Int, total: Int, landed: Boolean, onLanded: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val ink = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) BaliAccentGreen else StampGreenOnLight
    val startLanded = remember { landed }
    val scale = remember { Animatable(if (startLanded) 1f else 2.6f) }
    val rotation = remember { Animatable(if (startLanded) STAMP_ANGLE else -24f) }
    val alpha = remember { Animatable(if (startLanded) 1f else 0f) }
    val ringScale = remember { Animatable(1f) }
    val ringAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (startLanded) return@LaunchedEffect
        delay(STAMP_DELAY_MS)
        launch { alpha.animateTo(1f, tween(durationMillis = 120)) }
        launch { rotation.animateTo(STAMP_ANGLE, tween(durationMillis = 220, easing = FastOutLinearInEasing)) }
        scale.animateTo(1f, tween(durationMillis = 220, easing = FastOutLinearInEasing))

        // Impact: knock, ink ring, and a small squash and rebound.
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onLanded()
        launch {
            ringAlpha.snapTo(0.6f)
            launch { ringScale.animateTo(1.35f, tween(durationMillis = 450, easing = LinearOutSlowInEasing)) }
            ringAlpha.animateTo(0f, tween(durationMillis = 450))
        }
        scale.snapTo(0.92f)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    val detailsAlpha by animateFloatAsState(
        targetValue = if (landed) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150),
        label = "stampDetails"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.semantics { contentDescription = "Aprobado" },
            contentAlignment = Alignment.Center
        ) {
            // Ink ring that spreads from the stamp's edge on impact.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = ringScale.value
                        scaleY = ringScale.value
                        rotationZ = STAMP_ANGLE
                        this.alpha = ringAlpha.value
                    }
                    .border(3.dp, ink, RoundedCornerShape(14.dp))
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                        this.alpha = alpha.value
                    }
                    .border(4.dp, ink, RoundedCornerShape(14.dp))
                    .padding(5.dp)
                    .border(2.dp, ink, RoundedCornerShape(10.dp))
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "APROBADO",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = ink
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.graphicsLayer { this.alpha = detailsAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (total > 0) {
                val mistakes = total - score
                Text(
                    text = "$score de $total · ${if (mistakes == 1) "1 fallo" else "$mistakes fallos"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "Con este resultado aprobarías el teórico de la DGT.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Full-screen celebration of a new level: the level-up animation, the level reached, and a
 * hint that a tap closes it. It also closes by itself shortly after the animation ends, and on
 * back, so it never stands between the student and the result.
 *
 * @param newLevel the level reached; 0 when unknown, which shows a generic title instead
 * @param onDismiss invoked when the overlay should close
 */
@Composable
internal fun LevelUpOverlay(newLevel: Int, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.level_up))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)
    val contentScale = remember { Animatable(0.6f) }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        contentScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(composition) {
        val loaded = composition ?: return@LaunchedEffect
        delay(loaded.duration.toLong() + LEVEL_UP_LINGER_MS)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Opaque: the result screen showing through would clash with the overlay's text.
            .background(BaliDarkBackground)
            .background(
                Brush.radialGradient(listOf(BaliAccentYellow.copy(alpha = 0.18f), Color.Transparent))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Continuar",
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(220.dp)
            )
            Text(
                text = if (newLevel > 0) "¡NIVEL $newLevel!" else "¡SUBISTE DE NIVEL!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = BaliAccentYellow,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Has subido de nivel. Cada test te acerca al aprobado.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Toca para seguir",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
