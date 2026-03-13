package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingEvent
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingViewModel
import com.jesuskrastev.bali.ui.screens.onboarding.NotificationManager
import com.jesuskrastev.bali.ui.screens.onboarding.components.SocialProofBadge

@Composable
fun StepNotifications(
    viewModel: OnboardingViewModel,
    notificationManager: NotificationManager = NotificationManager()
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable {
                    scope.launch {
                        notificationManager.requestNotificationPermission()
                        viewModel.onEvent(OnboardingEvent.GoToNextStep)
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permitir notificaciones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Impulsa tu aprobado con recordatorios inteligentes.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(contentAlignment = Alignment.TopCenter) {
                    Button(
                        onClick = {
                            scope.launch {
                                notificationManager.requestNotificationPermission()
                                viewModel.onEvent(OnboardingEvent.GoToNextStep)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Activar avisos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    PointingFingerEmoji()
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { viewModel.onEvent(OnboardingEvent.GoToNextStep) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ahora no", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SocialProofBadge()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PointingFingerEmoji() {
    val infiniteTransition = rememberInfiniteTransition(label = "finger_bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -55f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Text(
        text = "👇",
        fontSize = 40.sp,
        modifier = Modifier.offset(y = offsetY.dp)
    )
}
