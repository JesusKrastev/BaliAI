package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.screens.stats.localDayFromPickerMillis
import com.jesuskrastev.bali.ui.screens.stats.pickerMillisFromLocalDay
import com.jesuskrastev.bali.ui.theme.BaliTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** How far ahead the exam can be: the DGT books within months, never years. */
private val MAX_DAYS_AHEAD = TimeUnit.DAYS.toMillis(365)

private val BUTTON_SHAPE = RoundedCornerShape(16.dp)

/** The side margin of the rest of the flow, applied here to the buttons only. */
private val SIDE_MARGIN = 24.dp

/**
 * Asks for the exam day with a calendar right on the screen, plus a way out for the many who
 * have not booked it yet.
 *
 * The confirm button names the day picked, so the user sees what they are saying yes to; "Aún
 * no tengo fecha" is just as visible, because most students reach this screen without a date
 * and must not feel the plan is not for them.
 *
 * @param examDate the day already given (local midnight), preselected when coming back
 * @param onConfirm called with the picker's selection, midnight UTC of the chosen day
 * @param onNoDate called when the user has no exam date yet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepExamDate(
    examDate: Long?,
    onConfirm: (Long) -> Unit,
    onNoDate: () -> Unit
) {
    val today = remember { pickerMillisFromLocalDay(System.currentTimeMillis()) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = examDate?.let(::pickerMillisFromLocalDay)?.takeIf { it >= today },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= today && utcTimeMillis <= today + MAX_DAYS_AHEAD
        }
    )
    val selected = state.selectedDateMillis

    // The calendar takes the whole width: Material's month grid needs 360 dp and would be clipped
    // on a 360 dp phone with the flow's side margins. Only the buttons keep them.
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            DatePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
            )
        }

        Column(modifier = Modifier.padding(horizontal = SIDE_MARGIN)) {
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = BUTTON_SHAPE
            ) {
                Text(
                    text = selected?.let { "Es el ${dayLabel(localDayFromPickerMillis(it))} →" }
                        ?: "Elige el día en el calendario",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onNoDate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = BUTTON_SHAPE
            ) {
                Text(text = "📝 Aún no tengo fecha", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Names a day the way the confirm button reads it: "24 de octubre".
 *
 * @param localMillis local midnight of the day
 * @return the day and month in Spanish
 */
private fun dayLabel(localMillis: Long): String =
    SimpleDateFormat("d 'de' MMMM", Locale("es", "ES")).format(Date(localMillis))

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun StepExamDatePreview() {
    BaliTheme {
        StepExamDate(examDate = null, onConfirm = {}, onNoDate = {})
    }
}
