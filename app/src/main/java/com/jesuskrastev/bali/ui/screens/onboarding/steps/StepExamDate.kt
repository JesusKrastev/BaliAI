package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingEvent
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingViewModel
import com.jesuskrastev.bali.ui.screens.onboarding.components.SingleSelectionCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepExamDate(
    selectedDate: Long?,
    viewModel: OnboardingViewModel
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateText = remember(selectedDate) {
        if (selectedDate != null) {
            SimpleDateFormat("dd 'de' MMMM yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(selectedDate))
        } else "Seleccionar fecha"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleSelectionCard(
            text = dateText,
            isSelected = selectedDate != null,
            onClick = { showDatePicker = true },
            iconEmoji = "📅"
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { viewModel.onEvent(OnboardingEvent.SelectExamDate(null)) }) {
            Text("No tengo fecha aún", color = MaterialTheme.colorScheme.outline)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis > System.currentTimeMillis()
        })
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    viewModel.onEvent(OnboardingEvent.SelectExamDate(datePickerState.selectedDateMillis))
                }) { Text("Perfecto") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Atrás") } }
        ) { DatePicker(state = datePickerState) }
    }
}
