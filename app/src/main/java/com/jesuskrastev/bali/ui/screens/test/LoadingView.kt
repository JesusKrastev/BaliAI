package com.jesuskrastev.bali.ui.screens.test

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import kotlin.math.PI
import kotlin.math.sin

/** One step of what the mascot "thinks" while the questions load: the bubble icon and its caption. */
private data class LoadingThought(val icon: ImageVector, val caption: String)

private val LoadingThoughts = listOf(
    LoadingThought(Icons.Rounded.QuestionMark, "Pensando qué preguntarte…"),
    LoadingThought(Icons.Rounded.Traffic, "Repasando señales y normas…"),
    LoadingThought(Icons.Rounded.DirectionsCar, "Imaginando situaciones de tráfico…"),
    LoadingThought(Icons.Rounded.Lightbulb, "¡Ya casi lo tengo!")
)

/** How long each [LoadingThought] stays on screen. */
private const val THOUGHT_DURATION_MS = 1800

/**
 * Full-screen loading state shown while the questions of a practice or an exam are prepared: Bali
 * sways under a pulsing beacon glow while a thought bubble and the caption below it cycle through
 * [LoadingThoughts], with three bouncing dots as the progress hint.
 */
@Composable
fun LoadingView() {
    val transition = rememberInfiniteTransition(label = "loading")
    val thoughtPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = LoadingThoughts.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(THOUGHT_DURATION_MS * LoadingThoughts.size, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thought"
    )
    val thought by remember {
        derivedStateOf { LoadingThoughts[thoughtPhase.value.toInt() % LoadingThoughts.size] }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            .compositeOver(MaterialTheme.colorScheme.surface)
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
            ThinkingMascot(transition = transition, icon = thought.icon)

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = thought.caption,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut())
                },
                contentAlignment = Alignment.Center,
                label = "caption"
            ) { caption ->
                Text(
                    text = caption,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Estoy preparando un test para ti",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            BouncingDots(transition = transition)
        }
    }
}

/**
 * Bali on the ground under a beacon glow, swaying as if pondering, with a chain of dots rising to
 * a thought bubble that shows [icon].
 */
@Composable
private fun ThinkingMascot(transition: InfiniteTransition, icon: ImageVector) {
    // -1 at the top of the bob, 1 resting on the ground; also drives the shadow.
    val bob = transition.easedSwing(from = -1f, to = 1f, durationMillis = 1600, label = "bob")
    val tilt = transition.easedSwing(from = -5f, to = 5f, durationMillis = 2600, label = "tilt")
    val glow = transition.easedSwing(from = 0.85f, to = 1.05f, durationMillis = 1000, label = "glow")
    val bubbleBob = transition.easedSwing(from = -1f, to = 1f, durationMillis = 1900, label = "bubble")
    val primary = MaterialTheme.colorScheme.primary
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(modifier = Modifier.size(width = 300.dp, height = 260.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 36.dp)
                .size(230.dp)
                .graphicsLayer {
                    scaleX = glow.value
                    scaleY = glow.value
                    alpha = 0.55f + (glow.value - 0.85f) * 2f
                }
                .background(
                    Brush.radialGradient(listOf(primary.copy(alpha = 0.45f), Color.Transparent)),
                    CircleShape
                )
        )

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 120.dp, height = 16.dp)
                .graphicsLayer {
                    val shadowScale = 0.8f + 0.2f * (bob.value + 1f) / 2f
                    scaleX = shadowScale
                    scaleY = shadowScale
                }
        ) {
            drawOval(color = shadowColor)
        }

        Image(
            painter = painterResource(id = R.drawable.bali),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .size(160.dp)
                .graphicsLayer {
                    translationY = (bob.value - 1f) * 5.dp.toPx()
                    rotationZ = tilt.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        )

        ThoughtDot(size = 10.dp, x = (-82).dp, y = 92.dp, bobPx = { bubbleBob.value * 2.dp.toPx() })
        ThoughtDot(size = 16.dp, x = (-62).dp, y = 66.dp, bobPx = { bubbleBob.value * 3.dp.toPx() })

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, primary.copy(alpha = 0.3f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(76.dp)
                .graphicsLayer { translationY = bubbleBob.value * 4.dp.toPx() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = icon,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.5f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 0.5f) + fadeOut())
                    },
                    label = "bubbleIcon"
                ) { shown ->
                    Icon(
                        imageVector = shown,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

/** One of the small circles that lead from Bali's head up to the thought bubble. */
@Composable
private fun BoxScope.ThoughtDot(size: Dp, x: Dp, y: Dp, bobPx: Density.() -> Float) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = x, y = y)
            .size(size)
            .graphicsLayer { translationY = bobPx() }
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
    )
}

/** Three dots that hop one after another, in place of a spinner. */
@Composable
private fun BouncingDots(transition: InfiniteTransition) {
    val cycle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "dots"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        val local = (cycle.value - index * 0.15f + 1f) % 1f
                        val hop = if (local < 0.5f) sin(local * 2f * PI.toFloat()) else 0f
                        translationY = -hop * 8.dp.toPx()
                        alpha = 0.4f + 0.6f * hop
                    }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/** A value that eases back and forth between [from] and [to], read at draw time. */
@Composable
private fun InfiniteTransition.easedSwing(
    from: Float,
    to: Float,
    durationMillis: Int,
    label: String
): State<Float> = animateFloat(
    initialValue = from,
    targetValue = to,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis, easing = EaseInOutSine),
        repeatMode = RepeatMode.Reverse
    ),
    label = label
)
