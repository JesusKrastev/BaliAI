package com.jesuskrastev.bali.ui.screens.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.domain.model.ReadinessResult
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliDarkGray
import com.jesuskrastev.bali.ui.theme.BaliGrayMedium
import com.jesuskrastev.bali.ui.theme.BaliSecondary

/** Most days left for which the strip of days is drawn: beyond it the dots would be too small to read. */
private const val STRIP_MAX_DAYS = 21

/** Most days left for which each day of the strip gets its larger dot. */
private const val STRIP_LARGE_DOT_DAYS = 14

/**
 * The statistics screen's top card: how long until the exam, said so it is felt. A large figure
 * beside a calendar page of the day, the stretch the student is in ("RECTA FINAL"), one dot per day
 * that is left, and a message built from what is really missing. It turns red in the last week.
 * Without a date it asks for one, since this is where the date is set.
 *
 * @param plan the date to count down to, or an empty summary to ask for the exam date
 * @param readiness the verdict, for the mock exams still missing
 * @param studiedToday whether today already has a study session
 * @param onDateClick opens the exam date picker
 * @param modifier layout modifier applied to the card
 */
@Composable
internal fun ExamCountdownCard(
    plan: PlanSummary,
    readiness: ReadinessResult,
    studiedToday: Boolean,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = examCountdownCopyOf(plan, readiness, studiedToday)
    val urgency = plan.urgency()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = if (urgency.isClose) BaliAccentRed else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (isDark) BaliDarkGray else BaliSecondary,
        contentColor = Color.White,
        border = when {
            urgency.isClose -> BorderStroke(1.5.dp, accent.copy(alpha = 0.8f))
            // On a dark background the slate card needs an edge to stand out.
            isDark -> BorderStroke(1.dp, accent.copy(alpha = 0.4f))
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                // A glow behind the figure rather than a wash over the whole card, which turned the slate brown.
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.32f), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.15f),
                            radius = size.width * 0.75f
                        )
                    )
                }
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
        ) {
            if (copy == null) {
                NoDateContent(onDateClick = onDateClick)
            } else {
                CountdownContent(
                    copy = copy,
                    plan = plan,
                    accent = accent,
                    studiedToday = studiedToday,
                    onDateClick = onDateClick
                )
            }
        }
    }
}

/**
 * The card when there is a date: label, figure, strip of days and message.
 *
 * @param copy the texts to show
 * @param plan the date counted down to, which sizes the strip of days
 * @param accent colour of the figure, the label and the strip
 * @param studiedToday whether today already has a session, which shows on the strip's first dot
 * @param onDateClick opens the exam date picker
 */
@Composable
private fun CountdownContent(
    copy: CountdownCopy,
    plan: PlanSummary,
    accent: Color,
    studiedToday: Boolean,
    onDateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatsPill(copy.stage, accent)
        Spacer(Modifier.weight(1f))
        ChangeDateButton(onDateClick)
    }

    Spacer(Modifier.height(4.dp))
    CountdownFigure(copy = copy, plan = plan, accent = accent)

    if (plan.daysLeft in 1..STRIP_MAX_DAYS) {
        Spacer(Modifier.height(20.dp))
        DaysStrip(
            daysLeft = plan.daysLeft,
            studiedToday = studiedToday,
            accent = accent,
            endLabel = if (plan.isExamDate) "EXAMEN" else "META"
        )
    }

    Spacer(Modifier.height(20.dp))
    CountdownMessage(copy)
}

/**
 * The figure and what it counts. A number shares its row with a calendar page of the day; the
 * words "MAÑANA" and "HOY" are too wide for that and stand alone, with the day written under them.
 *
 * @param copy the texts to show
 * @param plan the date counted down to, for the calendar page
 * @param accent colour of the figure and the page's header
 */
