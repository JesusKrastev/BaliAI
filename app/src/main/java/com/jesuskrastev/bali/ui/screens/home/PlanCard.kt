package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliDarkGray
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formats a plan date the way the onboarding promise reads it, e.g. "14 de noviembre". */
private val planDateFormat = SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))

/** Formats an exam date with its weekday, e.g. "viernes, 14 de noviembre". */
private val examDateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))

/** Ice blue for a day a streak freeze covered. */
private val FrozenDayColor = Color(0xFF38BDF8)

/**
 * Home's top card. It keeps the plan promised during onboarding in sight after payment and
 * turns it into today's task: a large countdown to the plan or exam date that turns red in the
 * final week, this week's sessions day by day with what is still missing, and the next lesson
 * one tap away. Its dark background sets it above the orange section cards of the path.
 *
 * @param plan the date to count down to, or an empty summary to ask for the exam date
 * @param week the seven days of the current week, Monday first; empty hides the week strip
 * @param weekSessions days practised so far this week
 * @param weeklyGoal sessions per week the student aims for
 * @param canStudy whether there is an unlocked lesson for the study button to open
 * @param onStudyClick opens the next unlocked lesson
 * @param onDateClick opens the exam date picker
 * @param modifier layout modifier applied to the card
 */
@Composable
internal fun PlanCard(
    plan: PlanSummary,
    week: List<DailyStreakState>,
    weekSessions: Int,
    weeklyGoal: Int,
    canStudy: Boolean,
    onStudyClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urgency = plan.urgency()
    val pace = weekPaceOf(week, weekSessions, weeklyGoal)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = if (urgency.isClose) BaliAccentRed else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) BaliDarkGray else BaliSecondary,
        contentColor = Color.White,
        border = when {
            urgency.isClose -> BorderStroke(1.5.dp, accent.copy(alpha = 0.8f))
            // On a dark background the slate card needs an edge to stand out.
            isDark -> BorderStroke(1.dp, accent.copy(alpha = 0.4f))
            else -> null
        }
    ) {
        // Less padding on top: the top row is already as tall as its button's touch target.
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)) {
            PlanCardTopRow(urgency = urgency, accent = accent, onDateClick = onDateClick)
            Spacer(modifier = Modifier.height(4.dp))
            PlanCountdown(plan = plan, urgency = urgency, accent = accent)

            if (week.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )
                WeekPaceSection(week = week, pace = pace, accent = accent)
            }

            PlanCardAction(
                plan = plan,
                urgency = urgency,
                practicedToday = pace.practicedToday,
                canStudy = canStudy,
                onStudyClick = onStudyClick,
                onDateClick = onDateClick
            )
        }
    }
}

/**
 * The card's label, which names the final stretch once the date is close, and the button to
 * change the date. The button is left out while there is no date, since the main action
 * already asks for one.
 *
 * @param urgency how close the date is
 * @param accent the card's accent colour for this urgency
 * @param onDateClick opens the exam date picker
 */
