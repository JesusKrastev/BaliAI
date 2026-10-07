package com.jesuskrastev.bali.ui.screens.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import kotlin.random.Random

/**
 * Deterministically shuffles [pool] using [sessionSeed] and returns the item for [roundIndex].
 * Every mini-game mechanic calls this instead of `pool.random()` so that, within one session, the
 * same question never comes up twice before every other one in the pool has been shown (each
 * round remounts a fresh composable instance, so this is the only state that carries across
 * rounds without the ViewModel needing to know each mechanic's own content type).
 */
fun <T> pickForRound(pool: List<T>, sessionSeed: Long, roundIndex: Int): T =
    pool.shuffled(Random(sessionSeed))[roundIndex % pool.size]

/** Renders the chrome shared by every mini-game round: back action, score pill, round counter and mascot. */
@Composable
fun GameSessionHeader(
    game: GameType,
    roundIndex: Int,
    totalRounds: Int,
    score: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulseTransition = rememberInfiniteTransition(label = "game_mascot_pulse")
    val mascotScale by pulseTransition.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "mascot_scale")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹ Volver",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onBackClick).padding(8.dp),
                color = BaliPrimary,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = BaliPrimary) {
                Text(
                    "PUNTOS $score",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "RONDA ${roundIndex + 1} DE $totalRounds",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Image(painterResource(R.drawable.bali), null, Modifier.size(58.dp).graphicsLayer { scaleX = mascotScale; scaleY = mascotScale })
        }
    }
}

/** Shows a finished round's outcome with a short driving-relevant explanation, then continues the loop. */
@Composable
fun RoundFeedbackPanel(isSuccess: Boolean, title: String, explanation: String, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider()
        Text(title, color = if (isSuccess) BaliAccentGreen else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Text(explanation, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) BaliAccentGreen else BaliPrimary),
        ) {
            Text("SIGUIENTE", fontWeight = FontWeight.Black)
        }
    }
}

/**
 * Draws a shrinking progress bar and a readable numeric countdown for the active round.
 *
 * @param progress remaining fraction from 1f to 0f
 * @param remainingSeconds whole seconds remaining, shown beside the bar
 * @param modifier layout customisation
 * @param color progress-bar colour
 */
@Composable
fun ShrinkingTimerBar(
    progress: Float,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
    color: Color = BaliPrimary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(10.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(50)),
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = if (remainingSeconds <= 1) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = "${remainingSeconds.coerceAtLeast(0)} s",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = if (remainingSeconds <= 1) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