@Composable
private fun CountdownFigure(copy: CountdownCopy, plan: PlanSummary, accent: Color) {
    val isWord = copy.number.any { it.isLetter() }
    val target = plan.targetMillis

    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        if (isWord || target == null) {
            CountdownNumber(text = copy.number, accent = accent, fontSize = 60.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountdownNumber(text = copy.number, accent = accent, fontSize = 88.sp, modifier = Modifier.weight(1f))
                CalendarPageTile(page = calendarPageOf(target), description = copy.date, accent = accent)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = copy.unit,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = Color.White
        )
        // The calendar page already says an exam day; a promise ("Carnet antes del…") or a word figure needs it written.
        if (isWord || !plan.isExamDate) {
            Text(
                text = copy.date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * The big figure of the countdown.
 *
 * @param text the days left, or "MAÑANA" / "HOY"
 * @param accent its colour
 * @param fontSize its size: smaller for the words, which are wider
 * @param modifier layout modifier
 */
@Composable
private fun CountdownNumber(text: String, accent: Color, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize, lineHeight = fontSize * 1.05f),
        fontWeight = FontWeight.Black,
        color = accent,
        modifier = modifier
    )
}

/**
 * A tear-off calendar page with the day counted down to: the month on a coloured header, the day
 * large and the weekday under it.
 *
 * @param page the day, split into its three lines
 * @param description the day written out, read by screen readers instead of the three lines
 * @param accent colour of the header
 */
@Composable
private fun CalendarPageTile(page: CalendarPage, description: String, accent: Color) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = page.month,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(accent)
                .padding(vertical = 5.dp)
        )
        Text(
            text = page.day,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp, lineHeight = 40.sp),
            fontWeight = FontWeight.Black,
            color = BaliSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = page.weekday,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = BaliGrayMedium,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp)
        )
    }
}

/**
 * The urgency message and what it means for today, on a faint panel so it reads as one block.
 *
 * @param copy the texts to show
 */
@Composable
private fun CountdownMessage(copy: CountdownCopy) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.07f),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = copy.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = copy.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

/**
 * The card when there is no date to count down to: asks for it and offers the picker.
 *
 * @param onDateClick opens the exam date picker
 */
@Composable
private fun NoDateContent(onDateClick: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    StatsPill("TU EXAMEN", MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(14.dp))
    Text(
        text = "¿Cuándo es tu examen?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = Color.White
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "Ponle fecha y verás cuántos días te quedan y a qué ritmo ir.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.7f)
    )
    Button(
        onClick = onDateClick,
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Rounded.EditCalendar, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "PONER FECHA DEL EXAMEN",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Small text button that opens the date picker, so a date the student already has can be corrected.
 * It says only "Cambiar" so it fits beside the longest stretch label; the date is right below it.
 * Home's plan sheet uses it too.
 *
 * @param onClick opens the exam date picker
 * @param contentColor colour of the icon and the label; the default suits the countdown's dark card
 */
@Composable
internal fun ChangeDateButton(onClick: () -> Unit, contentColor: Color = Color.White.copy(alpha = 0.7f)) {
    TextButton(
        onClick = onClick,
        // Shifted by its own padding so the label lines up with the container's right edge.
        modifier = Modifier
            .offset(x = 10.dp)
            .semantics { contentDescription = "Cambiar la fecha del examen" },
        contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
    ) {
        Icon(Icons.Rounded.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Cambiar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * One dot per day from today to the exam, so the time that is left can be seen shrinking: today
 * first (a check once studied, a breathing ring while pending), the days in between, and a flag
 * on the exam day, with both ends named under them. Decorative: the texts already say the same,
 * so it is hidden from screen readers.
 *
 * @param daysLeft days until the date, 1 or more
 * @param studiedToday whether today already has a session
 * @param accent colour of today's mark and the flag
 * @param endLabel name of the last day, under the flag
 */
@Composable
private fun DaysStrip(daysLeft: Int, studiedToday: Boolean, accent: Color, endLabel: String) {
    val pulse = rememberPendingPulse()
    val dot = if (daysLeft <= STRIP_LARGE_DOT_DAYS) 12.dp else 8.dp
    val today = dot + 8.dp

    Column(
        modifier = Modifier.clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TodayMark(studied = studiedToday, size = today, accent = accent, pulse = pulse)
            repeat(daysLeft - 1) {
                Box(
                    modifier = Modifier
                        .size(dot)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                )
            }
            Icon(
                imageVector = Icons.Rounded.Flag,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(today)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "HOY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = accent
            )
        }
    }
}

/**
 * Today's mark on the strip: filled with a check once there is a session, otherwise a ring that
 * breathes to say today is still pending.
 *
 * @param studied whether today already has a session
 * @param size diameter of the mark
 * @param accent colour of the mark
 * @param pulse the breathing alpha for the pending ring
 */
@Composable
private fun TodayMark(studied: Boolean, size: Dp, accent: Color, pulse: State<Float>) {
    if (studied) {
        Box(
            modifier = Modifier
                .size(size)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size - 6.dp))
        }
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { alpha = pulse.value }
                .border(2.5.dp, accent, CircleShape)
        )
    }
}

/**
 * An alpha that breathes between full and faint, for what is still pending today. Read it inside
 * `graphicsLayer` so only drawing repeats, not composition.
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
 * Date picker for the exam day, opened from the countdown card. Only today and later can be picked.
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
