package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the home dashboard: streak/coins status, the plan card, the AI-tutor entry point,
 * and the scrollable learning-path graph. The account menu that used to open from here as a
 * side drawer (profile, legal links, sign out) now lives in the Settings tab.
 *
 * @param viewModel supplies [HomeUiState], drives path generation and saves the exam date
 * @param onNodeTestClick invoked with a tapped path node's title, description, id, and node-type
 *   name; exam nodes open the mock exam directly, since it costs no coins
 * @param onShopClick opens the coin shop
 * @param onStreakClick opens the streak detail screen
 * @param onChatClick opens the AI tutor chat
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNodeTestClick: (String, String?, String, String) -> Unit = { _, _, _, _ -> },
    onShopClick: () -> Unit = {},
    onStreakClick: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExamDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showExamDatePicker) {
        ExamDatePickerDialog(
            initialDateMillis = uiState.plan.targetMillis,
            onConfirm = { pickerMillis ->
                viewModel.setExamDate(pickerMillis)
                showExamDatePicker = false
            },
            onDismiss = { showExamDatePicker = false }
        )
    }

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
                onStreakClick = onStreakClick
            )
        },
        floatingActionButton = {
            AskBaliFab(onClick = onChatClick)
        }
    ) { paddingValues ->
        LearningPathGraph(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            pathNodes = uiState.pathNodes,
            isPathLoading = uiState.isPathLoading,
            onNodeClick = { node ->
                onNodeTestClick(node.title, node.description, node.id, node.nodeType.name)
            },
            onGenerateClick = {
                viewModel.generateNextPathNodesCount()
            },
            header = {
                PlanCard(
                    plan = uiState.plan,
                    weekSessions = uiState.weekSessions,
                    weeklyGoal = uiState.weeklyGoal,
                    onClick = { showExamDatePicker = true }
                )
            }
        )
    }
}

// ─── Plan Card ──────────────────────────────────────────────────────────────

/** Formats a plan date the way the onboarding promise reads it, e.g. "14 de noviembre". */
private val planDateFormat = SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))

/**
 * Keeps the plan promised during onboarding in sight after payment: the day the student is
 * working towards, how many days are left, and this week's sessions against the weekly goal.
 * Tapping it lets the student set their real exam date.
 *
 * @param plan the date to count down to, or an empty summary to ask for the exam date
 * @param weekSessions days practised so far this week
 * @param weeklyGoal sessions per week the student aims for
 * @param onClick opens the exam date picker
 */
@Composable
private fun PlanCard(plan: PlanSummary, weekSessions: Int, weeklyGoal: Int, onClick: () -> Unit) {
    val goal = weeklyGoal.coerceAtLeast(1)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TU PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = planHeadline(plan),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = planCountdown(plan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Rounded.EditCalendar,
                    contentDescription = "Cambiar la fecha del examen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (weekSessions.toFloat() / goal).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BaliAccentYellow,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Esta semana: $weekSessions de $goal ${if (goal == 1) "sesión" else "sesiones"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Builds the plan card's headline.
 *
 * @param plan the date the card counts down to
 * @return "Examen el…" for the student's own exam date, "Carnet antes del…" for the onboarding
 *   promise, or a question when there is no date ahead
 */
private fun planHeadline(plan: PlanSummary): String {
    val target = plan.targetMillis ?: return "¿Cuándo es tu examen?"
    val date = planDateFormat.format(Date(target))
    return if (plan.isExamDate) "Examen el $date" else "Carnet antes del $date"
}

/**
 * Builds the line under the plan card's headline.
 *
 * @param plan the date the card counts down to
 * @return how many days are left, or an invitation to set the date when there is none
 */
private fun planCountdown(plan: PlanSummary): String = when {
    plan.targetMillis == null -> "Ponle fecha y te decimos cuántos días quedan"
    plan.daysLeft == 0 -> if (plan.isExamDate) "Es hoy. ¡Mucha suerte!" else "Es hoy"
    plan.daysLeft == 1 -> "Falta 1 día"
    else -> "Faltan ${plan.daysLeft} días"
}

/**
 * Date picker for the exam day, opened from the plan card. Only today and later can be picked.
 *
 * @param initialDateMillis the day to preselect (local time), or null to preselect nothing
 * @param onConfirm invoked with the picker's selection, midnight UTC of the chosen day
 * @param onDismiss invoked when the dialog is closed without saving
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDatePickerDialog(
    initialDateMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { pickerMillisFromLocalDay(System.currentTimeMillis()) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis?.let(::pickerMillisFromLocalDay)?.takeIf { it >= today },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { datePickerState.selectedDateMillis?.let(onConfirm) },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("GUARDAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", fontWeight = FontWeight.Bold)
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "¿Cuándo es tu examen?",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            }
        )
    }
}

/**
 * Entry point to the AI tutor chat. Carries the mascot rather than a generic chat glyph
 * so it reads as "ask Bali", the same character the student already talks to elsewhere.
 *
 * @param onClick invoked when the student wants to open the chat
 */
@Composable
fun AskBaliFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Image(
                painter = painterResource(id = R.drawable.bali),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        },
        text = { Text("Pregunta a Bali", fontWeight = FontWeight.Black) }
    )
}

/**
 * Shows the streak and coins pills anchored to the top of Home.
 *
 * @param modifier layout modifier applied to the row
 * @param streak current daily streak count
 * @param practicedToday whether today already counts; the flame stays grey until it does
 * @param coinsCount current coin balance
 * @param onCoinsClick opens the coin shop
 * @param onStreakClick opens the streak detail screen
 */
@Composable
fun UserStatusRow(
    modifier: Modifier = Modifier,
    streak: Int,
    practicedToday: Boolean = true,
    coinsCount: Int,
    onCoinsClick: () -> Unit = {},
    onStreakClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
    ) {
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
 * @param header content placed above the first section, scrolling away with the path
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningPathGraph(
    modifier: Modifier = Modifier,
    pathNodes: List<LessonNode>,
    isPathLoading: Boolean,
    onNodeClick: (LessonNode) -> Unit,
    onGenerateClick: () -> Unit,
    header: @Composable () -> Unit = {}
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
            // Extra bottom room so the "Pregunta a Bali" FAB never covers the last node.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)
        ) {
            item(key = "header") {
                Box(modifier = Modifier.padding(top = 8.dp)) { header() }
            }

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
                    if (nodeIdx > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Exam nodes always sit centered; the rest follow the zigzag.
                    val xOffset = if (node.nodeType == NodeType.EXAM) {
                        0.dp
                    } else {
                        PathZigzagOffsets[node.unitIndex % PathZigzagOffsets.size]
                    }

                    PathNodeItem(
                        node = node,
                        offset = xOffset,
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
 */
@Composable
fun PathNodeItem(
    node: LessonNode,
    offset: Dp,
    onSelect: () -> Unit,
    onPositioned: (LayoutCoordinates) -> Unit = {}
) {
    val isLocked = node.status == NodeStatus.LOCKED
    val isUnlocked = node.status == NodeStatus.UNLOCKED
    val isCompleted = node.status == NodeStatus.COMPLETED
    val context = LocalContext.current

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

    val bgColor = if (isUnlocked) BaliAccentYellow else MaterialTheme.colorScheme.surfaceVariant

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
                    .onGloballyPositioned { coords -> onPositioned(coords) }
                    .then(
                        if (!isLocked) Modifier.clickable { onSelect() } else Modifier
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
        }
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


