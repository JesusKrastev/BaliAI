package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.ui.screens.stats.ChangeDateButton
import com.jesuskrastev.bali.ui.screens.stats.ExamDatePickerDialog
import com.jesuskrastev.bali.ui.screens.stats.PlanUrgency
import com.jesuskrastev.bali.ui.screens.stats.StatsPill
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import kotlinx.coroutines.launch

/** Diameter of the dot that says today's goal is still pending. */
private val PendingDotSize = 8.dp

/** How many times the pending dot pulses when it first shows on a given day. */
private const val PENDING_PULSES = 3

/** Length of one pulse of the pending dot. */
private const val PENDING_PULSE_MILLIS = 1_100

/** How far the pulse ring grows, as a multiple of the dot. It stays inside the chip's padding. */
private const val PENDING_PULSE_SCALE = 2.4f

/**
 * Red for urgent text on the light theme: [BaliAccentRed] is too pale to read at label size on a
 * light pill, so the text takes a deeper red of the same hue while icons and edges keep the accent.
 */
private val UrgentRedOnLight = Color(0xFFB91C1C)

/**
 * The plan chip at the top of Home, beside the streak and the coins: the days left to the
 * onboarding plan's date (or to the student's own exam), in the countdown's tone, with a small
 * dot while today's session is pending. It lives in the top bar on purpose, so it never pushes or
 * covers the learning path. Tapping it opens a short sheet with the date, the days left and
 * today's goal, from where the student can start today's session, open the full plan or set the
 * date. Nothing shows until the profile has loaded.
 *
 * @param viewModel supplies the chip's state and saves the exam date
 * @param onStartSession opens the next unlocked node of the path, or null when there is none to open
 * @param onSeePlan opens the statistics tab, where the full countdown is
 * @param modifier layout modifier applied to the chip
 */
