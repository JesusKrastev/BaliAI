package com.jesuskrastev.bali.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.domain.model.DayActivity
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.MockExam
import com.jesuskrastev.bali.domain.model.TopicMastery
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import java.util.Calendar

/** Amber used for "almost" and "so-so" values: the theme's yellow is too pale for text. */
internal val StatsAmber = Color(0xFFF59E0B)

/**
 * Colors a share of correct answers by how good it is.
 *
 * @param accuracy share of correct answers from 0 to 1
 * @return green when strong, amber when middling, red when weak
 */
internal fun accuracyColor(accuracy: Float): Color = when {
    accuracy >= TopicMastery.STRONG_ACCURACY -> BaliAccentGreen
    accuracy >= TopicMastery.WEAK_ACCURACY -> StatsAmber
    else -> BaliAccentRed
}

/**
 * Card container shared by every statistics block: a title, an optional subtitle and the content.
 *
 * @param title block name
 * @param modifier layout modifier
 * @param subtitle one line of context under the title
 * @param content what the block shows
 */
@Composable
internal fun StatsCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

/**
 * Ring that fills up to a value, with a label in the middle.
 *
 * @param progress how full the ring is, from 0 to 1
 * @param color color of the filled arc
 * @param centerText main text inside the ring
 * @param centerCaption small text under [centerText]
 * @param description spoken description for accessibility
 * @param modifier layout modifier
 * @param size diameter of the ring
 */
@Composable
internal fun ProgressRing(
    progress: Float,
    color: Color,
    centerText: String,
    centerCaption: String,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 156.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "ring"
    )
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = RING_START,
                sweepAngle = RING_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = RING_START,
                    sweepAngle = RING_SWEEP * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = centerCaption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val RING_START = 135f
private const val RING_SWEEP = 270f

/**
 * Row of the latest mock exams as pass/fail dots, with empty slots for the ones still to take.
 *
 * @param recent the latest mock exams, oldest first
 * @param slots how many dots to draw
 */
@Composable
internal fun RecentMockDots(recent: List<MockExam>, slots: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(slots) { index ->
            val exam = recent.getOrNull(index)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MockDot(exam)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = exam?.let { "${it.score}/${it.total}" } ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * One dot: green with a tick when passed, red with a cross when failed, an empty ring when the
 * slot has no exam yet.
 *
 * @param exam the mock exam, or null for an empty slot
 */
@Composable
private fun MockDot(exam: MockExam?) {
    val base = Modifier.size(48.dp)
    when {
        exam == null -> Box(
            modifier = base.border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
        )
        else -> {
            val color = if (exam.passed) BaliAccentGreen else BaliAccentRed
            Box(
                modifier = base
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f))
                    .border(2.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (exam.passed) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = if (exam.passed) "Aprobado" else "Suspendido",
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Horizontal progress bar with rounded ends.
 *
 * @param fraction how full the bar is, from 0 to 1
 * @param color color of the filled part
 * @param modifier layout modifier
 * @param height thickness of the bar
 */
@Composable
internal fun StatsBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "bar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(height))
                .background(color)
        )
    }
}

/**
 * Bar chart of mock exam scores with the pass mark drawn as a dashed line.
 *
 * @param exams the exams to draw, oldest first
 */
@Composable
internal fun MockHistoryChart(exams: List<MockExam>) {
    val chartHeight = 150.dp
    val passLine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val passFraction = ExamRules.PASS_SCORE.toFloat() / ExamRules.QUESTION_COUNT

    Box(modifier = Modifier.fillMaxWidth().height(chartHeight + 28.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight).align(Alignment.BottomCenter)) {
            val y = size.height * (1f - passFraction)
            drawLine(
                color = passLine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(chartHeight + 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            exams.forEach { exam ->
                val color = if (exam.passed) BaliAccentGreen else BaliAccentRed
                val fraction = (exam.score.toFloat() / ExamRules.QUESTION_COUNT).coerceIn(0.04f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = exam.score.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(if (exams.size > 6) 18.dp else 28.dp)
                            .height(chartHeight * fraction)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(color)
                    )
                }
            }
        }
    }
    Text(
        text = "Línea discontinua: ${ExamRules.PASS_SCORE} aciertos, el mínimo para aprobar",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * A big number with its label and an icon, laid out for a two-by-two grid of totals.
 *
 * @param icon decorative glyph
 * @param value the figure
 * @param label what the figure counts
 * @param tint color of the icon badge
 * @param modifier layout modifier, usually a weight
 */
@Composable
internal fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = tint.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bar chart of questions answered on each of the last seven days.
 *
 * @param week seven days, oldest first
 */
@Composable
internal fun WeekChart(week: List<DayActivity>) {
    val maxQuestions = (week.maxOfOrNull { it.questions } ?: 0).coerceAtLeast(10)
    val barArea = 110.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEach { day ->
            val fraction = day.questions.toFloat() / maxQuestions
            val color = MaterialTheme.colorScheme.primary
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (day.questions > 0) day.questions.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(16.dp)
                )
                Box(modifier = Modifier.height(barArea), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height((barArea * fraction).coerceAtLeast(6.dp))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(
                                when {
                                    day.questions == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    day.isToday -> color
                                    else -> color.copy(alpha = 0.45f)
                                }
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dayLetter(day.dayMillis),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day.isToday) FontWeight.Black else FontWeight.Medium,
                    color = if (day.isToday) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Names a weekday with its Spanish initial.
 *
 * @param dayMillis any moment of the day
 * @return L, M, X, J, V, S or D
 */
internal fun dayLetter(dayMillis: Long): String {
    val day = Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_WEEK)
    return when (day) {
        Calendar.MONDAY -> "L"
        Calendar.TUESDAY -> "M"
        Calendar.WEDNESDAY -> "X"
        Calendar.THURSDAY -> "J"
        Calendar.FRIDAY -> "V"
        Calendar.SATURDAY -> "S"
        else -> "D"
    }
}

/**
 * Calendar grid of study days, seven per row, the last cell being today.
 *
 * @param studyDays one flag per day, oldest first, true when the user studied
 */
@Composable
internal fun StudyCalendar(studyDays: List<Boolean>) {
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        studyDays.chunked(7).forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { columnIndex, studied ->
                    val isToday = rowIndex * 7 + columnIndex == studyDays.lastIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (studied) active else idle)
                            .then(
                                if (isToday) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (studied) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Día de estudio",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact figure with a caption, used in rows of two or three summaries.
 *
 * @param value the figure
 * @param caption what it measures
 * @param modifier layout modifier, usually a weight
 * @param valueColor color of the figure
 */
@Composable
internal fun MiniStat(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = valueColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Small rounded label, used for status and for topics not practiced yet.
 *
 * @param text label text
 * @param color color of the text and the tinted background
 */
@Composable
internal fun StatsPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
