package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.onboarding.NarrativeContent
import com.jesuskrastev.bali.ui.theme.BaliTheme

/**
 * Offers the study reminder. The question itself is in the mascot bubble above; this screen
 * gives the reason and the two answers.
 *
 * It owns its buttons instead of using the flow's bottom bar because it needs a way to say
 * no: "Ahora no" must be as easy to find as yes, or the system dialog gets refused out of
 * distrust — and Android only lets the app show it twice.
 *
 * @param pitch the body line, with the highlighted part wrapped in pipes
 * @param isRequesting true while the system dialog is open, which disables both answers
 * @param onAccept invoked on "Sí, avísame"
 * @param onDecline invoked on "Ahora no"
 */
@Composable
fun StepNotifications(
    pitch: String,
    isRequesting: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepNarrative(
            content = NarrativeContent(emoji = "🔔", body = pitch, animation = R.raw.notification_bell),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAccept,
            enabled = !isRequesting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "Sí, avísame 🔔", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(
            onClick = onDecline,
            enabled = !isRequesting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Ahora no", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepNotificationsPreview() {
    BaliTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            StepNotifications(
                pitch = "Has dicho que estudiarás |varias veces por semana|. Un aviso a tu hora y no pierdes la racha.",
                isRequesting = false,
                onAccept = {},
                onDecline = {}
            )
        }
    }
}
