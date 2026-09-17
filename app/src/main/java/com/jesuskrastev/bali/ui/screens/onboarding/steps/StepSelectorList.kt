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

/**
 * Vertical list of tappable options for a selection step.
 *
 * The options sit right under the mascot bubble that asks the question, so they read as its
 * answers. Centering them in the leftover height opened a large blank gap between the two.
 *
 * @param options option labels to render, one card each
 * @param viewModel unused by this composable; kept so callers can pass it straight through
 *   to [onSelect] without a local capture
 * @param onSelect invoked with the tapped option and the navigation event that should follow
 */
@Composable
fun StepSelectorList(
    options: List<String>,
    viewModel: OnboardingViewModel,
    onSelect: (String, OnboardingEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(options, key = { it }) { option ->
            SingleSelectionCard(
                text = option,
                isSelected = false,
                onClick = { onSelect(option, OnboardingEvent.GoToNextStep) }
            )
        }
    }
}
