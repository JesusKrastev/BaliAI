package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.util.drawSafe
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private val GoldLight = Color(0xFFFFF3A6)
private val GoldDeep = Color(0xFFF59E0B)
private val AmberOutline = Color(0xFFB45309)
private val Ink = Color(0xFF1B1238)

/**
 * Covers the stopped game with the results screen and guards [onReplay] and [onExit] while rewards save.
 * It is a full-bleed overlay rather than a platform dialog so it can own the whole screen; it swallows
 * touches meant for the game underneath and handles the back gesture itself.
 * @param result the finished run
 * @param onReplay starts another run
 * @param onExit leaves the game
 * @param onStarShown plays feedback for each revealed star
 */
@Composable
internal fun DriveResultsDialog(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val pending = result.rewardStatus == DriveRewardStatus.PENDING
    BackHandler { if (!pending) onExit() }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xF2271A55), Color(0xF2140F33), Color(0xF20B1220))))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        DriveResultsContent(result, onReplay, onExit, onStarShown)
    }
}

/**
 * Renders [result] as a game-over showcase: a rotating sunburst behind a folded ribbon and an arc of
 * three glossy stars, a glowing score counter, pill-shaped stat badges that cascade in, falling
 * confetti, and rounded [onReplay] / [onExit] buttons that never scroll away.
 * @param result the finished run
 * @param onReplay starts another run
 * @param onExit leaves the game
 * @param onStarShown handles feedback for each star as it pops in
 */
