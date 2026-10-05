package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliPrimaryDark
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Night-blue backdrop of the results screen. */
private val ResultsTop = Color(0xFF1E1B4B)
private val ResultsBottom = Color(0xFF0B1022)

/**
 * Game-style end of a run, almost without words: rotating light rays, Bali, three big stars that
 * pop in one by one, the score counting up, a record ribbon (or a bar showing how close the
 * record was), icon badges, the rewards, the situations that went wrong as icons, and a chunky
 * replay button.
 *
 * @param onStarShown called as each rating star appears, with its 1-based index (for the sound)
 */
@Composable
internal fun BoxScope.DriveResultsOverlay(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val summary = result.summary
    var starsShown by remember { mutableIntStateOf(0) }
    val score = remember { Animatable(0f) }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(350)
        for (i in 1..summary.rating) {
            delay(320)
            starsShown = i
            onStarShown(i)
        }
        score.animateTo(summary.score.toFloat(), tween(1000, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier
            .matchParentSize()
            .background(Brush.verticalGradient(listOf(ResultsTop, ResultsBottom)))
            .clickable(enabled = false) {},
    )
    SunburstRays(Modifier.matchParentSize(), rating = summary.rating)
    if (result.isNewRecord || summary.rating == 3) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
        val lottieProgress by animateLottieCompositionAsState(composition, iterations = 1)
        LottieAnimation(composition, { lottieProgress }, Modifier.matchParentSize())
    }

    RoundIconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
        Icon(Icons.Rounded.Close, contentDescription = "Salir", tint = Color.White)
    }

    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                scaleX = 0.6f + 0.4f * entrance.value
                scaleY = 0.6f + 0.4f * entrance.value
                alpha = entrance.value.coerceIn(0f, 1f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BouncingBali(happy = summary.rating >= 2)
        Text(
            when (summary.rating) {
                3 -> "¡PERFECTO!"
                2 -> "¡GENIAL!"
                else -> "¡META!"
            },
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            style = TextStyle(shadow = Shadow(BaliPrimary, Offset(0f, 6f), 0f)),
        )
        RatingStars(starsShown)
        Text(
            points(score.value.toInt()),
            color = BaliAccentYellow,
            fontSize = 64.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 8f), 12f)),
        )
        RecordBadge(result)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge("✅", "${summary.resolved}/${summary.situations}")
            IconBadge("⭐", "${summary.starsCollected}")
            result.rewards?.let {
                IconBadge("⚡", "+${it.xpEarned.xpGained}")
                CoinBadge(it.coinsGained)
            }
        }
        if (result.rewards?.xpEarned?.levelUp == true) {
            Text("NIVEL ${result.rewards.xpEarned.newLevel} ⬆", color = BaliAccentGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        if (summary.faultKinds.isNotEmpty()) FaultIcons(summary.faultKinds)
        Spacer(Modifier.height(6.dp))
        ChunkyButton(onClick = onReplay)
    }
}

/** Light rays turning slowly behind the stars; warmer and brighter for a better rating. */
@Composable
private fun SunburstRays(modifier: Modifier, rating: Int) {
    val spin = rememberInfiniteTransition(label = "results_rays")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "results_rays_angle")
    val color = if (rating >= 2) BaliAccentYellow else Color(0xFF93C5FD)
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height * 0.36f)
        val radius = size.maxDimension
        rotate(angle, center) {
            for (i in 0 until 14) {
                val a0 = (i * 2 * PI / 14).toFloat()
                val a1 = a0 + (PI / 14).toFloat()
                val ray = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + cos(a0) * radius, center.y + sin(a0) * radius)
                    lineTo(center.x + cos(a1) * radius, center.y + sin(a1) * radius)
                    close()
                }
                drawPath(ray, color.copy(alpha = 0.07f))
            }
        }
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), center, radius * 0.35f), radius * 0.35f, center)
    }
}

/** Bali hopping for joy, or swaying gently after a rough run. */
@Composable
private fun BouncingBali(happy: Boolean) {
    val motion = rememberInfiniteTransition(label = "results_bali")
    val t by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(if (happy) 700 else 1600), RepeatMode.Reverse), label = "results_bali_t")
    Image(
        painter = painterResource(R.drawable.bali),
        contentDescription = null,
        modifier = Modifier.size(92.dp).graphicsLayer {
            if (happy) {
                translationY = -t * 14.dp.toPx()
                scaleY = 0.95f + t * 0.08f
            } else {
                rotationZ = (t - 0.5f) * 10f
            }
        },
    )
}

