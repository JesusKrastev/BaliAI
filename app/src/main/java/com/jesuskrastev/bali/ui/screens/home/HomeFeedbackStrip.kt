package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.util.openWhatsAppChat

/**
 * Slim, always-visible invitation under Home's status row to write to the team on WhatsApp. It is a
 * single line so it never competes with the learning path, and it is not dismissible: the team
 * wants the student's opinion available at every visit to Home.
 *
 * @param modifier layout modifier for the strip
 */
@Composable
fun HomeFeedbackStrip(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val green = Color(0xFF25D366)
    Surface(
        onClick = { context.openWhatsAppChat() },
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = green.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, green.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = green, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Forum, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = "Tu opinión vale oro · Escríbenos por WhatsApp",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
