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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus

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
        painter = painterResource(id = R.drawable.streak_icon),
        contentDescription = null,
        colorFilter = if (lit) null else Greyscale,
        alpha = if (lit) 1f else 0.45f,
        modifier = modifier.size(size)
    )
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
