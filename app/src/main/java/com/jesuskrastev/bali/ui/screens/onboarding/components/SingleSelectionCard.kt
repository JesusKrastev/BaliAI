package com.jesuskrastev.bali.ui.screens.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Surface(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale),
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, borderColor),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji badge
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

            // Option text — split emoji prefix from the rest if no separate iconEmoji
            val (emoji, label) = remember(text) { splitEmojiAndText(text) }
            if (iconEmoji == null && emoji != null) {
                Text(text = emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                text = if (iconEmoji == null && emoji != null) label else text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Right chevron
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

/** Extracts a leading emoji from the text string, if present. */
private fun splitEmojiAndText(text: String): Pair<String?, String> {
    if (text.isEmpty()) return Pair(null, text)
    val codePoint = text.codePointAt(0)
    val type = Character.getType(codePoint)
    return if (type == Character.OTHER_SYMBOL.toInt() || type == Character.SURROGATE.toInt() ||
        Character.isSurrogate(text[0])
    ) {
        val emojiEnd = Character.charCount(codePoint)
        // also skip a trailing space
        val rest = text.substring(emojiEnd).trimStart()
        Pair(text.substring(0, emojiEnd), rest)
    } else {
        Pair(null, text)
    }
}
