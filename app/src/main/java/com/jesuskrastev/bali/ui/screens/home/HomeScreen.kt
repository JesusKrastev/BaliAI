package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliFlameColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the home dashboard: the plan chip and the streak/coins status at the top, and the
 * scrollable learning-path graph. The AI-tutor chat opens from the app's bottom bar. The account
 * menu that used to open from here as a side drawer (profile, legal links, sign out) now lives in
 * the Settings tab. The full countdown to the exam lives in the statistics screen; Home only shows
 * it as the small [HomePlanChip], which never pushes or covers the path.
 *
 * @param viewModel supplies [HomeUiState] and drives path generation
 * @param planViewModel supplies the plan chip's state
 * @param onNodeTestClick invoked with a path node's title, description, id, and node-type name,
 *   when it is tapped or opened from the plan sheet's study button; exam nodes open the mock exam
 *   directly, since it costs no coins
 * @param onShopClick opens the coin shop
 * @param onStreakClick opens the streak detail screen
 * @param onSeePlanClick opens the statistics tab, from the plan sheet
 * @param pathUnlockViewModel tells the path which nodes opened since Home last showed it
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    planViewModel: HomePlanViewModel = hiltViewModel(),
    onNodeTestClick: (String, String?, String, String) -> Unit = { _, _, _, _ -> },
    onShopClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    onSeePlanClick: () -> Unit = {},
    pathUnlockViewModel: PathUnlockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nextNode = uiState.pathNodes.firstOrNull { it.status == NodeStatus.UNLOCKED }
    val unlockState by pathUnlockViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            UserStatusRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                streak = uiState.streak,
                practicedToday = uiState.practicedToday,
                coinsCount = uiState.coinsCount,
                onCoinsClick = onShopClick,
                onStreakClick = onStreakClick,
                leading = {
                    HomePlanChip(
                        viewModel = planViewModel,
                        onStartSession = nextNode?.let { node ->
                            { onNodeTestClick(node.title, node.description, node.id, node.nodeType.name) }
                        },
                        onSeePlan = onSeePlanClick
                    )
                }
            )
        }
    ) { paddingValues ->
        LearningPathGraph(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            // Held back until the unlock check is done, so a node that is about to animate open
            // is not drawn open for a frame first.
            pathNodes = if (unlockState.isReady) uiState.pathNodes else emptyList(),
            isPathLoading = uiState.isPathLoading,
            unlock = unlockState.unlock,
            onUnlockPlayed = pathUnlockViewModel::onUnlockPlayed,
            onNodeClick = { node ->
                onNodeTestClick(node.title, node.description, node.id, node.nodeType.name)
            },
            onGenerateClick = {
                viewModel.generateNextPathNodesCount()
            }
        )
    }
}

/**
 * Shows the streak and coins pills anchored to the top end of Home, with [leading] content (the
 * plan chip) at the start. [leading] takes only the width the pills leave free.
 *
 * @param modifier layout modifier applied to the row
 * @param streak current daily streak count
 * @param practicedToday whether today already counts; the flame stays grey until it does
 * @param coinsCount current coin balance
 * @param onCoinsClick opens the coin shop
 * @param onStreakClick opens the streak detail screen
 * @param leading content placed at the start of the row
 */
