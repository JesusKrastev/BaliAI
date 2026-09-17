package com.jesuskrastev.bali.ui.screens.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tappable option card used by the onboarding selection steps.
 *
 * Grows with its label instead of using a fixed height, so long answers wrap onto a second
 * line instead of being clipped on narrow screens.
 *
 * @param text option label; a leading emoji is split off and drawn before the label
 * @param isSelected whether the option is currently chosen, which tints the card
 * @param onClick invoked when the card is tapped
 * @param iconEmoji optional emoji shown in a round badge instead of the label's own emoji
 */
@Composable
fun SingleSelectionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconEmoji: String? = null
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "bg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "border"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(28.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, borderColor),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconEmoji != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            val (emoji, label) = remember(text) { splitEmojiAndText(text) }
            val inlineEmoji = emoji.takeIf { iconEmoji == null }
            if (inlineEmoji != null) {
                // Fixed-width slot so every label starts at the same x, whatever the glyph width.
                Text(
                    text = inlineEmoji,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = if (inlineEmoji != null) label else text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Splits a leading emoji off an option label.
 *
 * Takes everything up to the first space rather than a single code point, so composed emoji
 * (ZWJ sequences such as 😵‍💫, or ones carrying a variation selector such as 🕊️) stay whole
 * instead of leaking their tail into the label.
 *
 * @param text the option label, e.g. "🎯 Tests adaptados a mis fallos"
 * @return the emoji (or null when the label does not start with one) and the remaining label
 */
private fun splitEmojiAndText(text: String): Pair<String?, String> {
    if (text.isEmpty() || ' ' !in text) return null to text
    val startsWithEmoji = Character.isSurrogate(text[0]) ||
        Character.getType(text.codePointAt(0)) == Character.OTHER_SYMBOL.toInt()
    if (!startsWithEmoji) return null to text
    return text.substringBefore(' ') to text.substringAfter(' ').trim()
}
