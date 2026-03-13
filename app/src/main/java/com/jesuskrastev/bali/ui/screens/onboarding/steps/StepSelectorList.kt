package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingEvent
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingViewModel
import com.jesuskrastev.bali.ui.screens.onboarding.components.SingleSelectionCard

@Composable
fun StepSelectorList(
    options: List<String>,
    viewModel: OnboardingViewModel,
    onSelect: (String, OnboardingEvent) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(options) { option ->
            SingleSelectionCard(
                text = option,
                isSelected = false,
                onClick = { onSelect(option, OnboardingEvent.GoToNextStep) }
            )
        }
    }
}
