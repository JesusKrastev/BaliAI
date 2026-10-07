package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Displays [result] over the stopped game and guards [onReplay] and [onExit] while rewards save.
 * [onStarShown] plays feedback for each revealed star; returns dialog UI content.
 */
@Composable
internal fun DriveResultsDialog(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val pending = result.rewardStatus == DriveRewardStatus.PENDING
    Dialog(
        onDismissRequest = { if (!pending) onExit() },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        DriveResultsContent(result, onReplay, onExit, onStarShown)
    }
}

/**
 * Renders [result] as an arcade scoreboard with fixed [onReplay] and [onExit] actions.
 * [onStarShown] handles star feedback; returns the panel UI content.
 */
@Composable
internal fun DriveResultsContent(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val summary = result.summary
    val pending = result.rewardStatus == DriveRewardStatus.PENDING
    var revealedStars by remember(summary) { mutableIntStateOf(0) }
    var scoreTarget by remember(summary) { mutableIntStateOf(0) }
    val shownScore by animateIntAsState(scoreTarget, tween(650), label = "drive_result_score")
    LaunchedEffect(summary) {
        scoreTarget = summary.score
        for (star in 1..summary.rating) {
            delay(180)
            revealedStars = star
            onStarShown(star)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = BaliSecondary,
            border = BorderStroke(2.dp, BaliAccentYellow),
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.92f)) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(BaliPrimary, Color(0xFFE95A29))))
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("BALI DRIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DriveIcon(DriveIconKind.FLAG, Modifier.size(30.dp), Color.White)
                        Text(stringResource(R.string.drive_result_title), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        DriveIcon(DriveIconKind.FLAG, Modifier.size(30.dp), Color.White)
                    }
                    val ratingLabel = stringResource(R.string.drive_rating, summary.rating)
                    Row(Modifier.semantics { contentDescription = ratingLabel }, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        for (i in 1..3) {
                            val visibility by animateFloatAsState(
                                if (i <= revealedStars) 1f else 0f,
                                tween(220),
                                label = "drive_result_star_$i",
                            )
                            Box(Modifier.size(34.dp)) {
                                DriveIcon(DriveIconKind.STAR, Modifier.matchParentSize(), Color.White.copy(alpha = 0.35f))
                                DriveIcon(
                                    DriveIconKind.STAR,
                                    Modifier.matchParentSize().graphicsLayer {
                                        alpha = visibility
                                        scaleX = 0.78f + visibility * 0.22f
                                        scaleY = scaleX
                                    },
                                    BaliAccentYellow,
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(
                            when (summary.rating) {
                                3 -> R.string.drive_result_three_stars
                                2 -> R.string.drive_result_two_stars
                                else -> R.string.drive_result_one_star
                            }
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }

                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = DrivePalette.Road,
                        border = BorderStroke(1.dp, BaliAccentYellow.copy(alpha = 0.75f)),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.drive_points).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Text(points(shownScore), color = BaliAccentYellow, fontSize = 39.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black)
                            Text(
                                stringResource(if (result.isNewRecord) R.string.drive_new_record else R.string.drive_record) +
                                    " · " + points(maxOf(summary.score, result.previousBest)),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    ResultStat(DriveIconKind.CHECK, stringResource(R.string.drive_resolved), "${summary.resolved}/${summary.situations}")
                    ResultStat(DriveIconKind.STAR, stringResource(R.string.drive_collected), "${summary.starsCollected}/${summary.starsTotal}")
                    ResultStat(DriveIconKind.ACCURACY, stringResource(R.string.drive_accuracy), "${summary.resolved * 100 / summary.situations.coerceAtLeast(1)} %")
                    ResultStat(DriveIconKind.TIME, stringResource(R.string.drive_duration), stringResource(R.string.drive_seconds, summary.durationSeconds))
                    when (result.rewardStatus) {
                        DriveRewardStatus.PENDING -> Text(stringResource(R.string.drive_rewards_pending), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        DriveRewardStatus.FAILED -> Text(stringResource(R.string.drive_rewards_failed), color = BaliAccentYellow, style = MaterialTheme.typography.bodySmall)
                        DriveRewardStatus.COMPLETE -> result.rewards?.let { rewards ->
                            ResultStat(DriveIconKind.XP, stringResource(R.string.drive_xp), "+${rewards.xpEarned.xpGained}")
                            ResultStat(DriveIconKind.COIN, stringResource(R.string.drive_coins), "+${rewards.coinsGained}")
                            val xp = rewards.xpEarned
                            Text(stringResource(R.string.drive_xp_base, xp.baseXp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                            xp.bonusPerfection?.let { Text(stringResource(R.string.drive_xp_perfect, it), color = Color.White, style = MaterialTheme.typography.bodySmall) }
                            xp.bonusFast?.let { Text(stringResource(R.string.drive_xp_fast, it), color = Color.White, style = MaterialTheme.typography.bodySmall) }
                            xp.bonusStreak?.let { Text(stringResource(R.string.drive_xp_streak, it), color = Color.White, style = MaterialTheme.typography.bodySmall) }
                            if (xp.levelUp) Text(stringResource(R.string.drive_level_up, xp.newLevel), color = BaliAccentYellow, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (summary.faults.isNotEmpty()) {
                        Text(stringResource(R.string.drive_faults, summary.faults.size), color = BaliAccentYellow, style = MaterialTheme.typography.titleSmall)
                        summary.faults.forEachIndexed { index, explanation ->
                            Surface(shape = RoundedCornerShape(12.dp), color = DrivePalette.Road) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DriveIcon(DriveIconKind.FAULT, Modifier.size(24.dp), BaliAccentRed)
                                    Column(Modifier.weight(1f)) {
                                        summary.faultKinds.getOrNull(index)?.let { Text(it.hint, color = Color.White, style = MaterialTheme.typography.labelMedium) }
                                        Text(explanation, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().background(Color(0xFF172238)).padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onReplay,
                        enabled = !pending,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BaliPrimary, contentColor = Color.White),
                        border = BorderStroke(2.dp, BaliAccentYellow),
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = null)
                        Text(stringResource(R.string.drive_replay), modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    OutlinedButton(
                        onClick = onExit,
                        enabled = !pending,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text(stringResource(R.string.drive_exit), fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** Pairs vector [kind] with readable [label] and [value]; returns a scoreboard row. */
@Composable
private fun ResultStat(kind: DriveIconKind, label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DrivePalette.Road,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DriveIcon(kind, Modifier.size(25.dp), BaliAccentYellow)
            if (LocalDensity.current.fontScale >= 1.3f) {
                Column(Modifier.weight(1f)) {
                    Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Text(value, color = BaliAccentYellow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(label, Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text(value, color = BaliAccentYellow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Returns [value] formatted with Spanish thousands separators. */
internal fun points(value: Int): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-ES")).format(value)
