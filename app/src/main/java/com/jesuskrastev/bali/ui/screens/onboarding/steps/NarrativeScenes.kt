package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Hand-drawn scenes of the emotional arc, for answers no stock animation illustrates well. */
enum class NarrativeScene {
    /** Job offers arrive one after another and each is stamped "Sin carnet" and lost. */
    JobOffersLost,

    /** The same kind of offer, now stamped "Carnet B ✓": the job is yours. */
    JobOfferWon
}

private val STAMP_RED = Color(0xFFE5484D)
private val STAMP_GREEN = Color(0xFF22A06B)

/** Job offers that ask for the licence. No salaries or figures: nothing here needs a source. */
private val OFFERS = listOf(
    "🚚" to "Reparto en furgoneta",
    "🌙" to "Turno de noche",
    "🛠️" to "Técnico/a a domicilio"
)

/**
 * Draws [scene] at the size of the narrative visual.
 *
 * @param scene the scene to play
 * @param modifier size and position of the scene
 */
@Composable
fun NarrativeSceneView(scene: NarrativeScene, modifier: Modifier = Modifier) {
    when (scene) {
        NarrativeScene.JobOffersLost -> JobOffersLost(modifier)
        NarrativeScene.JobOfferWon -> JobOfferWon(modifier)
    }
}

/**
 * Offers slide in one by one, a red "SIN CARNET" stamp slams onto each, and it slides away greyed
 * out before the next one comes. Loops through the three offers.
 */
@Composable
private fun JobOffersLost(modifier: Modifier) {
    var index by remember { mutableIntStateOf(0) }
    val slide = remember { Animatable(1f) }
    val stamp = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            slide.snapTo(1f)
            stamp.snapTo(0f)
            leave.snapTo(0f)
            slide.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
            delay(450)
            stamp.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            delay(1_100)
            leave.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            index = (index + 1) % OFFERS.size
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        // The offer that was lost just before, faded at the back of the pile.
        OfferCard(
            emoji = OFFERS[(index + OFFERS.size - 1) % OFFERS.size].first,
            title = OFFERS[(index + OFFERS.size - 1) % OFFERS.size].second,
            modifier = Modifier.graphicsLayer {
                translationY = 18.dp.toPx()
                scaleX = 0.92f
                scaleY = 0.92f
                alpha = 0.35f
                rotationZ = -4f
            }
        ) { Stamp("SIN CARNET", STAMP_RED, rotation = -12f) }

        OfferCard(
            emoji = OFFERS[index].first,
            title = OFFERS[index].second,
            modifier = Modifier.graphicsLayer {
                translationX = slide.value * size.width * 1.2f - leave.value * size.width * 0.15f
                translationY = leave.value * 18.dp.toPx()
                rotationZ = slide.value * 8f - leave.value * 4f
                alpha = 1f - 0.65f * leave.value
                val shrink = 1f - 0.08f * leave.value
                scaleX = shrink
                scaleY = shrink
            }
        ) {
            if (stamp.value > 0f) {
                Stamp(
                    "SIN CARNET",
                    STAMP_RED,
                    rotation = -12f,
                    modifier = Modifier.graphicsLayer {
                        val s = 2.2f - 1.2f * stamp.value
                        scaleX = s
                        scaleY = s
                        alpha = stamp.value
                    }
                )
            }
        }
    }
}

/**
 * One offer, with confetti behind it: a green "CARNET B ✓" stamp lands and the card lifts and
 * glows, gently bobbing.
 */
@Composable
private fun JobOfferWon(modifier: Modifier) {
    val stamp = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(500)
        launch { stamp.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
        lift.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    val bob = rememberInfiniteTransition(label = "offer_won")
    val dy by bob.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "offer_won_bob"
    )
    val confetti by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val confettiProgress by animateLottieCompositionAsState(confetti, iterations = LottieConstants.IterateForever)

    Box(modifier, contentAlignment = Alignment.Center) {
        if (stamp.value > 0.5f) {
            LottieAnimation(confetti, progress = { confettiProgress }, modifier = Modifier.fillMaxSize())
        }
        OfferCard(
            emoji = "💼",
            title = "Tu próximo trabajo",
            subtitle = "Incorporación: cuando tú digas",
            highlighted = lift.value > 0.5f,
            modifier = Modifier.graphicsLayer {
                translationY = dy.dp.toPx() - lift.value * 8.dp.toPx()
                val s = 1f + 0.04f * lift.value
                scaleX = s
                scaleY = s
            }
        ) {
            if (stamp.value > 0f) {
                Stamp(
                    "CARNET B ✓",
                    STAMP_GREEN,
                    rotation = -10f,
                    modifier = Modifier.graphicsLayer {
                        val s = 2f - stamp.value
                        scaleX = s
                        scaleY = s
                        alpha = stamp.value.coerceIn(0f, 1f)
                    }
                )
            }
        }
    }
}

/**
 * A job offer as a small card: icon, title, the licence requirement, and [overlay] (the stamp)
 * on top.
 */
@Composable
private fun OfferCard(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "Requisito: carnet B",
    highlighted: Boolean = false,
    overlay: @Composable () -> Unit = {}
) {
    Surface(
        modifier = modifier.width(230.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (highlighted) 14.dp else 6.dp,
        border = BorderStroke(
            if (highlighted) 2.dp else 1.dp,
            if (highlighted) STAMP_GREEN else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 20.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "OFERTA DE EMPLEO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                // Placeholder lines for the rest of the ad; the stamp lands on them.
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(1f, 0.85f, 0.6f).forEach { w ->
                        Box(
                            Modifier
                                .fillMaxWidth(w)
                                .height(7.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
            // Over the placeholder lines, so the stamp never hides what the offer is or asks for.
            Box(
                Modifier
                    .matchParentSize()
                    .padding(top = 78.dp),
                contentAlignment = Alignment.Center
            ) { overlay() }
        }
    }
}

/** A rubber stamp: bold text in a rounded double border, tilted. */
@Composable
private fun Stamp(text: String, color: Color, rotation: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .border(3.dp, color, RoundedCornerShape(10.dp))
            .padding(3.dp)
            .border(1.5.dp, color.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.5.sp)
    }
}
