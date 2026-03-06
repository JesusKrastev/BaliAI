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
    newWeekSessions: Int,
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = if (newWeekSessions == 1) "¡PRIMERA SESIÓN!" else "¡SESIÓN COMPLETADA!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = colorPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Box(contentAlignment = Alignment.Center) {
                    // Resplandor
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .scale(scale)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(colorPrimary.copy(alpha = 0.4f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )
                    Image(
                        painter = painterResource(id = R.drawable.streak),
                        contentDescription = "Llama de Racha",
                        modifier = Modifier.size(200.dp).scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$newWeekSessions / ${uiState.weeklyGoal}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "sesiones esta semana",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                StreakTracker(weeklyStreak = uiState.weeklyStreak)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val motivationalText = when {
                    newWeekSessions >= uiState.weeklyGoal -> "¡Semana completada! Tu racha de ${uiState.currentStreak} semanas sigue en pie 🔥"
                    newWeekSessions == uiState.weeklyGoal - 1 -> "¡A una sesión de completar la semana!"
                    else -> "Llevas $newWeekSessions de ${uiState.weeklyGoal} sesiones esta semana"
                }
                
                val textColor = when {
                    newWeekSessions >= uiState.weeklyGoal -> colorPrimary
                    newWeekSessions == uiState.weeklyGoal - 1 -> Color.White.copy(alpha = 0.7f)
                    else -> Color.White.copy(alpha = 0.5f)
                }
                
                Text(
                    text = motivationalText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = onContinueClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                ) {
                    Text("CONTINUAR", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun StreakTracker(weeklyStreak: List<DailyStreakState>) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
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
private fun StreakDayItem(state: DailyStreakState) {
    val isCompleted = state.status == StreakStatus.COMPLETED
    val isToday = state.isToday
    
    val baseColor = when (state.status) {
        StreakStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        StreakStatus.FROZEN -> Color(0xFF2196F3)
        StreakStatus.TODAY -> MaterialTheme.colorScheme.surfaceVariant
        StreakStatus.FAILED, StreakStatus.FUTURE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = state.dayOfWeek,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(baseColor)
                .then(
                    if (isToday && !isCompleted) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else if (state.status == StreakStatus.TODAY) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
