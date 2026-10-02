package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.FIRST_STEPS_BONUS_COINS
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import kotlinx.coroutines.delay

/** Test tag of the bar's container, used by UI and screenshot tests. */
const val FIRST_STEPS_BAR_TAG = "first_steps_bar"

/** How long a reward stays celebrated in the bar before it goes back to the tasks. */
private const val CELEBRATION_MILLIS = 2_800L

/** Short label of a [FirstStepTask], sized to fit the bar's single line. */
private val FirstStepTask.label: String
    get() = when (this) {
        FirstStepTask.FIRST_TEST -> "Haz tu primer test"
        FirstStepTask.ASK_BALI -> "Pregunta una duda a Bali"
        FirstStepTask.PLAY_GAME -> "Juega un minijuego"
    }

/** What the celebration says right after a [FirstStepTask] pays out. */
private val FirstStepTask.celebration: String
    get() = when (this) {
        FirstStepTask.FIRST_TEST -> "¡Primer test completado!"
        FirstStepTask.ASK_BALI -> "¡Primera duda resuelta!"
        FirstStepTask.PLAY_GAME -> "¡Primer minijuego superado!"
    }

/** The three faces of the bar; [AnimatedContent] cross-fades between them. */
private sealed interface FirstStepsBarContent {
    /** Coins just earned, shown for a moment instead of the tasks. */
    data class Celebrating(val reward: FirstStepReward) : FirstStepsBarContent

    /** Tasks still pending: the next one up front, the full list on demand. */
    data class Tasks(val progress: FirstStepsProgress) : FirstStepsBarContent

    /** Everything done: the closing invitation to the first simulacro. */
    data class Exam(val progress: FirstStepsProgress) : FirstStepsBarContent
}

/**
 * Compact day-0 "Tus primeros pasos" bar, pinned above the bottom navigation so it stays in sight
 * while the student scrolls the path. Collapsed it is a single row: progress ring, the next task
 * with its coins and a button that goes straight to it; tapping the row unfolds the whole list.
 * When a task pays out, the bar itself celebrates for a moment (no dialog), and once the three
 * tasks are done it turns into the invitation to the first simulacro.
 *
 * Renders nothing when there is neither progress to show nor a reward to celebrate.
 *
 * @param progress the bar's progress, or null when the account must not see it
 * @param reward coins just earned on another screen that are still to be celebrated
 * @param onTaskClick takes the student to a pending task
 * @param onExamClick opens the first simulacro
 * @param onDismissClick asks to hide the bar for good
 * @param onRewardShown marks [reward] as celebrated, after the celebration or a tap on it
 * @param onShown reports that the bar reached the screen (analytics)
 * @param modifier layout modifier applied to the bar
 * @param isTaskEnabled whether a pending task can be started right now (the first test needs
 *   the learning path to exist)
 */
@Composable
fun FirstStepsBar(
    progress: FirstStepsProgress?,
    reward: FirstStepReward?,
    onTaskClick: (FirstStepTask) -> Unit,
    onExamClick: () -> Unit,
    onDismissClick: () -> Unit,
    onRewardShown: () -> Unit,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
    isTaskEnabled: (FirstStepTask) -> Boolean = { true },
) {
    val content: FirstStepsBarContent? = when {
        reward != null -> FirstStepsBarContent.Celebrating(reward)
        progress == null -> null
        progress.isComplete -> FirstStepsBarContent.Exam(progress)
        else -> FirstStepsBarContent.Tasks(progress)
    }
    // Keeps drawing the last face while the bar slides out, instead of an empty box.
    val shownContent = rememberLastNonNull(content)

    AnimatedVisibility(
        visible = content != null,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        LaunchedEffect(Unit) { onShown() }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .testTag(FIRST_STEPS_BAR_TAG),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            shadowElevation = 10.dp,
        ) {
            AnimatedContent(
                targetState = shownContent,
                contentKey = { it?.let { face -> face::class } },
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(120)))
                        .using(SizeTransform(clip = false))
                },
                label = "first_steps_bar_content",
            ) { face ->
                when (face) {
                    is FirstStepsBarContent.Celebrating -> CelebrationRow(face.reward, onRewardShown)
                    is FirstStepsBarContent.Tasks -> TasksContent(
                        progress = face.progress,
                        onTaskClick = onTaskClick,
                        onDismissClick = onDismissClick,
                        isTaskEnabled = isTaskEnabled,
                    )
                    is FirstStepsBarContent.Exam -> ExamRow(onExamClick = onExamClick, onDismissClick = onDismissClick)
                    null -> Unit
                }
            }
        }
    }
}