@Composable
private fun PlanCardTopRow(urgency: PlanUrgency, accent: Color, onDateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (urgency) {
                PlanUrgency.FINAL_WEEK -> "RECTA FINAL"
                PlanUrgency.TODAY -> "HA LLEGADO EL DÍA"
                else -> "TU PLAN"
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            // Named in words as well as in red, so the final stretch never relies on colour alone.
            color = if (urgency.isClose) accent else Color.White.copy(alpha = 0.6f)
        )
        if (urgency != PlanUrgency.NO_DATE) {
            TextButton(
                onClick = onDateClick,
                // Shifted by its own padding so the label lines up with the card's right edge.
                modifier = Modifier.offset(x = 10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.EditCalendar,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cambiar fecha",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * The countdown: days left in large type next to what they lead to, or a question when there
 * is no date yet.
 *
 * @param plan the date the card counts down to
 * @param urgency how close the date is
 * @param accent the colour of the number
 */
@Composable
private fun PlanCountdown(plan: PlanSummary, urgency: PlanUrgency, accent: Color) {
    if (urgency == PlanUrgency.NO_DATE) {
        Text(
            text = "¿Cuándo es tu examen?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ponle fecha y te diremos cuántos días te quedan y a qué ritmo ir.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        return
    }

    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (urgency == PlanUrgency.TODAY) "HOY" else "${plan.daysLeft}",
            style = MaterialTheme.typography.displayLarge.copy(lineHeight = 60.sp),
            fontWeight = FontWeight.Black,
            color = accent
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = countdownLabel(plan),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = countdownDetail(plan),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Builds the words next to the countdown number.
 *
 * @param plan the date the card counts down to, which must be set
 * @return what the number counts towards, e.g. "días para tu examen"
 */
private fun countdownLabel(plan: PlanSummary): String = when {
    plan.daysLeft == 0 -> if (plan.isExamDate) "es tu examen" else "es tu fecha meta"
    plan.isExamDate -> if (plan.daysLeft == 1) "día para tu examen" else "días para tu examen"
    else -> if (plan.daysLeft == 1) "día para tu meta" else "días para tu meta"
}

/**
 * Builds the line under [countdownLabel]: the date itself, worded as the onboarding promise
 * for a plan date. The plan date is an estimate, so it is never presented as a guarantee.
 *
 * @param plan the date the card counts down to, which must be set
 * @return the date line, or a closing line on the day itself
 */
private fun countdownDetail(plan: PlanSummary): String {
    val date = Date(plan.targetMillis ?: return "")
    return when {
        plan.daysLeft == 0 && plan.isExamDate -> "¡Mucha suerte!"
        plan.daysLeft == 0 -> "Si tu examen es otro día, cambia la fecha"
        plan.isExamDate -> examDateFormat.format(date).replaceFirstChar { it.uppercase() }
        else -> "Carnet antes del ${planDateFormat.format(date)}"
    }
}

/**
 * This week at a glance: sessions against the goal, whether today is done, one dot per day,
 * and what is still missing in the days left.
 *
 * @param week the seven days of the current week, Monday first
 * @param pace this week's pace
 * @param accent the colour of today's pending marks
 */
@Composable
private fun WeekPaceSection(week: List<DailyStreakState>, pace: WeekPace, accent: Color) {
    val pulse = rememberPendingPulse()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "ESTA SEMANA · ${pace.sessions} DE ${pace.goal}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        TodayStatus(practicedToday = pace.practicedToday, accent = accent, pulse = pulse)
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        week.forEach { day -> WeekDayDot(day = day, accent = accent, pulse = pulse) }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = weekPaceMessage(pace),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (pace.missing == 0) FontWeight.Bold else FontWeight.Normal,
        color = if (pace.missing == 0) BaliAccentGreen else Color.White.copy(alpha = 0.85f)
    )
}

/**
 * Says whether today is still pending, with a breathing dot, or already done.
 *
 * @param practicedToday whether today already has a session
 * @param accent the colour of the pending dot
 * @param pulse the shared breathing alpha for pending marks
 */
@Composable
private fun TodayStatus(practicedToday: Boolean, accent: Color, pulse: State<Float>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (practicedToday) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = BaliAccentGreen,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { alpha = pulse.value }
                    .background(accent, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (practicedToday) "Hoy, hecho" else "Hoy te toca",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (practicedToday) BaliAccentGreen else Color.White
        )
    }
}

/**
 * One day of the week strip: a check on days with a session, a snowflake on days a freeze
 * covered, a breathing ring on today while it is still pending, and the day's letter otherwise.
 *
 * @param day the day to draw
 * @param accent the colour of today's ring while pending
 * @param pulse the shared breathing alpha for pending marks
 */
@Composable
private fun WeekDayDot(day: DailyStreakState, accent: Color, pulse: State<Float>) {
    val primary = MaterialTheme.colorScheme.primary
    val base = Modifier
        .size(34.dp)
        .clip(CircleShape)
    val dot = when (day.status) {
        StreakStatus.COMPLETED -> base.background(primary)
        StreakStatus.FROZEN -> base.background(FrozenDayColor.copy(alpha = 0.25f))
        StreakStatus.TODAY -> base.background(accent.copy(alpha = 0.15f))
        StreakStatus.FUTURE -> base.background(Color.White.copy(alpha = 0.08f))
        StreakStatus.FAILED -> base.border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
    }

    Box(contentAlignment = Alignment.Center) {
        if (day.status == StreakStatus.TODAY) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer { alpha = pulse.value }
                    .border(2.dp, accent, CircleShape)
            )
        }
        Box(modifier = dot, contentAlignment = Alignment.Center) {
            when (day.status) {
                StreakStatus.COMPLETED -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                StreakStatus.FROZEN -> Icon(
                    imageVector = Icons.Rounded.AcUnit,
                    contentDescription = null,
                    tint = FrozenDayColor,
                    modifier = Modifier.size(16.dp)
                )
                else -> Text(
                    text = day.dayOfWeek,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day.isToday) FontWeight.Black else FontWeight.Bold,
                    color = when (day.status) {
                        StreakStatus.TODAY -> Color.White
                        StreakStatus.FAILED -> Color.White.copy(alpha = 0.3f)
                        else -> Color.White.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}

/**
 * The card's one button. Without a date it asks for one; otherwise it opens the next lesson,
 * as the strong call to action while today is pending and as a quieter one once it is done.
 * Nothing is shown when there is no unlocked lesson to open.
 *
 * @param plan the date the card counts down to
 * @param urgency how close the date is
 * @param practicedToday whether today already has a session
 * @param canStudy whether there is an unlocked lesson to open
 * @param onStudyClick opens the next unlocked lesson
 * @param onDateClick opens the exam date picker
 */
@Composable
private fun PlanCardAction(
    plan: PlanSummary,
    urgency: PlanUrgency,
    practicedToday: Boolean,
    canStudy: Boolean,
    onStudyClick: () -> Unit,
    onDateClick: () -> Unit
) {
    val buttonModifier = Modifier
        .padding(top = 16.dp)
        .fillMaxWidth()
        .height(52.dp)
    val shape = RoundedCornerShape(16.dp)

    when {
        urgency == PlanUrgency.NO_DATE -> Button(
            onClick = onDateClick,
            modifier = buttonModifier,
            shape = shape
        ) {
            Icon(imageVector = Icons.Rounded.EditCalendar, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            PlanButtonText("PONER FECHA DEL EXAMEN")
        }

        !canStudy -> Unit

        !practicedToday -> Button(
            onClick = onStudyClick,
            modifier = buttonModifier,
            shape = shape
        ) {
            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            PlanButtonText(
                if (urgency == PlanUrgency.TODAY && plan.isExamDate) "ÚLTIMO REPASO" else "EMPEZAR LA SESIÓN DE HOY"
            )
        }

        else -> OutlinedButton(
            onClick = onStudyClick,
            modifier = buttonModifier,
            shape = shape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            PlanButtonText("SEGUIR PRACTICANDO")
        }
    }
}

/**
 * Label of the plan card's button.
 *
 * @param text the label, in capitals like the app's other buttons
 */
@Composable
private fun PlanButtonText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp
    )
}

/**
 * An alpha that breathes between full and faint, shared by everything on the card that is
 * still pending today. Read it inside `graphicsLayer` so only drawing repeats, not composition.
 *
 * @return the animated alpha
 */
@Composable
private fun rememberPendingPulse(): State<Float> =
    rememberInfiniteTransition(label = "pendingPulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pendingPulseAlpha"
    )

/**
 * Date picker for the exam day, opened from the plan card. Only today and later can be picked.
 *
 * @param initialDateMillis the day to preselect (local time), or null to preselect nothing
 * @param onConfirm invoked with the picker's selection, midnight UTC of the chosen day
 * @param onDismiss invoked when the dialog is closed without saving
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExamDatePickerDialog(
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