@Composable
internal fun DriveResultsContent(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val summary = result.summary
    val pending = result.rewardStatus == DriveRewardStatus.PENDING
    var revealedStars by remember(summary) { mutableIntStateOf(0) }
    var scoreTarget by remember(summary) { mutableIntStateOf(0) }
    var entered by remember(summary) { mutableStateOf(false) }
    val shownScore by animateIntAsState(scoreTarget, tween(1100), label = "drive_result_score")
    val entrance by animateFloatAsState(
        if (entered) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "drive_result_entrance",
    )
    LaunchedEffect(summary) {
        entered = true
        delay(420)
        scoreTarget = summary.score
        for (star in 1..summary.rating) {
            delay(300)
            revealedStars = star
            onStarShown(star)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Confetti(summary.rating)
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize()
                .windowInsetsPadding(WindowInsets.drawSafe),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                Hero(summary.rating, revealedStars, entrance)
                ScoreBoard(shownScore, summary.score, result)
                val accuracy = summary.resolved * 100 / summary.situations.coerceAtLeast(1)
                StatGrid(
                    listOf(
                        StatTile(DriveIconKind.CHECK, stringResource(R.string.drive_resolved), "${summary.resolved}/${summary.situations}"),
                        StatTile(DriveIconKind.STAR, stringResource(R.string.drive_collected), "${summary.starsCollected}/${summary.starsTotal}"),
                        StatTile(DriveIconKind.ACCURACY, stringResource(R.string.drive_accuracy), "$accuracy %"),
                        StatTile(DriveIconKind.TIME, stringResource(R.string.drive_duration), stringResource(R.string.drive_seconds, summary.durationSeconds)),
                    ),
                    firstDelay = 900,
                )
                when (result.rewardStatus) {
                    DriveRewardStatus.PENDING -> Text(stringResource(R.string.drive_rewards_pending), color = Color.White, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    DriveRewardStatus.FAILED -> Text(stringResource(R.string.drive_rewards_failed), color = BaliAccentYellow, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    DriveRewardStatus.COMPLETE -> result.rewards?.let { rewards ->
                        val xp = rewards.xpEarned
                        StatGrid(
                            listOf(
                                StatTile(DriveIconKind.XP, stringResource(R.string.drive_xp), "+${xp.xpGained}", highlight = true),
                                StatTile(DriveIconKind.COIN, stringResource(R.string.drive_coins), "+${rewards.coinsGained}", highlight = true),
                            ),
                            firstDelay = 1350,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val soft = Color.White.copy(alpha = 0.8f)
                            Text(stringResource(R.string.drive_xp_base, xp.baseXp), color = soft, style = MaterialTheme.typography.bodySmall)
                            xp.bonusPerfection?.let { Text(stringResource(R.string.drive_xp_perfect, it), color = soft, style = MaterialTheme.typography.bodySmall) }
                            xp.bonusFast?.let { Text(stringResource(R.string.drive_xp_fast, it), color = soft, style = MaterialTheme.typography.bodySmall) }
                            xp.bonusStreak?.let { Text(stringResource(R.string.drive_xp_streak, it), color = soft, style = MaterialTheme.typography.bodySmall) }
                        }
                        if (xp.levelUp) LevelUpBanner(stringResource(R.string.drive_level_up, xp.newLevel))
                    }
                }
                if (summary.faults.isNotEmpty()) {
                    Text(stringResource(R.string.drive_faults, summary.faults.size), color = BaliAccentYellow, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    summary.faults.forEachIndexed { index, explanation ->
                        Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.09f)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).background(BaliAccentRed.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
                                    DriveIcon(DriveIconKind.FAULT, Modifier.size(22.dp), BaliAccentRed)
                                }
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
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 6.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PillButton(onClick = onReplay, enabled = !pending) {
                    Icon(Icons.Rounded.Replay, contentDescription = null)
                    Text(stringResource(R.string.drive_replay), modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                }
                Surface(
                    onClick = onExit,
                    enabled = !pending,
                    modifier = Modifier.graphicsLayer { alpha = if (pending) 0.5f else 1f }.semantics { role = Role.Button },
                    shape = CircleShape,
                    color = Color.Transparent,
                    contentColor = Color.White,
                ) {
                    Text(stringResource(R.string.drive_exit), Modifier.padding(horizontal = 28.dp, vertical = 10.dp), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

/**
 * Ribbon title over a sunburst, with [rating] stars arched beneath it.
 * @param rating stars earned, 1 to 3
 * @param revealed how many of them have popped in so far
 * @param entrance 0..1 spring that drops the ribbon in
 */
@Composable
private fun Hero(rating: Int, revealed: Int, entrance: Float) {
    val spin by rememberInfiniteTransition(label = "drive_sunburst").animateFloat(
        0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "drive_sunburst_angle",
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height * 0.62f)
            val radius = size.width * 0.9f
            val rays = 14
            val ray = Brush.radialGradient(
                listOf(BaliAccentYellow.copy(alpha = 0.45f), Color.Transparent),
                center = center,
                radius = radius,
            )
            for (i in 0 until rays) {
                val a = (spin + i * 360f / rays) * PI.toFloat() / 180f
                val half = 0.085f
                val path = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + cos(a - half) * radius, center.y + sin(a - half) * radius)
                    lineTo(center.x + cos(a + half) * radius, center.y + sin(a + half) * radius)
                    close()
                }
                drawPath(path, ray)
            }
            drawCircle(
                Brush.radialGradient(listOf(BaliPrimary.copy(alpha = 0.55f), Color.Transparent), center = center, radius = size.width * 0.5f),
                radius = size.width * 0.5f,
                center = center,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("BALI DRIVE", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Ribbon(
                Modifier.graphicsLayer {
                    alpha = entrance.coerceIn(0f, 1f)
                    translationY = (1f - entrance) * -80.dp.toPx()
                    rotationZ = -2.5f
                },
            ) {
                DriveIcon(DriveIconKind.FLAG, Modifier.size(26.dp), Color.White)
                Text(
                    stringResource(R.string.drive_result_title),
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 2.sp,
                        shadow = Shadow(Color(0xFF7A2208), Offset(0f, 6f), blurRadius = 0f),
                    ),
                )
                DriveIcon(DriveIconKind.FLAG, Modifier.size(26.dp), Color.White)
            }
            val ratingLabel = stringResource(R.string.drive_rating, rating)
            Row(
                Modifier.semantics { contentDescription = ratingLabel }.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy((-6).dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                for (i in 1..3) {
                    // An arc: the middle star is the biggest and highest, the sides tilt away.
                    val size = if (i == 2) 104.dp else 74.dp
                    val pop by animateFloatAsState(
                        if (i <= revealed) 1f else 0f,
                        spring(dampingRatio = 0.35f, stiffness = 420f),
                        label = "drive_result_star_$i",
                    )
                    TrophyStar(size, pop, tilt = (i - 2) * 14f, lift = if (i == 2) 0.dp else (-6).dp)
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
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * A folded orange ribbon with swallow-tail ends drawn behind [content].
 * @param modifier placement and entrance transform
 * @param content row laid out over the band
 */
@Composable
private fun Ribbon(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Box(modifier.fillMaxWidth().padding(horizontal = 4.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val drop = 14.dp.toPx()
            val bandBottom = h - drop
            val tailW = w * 0.15f
            val inset = w * 0.07f
            val join = Stroke(width = 8f, join = StrokeJoin.Round)
            val tailColor = Color(0xFFC2410C)
            for (side in listOf(-1f, 1f)) {
                val edge = if (side < 0) 0f else w
                val inner = if (side < 0) tailW else w - tailW
                val notch = if (side < 0) tailW * 0.3f else w - tailW * 0.3f
                val tail = Path().apply {
                    moveTo(edge, drop)
                    lineTo(inner, drop)
                    lineTo(inner, h)
                    lineTo(edge, h)
                    lineTo(notch, (drop + h) / 2f)
                    close()
                }
                drawPath(tail, tailColor)
                drawPath(tail, tailColor, style = join)
                val fx = if (side < 0) inset else w - inset
                val fold = Path().apply {
                    moveTo(fx, bandBottom)
                    lineTo(inner, bandBottom)
                    lineTo(inner, h)
                    close()
                }
                drawPath(fold, Color(0xFF6E1F06))
            }
            val band = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(inset, 0f, w - inset, bandBottom, CornerRadius(10f)))
            }
            val face = Brush.verticalGradient(listOf(Color(0xFFFFA25F), BaliPrimary, Color(0xFFE5501A)), startY = 0f, endY = bandBottom)
            drawPath(band, face)
            drawPath(band, face, style = Stroke(6f, join = StrokeJoin.Round))
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), startY = 0f, endY = bandBottom * 0.5f),
                topLeft = Offset(inset + 8f, 5f),
                size = Size(w - inset * 2 - 16f, bandBottom * 0.45f),
                cornerRadius = CornerRadius(14f),
            )
        }
        Row(
            Modifier.padding(horizontal = 40.dp).padding(top = 4.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Builds a five-point star centred on [center] with the given [outer] and [inner] radii. */
private fun starPath(center: Offset, outer: Float, inner: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5
        val r = if (i % 2 == 0) outer else inner
        val x = center.x + cos(a).toFloat() * r
        val y = center.y + sin(a).toFloat() * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * One glossy rounded star: a dim socket that is always drawn and a golden star that pops over it.
 * @param size box side
 * @param pop 0..1 progress of the pop-in; 0 leaves only the socket
 * @param tilt resting rotation in degrees, to arch the row
 * @param lift vertical offset applied to the whole star
 */
@Composable
private fun TrophyStar(size: Dp, pop: Float, tilt: Float, lift: Dp) {
    Box(Modifier.size(size).offset(y = lift).graphicsLayer { rotationZ = tilt }) {
        Canvas(Modifier.matchParentSize()) {
            val outer = this.size.minDimension * 0.4f
            val path = starPath(center, outer, outer * 0.5f)
            val rounded = Stroke(outer * 0.34f, join = StrokeJoin.Round)
            drawPath(path, Color.Black.copy(alpha = 0.3f), style = rounded)
            drawPath(path, Color.White.copy(alpha = 0.1f), style = rounded)
            drawPath(path, Color.White.copy(alpha = 0.1f), style = Fill)
        }
        Canvas(
            Modifier.matchParentSize().graphicsLayer {
                alpha = pop.coerceIn(0f, 1f)
                scaleX = 0.3f + pop * 0.7f
                scaleY = scaleX
                rotationZ = (1f - pop) * -60f
            },
        ) {
            val outer = this.size.minDimension * 0.4f
            val path = starPath(center, outer, outer * 0.5f)
            drawCircle(
                Brush.radialGradient(listOf(BaliAccentYellow.copy(alpha = 0.55f), Color.Transparent), center = center, radius = outer * 1.5f),
                radius = outer * 1.5f,
            )
            drawPath(path, AmberOutline, style = Stroke(outer * 0.42f, join = StrokeJoin.Round))
            val gold = Brush.verticalGradient(listOf(GoldLight, BaliAccentYellow, GoldDeep), startY = center.y - outer, endY = center.y + outer)
            drawPath(path, gold, style = Stroke(outer * 0.3f, join = StrokeJoin.Round))
            drawPath(path, gold, style = Fill)
            drawCircle(Color.White.copy(alpha = 0.6f), radius = outer * 0.11f, center = center + Offset(-outer * 0.3f, -outer * 0.32f))
            drawCircle(Color.White.copy(alpha = 0.35f), radius = outer * 0.05f, center = center + Offset(-outer * 0.14f, -outer * 0.5f))
        }
    }
}

/**
 * Falling confetti behind the screen; the better the [rating], the more pieces.
 * @param rating stars earned, 1 to 3
 */
@Composable
private fun Confetti(rating: Int) {
    val colors = listOf(BaliAccentYellow, BaliPrimary, Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFF4ADE80), Color.White)
    val pieces = remember(rating) {
        val random = Random(rating)
        List(rating * 9) {
            ConfettiPiece(
                x = random.nextFloat(),
                phase = random.nextFloat(),
                speed = 1 + random.nextInt(2),
                width = 5f + random.nextFloat() * 6f,
                height = 9f + random.nextFloat() * 8f,
                color = colors[random.nextInt(colors.size)],
                spin = 1 + random.nextInt(3),
            )
        }
    }
    val t by rememberInfiniteTransition(label = "drive_confetti").animateFloat(
        0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "drive_confetti_t",
    )
    Canvas(Modifier.fillMaxSize()) {
        pieces.forEach { p ->
            val progress = (t * p.speed + p.phase) % 1f
            val y = progress * (size.height + 60f) - 30f
            val x = p.x * size.width + sin(2 * PI * (t * p.speed + p.phase)).toFloat() * 18f
            rotate(degrees = t * 360f * p.spin + p.phase * 360f, pivot = Offset(x, y)) {
                drawRoundRect(p.color.copy(alpha = 0.85f), Offset(x, y), Size(p.width, p.height), CornerRadius(2f))
            }
        }
    }
}

/** One confetti fleck; [speed] and [spin] are whole numbers so the loop restarts without a jump. */
private data class ConfettiPiece(
    val x: Float, val phase: Float, val speed: Int, val width: Float, val height: Float, val color: Color, val spin: Int,
)

/**
 * Big glowing score counter with the record badge.
 * @param shown the score currently displayed while it counts up
 * @param score the final score
 * @param result supplies the previous best and whether this run beat it
 */
@Composable
private fun ScoreBoard(shown: Int, score: Int, result: DriveResult) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val line = Modifier.width(32.dp).height(2.dp)
            Box(line.background(Brush.horizontalGradient(listOf(Color.Transparent, BaliAccentYellow))))
            Text(stringResource(R.string.drive_points).uppercase(), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Box(line.background(Brush.horizontalGradient(listOf(BaliAccentYellow, Color.Transparent))))
        }
        Text(
            points(shown),
            style = TextStyle(
                brush = Brush.verticalGradient(listOf(GoldLight, BaliAccentYellow, BaliPrimary)),
                fontSize = 64.sp,
                lineHeight = 68.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                shadow = Shadow(BaliAccentYellow.copy(alpha = 0.6f), Offset.Zero, blurRadius = 36f),
            ),
        )
        val best = points(maxOf(score, result.previousBest))
        if (result.isNewRecord) {
            val blink by rememberInfiniteTransition(label = "drive_record_blink").animateFloat(
                1f, 0.6f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "drive_record_alpha",
            )
            Row(
                Modifier.graphicsLayer { alpha = blink }.clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(GoldLight, BaliAccentYellow, GoldDeep)))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DriveIcon(DriveIconKind.RECORD, Modifier.size(18.dp), Ink)
                Text(stringResource(R.string.drive_new_record) + " · " + best, color = Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
        } else {
            Text(stringResource(R.string.drive_record) + " · " + best, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** One stat badge: [kind] icon, [value] and [label]; [highlight] marks rewards. */
private data class StatTile(val kind: DriveIconKind, val label: String, val value: String, val highlight: Boolean = false)

/**
 * Lays [tiles] out two per row, or one per row with large text; the pills cascade in.
 * @param tiles badges to show
 * @param firstDelay milliseconds before the first pill appears; each next one follows 90 ms later
 */
@Composable
private fun StatGrid(tiles: List<StatTile>, firstDelay: Int) {
    val perRow = if (LocalDensity.current.fontScale >= 1.3f) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(perRow).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { col, tile ->
                    StatPill(tile, firstDelay + (rowIndex * perRow + col) * 90L, Modifier.weight(1f).fillMaxHeight())
                }
                if (row.size < perRow) Spacer(Modifier.weight((perRow - row.size).toFloat()))
            }
        }
    }
}

/**
 * Renders [tile] as a fully rounded pill with a round icon badge; rewards get a gold face.
 * @param tile what to show
 * @param delayMs wait before the pill springs in
 * @param modifier size and position
 */
@Composable
private fun StatPill(tile: StatTile, delayMs: Long, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs); shown = true }
    val pop by animateFloatAsState(if (shown) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "drive_stat_pop")
    val face = if (tile.highlight) {
        Brush.horizontalGradient(listOf(GoldLight, BaliAccentYellow, GoldDeep))
    } else {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f)))
    }
    val textColor = if (tile.highlight) Ink else Color.White
    Row(
        modifier.graphicsLayer {
            alpha = pop.coerceIn(0f, 1f)
            translationY = (1f - pop) * 36.dp.toPx()
            scaleX = 0.85f + 0.15f * pop
            scaleY = scaleX
        }.clip(CircleShape).background(face).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(42.dp).background(if (tile.highlight) Ink else Color.Black.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            DriveIcon(tile.kind, Modifier.size(24.dp), BaliAccentYellow)
        }
        Column(Modifier.weight(1f)) {
            Text(tile.value, color = if (tile.highlight) Ink else BaliAccentYellow, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(tile.label, color = textColor.copy(alpha = if (tile.highlight) 0.8f else 0.85f), fontSize = 11.sp, lineHeight = 13.sp)
        }
    }
}

/** Shows [text] as a celebratory pill-shaped level-up ribbon. */
@Composable
private fun LevelUpBanner(text: String) {
    Box(
        Modifier.fillMaxWidth().clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(BaliPrimary, BaliAccentYellow, BaliPrimary)))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

/**
 * Glossy pill button with a darker slab under it so it looks raised and pressable.
 * @param onClick action when pressed
 * @param enabled false while rewards save; the button dims
 * @param content label and icon laid out in a row
 */
@Composable
private fun PillButton(onClick: () -> Unit, enabled: Boolean, content: @Composable RowScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else 0.5f }) {
        Box(Modifier.matchParentSize().padding(top = 6.dp).background(Color(0xFF9A3412), CircleShape))
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).semantics { role = Role.Button },
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = Color.White,
            border = BorderStroke(2.dp, Brush.verticalGradient(listOf(GoldLight, BaliAccentYellow))),
        ) {
            Row(
                Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFF9457), BaliPrimary, Color(0xFFE5501A))))
                    .drawBehind {
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.Transparent), endY = size.height * 0.5f),
                            topLeft = Offset(size.height * 0.3f, 4.dp.toPx()),
                            size = Size(size.width - size.height * 0.6f, size.height * 0.42f),
                            cornerRadius = CornerRadius(size.height),
                        )
                    }
                    .padding(horizontal = 24.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Returns [value] formatted with Spanish thousands separators. */
internal fun points(value: Int): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-ES")).format(value)
