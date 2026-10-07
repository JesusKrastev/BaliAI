package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.screens.onboarding.LearningStyle
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.theme.BaliTheme

/**
 * "¿Cómo te gusta practicar?" as a two-by-two grid of big tiles.
 *
 * It used to be the slowest screen of the flow: four long sentences in a list, read one by one.
 * A grid is taken in at a glance, each tile says two words, and one tap answers it.
 *
 * @param styles the options, laid out two per row
 * @param selectedKey the style already picked, highlighted when coming back
 * @param onSelect called with the tapped style
 */
@Composable
fun StepLearningStyle(
    styles: List<LearningStyle>,
    selectedKey: String?,
    onSelect: (LearningStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        styles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { style ->
                    StyleTile(
                        style = style,
                        isSelected = style.key == selectedKey,
                        onClick = { onSelect(style) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * One tile of the grid: a big emoji, the style in two words and a short hint.
 *
 * @param style the option
 * @param isSelected whether it is the current answer
 * @param onClick called when tapped
 * @param modifier layout modifier
 */
@Composable
private fun StyleTile(
    style: LearningStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = style.emoji, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = style.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = style.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepLearningStylePreview() {
    BaliTheme {
        StepLearningStyle(styles = OnboardingConfig.learningStyles, selectedKey = null, onSelect = {})
    }
}
