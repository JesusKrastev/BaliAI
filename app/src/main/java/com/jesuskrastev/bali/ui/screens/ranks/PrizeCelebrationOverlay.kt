package com.jesuskrastev.bali.ui.screens.ranks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliDarkBackground
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** How long taps are ignored once the celebration opens, so the tap that claimed can't close it. */
private const val CLOSE_GUARD_MS = 900L

/** How long the coin counter takes to climb to the prize. */
private const val COUNT_UP_MS = 900

/** Tag of the celebration's full-screen tap target, for tests. */
internal const val PRIZE_CELEBRATION_TAG = "prize_celebration"

/**
 * Full-screen celebration of a prize collected on the rank road: rotating rays, confetti, the
 * rank wheel or prize illustration popping in, the guaranteed contents, the updated balance
 * or inventory usage and what comes next. A tap anywhere closes it after the guard delay.
 *
 * @param celebration what was collected and what is next
 * @param onDismiss invoked when the user taps it away or presses back
 */
@Composable
fun PrizeCelebrationOverlay(celebration: PrizeCelebration, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val pop = remember { Animatable(0.3f) }
    var shownCoins by remember { mutableIntStateOf(0) }
    var canClose by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        val counter = Animatable(0f)
        counter.animateTo(celebration.reward.coins.toFloat(), tween(COUNT_UP_MS, delayMillis = 250)) {
            shownCoins = value.toInt()
        }
        shownCoins = celebration.reward.coins
    }
    LaunchedEffect(Unit) {
        delay(CLOSE_GUARD_MS)
        canClose = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        PrizeCelebrationContent(
            celebration = celebration,
            shownCoins = shownCoins,
            badgeScale = pop.value,
            canClose = canClose,
            onTap = { if (canClose) onDismiss() }
        )
    }
}

/**
 * What the celebration draws, without the dialog window around it.
 *
 * @param shownCoins the coin counter's current value, climbing to the prize
 * @param badgeScale scale of the badge or coin as it pops in
 * @param canClose whether "Toca para continuar" shows
 * @param onTap invoked for a tap anywhere
 */
@Composable
internal fun PrizeCelebrationContent(
    celebration: PrizeCelebration,
    shownCoins: Int,
    badgeScale: Float,
    canClose: Boolean,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaliDarkBackground)
            .testTag(PRIZE_CELEBRATION_TAG)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Continuar",
                role = Role.Button,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Confetti()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (celebration.rank != null) "¡PREMIO DE RANGO!" else "¡PREMIO DEL CAMINO!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = BaliAccentYellow
            )
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                Rays()
                Image(
                    painter = painterResource(celebration.rank?.badgeRes() ?: celebration.reward.prizeRes()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (celebration.rank != null) 170.dp else 130.dp)
                        .graphicsLayer {
                            scaleX = badgeScale
                            scaleY = badgeScale
                        }
                )
            }
            if (celebration.reward.coins > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.coin), contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "+$shownCoins",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = BaliAccentYellow
                    )
                }
            }
            celebration.reward.lines().filter { it.image != R.drawable.coin }.forEach { line ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(line.image), contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        line.label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = BaliAccentYellow
                    )
                }
            }
            Text(
                headline(celebration),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (celebration.reward.hasInventory) celebration.reward.usageText()
                else "Ahora tienes ${celebration.coinsAfter} monedas para la tienda.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            WhatsNext(celebration)
            Spacer(Modifier.height(40.dp))
            TapToContinue(visible = canClose)
        }
    }
}

/** Returns the rank reached or XP milestone associated with [celebration]. */
private fun headline(celebration: PrizeCelebration): String = celebration.rank
    ?.let { "¡Ya eres ${it.name}!" }
    ?: "Por llegar a ${celebration.reward.requiredXp} XP"

/** Displays unclaimed prizes or the next guaranteed reward described by [celebration]. */
@Composable
private fun WhatsNext(celebration: PrizeCelebration) {
    val text = when {
        celebration.stillClaimable == 1 -> "🎁 Te queda 1 premio más por recoger"
        celebration.stillClaimable > 1 -> "🎁 Te quedan ${celebration.stillClaimable} premios más por recoger"
        celebration.next != null ->
            "Siguiente premio: ${celebration.next.contents()} a los ${celebration.next.requiredXp} XP\nTe faltan ${celebration.xpToNext} XP"
        else -> "🏆 ¡Has recogido todos los premios del camino!"
    }
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.1f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/** Gently pulsing "tap to continue", shown once the celebration can be closed. */
@Composable
private fun TapToContinue(visible: Boolean) {
    val pulse = rememberInfiniteTransition(label = "tap_to_continue")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "tap_alpha"
    )
    Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
        if (visible) {
            Text(
                "Toca para continuar",
                modifier = Modifier.alpha(alpha),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/** Slowly turning sunburst behind the prize. */
@Composable
private fun Rays() {
    val spin = rememberInfiniteTransition(label = "rays")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "rays_angle"
    )
    val glow by spin.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rays_glow"
    )
    Canvas(
        Modifier
            .fillMaxSize()
            .rotate(angle)
    ) {
        val centre = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * glow
        drawCircle(
            Brush.radialGradient(
                listOf(BaliAccentYellow.copy(alpha = 0.45f), Color.Transparent),
                center = centre,
                radius = radius
            ),
            radius = radius,
            center = centre
        )
        val rays = 12
        val half = (PI / rays / 2).toFloat()
        repeat(rays) { i ->
            val a = (2 * PI * i / rays).toFloat()
            val path = Path().apply {
                moveTo(centre.x, centre.y)
                lineTo(centre.x + radius * cos(a - half), centre.y + radius * sin(a - half))
                lineTo(centre.x + radius * cos(a + half), centre.y + radius * sin(a + half))
                close()
            }
            drawPath(path, BaliAccentYellow.copy(alpha = 0.16f))
        }
    }
}

/** One burst of confetti over the whole screen. */
@Composable
private fun Confetti() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
