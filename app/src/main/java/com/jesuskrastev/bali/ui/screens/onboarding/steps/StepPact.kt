package com.jesuskrastev.bali.ui.screens.onboarding.steps

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StepPact(onComplete: () -> Unit) {
    var progress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100),
        label = "pact_progress"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Me comprometo a usar Bali para asegurar mi aprobado",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(64.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val startTime = System.currentTimeMillis()
                            val duration = 2500L
                            val job = scope.launch {
                                while (progress < 1f) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                                    if (progress % 0.1f < 0.01f) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                    delay(16)
                                }
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onComplete()
                            }
                            try {
                                awaitRelease()
                            } finally {
                                job.cancel()
                                if (progress < 1f) progress = 0f
                            }
                        }
                    )
                }
        ) {
            // Background Circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    style = Stroke(width = 8.dp.toPx())
                )
            }
            
            // Progress Circle
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round,
                trackColor = Color.Transparent
            )

            // Fingerprint Icon
            Surface(
                shape = CircleShape,
                color = if (progress > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(120.dp),
                shadowElevation = if (progress > 0f) 8.dp else 2.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                        .scale(if (progress > 0f) 1.1f else 1f),
                    tint = if (progress > 0f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = if (progress > 0f) "Mantén pulsado..." else "Pulsa y mantén para firmar",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