/**
 * Returns [value], or the last non-null value it had, so exit animations still have content.
 *
 * @param value the current value, possibly null
 * @return [value] when non-null, otherwise the latest non-null value seen
 */
@Composable
private fun <T : Any> rememberLastNonNull(value: T?): T? {
    val last = remember { mutableStateOf(value) }
    SideEffect { if (value != null) last.value = value }
    return value ?: last.value
}

/**
 * Tasks face: one summary row (always visible) plus the unfoldable checklist.
 *
 * @param progress the student's progress
 * @param onTaskClick takes the student to a pending task
 * @param onDismissClick asks to hide the bar
 * @param isTaskEnabled whether a pending task can be started right now
 */
@Composable
private fun TasksContent(
    progress: FirstStepsProgress,
    onTaskClick: (FirstStepTask) -> Unit,
    onDismissClick: () -> Unit,
    isTaskEnabled: (FirstStepTask) -> Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val pending = FirstStepTask.entries.filterNot(progress::isDone)
    // The path can still be generating on day 0: offer something that can start right now.
    val nextTask = pending.firstOrNull(isTaskEnabled) ?: pending.first()

    Column(modifier = Modifier.animateContentSize()) {
        SummaryRow(
            doneCount = progress.doneCount,
            nextTask = nextTask,
            canStartNext = isTaskEnabled(nextTask),
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onStartClick = { onTaskClick(nextTask) },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            TaskChecklist(
                progress = progress,
                onTaskClick = onTaskClick,
                onDismissClick = onDismissClick,
                isTaskEnabled = isTaskEnabled,
            )
        }
    }
}

/**
 * The bar's always-visible row: progress ring, title with the next task and its coins, and the
 * button that starts that task. Tapping anywhere else unfolds or folds the checklist.
 *
 * @param doneCount tasks already completed
 * @param nextTask the task the button starts
 * @param canStartNext whether [nextTask] can be started right now
 * @param expanded whether the checklist is unfolded
 * @param onToggle folds or unfolds the checklist
 * @param onStartClick starts [nextTask]
 */
