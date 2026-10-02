package com.jesuskrastev.bali.ui.screens.streak

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.theme.BaliFlameAmber
import com.jesuskrastev.bali.ui.theme.BaliFlameColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Number of light rays behind the milestone flame. */
private const val RAY_COUNT = 12

/** Half the angular width of each ray, in degrees. */
private const val RAY_HALF_WIDTH_DEGREES = 6f

/** How far the rays turn while they fade in, in degrees. They stop there: nothing loops. */
private const val RAY_TURN_DEGREES = 40f

/** Number of sparks in the burst. */
private const val SPARK_COUNT = 20

/** Wait before the sparks and rays start, so they come out of the growing flame. */
private const val SPARKS_DELAY_MS = 250L

/** Wait before the badge comes down, once the flame has grown. */
private const val BADGE_DELAY_MS = 650L

/** Resting tilt of the badge, in degrees, as a stamp lands slightly crooked. */
private const val BADGE_ANGLE = -4f

/**
 * Hero of a milestone day (see [isStreakMilestone]): light rays, a flame that grows past its
 * daily size, a burst of sparks in the flame's colours, the count, and a "¡HITO DE RACHA!" badge
 * stamped under it with a haptic knock. It replaces the daily confetti on those days, so a
 * milestone looks unmistakably bigger than an ordinary day. Every part plays once; nothing loops.
 *
 * @param days the streak including today, a milestone
 * @param played true when the celebration already ran (for example before a rotation), so it is
 *   drawn at rest instead of replayed
 * @param onPlayed invoked once, when the badge lands
 */
@Composable
internal fun MilestoneFlame(days: Int, played: Boolean, onPlayed: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val startAtRest = remember { played }
    val rest = if (startAtRest) 1f else 0f
    val flameScale = remember { Animatable(if (startAtRest) 1f else 0.4f) }
    val rays = remember { Animatable(rest) }
    val sparks = remember { Animatable(rest) }
    val badgeScale = remember { Animatable(if (startAtRest) 1f else 2.2f) }
    val badgeAlpha = remember { Animatable(rest) }

    LaunchedEffect(Unit) {
        if (startAtRest) return@LaunchedEffect
        launch { flameScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)) }
        launch {
            delay(SPARKS_DELAY_MS)
            launch { sparks.animateTo(1f, tween(durationMillis = 900, easing = LinearOutSlowInEasing)) }
            rays.animateTo(1f, tween(durationMillis = 2400, easing = LinearOutSlowInEasing))
        }
        delay(BADGE_DELAY_MS)
        launch { badgeAlpha.animateTo(1f, tween(durationMillis = 120)) }
        badgeScale.animateTo(1f, tween(durationMillis = 220, easing = FastOutLinearInEasing))
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onPlayed()
        badgeScale.animateTo(0.94f, tween(durationMillis = 70))
        badgeScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawRays(rays.value) }
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = flameScale.value
                        scaleY = flameScale.value
                    }
                    .background(
                        brush = Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            StreakFlame(
                lit = true,
                size = 150.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = flameScale.value
                    scaleY = flameScale.value
                }
            )
            Canvas(modifier = Modifier.fillMaxSize()) { drawSparks(sparks.value) }
        }
        Text(
            text = days.toString(),
            fontSize = 80.sp,
            lineHeight = 80.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        MilestoneBadge(
            modifier = Modifier.graphicsLayer {
                scaleX = badgeScale.value
                scaleY = badgeScale.value
                alpha = badgeAlpha.value
                rotationZ = BADGE_ANGLE
            }
        )
    }
}

/**
 * The "¡HITO DE RACHA!" pill, in the flame's gradient.
 *
 * @param modifier applied to the pill; carries the stamp animation
 */
@Composable
private fun MilestoneBadge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        text = "¡HITO DE RACHA!",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = Color.White,
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(Brush.horizontalGradient(BaliFlameColors))
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

/**
 * Light rays fanning out from the centre, fading towards their tips.
 *
 * @param progress 0 before they appear, 1 at rest: they fade in, grow and turn a little on the way
 */
private fun DrawScope.drawRays(progress: Float) {
    if (progress <= 0f) return
    val radius = size.minDimension * (0.55f + 0.45f * progress)
    val turn = RAY_TURN_DEGREES * progress
    val alpha = (progress * 4f).coerceAtMost(1f)
    val brush = Brush.radialGradient(
        colors = listOf(BaliFlameAmber.copy(alpha = 0.45f * alpha), Color.Transparent),
        center = center,
        radius = radius
    )
    repeat(RAY_COUNT) { index ->
        val angle = turn + index * (360f / RAY_COUNT)
        val ray = Path().apply {
            moveTo(center.x, center.y)
            val left = center + polar(radius, angle - RAY_HALF_WIDTH_DEGREES)
            val right = center + polar(radius, angle + RAY_HALF_WIDTH_DEGREES)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
        drawPath(ray, brush = brush)
    }
}

/**
 * A burst of sparks flying out of the flame and fading, alternating long and short throws and
 * cycling through the flame's colours.
 *
 * @param progress 0 before the burst, 1 once every spark has faded
 */
private fun DrawScope.drawSparks(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val start = size.minDimension * 0.2f
    val far = size.minDimension * 0.62f
    val alpha = 1f - progress * progress
    repeat(SPARK_COUNT) { index ->
        val reach = if (index % 2 == 0) far else far * 0.72f
        val angle = index * (360f / SPARK_COUNT) + if (index % 2 == 0) 0f else 7f
        val distance = start + (reach - start) * progress
        val sparkRadius = (6f - 4f * progress).dp.toPx()
        drawCircle(
            color = BaliFlameColors[index % BaliFlameColors.size].copy(alpha = alpha),
            radius = sparkRadius,
            center = center + polar(distance, angle)
        )
    }
}

/**
 * A point at a distance and angle from the origin.
 *
 * @param distance length, in pixels
 * @param degrees angle clockwise from the positive x axis
 * @return the point as an [Offset]
 */
private fun polar(distance: Float, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(distance * cos(radians), distance * sin(radians))
}
