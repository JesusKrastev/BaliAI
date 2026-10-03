package com.jesuskrastev.bali.ui.screens.ranks

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.domain.model.RankReward
import com.jesuskrastev.bali.domain.model.RankTier
import com.jesuskrastev.bali.ui.theme.BaliBackgroundGradient

private val GOLD_LIGHT = Color(0xFFFFD54F)
private val GOLD_DARK = Color(0xFFFF9F1C)
private val ROAD_DASH = Color.White.copy(alpha = 0.85f)

private val PRIZE_ROW_HEIGHT = 128.dp
private val RANK_ROW_HEIGHT = 250.dp
private val PRIZE_NODE = 76.dp
private val ROAD_WIDTH = 22.dp

/** Horizontal position of a stop's centre, as a fraction of the row width. */
private fun PathStop.xFraction(): Float = when (this) {
    is PathStop.Rank -> 0.5f
    is PathStop.Prize -> if (side == 0) 0.27f else 0.73f
}

/** Renders the rank road, using [viewModel] for claims and [onBackClick] for navigation. */
@Composable
fun RankRewardsScreen(onBackClick: () -> Unit, viewModel: RankRewardsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RankRewardsContent(
        state = state,
        onBackClick = onBackClick,
        onClaim = viewModel::claim,
        onMessageShown = viewModel::messageShown
    )
}