@Composable
fun UserStatusRow(
    modifier: Modifier = Modifier,
    streak: Int,
    practicedToday: Boolean = true,
    coinsCount: Int,
    onCoinsClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    leading: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) { leading() }

        StatusPill(onClick = onStreakClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp), spacing = 4.dp) {
            Image(
                painter = painterResource(id = R.drawable.streak_icon),
                contentDescription = null,
                colorFilter = if (practicedToday) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                alpha = if (practicedToday) 1f else 0.5f,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$streak",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelLarge
            )
        }

        StatusPill(onClick = onCoinsClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), spacing = 6.dp) {
            Image(
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
                painter = painterResource(id = R.drawable.coin),
                contentDescription = null,
            )
            Text(
                text = "$coinsCount",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelLarge
            )
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Rounded, tappable pill used by [UserStatusRow] for each status counter.
 *
 * @param onClick invoked when the pill is tapped
 * @param contentPadding padding between the pill's border and its content
 * @param spacing horizontal gap between the content items
 * @param content row content laid out inside the pill
 */
@Composable
private fun StatusPill(
    onClick: () -> Unit,
    contentPadding: PaddingValues,
    spacing: Dp,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
}

/** Horizontal zigzag offsets applied to consecutive path nodes, indexed by `unitIndex % 4`. */
private val PathZigzagOffsets = listOf(0.dp, 60.dp, 0.dp, (-60).dp)

/** Amber outline drawn around the currently unlocked path node. */
private val UnlockedNodeBorder = Color(0xFFF59E0B)

/**
 * Scrollable learning path: sticky section headers, zigzag-laid-out lesson nodes and a popup
 * anchored to the tapped node. Shows a loading state while the first nodes are generated and
 * renders nothing when there are no nodes and nothing is loading.
 *
 * @param modifier layout modifier applied to the container
 * @param pathNodes every node of the path, in display order
 * @param isPathLoading true while new nodes are being generated
 * @param onNodeClick invoked when the popup's action button is tapped for a node
 * @param onGenerateClick requests a new batch of nodes once the whole path is completed
 * @param unlock nodes that opened since Home last showed the path: the connectors into them fill
 *   in one after another and each new node pops open; null for no animation
 * @param onUnlockPlayed invoked once that animation has finished
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningPathGraph(
    modifier: Modifier = Modifier,
    pathNodes: List<LessonNode>,
    isPathLoading: Boolean,
    onNodeClick: (LessonNode) -> Unit,
    onGenerateClick: () -> Unit,
    unlock: PathUnlock? = null,
    onUnlockPlayed: () -> Unit = {}
) {
    if (pathNodes.isEmpty()) {
        if (isPathLoading) PathLoadingState(modifier)
        return
    }

    // Group nodes by section (derived, stable)
    val nodesBySection by remember(pathNodes) {
        derivedStateOf { pathNodes.groupBy { it.sectionIndex } }
    }
    val sortedSectionKeys by remember(nodesBySection) {
        derivedStateOf { nodesBySection.keys.sorted() }
    }

    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val nodeCoordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val listState = rememberLazyListState()

    // The unlock animation: each newly opened node takes one step, in path order.
    val unlockSteps = remember(unlock, pathNodes) {
        if (unlock == null) {
            emptyMap()
        } else {
            pathNodes
                .filter { it.status != NodeStatus.LOCKED && it.orderIndex > unlock.fromOrder && it.orderIndex <= unlock.toOrder }
                .sortedBy { it.orderIndex }
                .mapIndexed { step, node -> node.id to step }
                .toMap()
        }
    }
    LaunchedEffect(unlockSteps) {
        if (unlockSteps.isNotEmpty()) {
            delay(unlockSteps.size * UNLOCK_STEP_MS + NODE_BURST_MS + 100L)
            onUnlockPlayed()
        }
    }

    // Close popup on scroll
    LaunchedEffect(listState.firstVisibleItemScrollOffset) {
        if (selectedNodeId != null) {
            selectedNodeId = null
        }
    }

    Box(modifier = modifier.onGloballyPositioned { containerCoords = it }) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp)
        ) {
            sortedSectionKeys.forEachIndexed { sectionIdx, sectionKey ->
                val sectionNodes = nodesBySection[sectionKey].orEmpty()
                if (sectionNodes.isEmpty()) return@forEachIndexed

                // Breathing room before every section after the first — a regular scrolling item
                // rather than padding baked into the sticky header, so it scrolls away instead of
                // showing up as a stuck gap once the header pins to the top.
                if (sectionIdx > 0) {
                    item(key = "section_gap_$sectionKey") {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                stickyHeader(key = "section_$sectionKey") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 16.dp)
                    ) {
                        SectionHeaderCard(
                            sectionIndex = sectionKey,
                            sectionTitle = sectionNodes.first().sectionTitle,
                            completedCount = sectionNodes.count { it.status == NodeStatus.COMPLETED },
                            totalCount = sectionNodes.size
                        )
                    }
                }

                itemsIndexed(sectionNodes, key = { _, node -> node.id }) { nodeIdx, node ->
                    val xOffset = pathNodeOffset(node)
                    val unlockStep = unlockSteps[node.id]
                    if (nodeIdx > 0) {
                        val previous = sectionNodes[nodeIdx - 1]
                        PathConnector(
                            fromOffset = pathNodeOffset(previous),
                            toOffset = xOffset,
                            filled = previous.status == NodeStatus.COMPLETED && node.status != NodeStatus.LOCKED,
                            fillDelayMillis = unlockStep?.let { it * UNLOCK_STEP_MS }
                        )
                    }

                    PathNodeItem(
                        node = node,
                        offset = xOffset,
                        unlockDelayMillis = unlockStep?.let { (it + 1) * UNLOCK_STEP_MS },
                        onSelect = {
                            selectedNodeId = if (selectedNodeId == node.id) null else node.id
                        },
                        onPositioned = { coords ->
                            nodeCoordsMap[node.id] = coords
                        }
                    )
                }
            }

            if (pathNodes.all { it.status == NodeStatus.COMPLETED }) {
                item(key = "generate_more") {
                    Spacer(modifier = Modifier.height(32.dp))
                    if (isPathLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        OutlinedButton(
                            onClick = onGenerateClick,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("GENERAR MÁS LECCIONES", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val selectedId = selectedNodeId
        val selectedNode = pathNodes.find { it.id == selectedId }
        val selectedNodeCoords = selectedId?.let { nodeCoordsMap[it] }
        val currentContainerCoords = containerCoords

        if (selectedNode != null && selectedNodeCoords != null && currentContainerCoords != null) {
            FloatingNodePopup(
                node = selectedNode,
                nodeCoords = selectedNodeCoords,
                containerCoords = currentContainerCoords,
                onActionClick = {
                    selectedNodeId = null
                    onNodeClick(selectedNode)
                },
                onDismiss = { selectedNodeId = null }
            )
        }
    }
}

/**
 * Horizontal shift of a node: exam nodes always sit centered, the rest follow the zigzag.
 *
 * @param node the node to place
 * @return the shift from the centre of the path
 */
private fun pathNodeOffset(node: LessonNode): Dp =
    if (node.nodeType == NodeType.EXAM) 0.dp else PathZigzagOffsets[node.unitIndex % PathZigzagOffsets.size]

/** Height of the connector between two nodes, which is also the gap between them. */
private val PathConnectorHeight = 32.dp

/** How long one step of the unlock animation takes: a connector filling in. */
private const val UNLOCK_STEP_MS = 650L

/** How long the pop of a newly opened node lasts, counted from the moment it opens. */
private const val NODE_BURST_MS = 800L

/**
 * The curved stretch of path between two consecutive nodes of a section. It is empty until the
 * user has walked it. When [fillDelayMillis] is set it fills in from top to bottom once, after
 * that delay, instead of appearing already full: that is the moment a node is completed.
 *
 * @param fromOffset horizontal shift of the node above
 * @param toOffset horizontal shift of the node below
 * @param filled whether the stretch has been walked: the node above is completed and the one
 *   below is open
 * @param fillDelayMillis delay before the fill-in animation starts, or null to draw the stretch
 *   in its final state without animating
 */
@Composable
private fun PathConnector(
    fromOffset: Dp,
    toOffset: Dp,
    filled: Boolean,
    fillDelayMillis: Long?
) {
    val fill = remember { Animatable(if (filled && fillDelayMillis == null) 1f else 0f) }
    LaunchedEffect(filled, fillDelayMillis) {
        when {
            !filled -> fill.snapTo(0f)
            fillDelayMillis == null -> fill.snapTo(1f)
            else -> {
                fill.snapTo(0f)
                delay(fillDelayMillis)
                fill.animateTo(1f, tween(durationMillis = UNLOCK_STEP_MS.toInt(), easing = FastOutSlowInEasing))
            }
        }
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(PathConnectorHeight)
    ) {
        val startX = center.x + fromOffset.toPx()
        val endX = center.x + toOffset.toPx()
        val curve = Path().apply {
            moveTo(startX, 0f)
            cubicTo(startX, size.height / 2f, endX, size.height / 2f, endX, size.height)
        }
        val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        drawPath(curve, color = track, style = stroke)
        if (fill.value > 0f) {
            val measure = PathMeasure().apply { setPath(curve, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * fill.value, partial, true)
            drawPath(partial, color = ink, style = stroke)
        }
    }
}

// ─── Section Header ─────────────────────────────────────────────────────────

/**
 * Card that heads a section of the path: its label, title and completion progress.
 *
 * @param sectionIndex zero-based index of the section, shown one-based
 * @param sectionTitle name of the section
 * @param completedCount number of completed nodes in the section
 * @param totalCount total number of nodes in the section
 */
@Composable
fun SectionHeaderCard(
    sectionIndex: Int,
    sectionTitle: String,
    completedCount: Int,
    totalCount: Int
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "SECCIÓN ${sectionIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BaliAccentYellow,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$completedCount de $totalCount completadas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Path Node Item ─────────────────────────────────────────────────────────

/**
 * One circular lesson node on the learning path. Unlocked nodes get a golden fill and a
 * rotating glow arc, locked nodes are dimmed and not tappable.
 *
 * @param node lesson the node represents
 * @param offset horizontal shift that produces the path's zigzag
 * @param onSelect invoked when an unlocked or completed node is tapped
 * @param onPositioned reports the node's layout coordinates so a popup can anchor to it
 * @param unlockDelayMillis for a node that has just opened: the wait before it pops open, which
 *   it spends drawn as locked while the connector into it fills in; null for a node that did
 *   not just open. The node is tappable throughout.
 */
@Composable
fun PathNodeItem(
    node: LessonNode,
    offset: Dp,
    onSelect: () -> Unit,
    onPositioned: (LayoutCoordinates) -> Unit = {},
    unlockDelayMillis: Long? = null
) {
    val isCompleted = node.status == NodeStatus.COMPLETED
    val context = LocalContext.current

    // A node that just opened is drawn locked until its turn, then pops open with a burst.
    var revealed by remember(node.id, unlockDelayMillis) { mutableStateOf(unlockDelayMillis == null) }
    val popScale = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(unlockDelayMillis) {
        if (unlockDelayMillis == null) return@LaunchedEffect
        delay(unlockDelayMillis)
        revealed = true
        popScale.snapTo(0.8f)
        launch { popScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) }
        burst.snapTo(0f)
        burst.animateTo(1f, tween(durationMillis = NODE_BURST_MS.toInt(), easing = LinearOutSlowInEasing))
    }
    val isLocked = node.status == NodeStatus.LOCKED || !revealed
    val isUnlocked = node.status == NodeStatus.UNLOCKED && revealed

    val rotation = if (isUnlocked) {
        val infiniteTransition = rememberInfiniteTransition(label = "node_rotate")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing)
            ),
            label = "rotation"
        ).value
    } else {
        0f
    }

    val bgColor by animateColorAsState(
        targetValue = if (isUnlocked) BaliAccentYellow else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "node_color"
    )

    // Resolve drawable icon
    val resId = remember(node.iconResName) {
        context.resources.getIdentifier(node.iconResName, "drawable", context.packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offset),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Rotating glow arc behind the node for UNLOCKED state
            if (isUnlocked) {
                Canvas(modifier = Modifier.size(86.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(BaliAccentYellow, Color.Transparent, BaliAccentYellow)
                        ),
                        startAngle = rotation,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        scaleX = popScale.value
                        scaleY = popScale.value
                    }
                    .onGloballyPositioned { coords -> onPositioned(coords) }
                    .then(
                        if (node.status != NodeStatus.LOCKED) Modifier.clickable { onSelect() } else Modifier
                    ),
                shape = CircleShape,
                color = bgColor,
                shadowElevation = if (isUnlocked) 12.dp else 0.dp,
                border = if (isUnlocked) BorderStroke(3.dp, UnlockedNodeBorder) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = node.title,
                            modifier = Modifier.size(48.dp),
                            alpha = if (isLocked) 0.4f else 1.0f,
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Fallback icon if drawable not found
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = when {
                                isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                                isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else -> Color.White
                            }
                        )
                    }
                }
            }

            if (burst.value > 0f && burst.value < 1f) {
                Canvas(modifier = Modifier.requiredSize(NodeBurstSize)) {
                    drawNodeBurst(progress = burst.value, ringColor = BaliAccentYellow)
                }
            }
        }
    }
}

