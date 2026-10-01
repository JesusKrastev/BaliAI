package com.jesuskrastev.bali.ui.screens.streak

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Light blue of the days a freeze covered, taken from the freezer illustration. */
private val FreezeBlue = Color(0xFF38BDF8)

/** Turns the flame grey while today does not count yet. */
private val Greyscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Words for a number of days.
 *
 * @param days how many days
 * @return "día" for one, "días" otherwise
 */
internal fun daysWord(days: Int): String = if (days == 1) "día" else "días"

/**
 * Section title in the style Settings uses: small, uppercase and heavy.
 *
 * @param text the title, written in normal case
 */
@Composable
internal fun StreakSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 8.dp)
    )
}

/**
 * Flat rounded card, the same surface as Home's plan card and the Settings groups.
 *
 * @param modifier applied to the card
 * @param content the card body, already padded
 */
@Composable
internal fun StreakCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/**
 * The app's flame, lit once today counts and grey while it does not.
 *
 * @param lit whether the user has studied today
 * @param size width and height of the flame
 * @param modifier applied to the image
 */
@Composable
internal fun StreakFlame(lit: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.streak),
        contentDescription = null,
        colorFilter = if (lit) null else Greyscale,
        alpha = if (lit) 1f else 0.45f,
        modifier = modifier.size(size)
    )
}

/**
 * Draws the animated seven-level speedometer that represents current streak momentum.
 *
 * @param level current momentum from zero to [DailyStreak.MAX_LEVEL]
 * @param modifier applied to the gauge canvas
 */
@Composable
internal fun StreakSpeedometer(level: Int, modifier: Modifier = Modifier) {
    val safeLevel = level.coerceIn(0, DailyStreak.MAX_LEVEL)
    val animatedLevel by animateFloatAsState(
        targetValue = safeLevel.toFloat(),
        animationSpec = tween(durationMillis = 850),
        label = "streak_speedometer_needle"
    )
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .size(width = 272.dp, height = 184.dp)
            .semantics { contentDescription = "Ritmo de racha: $safeLevel de ${DailyStreak.MAX_LEVEL}" }
            .drawBehind {
                val radius = min(size.width, size.height) * 0.43f
                val center = Offset(x = size.width / 2f, y = size.height * 0.76f)
                val arcBounds = Rect(
                    left = center.x - radius,
                    top = center.y - radius,
                    right = center.x + radius,
                    bottom = center.y + radius
                )
                val startAngle = 145f
                val sweepAngle = 250f
                val stroke = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)

                drawArc(
                    color = muted,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = stroke,
                    topLeft = arcBounds.topLeft,
                    size = arcBounds.size
                )
                drawArc(
                    color = primary,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * (animatedLevel / DailyStreak.MAX_LEVEL),
                    useCenter = false,
                    style = stroke,
                    topLeft = arcBounds.topLeft,
                    size = arcBounds.size
                )
                repeat(DailyStreak.MAX_LEVEL + 1) { index ->
                    val angle = Math.toRadians((startAngle + sweepAngle * index / DailyStreak.MAX_LEVEL).toDouble())
                    val outer = Offset(
                        center.x + cos(angle).toFloat() * (radius + 12.dp.toPx()),
                        center.y + sin(angle).toFloat() * (radius + 12.dp.toPx())
                    )
                    val inner = Offset(
                        center.x + cos(angle).toFloat() * (radius - 2.dp.toPx()),
                        center.y + sin(angle).toFloat() * (radius - 2.dp.toPx())
                    )
                    drawLine(muted, outer, inner, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
                val needleAngle = Math.toRadians(
                    (startAngle + sweepAngle * animatedLevel / DailyStreak.MAX_LEVEL).toDouble()
                )
                val needleEnd = Offset(
                    center.x + cos(needleAngle).toFloat() * radius * 0.73f,
                    center.y + sin(needleAngle).toFloat() * radius * 0.73f
                )
                drawLine(primary, center, needleEnd, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(primary, radius = 9.dp.toPx(), center = center)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = safeLevel.toString(),
                fontSize = 54.sp,
                lineHeight = 54.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "/ ${DailyStreak.MAX_LEVEL}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The current week, Monday to Sunday: a flame on the days with study, ice on the days a freeze
 * covered and a ring around today while it is still open.
 *
 * @param days seven entries, Monday first
 */
@Composable
internal fun StreakWeek(days: List<DailyStreakState>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { StreakDayCell(it) }
    }
}

/**
 * One day of [StreakWeek].
 *
 * @param day the day's initial and status
 */
@Composable
private fun StreakDayCell(day: DailyStreakState) {
    val primary = MaterialTheme.colorScheme.primary
    val neutral = MaterialTheme.colorScheme.onSurface
    val background = when (day.status) {
        StreakStatus.COMPLETED -> primary.copy(alpha = 0.15f)
        StreakStatus.FROZEN -> FreezeBlue.copy(alpha = 0.18f)
        StreakStatus.TODAY -> Color.Transparent
        StreakStatus.FAILED -> neutral.copy(alpha = 0.06f)
        StreakStatus.FUTURE -> neutral.copy(alpha = 0.03f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = day.dayOfWeek,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (day.isToday) FontWeight.Black else FontWeight.Bold,
            color = if (day.isToday) primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(background)
                .then(
                    if (day.status == StreakStatus.TODAY) {
                        Modifier.border(2.dp, primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (day.status) {
                StreakStatus.COMPLETED -> StreakFlame(lit = true, size = 20.dp)
                StreakStatus.FROZEN -> Image(
                    painter = painterResource(id = R.drawable.streak_freezer),
                    contentDescription = "Día salvado con un congelador",
                    modifier = Modifier.size(20.dp)
                )
                else -> Unit
            }
        }
    }
}