/**
 * The rank road: a header with the current rank, then a winding road where every rank is a
 * milestone and the coin prizes earned on the way to the next rank are stops between them.
 * The stretch already driven is painted in the brand colour; claimable prizes pulse.
 *
 * @param state XP, claimed prizes and the claim in progress
 * @param onBackClick closes the screen
 * @param onClaim collects the prize with that id
 * @param onMessageShown marks the snackbar message as shown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankRewardsContent(
    state: RankRewardsUiState,
    onBackClick: () -> Unit,
    onClaim: (String) -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); onMessageShown() }
    }
    val stops = remember { rankPath() }
    val listState = rememberLazyListState()
    var scrolled by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val claim: (String) -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onClaim(it)
    }
    val nextGoal = stops.indexOfFirst { it.requiredXp > state.xp }

    // Opens the road where the user is: the first prize to collect, else the next one to reach.
    LaunchedEffect(state.isLoaded) {
        if (!state.isLoaded || scrolled) return@LaunchedEffect
        val claimable = stops.indexOfFirst { stop ->
            stop.prizes().any { prizeState(it, state.xp, state.claimedIds) == PrizeState.Claimable }
        }
        val target = claimable.takeIf { it >= 0 } ?: nextGoal.takeIf { it >= 0 } ?: stops.lastIndex
        if (target > 1) listState.scrollToItem(target)
        scrolled = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Camino de premios", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(BaliBackgroundGradient())
                .padding(padding)
        ) {
            RankHeader(
                xp = state.xp,
                claimable = RankProgression.rewards.count {
                    prizeState(it, state.xp, state.claimedIds) == PrizeState.Claimable
                }
            )
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = "start") { Spacer(Modifier.height(12.dp)) }
                itemsIndexed(stops, key = { _, stop -> stop.key() }) { index, stop ->
                    PathRow(
                        stop = stop,
                        previous = stops.getOrNull(index - 1),
                        next = stops.getOrNull(index + 1),
                        xp = state.xp,
                        claimedIds = state.claimedIds,
                        claimingId = state.claimingId,
                        isNextGoal = index == nextGoal,
                        onClaim = claim
                    )
                }
                item(key = "finish") { FinishLine(reached = state.xp >= stops.last().requiredXp) }
            }
        }
    }
}

private fun PathStop.key(): String = when (this) {
    is PathStop.Rank -> "rank_${rank.id}"
    is PathStop.Prize -> "prize_${reward.id}"
}

private fun PathStop.prizes(): List<RankReward> = when (this) {
    is PathStop.Rank -> listOfNotNull(prize)
    is PathStop.Prize -> listOf(reward)
}

/** Current rank, XP, how far the next rank is and how many prizes wait to be collected. */
@Composable
private fun RankHeader(xp: Int, claimable: Int) {
    val current = RankProgression.rankFor(xp)
    val next = RankProgression.ranks.firstOrNull { it.requiredXp > xp }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(current.badgeRes()), contentDescription = null, modifier = Modifier.size(76.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "TU RANGO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(current.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                if (next != null) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (xp - current.requiredXp).toFloat() / (next.requiredXp - current.requiredXp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        strokeCap = StrokeCap.Round,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {}
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$xp XP · te faltan ${next.requiredXp - xp} para ${next.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("$xp XP · ¡rango máximo!", style = MaterialTheme.typography.labelMedium)
                }
                if (claimable > 0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = GOLD_DARK) {
                        Text(
                            if (claimable == 1) "🎁 1 premio por recoger" else "🎁 $claimable premios por recoger",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * One stop with its stretch of road: half a curve in from the previous stop and half a curve out
 * to the next, meeting at the midpoints so the road is continuous from row to row.
 */
@Composable
private fun PathRow(
    stop: PathStop,
    previous: PathStop?,
    next: PathStop?,
    xp: Int,
    claimedIds: Set<String>,
    claimingId: String?,
    isNextGoal: Boolean,
    onClaim: (String) -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(if (stop is PathStop.Rank) RANK_ROW_HEIGHT else PRIZE_ROW_HEIGHT)
    ) {
        Road(
            fromX = previous?.let { (it.xFraction() + stop.xFraction()) / 2 },
            atX = stop.xFraction(),
            toX = next?.let { (it.xFraction() + stop.xFraction()) / 2 },
            drivenIn = xp >= stop.requiredXp,
            drivenOut = next != null && xp >= next.requiredXp
        )
        when (stop) {
            is PathStop.Rank -> RankMilestone(
                stop = stop,
                xp = xp,
                claimedIds = claimedIds,
                claimingId = claimingId,
                isNextGoal = isNextGoal,
                onClaim = onClaim,
                modifier = Modifier.align(Alignment.Center)
            )
            is PathStop.Prize -> PrizeStop(
                stop = stop,
                state = prizeState(stop.reward, xp, claimedIds),
                claiming = claimingId == stop.reward.id,
                missingXp = if (isNextGoal) stop.requiredXp - xp else null,
                onClaim = { onClaim(stop.reward.id) },
                centre = maxWidth * stop.xFraction(),
                rowWidth = maxWidth
            )
        }
    }
}

/** The road for one row, drawn in brand colour where driven and grey where still ahead. */
@Composable
private fun Road(fromX: Float?, atX: Float, toX: Float?, drivenIn: Boolean, drivenOut: Boolean) {
    val driven = MaterialTheme.colorScheme.primary
    val ahead = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        val dash = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 12.dp.toPx()))

        fun half(startX: Float, startY: Float, endX: Float, endY: Float, isDriven: Boolean) {
            val midY = (startY + endY) / 2
            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(startX, midY, endX, midY, endX, endY)
            }
            drawPath(path, if (isDriven) driven else ahead, style = Stroke(ROAD_WIDTH.toPx(), cap = StrokeCap.Round))
            drawPath(
                path,
                if (isDriven) ROAD_DASH else Color.White.copy(alpha = 0.45f),
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash)
            )
        }

        fromX?.let { half(it * w, 0f, atX * w, h / 2, drivenIn) }
        toX?.let { half(atX * w, h / 2, it * w, h, drivenOut) }
    }
}

/**
 * A coin prize on the road, with its amount and status on the other side of the road.
 *
 * @param missingXp XP left to reach it when it is the next goal, else null
 * @param centre horizontal centre of the node within the row
 * @param rowWidth width of the row
 */