@Composable
fun HomePlanChip(
    viewModel: HomePlanViewModel,
    onStartSession: (() -> Unit)?,
    onSeePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val copy = state.copy ?: return
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    PlanChip(
        copy = copy,
        onClick = {
            viewModel.onChipClick()
            showSheet = true
        },
        modifier = modifier
    )

    if (showSheet) {
        HomePlanSheet(
            copy = copy,
            canStartSession = onStartSession != null,
            onAction = { action ->
                viewModel.onActionClick(action)
                when (action) {
                    HomePlanAction.START_SESSION -> onStartSession?.invoke()
                    HomePlanAction.SEE_PLAN -> onSeePlan()
                    HomePlanAction.SET_DATE, HomePlanAction.CHANGE_DATE -> showDatePicker = true
                }
            },
            onDismiss = { showSheet = false }
        )
    }

    if (showDatePicker) {
        ExamDatePickerDialog(
            initialDateMillis = state.plan.targetMillis,
            onConfirm = { pickerMillis ->
                viewModel.setExamDate(pickerMillis)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * The chip itself, stateless. It looks like the streak and coin pills beside it until the last
 * week, when it turns red like the statistics countdown; the dot after the label stays while
 * today's session is pending and fades out once it is done.
 *
 * @param copy what the chip says and how close the date is
 * @param onClick opens the plan sheet
 * @param modifier layout modifier applied to the chip
 */
@Composable
internal fun PlanChip(copy: HomePlanCopy, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isClose = copy.urgency.isClose
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = planAccentOf(copy.urgency)

    Surface(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = copy.chipDescription
            role = Role.Button
        },
        shape = RoundedCornerShape(16.dp),
        color = if (isClose) {
            BaliAccentRed.copy(alpha = if (isDark) 0.18f else 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isClose) BaliAccentRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .clearAndSetSemantics {}
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (copy.hasDate) Icons.Rounded.Event else Icons.Rounded.EditCalendar,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = copy.chipLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = if (isClose) planTextAccentOf(copy.urgency) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedVisibility(visible = copy.showsPendingDot) {
                PendingDot(color = accent)
            }
        }
    }
}

/**
 * The dot that says today's session is still pending. The first time it shows on a day it pulses
 * a few times and then stays still: enough to be noticed, never a loop that competes with the path.
 * The day is part of the saved state, so coming back to Home the same day does not pulse again.
 *
 * @param color colour of the dot and its pulse
 */
@Composable
private fun PendingDot(color: Color) {
    val today = remember { DailyStreak.epochDay(System.currentTimeMillis()) }
    var hasPulsed by rememberSaveable(today) { mutableStateOf(false) }
    val pulse = remember { Animatable(0f) }

    LaunchedEffect(hasPulsed) {
        if (hasPulsed) return@LaunchedEffect
        repeat(PENDING_PULSES) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(PENDING_PULSE_MILLIS, easing = LinearOutSlowInEasing))
        }
        hasPulsed = true
    }

    Box(modifier = Modifier.size(PendingDotSize)) {
        Box(
            modifier = Modifier
                .size(PendingDotSize)
                .graphicsLayer {
                    val progress = pulse.value
                    val scale = 1f + progress * (PENDING_PULSE_SCALE - 1f)
                    scaleX = scale
                    scaleY = scale
                    alpha = if (progress == 0f) 0f else (1f - progress) * 0.6f
                }
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(PendingDotSize)
                .background(color, CircleShape)
        )
    }
}

/**
 * Hosts the plan sheet. A button first slides the sheet away and then acts, so navigating or
 * opening the date picker never happens behind a sheet that is still on screen.
 *
 * @param copy what the sheet says
 * @param canStartSession whether there is a path node to open
 * @param onAction invoked with the button pressed, once the sheet has closed
 * @param onDismiss invoked when the sheet closes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomePlanSheet(
    copy: HomePlanCopy,
    canStartSession: Boolean,
    onAction: (HomePlanAction) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        HomePlanSheetContent(
            copy = copy,
            canStartSession = canStartSession,
            onAction = { action ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                    onAction(action)
                }
            }
        )
    }
}

/**
 * What the plan sheet shows, stateless: the stretch and a way to change the date, the plan's date,
 * the days left, today's goal and the buttons. Without a date ahead it asks for one instead.
 *
 * @param copy what the sheet says
 * @param canStartSession whether there is a path node to open; the study button hides otherwise
 * @param onAction invoked with the button pressed
 * @param modifier layout modifier applied to the content
 */
@Composable
internal fun HomePlanSheetContent(
    copy: HomePlanCopy,
    canStartSession: Boolean,
    onAction: (HomePlanAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = planAccentOf(copy.urgency)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatsPill(copy.stage, accent)
            Spacer(Modifier.weight(1f))
            if (copy.hasDate) {
                ChangeDateButton(
                    onClick = { onAction(HomePlanAction.CHANGE_DATE) },
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = copy.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        if (copy.hasDate) {
            Text(
                text = copy.subtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = planTextAccentOf(copy.urgency)
            )
        } else {
            Text(
                text = copy.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        copy.goal?.let { goal ->
            Spacer(Modifier.height(20.dp))
            DailyGoalRow(goal = goal, accent = accent)
        }
        Spacer(Modifier.height(24.dp))
        SheetButtons(copy = copy, canStartSession = canStartSession, onAction = onAction)
    }
}

/**
 * The sheet's buttons. With a date: start today's session (filled while it is pending, outlined
 * once done) and open the full plan. Without one: set the date.
 *
 * @param copy what the sheet says
 * @param canStartSession whether there is a path node to open
 * @param onAction invoked with the button pressed
 */
@Composable
private fun SheetButtons(copy: HomePlanCopy, canStartSession: Boolean, onAction: (HomePlanAction) -> Unit) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    val buttonShape = RoundedCornerShape(16.dp)

    if (!copy.hasDate) {
        Button(onClick = { onAction(HomePlanAction.SET_DATE) }, modifier = buttonModifier, shape = buttonShape) {
            Icon(Icons.Rounded.EditCalendar, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            SheetButtonLabel("PONER FECHA DEL EXAMEN")
        }
        return
    }

    if (canStartSession) {
        val onStart = { onAction(HomePlanAction.START_SESSION) }
        when (copy.goal?.state) {
            DailyGoalState.PENDING, null -> Button(onClick = onStart, modifier = buttonModifier, shape = buttonShape) {
                SheetButtonLabel("EMPEZAR LA SESIÓN DE HOY")
            }
            DailyGoalState.DONE -> OutlinedButton(onClick = onStart, modifier = buttonModifier, shape = buttonShape) {
                SheetButtonLabel("SEGUIR ESTUDIANDO")
            }
            DailyGoalState.EXAM_DAY -> OutlinedButton(onClick = onStart, modifier = buttonModifier, shape = buttonShape) {
                SheetButtonLabel("REPASO RÁPIDO")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    TextButton(onClick = { onAction(HomePlanAction.SEE_PLAN) }, modifier = buttonModifier, shape = buttonShape) {
        SheetButtonLabel("VER MI PLAN")
    }
}

/**
 * Label of a sheet button, in the app's bold capitals.
 *
 * @param text the label
 */
@Composable
private fun SheetButtonLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp
    )
}

/**
 * Today's goal: a ring while pending, a green check once done, a flag on exam day, beside what it means.
 *
 * @param goal the goal to show
 * @param accent colour of the pending ring and the exam-day flag
 */
@Composable
private fun DailyGoalRow(goal: DailyGoalCopy, accent: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GoalMark(state = goal.state, accent = accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = goal.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The goal row's mark. Static: the sheet is opened on purpose, so nothing in it needs to move.
 *
 * @param state where today's study stands
 * @param accent colour of the pending ring and the exam-day flag
 */
@Composable
private fun GoalMark(state: DailyGoalState, accent: Color) {
    val size = 36.dp
    when (state) {
        DailyGoalState.PENDING -> Box(
            modifier = Modifier
                .size(size)
                .border(2.5.dp, accent, CircleShape)
        )
        DailyGoalState.DONE -> Box(
            modifier = Modifier
                .size(size)
                .background(BaliAccentGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        DailyGoalState.EXAM_DAY -> Box(
            modifier = Modifier
                .size(size)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * The plan's accent, the same rule as the statistics countdown: red in the last week, the
 * brand orange before. For icons, edges and marks; text takes [planTextAccentOf].
 *
 * @param urgency how close the date is
 * @return the accent colour
 */
@Composable
private fun planAccentOf(urgency: PlanUrgency): Color =
    if (urgency.isClose) BaliAccentRed else MaterialTheme.colorScheme.primary

/**
 * The plan's accent for text: as [planAccentOf], but a deeper red on the light theme, where
 * [BaliAccentRed] is too pale to read at label size.
 *
 * @param urgency how close the date is
 * @return the text colour
 */
@Composable
private fun planTextAccentOf(urgency: PlanUrgency): Color = when {
    !urgency.isClose -> MaterialTheme.colorScheme.primary
    MaterialTheme.colorScheme.background.luminance() < 0.5f -> BaliAccentRed
    else -> UrgentRedOnLight
}
