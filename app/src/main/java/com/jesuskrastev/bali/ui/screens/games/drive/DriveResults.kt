package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
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
 * Renders [result] as an arcade scoreboard: chequered-flag header, popping stars, a big score
 * counter, stat tiles and raised [onReplay] / [onExit] buttons that never scroll away.
 * [onStarShown] handles star feedback; returns the panel UI content.
 */
@Composable
internal fun DriveResultsContent(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val summary = result.summary
    val pending = result.rewardStatus == DriveRewardStatus.PENDING
    var revealedStars by remember(summary) { mutableIntStateOf(0) }
    var scoreTarget by remember(summary) { mutableIntStateOf(0) }
    var entered by remember(summary) { mutableStateOf(false) }
    val shownScore by animateIntAsState(scoreTarget, tween(900), label = "drive_result_score")
    val entrance by animateFloatAsState(
        if (entered) 1f else 0f,
        spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "drive_result_entrance",
    )
    LaunchedEffect(summary) {
        entered = true
        delay(260)
        scoreTarget = summary.score
        for (star in 1..summary.rating) {
            delay(280)
            revealedStars = star
            onStarShown(star)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Surface(
            modifier = Modifier.graphicsLayer {
                alpha = entrance.coerceIn(0f, 1f)
                scaleX = 0.82f + entrance * 0.18f
                scaleY = scaleX
            },
            shape = RoundedCornerShape(28.dp),
            color = BaliSecondary,
            border = BorderStroke(3.dp, BaliAccentYellow),
            shadowElevation = 20.dp,
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.94f)) {
                ResultHeader(summary.rating, revealedStars)

                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ScoreBoard(shownScore, summary.score, result)
                    val accuracy = summary.resolved * 100 / summary.situations.coerceAtLeast(1)
                    StatGrid(
                        listOf(
                            StatTile(DriveIconKind.CHECK, stringResource(R.string.drive_resolved), "${summary.resolved}/${summary.situations}"),
                            StatTile(DriveIconKind.STAR, stringResource(R.string.drive_collected), "${summary.starsCollected}/${summary.starsTotal}"),
                            StatTile(DriveIconKind.ACCURACY, stringResource(R.string.drive_accuracy), "$accuracy %"),
                            StatTile(DriveIconKind.TIME, stringResource(R.string.drive_duration), stringResource(R.string.drive_seconds, summary.durationSeconds)),
                        ),
                    )
                    when (result.rewardStatus) {
                        DriveRewardStatus.PENDING -> Text(stringResource(R.string.drive_rewards_pending), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        DriveRewardStatus.FAILED -> Text(stringResource(R.string.drive_rewards_failed), color = BaliAccentYellow, style = MaterialTheme.typography.bodySmall)
                        DriveRewardStatus.COMPLETE -> result.rewards?.let { rewards ->
                            val xp = rewards.xpEarned
                            StatGrid(
                                listOf(
                                    StatTile(DriveIconKind.XP, stringResource(R.string.drive_xp), "+${xp.xpGained}", highlight = true),
                                    StatTile(DriveIconKind.COIN, stringResource(R.string.drive_coins), "+${rewards.coinsGained}", highlight = true),
                                ),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.drive_xp_base, xp.baseXp), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                                xp.bonusPerfection?.let { Text(stringResource(R.string.drive_xp_perfect, it), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall) }
                                xp.bonusFast?.let { Text(stringResource(R.string.drive_xp_fast, it), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall) }
                                xp.bonusStreak?.let { Text(stringResource(R.string.drive_xp_streak, it), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall) }
                            }
                            if (xp.levelUp) LevelUpBanner(stringResource(R.string.drive_level_up, xp.newLevel))
                        }
                    }
                    if (summary.faults.isNotEmpty()) {
                        Text(stringResource(R.string.drive_faults, summary.faults.size), color = BaliAccentYellow, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
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
                    Modifier.fillMaxWidth().background(Color(0xFF111A2E)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ArcadeButton(onClick = onReplay, enabled = !pending, color = BaliPrimary, slab = Color(0xFFB8400F)) {
                        Icon(Icons.Rounded.Replay, contentDescription = null)
                        Text(stringResource(R.string.drive_replay), modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    }
                    ArcadeButton(onClick = onExit, enabled = !pending, color = Color(0xFF2B3A58), slab = Color(0xFF1A2540), compact = true) {
                        Text(stringResource(R.string.drive_exit), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Header with a chequered strip, the META title and [rating] stars of which [revealed] are lit.
 * @param rating stars earned, 1 to 3
 * @param revealed how many of them have popped in so far
 */
@Composable
private fun ResultHeader(rating: Int, revealed: Int) {
    Column(Modifier.fillMaxWidth()) {
        ChequeredStrip(Modifier.fillMaxWidth().height(14.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFFF7A3D), BaliPrimary)))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("BALI DRIVE", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DriveIcon(DriveIconKind.FLAG, Modifier.size(30.dp), Color.White)
                Text(
                    stringResource(R.string.drive_result_title),
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 2.sp,
                        shadow = Shadow(Color(0xFF8A2A08), Offset(0f, 6f), blurRadius = 0f),
                    ),
                )
                DriveIcon(DriveIconKind.FLAG, Modifier.size(30.dp), Color.White)
            }
            val ratingLabel = stringResource(R.string.drive_rating, rating)
            Row(
                Modifier.semantics { contentDescription = ratingLabel },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 1..3) {
                    // The middle star is the biggest, as on a podium.
                    val size = if (i == 2) 52.dp else 42.dp
                    val pop by animateFloatAsState(
                        if (i <= revealed) 1f else 0f,
                        spring(dampingRatio = 0.35f, stiffness = 500f),
                        label = "drive_result_star_$i",
                    )
                    Box(Modifier.size(size)) {
                        DriveIcon(DriveIconKind.STAR, Modifier.matchParentSize(), Color.Black.copy(alpha = 0.28f))
                        DriveIcon(
                            DriveIconKind.STAR,
                            Modifier.matchParentSize().graphicsLayer {
                                alpha = pop.coerceIn(0f, 1f)
                                scaleX = 0.3f + pop * 0.7f
                                scaleY = scaleX
                                rotationZ = (1f - pop) * -50f
                            },
                            BaliAccentYellow,
                        )
                    }
                }
            }
            Text(
                stringResource(
                    when (rating) {
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
    }
}

/** Draws a two-row black-and-white chequered finish line across [modifier]'s bounds. */
@Composable
private fun ChequeredStrip(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val square = size.height / 2f
        val columns = (size.width / square).toInt() + 1
        for (row in 0..1) {
            for (col in 0 until columns) {
                drawRect(
                    if ((row + col) % 2 == 0) Color.White else Color(0xFF111A2E),
                    topLeft = Offset(col * square, row * square),
                    size = Size(square, square),
                )
            }
        }
    }
}

/**
 * Big score counter with the record badge.
 * @param shown the score currently displayed while it counts up
 * @param score the final score
 * @param result supplies the previous best and whether this run beat it
 */
@Composable
private fun ScoreBoard(shown: Int, score: Int, result: DriveResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0B1220),
        border = BorderStroke(2.dp, BaliAccentYellow.copy(alpha = 0.8f)),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.drive_points).uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text(
                points(shown),
                color = BaliAccentYellow,
                style = TextStyle(
                    fontSize = 50.sp,
                    lineHeight = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    shadow = Shadow(BaliAccentYellow.copy(alpha = 0.55f), Offset.Zero, blurRadius = 24f),
                ),
            )
            val best = points(maxOf(score, result.previousBest))
            if (result.isNewRecord) {
                val blink by rememberInfiniteTransition(label = "drive_record_blink").animateFloat(
                    1f, 0.55f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "drive_record_alpha",
                )
                Row(
                    Modifier.graphicsLayer { alpha = blink }.clip(RoundedCornerShape(50)).background(BaliAccentYellow)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DriveIcon(DriveIconKind.RECORD, Modifier.size(16.dp), BaliSecondary)
                    Text(stringResource(R.string.drive_new_record) + " · " + best, color = BaliSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
            } else {
                Text(stringResource(R.string.drive_record) + " · " + best, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** One scoreboard tile: [kind] icon, [value] and [label]; [highlight] marks rewards. */
private data class StatTile(val kind: DriveIconKind, val label: String, val value: String, val highlight: Boolean = false)

/** Lays [tiles] out two per row, or one per row with large text; returns the grid. */
@Composable
private fun StatGrid(tiles: List<StatTile>) {
    val perRow = if (LocalDensity.current.fontScale >= 1.3f) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { StatTileView(it, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size < perRow) Spacer(Modifier.weight((perRow - row.size).toFloat()))
            }
        }
    }
}

/** Renders [tile] as a rounded scoreboard cell sized by [modifier]. */
@Composable
private fun StatTileView(tile: StatTile, modifier: Modifier = Modifier) {
    val accent = if (tile.highlight) BaliAccentYellow else Color.White.copy(alpha = 0.18f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (tile.highlight) Color(0xFF3A3316) else DrivePalette.Road.copy(alpha = 0.55f),
        border = BorderStroke(if (tile.highlight) 2.dp else 1.dp, accent),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DriveIcon(tile.kind, Modifier.size(26.dp), BaliAccentYellow)
            Column(Modifier.weight(1f)) {
                Text(tile.value, color = BaliAccentYellow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(tile.label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 13.sp)
            }
        }
    }
}

/** Shows [text] as a celebratory level-up ribbon. */
@Composable
private fun LevelUpBanner(text: String) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(BaliPrimary, BaliAccentYellow)))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = BaliSecondary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

/**
 * Chunky arcade button with a darker [slab] under it so it looks raised.
 * @param onClick action when pressed
 * @param enabled false while rewards save; the button dims
 * @param color face colour
 * @param slab colour of the base showing under the face
 * @param compact smaller padding and a thin border for the secondary action
 * @param content label and icon laid out in a row
 */
@Composable
private fun ArcadeButton(
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    slab: Color,
    compact: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Box(Modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else 0.5f }) {
        Box(Modifier.matchParentSize().padding(top = 5.dp).background(slab, RoundedCornerShape(18.dp)))
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp).semantics { role = Role.Button },
            shape = RoundedCornerShape(18.dp),
            color = color,
            contentColor = Color.White,
            border = BorderStroke(if (compact) 1.dp else 2.dp, if (compact) Color.White.copy(alpha = 0.5f) else BaliAccentYellow),
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = if (compact) 10.dp else 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Returns [value] formatted with Spanish thousands separators. */
internal fun points(value: Int): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-ES")).format(value)