@Composable
private fun PrizeStop(
    stop: PathStop.Prize,
    state: PrizeState,
    claiming: Boolean,
    missingXp: Int?,
    onClaim: () -> Unit,
    centre: Dp,
    rowWidth: Dp
) {
    val onLeft = stop.side == 0
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = centre - PRIZE_NODE / 2)
        ) {
            PrizeNode(state = state, claiming = claiming, onClaim = onClaim)
        }
        Column(
            modifier = Modifier
                .align(if (onLeft) Alignment.CenterEnd else Alignment.CenterStart)
                .width(rowWidth * 0.46f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = if (onLeft) Alignment.Start else Alignment.End
        ) {
            PrizeLabel(reward = stop.reward, state = state, missingXp = missingXp, alignEnd = !onLeft)
        }
    }
}

/** Coins and status text for a prize. */
@Composable
private fun PrizeLabel(reward: RankReward, state: PrizeState, missingXp: Int?, alignEnd: Boolean) {
    val locked = state == PrizeState.Locked
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.coin),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = if (locked) grayscale() else null
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "+${reward.coins}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else GOLD_DARK
        )
    }
    Text(
        when {
            state == PrizeState.Claimed -> "Recogido"
            state == PrizeState.Claimable -> "¡Toca para recoger!"
            missingXp != null -> "Te faltan $missingXp XP"
            else -> "A los ${reward.requiredXp} XP"
        },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (state == PrizeState.Claimable || missingXp != null) FontWeight.Bold else FontWeight.Normal,
        color = if (state == PrizeState.Claimable || missingXp != null) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

/** The round coin bubble: gold and pulsing when claimable, ticked when claimed, grey when locked. */
@Composable
private fun PrizeNode(state: PrizeState, claiming: Boolean, onClaim: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "prize_pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (state == PrizeState.Claimable) 1.1f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "prize_scale"
    )
    val fill = when (state) {
        PrizeState.Claimable -> Brush.linearGradient(listOf(GOLD_LIGHT, GOLD_DARK))
        PrizeState.Claimed -> SolidBrush(MaterialTheme.colorScheme.primaryContainer)
        PrizeState.Locked -> SolidBrush(MaterialTheme.colorScheme.surfaceVariant)
    }
    val ring = when (state) {
        PrizeState.Claimable -> Color.White
        PrizeState.Claimed -> MaterialTheme.colorScheme.primary
        PrizeState.Locked -> MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .size(PRIZE_NODE)
            .scale(scale)
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .padding(4.dp)
            .background(fill, CircleShape)
            .border(3.dp, ring, CircleShape)
            .clickable(enabled = state == PrizeState.Claimable && !claiming, role = Role.Button, onClick = onClaim),
        contentAlignment = Alignment.Center
    ) {
        if (claiming) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = Color.White)
        } else {
            Image(
                painterResource(R.drawable.coin),
                contentDescription = when (state) {
                    PrizeState.Claimable -> "Recoger premio"
                    PrizeState.Claimed -> "Premio recogido"
                    PrizeState.Locked -> "Premio bloqueado"
                },
                modifier = Modifier
                    .size(PRIZE_NODE * 0.5f)
                    .alpha(if (state == PrizeState.Claimed) 0.5f else 1f),
                colorFilter = if (state == PrizeState.Locked) grayscale() else null
            )
            StateBadge(state, Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Suppress("FunctionName")
private fun SolidBrush(color: Color): Brush = Brush.linearGradient(listOf(color, color))

/** Small corner badge: a tick when claimed, a lock when locked. */
@Composable
private fun StateBadge(state: PrizeState, modifier: Modifier = Modifier) {
    val (icon, tint) = when (state) {
        PrizeState.Claimed -> Icons.Rounded.Check to MaterialTheme.colorScheme.primary
        PrizeState.Locked -> Icons.Rounded.Lock to MaterialTheme.colorScheme.onSurfaceVariant
        PrizeState.Claimable -> return
    }
    Box(
        modifier = modifier
            .size(22.dp)
            .background(tint, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
    }
}

/**
 * A rank as a milestone on the road: its badge, name, the XP to reach it, its own prize and what
 * the road pays on the way to the next rank.
 */
@Composable
private fun RankMilestone(
    stop: PathStop.Rank,
    xp: Int,
    claimedIds: Set<String>,
    claimingId: String?,
    isNextGoal: Boolean,
    onClaim: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reached = xp >= stop.rank.requiredXp
    val isCurrent = RankProgression.rankFor(xp) == stop.rank
    Surface(
        modifier = modifier.widthIn(min = 230.dp, max = 290.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isCurrent) 10.dp else 3.dp,
        border = BorderStroke(
            if (isCurrent) 3.dp else 1.dp,
            if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCurrent) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        "ESTÁS AQUÍ",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Image(
                painterResource(stop.rank.badgeRes()),
                contentDescription = null,
                modifier = Modifier
                    .size(if (isCurrent) 84.dp else 72.dp)
                    .alpha(if (reached) 1f else 0.6f),
                colorFilter = if (reached) null else grayscale()
            )
            Text(stop.rank.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                when {
                    stop.rank.requiredXp == 0 -> "Tu punto de partida"
                    reached -> "Conseguido a los ${stop.rank.requiredXp} XP"
                    isNextGoal -> "Te faltan ${stop.rank.requiredXp - xp} XP"
                    else -> "A los ${stop.rank.requiredXp} XP"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isNextGoal) FontWeight.Bold else FontWeight.Normal,
                color = if (isNextGoal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            stop.prize?.let { prize ->
                Spacer(Modifier.height(10.dp))
                RankPrizeButton(
                    prize = prize,
                    state = prizeState(prize, xp, claimedIds),
                    claiming = claimingId == prize.id,
                    onClaim = { onClaim(prize.id) }
                )
            }
            if (stop.prizesOnTheWay.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val count = stop.prizesOnTheWay.size
                Text(
                    "Por el camino: ${if (count == 1) "1 premio" else "$count premios"} · " +
                        "${stop.prizesOnTheWay.sumOf { it.coins }} monedas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** The prize for reaching a rank, as a pill that becomes a button when it can be collected. */
@Composable
private fun RankPrizeButton(prize: RankReward, state: PrizeState, claiming: Boolean, onClaim: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val background = when (state) {
        PrizeState.Claimable -> Modifier
            .background(Brush.horizontalGradient(listOf(GOLD_LIGHT, GOLD_DARK)), shape)
            .clickable(enabled = !claiming, role = Role.Button, onClick = onClaim)
        PrizeState.Claimed -> Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape)
        PrizeState.Locked -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape)
    }
    Row(
        modifier = background.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            claiming -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
            state == PrizeState.Claimed ->
                Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            state == PrizeState.Locked ->
                Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Image(painterResource(R.drawable.coin), null, Modifier.size(18.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            when (state) {
                PrizeState.Claimable -> "Recoger +${prize.coins}"
                PrizeState.Claimed -> "+${prize.coins} recogidas"
                PrizeState.Locked -> "Premio de rango: +${prize.coins}"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = when (state) {
                PrizeState.Claimable -> Color.White
                PrizeState.Claimed -> MaterialTheme.colorScheme.primary
                PrizeState.Locked -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** The end of the road. */
@Composable
private fun FinishLine(reached: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = if (reached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (reached) "¡Has completado el camino!" else "Meta: completa el camino entero",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black
        )
    }
}

private fun grayscale(): ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Returns the generated badge drawable for this rank's stable id. */
fun RankTier.badgeRes(): Int = when (id) {
    "aprendiz" -> R.drawable.rank_aprendiz
    "conductor" -> R.drawable.rank_conductor
    "explorador" -> R.drawable.rank_explorador
    "piloto" -> R.drawable.rank_piloto
    "experto" -> R.drawable.rank_experto
    "maestro" -> R.drawable.rank_maestro
    else -> R.drawable.rank_leyenda
}
