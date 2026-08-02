package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.ui.screens.onboarding.NarrativeContent
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliTheme

private const val TEXT_ENTRANCE_DELAY_MS = 220

/**
 * Single-idea onboarding screen used by the whole emotional arc (empathy, the three
 * loss screens and the three gain screens).
 *
 * The headline is delivered by the mascot bubble above, so this body carries only the
 * visual and one line. There is deliberately no card: the copy sits straight on the
 * background over a soft glow, and the visual and the text land one after the other so
 * the sentence reads as a beat rather than as a label inside a box.
 *
 * @param content the visual and body copy for this particular screen
 * @param modifier modifier applied to the root container
 */
@Composable
fun StepNarrative(
    content: NarrativeContent,
    modifier: Modifier = Modifier
) {
    var started by remember(content) { mutableStateOf(false) }
    LaunchedEffect(content) { started = true }

    val visualAppear by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "narrative_visual"
    )
    val textAppear by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = TEXT_ENTRANCE_DELAY_MS,
            easing = FastOutSlowInEasing
        ),
        label = "narrative_text"
    )

    // Centred while the content fits, scrollable once it does not: a long line on a short
    // screen has to push the layout, never get clipped by it.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NarrativeVisual(content = content, appear = visualAppear)

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .graphicsLayer { alpha = textAppear }
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = content.body.highlightPipes(MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        alpha = textAppear
                        translationY = (1f - textAppear) * 28.dp.toPx()
                    }
            )
        }
    }
}

/**
 * The animation or emoji of the screen, sitting on a soft radial glow that replaces
 * the old container card.
 *
 * @param content the screen content, which decides between Lottie and emoji
 * @param appear entrance progress between 0f and 1f
 */
@Composable
private fun NarrativeVisual(content: NarrativeContent, appear: Float) {
    val glowColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(220.dp)
            .graphicsLayer {
                alpha = appear
                scaleX = 0.88f + 0.12f * appear
                scaleY = 0.88f + 0.12f * appear
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.20f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (content.animation != null) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(content.animation)
            )
            val lottieProgress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )

            LottieAnimation(
                composition = composition,
                progress = { lottieProgress },
                modifier = Modifier.size(172.dp)
            )
        } else {
            Text(text = content.emoji, fontSize = 84.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepNarrativePreview() {
    BaliTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            StepNarrative(
                content = NarrativeContent(
                    emoji = "⛓️",
                    body = "Sin carnet, |tu vida la deciden otros|: los horarios del transporte y los favores ajenos."
                ),
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}
