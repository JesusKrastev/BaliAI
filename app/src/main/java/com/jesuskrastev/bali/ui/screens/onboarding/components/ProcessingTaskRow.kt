package com.jesuskrastev.bali.ui.screens.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SUCCESS_GREEN = Color(0xFF10B981)

/**
 * One line of the "building your plan" checklist.
 *
 * The tick pops in with a spring instead of appearing on a frame boundary, which is what
 * makes the list feel like work finishing rather than like a static image being swapped.
 * The badge on the left carries the *state* of the task and the emoji carries its *subject*,
 * so the two never compete: the badge always looks the same across rows, the emoji never does.
 *
 * @param emoji symbol identifying what this step is about
 * @param text the task description
 * @param isCompleted true once the task is done
 * @param isActive true while the task is the one in progress
 */
@Composable
fun ProcessingTaskRow(
    emoji: String,
    text: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    val rowAlpha by animateFloatAsState(
        targetValue = if (isActive || isCompleted) 1f else 0.35f,
        animationSpec = tween(400),
        label = "row_alpha"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    val badgeColor by animateColorAsState(
        targetValue = if (isCompleted) SUCCESS_GREEN else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(400),
        label = "badge_color"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer { alpha = rowAlpha }
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(18.dp)
                    .scale(checkScale)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(text = emoji, fontSize = 18.sp)

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isCompleted -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