/** Three big stars on an arc; each pops with a bounce when it is earned. */
@Composable
private fun RatingStars(shown: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..3) {
            val earned = i <= shown
            val scale by animateFloatAsState(
                if (earned) 1f else 0.75f,
                spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMediumLow),
                label = "results_star_$i",
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(y = if (i == 2) (-14).dp else 0.dp)) {
                if (earned) {
                    Box(Modifier.size(if (i == 2) 78.dp else 62.dp).background(Brush.radialGradient(listOf(BaliAccentYellow.copy(alpha = 0.55f), Color.Transparent)), CircleShape))
                }
                Text(
                    "★",
                    modifier = Modifier.scale(scale),
                    fontSize = if (i == 2) 76.sp else 60.sp,
                    color = if (earned) BaliAccentYellow else Color.White.copy(alpha = 0.15f),
                    style = TextStyle(shadow = if (earned) Shadow(Color(0xFFB45309), Offset(0f, 5f), 0f) else null),
                )
            }
        }
    }
}

/** New-record ribbon, or a bar with the gap to the record, or the record itself. */
@Composable
private fun RecordBadge(result: DriveResult) {
    val best = result.previousBest
    val score = result.summary.score
    when {
        result.isNewRecord -> {
            val pulse = rememberInfiniteTransition(label = "record_pulse")
            val scale by pulse.animateFloat(0.94f, 1.08f, infiniteRepeatable(tween(480), RepeatMode.Reverse), label = "record_scale")
            Box(
                Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(BaliAccentYellow, BaliPrimary)))
                    .padding(horizontal = 18.dp, vertical = 7.dp),
            ) {
                Text("🏆 NUEVO RÉCORD", color = BaliSecondary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        best > 0 -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.width(200.dp).height(10.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f))) {
                Box(Modifier.fillMaxWidth((score.toFloat() / best).coerceIn(0f, 1f)).height(10.dp).background(BaliPrimary))
            }
            Text("🏆 ${points(best)}   −${points(best - score)}", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        else -> Unit
    }
}

/** Round glassy badge with an icon over a number. */
@Composable
private fun IconBadge(icon: String, value: String) {
    Column(
        Modifier
            .size(width = 70.dp, height = 66.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, fontSize = 20.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

/** Badge with a drawn gold coin, so it never depends on a newer emoji font. */
@Composable
private fun CoinBadge(coins: Int) {
    Column(
        Modifier
            .size(width = 70.dp, height = 66.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))))
                .border(2.dp, Color(0xFFB45309), CircleShape),
        )
        Text("+$coins", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

/** The situations that went wrong this run, as crossed-out icons: what to practise next. */
@Composable
private fun FaultIcons(kinds: List<SituationKind>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        kinds.distinct().take(5).forEach { kind ->
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(BaliAccentRed.copy(alpha = 0.18f))
                        .border(2.dp, BaliAccentRed.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(kind.icon, fontSize = 22.sp) }
                Box(Modifier.size(16.dp).clip(CircleShape).background(BaliAccentRed), contentAlignment = Alignment.Center) {
                    Text("✕", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** Big 3D game button that sinks when pressed and breathes while idle. */
@Composable
private fun ChunkyButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val breathe = rememberInfiniteTransition(label = "replay_breathe")
    val idle by breathe.animateFloat(1f, 1.05f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "replay_scale")
    val depth = if (pressed) 2.dp else 8.dp
    Box(
        Modifier
            .scale(if (pressed) 0.98f else idle)
            .fillMaxWidth(0.85f)
            .height(72.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(24.dp)).background(BaliPrimaryDark))
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset(y = 8.dp - depth)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFF8B5E), BaliPrimary))),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Text("OTRA VEZ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
        }
    }
}

/** Small round translucent button for secondary actions. */
@Composable
private fun RoundIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Formats points with Spanish thousands separators (1.234). */
internal fun points(value: Int): String = NumberFormat.getIntegerInstance(Locale("es", "ES")).format(value)