/** Size of the area the burst of a newly opened node is drawn in; it spills past the node. */
private val NodeBurstSize = 180.dp

/** Number of sparks in the burst of a newly opened node. */
private const val NODE_BURST_SPARKS = 10

/**
 * Burst for a node that has just opened: one ring that widens and fades, and sparks that fly
 * out, shrink and fade. Nothing loops; it ends when [progress] reaches 1.
 *
 * @param progress 0 at the start, 1 once everything has faded
 * @param ringColor colour of the ring; the sparks use the app's flame colours
 */
private fun DrawScope.drawNodeBurst(progress: Float, ringColor: Color) {
    val nodeRadius = 40.dp.toPx()
    val reach = size.minDimension / 2f
    val fade = 1f - progress
    drawCircle(
        color = ringColor.copy(alpha = 0.8f * fade),
        radius = nodeRadius + (reach - nodeRadius) * progress,
        style = Stroke(width = (5f * fade + 1f).dp.toPx())
    )
    repeat(NODE_BURST_SPARKS) { index ->
        val angle = Math.toRadians(index * (360.0 / NODE_BURST_SPARKS) + 9.0)
        val distance = nodeRadius + (reach - nodeRadius) * (0.35f + 0.65f * progress)
        drawCircle(
            color = BaliFlameColors[index % BaliFlameColors.size].copy(alpha = fade),
            radius = (5f * fade + 1f).dp.toPx(),
            center = center + Offset((distance * cos(angle)).toFloat(), (distance * sin(angle)).toFloat())
        )
    }
}