@Composable
private fun SummaryRow(
    doneCount: Int,
    nextTask: FirstStepTask,
    canStartNext: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStartClick: () -> Unit,
) {
    // The bar sits at the bottom and unfolds upwards: the arrow points up to open, down to close.
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "first_steps_chevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(
                onClickLabel = if (expanded) "Ocultar tareas" else "Ver tareas",
                role = Role.Button,
                onClick = onToggle,
            )
            .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(doneCount = doneCount, total = FirstStepTask.entries.size)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tus primeros pasos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nextTask.label,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(6.dp))
                CoinAmount(coins = nextTask.coins)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onStartClick,
            enabled = canStartNext,
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(start = 14.dp, end = 10.dp),
        ) {
            Text("Ir", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Circular progress with the "done/total" count in the middle.
 *
 * @param doneCount tasks completed
 * @param total tasks in the bar
 */
@Composable
private fun ProgressRing(doneCount: Int, total: Int) {
    val fraction by animateFloatAsState(
        targetValue = doneCount.toFloat() / total,
        animationSpec = tween(600),
        label = "first_steps_progress",
    )
    Box(
        modifier = Modifier
            .size(42.dp)
            .clearAndSetSemantics { contentDescription = "$doneCount de $total completados" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = "$doneCount/$total",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Unfolded part of the bar: every task with its state and coins, the completion bonus and the
 * way to hide the bar.
 *
 * @param progress the student's progress
 * @param onTaskClick takes the student to a pending task
 * @param onDismissClick asks to hide the bar
 * @param isTaskEnabled whether a pending task can be started right now
 */
@Composable
private fun TaskChecklist(
    progress: FirstStepsProgress,
    onTaskClick: (FirstStepTask) -> Unit,
    onDismissClick: () -> Unit,
    isTaskEnabled: (FirstStepTask) -> Boolean,
) {
    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(4.dp))
        FirstStepTask.entries.forEach { task ->
            val isDone = progress.isDone(task)
            TaskRow(
                task = task,
                isDone = isDone,
                isEnabled = !isDone && isTaskEnabled(task),
                onClick = { onTaskClick(task) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+$FIRST_STEPS_BONUS_COINS extra al completar los ${FirstStepTask.entries.size}",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onDismissClick) {
                Text(
                    text = "Ocultar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One line of the checklist: tick, task, coins and, while pending, an arrow that goes to it.
 *
 * @param task the task the row stands for
 * @param isDone whether it was already completed and paid
 * @param isEnabled whether tapping it starts the task right now
 * @param onClick starts the task
 */
@Composable
private fun TaskRow(task: FirstStepTask, isDone: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckDot(isDone = isDone)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isDone) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CoinAmount(coins = task.coins, dimmed = isDone)
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { alpha = if (isEnabled) 1f else 0f },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Round tick: a hollow ring while pending, a green disc with a check once done.
 *
 * @param isDone whether to draw the completed state
 */
@Composable
private fun CheckDot(isDone: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (isDone) {
                    Modifier.background(BaliAccentGreen)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Hecho",
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * Coin icon followed by "+N".
 *
 * @param coins amount to show
 * @param dimmed fades it once the coins were already collected
 */
@Composable
private fun CoinAmount(coins: Int, dimmed: Boolean = false) {
    Row(
        modifier = Modifier.graphicsLayer { alpha = if (dimmed) 0.4f else 1f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.coin),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "+$coins",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Closing face: the three tasks are done, so the bar invites to the first simulacro.
 *
 * @param onExamClick opens the simulacro
 * @param onDismissClick hides the bar (nothing is lost any more)
 */
@Composable
private fun ExamRow(onExamClick: () -> Unit, onDismissClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.bali),
            contentDescription = null,
            modifier = Modifier.size(42.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "¿Aprobarías hoy?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = "Haz tu primer simulacro",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onExamClick,
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Text("Simulacro", fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onDismissClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Ocultar primeros pasos",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The bar's small celebration: a coin pops in next to the amount earned and what it was for.
 * It goes back to the tasks on its own after [CELEBRATION_MILLIS], or straight away on a tap.
 *
 * @param reward what was just earned
 * @param onRewardShown marks the reward as celebrated
 */
@Composable
private fun CelebrationRow(reward: FirstStepReward, onRewardShown: () -> Unit) {
    val coinScale = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(reward) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        coinScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
        delay(CELEBRATION_MILLIS)
        onRewardShown()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(BaliAccentYellow.copy(alpha = 0.18f))
            .clickable(onClickLabel = "Cerrar", onClick = onRewardShown)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Image(
            painter = painterResource(id = R.drawable.coin),
            contentDescription = null,
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = coinScale.value
                    scaleY = coinScale.value
                },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "+${reward.coins} monedas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (reward.completedAll) {
                    "¡Primeros pasos completados! Bonus incluido"
                } else {
                    reward.task.celebration
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Confirmation shown before hiding the bar while coins are still up for grabs, since hiding it
 * stops the remaining tasks from paying.
 *
 * @param pendingCoins coins the student would give up
 * @param onConfirm invoked when they choose to hide the bar anyway
 * @param onCancel invoked when they keep it
 */
@Composable
fun DismissFirstStepsDialog(
    pendingCoins: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("¿Ocultar tus primeros pasos?", fontWeight = FontWeight.Black) },
        text = {
            Text("Si los ocultas, dejarás de poder ganar las $pendingCoins monedas que te quedan.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OCULTAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("CONTINUAR", fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
