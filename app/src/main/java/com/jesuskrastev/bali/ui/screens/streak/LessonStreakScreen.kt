package com.jesuskrastev.bali.ui.screens.streak

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus

@Composable
fun LessonStreakScreen(
    viewModel: LessonStreakViewModel,
    newStreak: Int,
    onContinueClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val colorPrimary = MaterialTheme.colorScheme.primary
    
    Scaffold(
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                Button(
                    onClick = onContinueClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                ) {
                    Text("CONTINUAR", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (!uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    
                    Text(
                        text = if (newStreak == 1) "¡RACHA INICIADA!" else "¡RACHA MANTENIDA!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = colorPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Box(contentAlignment = Alignment.Center) {
                        // Resplandor
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .scale(scale)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(colorPrimary.copy(alpha = 0.3f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )
                        Image(
                            painter = painterResource(id = R.drawable.streak),
                            contentDescription = "Racha",
                            modifier = Modifier.size(140.dp).scale(scale)
                        )
                        // Añadiendo el número de días dentro
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 20.dp)) {
                            Text(
                                text = newStreak.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "$newStreak ${if (newStreak == 1) "día" else "días"} de racha",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    StreakTracker(weeklyStreak = uiState.weeklyStreak)
                }
            }
        }
    }
}

@Composable
fun StreakTracker(weeklyStreak: List<DailyStreakState>) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weeklyStreak.forEach { day ->
                StreakDayItem(state = day)
            }
        }
    }
}

@Composable
fun StreakDayItem(state: DailyStreakState) {
    val circleColor = when (state.status) {
        StreakStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        StreakStatus.FROZEN -> Color(0xFF2196F3)
        StreakStatus.TODAY -> MaterialTheme.colorScheme.surfaceVariant
        StreakStatus.FAILED, StreakStatus.FUTURE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = state.dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(circleColor)
                .then(
                    if (state.isToday && state.status != StreakStatus.COMPLETED) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.status == StreakStatus.COMPLETED) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