// ─── Floating Node Popup ───────────────────────────────────────────────────

/** Top XP tier a lesson/review node can award — mirrors IncrementXpUseCase.calculateBaseXp's PRACTICE ceiling. */
private const val MAX_PRACTICE_XP = 20

/** Top XP tier an exam node can award — mirrors IncrementXpUseCase.calculateBaseXp's EXAM ceiling. */
private const val MAX_EXAM_XP = 30

/** Multiplier IncrementXpUseCase applies to base XP when isRepeat is true. */
private const val REPEAT_XP_FACTOR = 0.3

/**
 * Speech-bubble popup shown below a tapped node, with the lesson title, description and the
 * action button that starts (or reviews) it. Positioned in the container's coordinate space.
 *
 * @param node lesson the popup describes
 * @param nodeCoords layout coordinates of the tapped node
 * @param containerCoords layout coordinates of the container the popup is positioned against
 * @param onActionClick invoked when the start/review button is tapped
 * @param onDismiss invoked when the popup is dismissed without taking the action
 */
@Composable
private fun FloatingNodePopup(
    node: LessonNode,
    nodeCoords: LayoutCoordinates,
    containerCoords: LayoutCoordinates,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isCompleted = node.status == NodeStatus.COMPLETED
    // Actual reward depends on accuracy/speed/streak, known only after the attempt — promise a
    // ceiling, not a fixed number, so this stays true regardless of how the user performs.
    // Exam nodes skip the repeat discount: ExamViewModel's isRepeat tracks "any prior official
    // exam", not this specific node, so this node's own completion isn't the trigger there.
    val maxXp = when {
        node.nodeType == NodeType.EXAM -> MAX_EXAM_XP
        isCompleted -> (MAX_PRACTICE_XP * REPEAT_XP_FACTOR).toInt()
        else -> MAX_PRACTICE_XP
    }
    val bubbleColor = MaterialTheme.colorScheme.primaryContainer
    val density = LocalDensity.current

    // Animación de entrada
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val transition = rememberTransition(visibleState, label = "popup_animation")
    
    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) },
        label = "scale"
    ) { if (it) 1f else 0.8f }
    
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200) },
        label = "alpha"
    ) { if (it) 1f else 0f }

    // Calcula la posición relativa al contenedor (Box)
    val containerPos = containerCoords.positionInRoot()
    val nodePos = nodeCoords.positionInRoot()
    val nodeSize = nodeCoords.size

    val relativeNodeX = nodePos.x - containerPos.x
    val relativeNodeY = nodePos.y - containerPos.y

    val popupWidthPx = with(density) { 260.dp.toPx() }
    val spacingPx = with(density) { 8.dp.toPx() }

    // Centrar sobre el nodo
    val nodeCenterX = relativeNodeX + nodeSize.width / 2f
    val popupStartX = nodeCenterX - popupWidthPx / 2f

    // Posicionar DEBAJO del nodo
    val popupStartY = relativeNodeY + nodeSize.height + spacingPx

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(popupStartX.toInt(), popupStartY.toInt()),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, dismissOnBackPress = true)
    ) {
        // Contenido del bocadillo
        Column(
            modifier = Modifier
                .width(260.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(0.5f, 0f) // Escala desde el centro superior (donde está el triángulo)
                }
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Triángulo apuntando HACIA ARRIBA (señalando el nodo desde abajo)
            Canvas(modifier = Modifier.size(width = 24.dp, height = 12.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = bubbleColor)
            }

            // Tarjeta del bocadillo
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = bubbleColor,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (node.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = node.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (isCompleted) "REPASAR  ⚡ hasta +$maxXp XP" else "EMPEZAR  ⚡ hasta +$maxXp XP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Loading State ──────────────────────────────────────────────────────────

/**
 * Placeholder shown while the first learning-path nodes are being generated: the bobbing
 * mascot above a short message and an indeterminate progress bar.
 *
 * @param modifier layout modifier applied to the centered column
 */
@Composable
fun PathLoadingState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.bali),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .offset(y = offsetY.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Preparando tu ruta de aprendizaje…",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}


