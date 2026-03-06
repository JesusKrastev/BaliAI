package com.jesuskrastev.bali.ui.screens.streak

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus

@Composable
fun HomeStreakScreen(
    viewModel: HomeStreakViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorPrimary = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0905))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0905),
                        Color(0xFF1A1108)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        if (!uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .systemBarsPadding()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver",
                                tint = colorPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "TU RACHA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = colorPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 40.dp),
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Gauge Segment
                StreakGauge(
                    current = uiState.currentStreak,
                    total = 7,
                    color = colorPrimary
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Weekly Progress Card
                ProgressCard(uiState = uiState, color = colorPrimary)

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.LocalFireDepartment,
                        label = "RACHA ACTUAL",
                        value = uiState.currentStreak.toString(),
                        subLabel = "Días consecutivos",
                        color = colorPrimary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.EmojiEvents,
                        label = "MEJOR RACHA",
                        value = uiState.highestStreak.toString(),
                        subLabel = "Récord personal",
                        color = colorPrimary
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Footer Section
                FooterSection(message = uiState.encouragingMessage, color = colorPrimary)
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun StreakGauge(current: Int, total: Int, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = current.toFloat() / total.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "gauge"
    )

    Box(
        modifier = Modifier.size(280.dp, 160.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height
            val radius = size.width * 0.45f
            val strokeWidth = 24.dp.toPx()

            // Arch background
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Progress Arch with Glow
            drawArc(
                brush = Brush.sweepGradient(
                    0.5f to color,
                    1.0f to color.copy(alpha = 0.5f)
                ),
                startAngle = 180f,
                sweepAngle = 180f * animatedProgress,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Needle Indicator
            val angle = 180f + (180f * animatedProgress)
            val rad = Math.toRadians(angle.toDouble())
            val tipX = cx + (radius) * Math.cos(rad).toFloat()
            val tipY = cy + (radius) * Math.sin(rad).toFloat()
            
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(cx, cy),
                end = androidx.compose.ui.geometry.Offset(tipX, tipY),
                strokeWidth = 4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        ) {
            Text(
                text = "$current/$total",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "DÍAS SEMANALES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun ProgressCard(uiState: HomeStreakUiState, color: Color) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Progreso de la Semana",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.weeklyStreak.forEach { day ->
                    DayIndicator(day = day, color = color)
                }
            }
        }
    }
}

@Composable
fun DayIndicator(day: DailyStreakState, color: Color) {
    val isCompleted = day.status == StreakStatus.COMPLETED
    val isToday = day.isToday
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isCompleted) color else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (isCompleted) color else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                )
                .then(
                    if (isCompleted) Modifier.background(
                        Brush.radialGradient(
                            listOf(color, color.copy(alpha = 0.6f))
                        )
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.dayOfWeek,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) Color.Black else Color.White.copy(alpha = 0.4f)
            )
            
            if (isCompleted) {
                // Glow effect simulation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color.Transparent, color.copy(alpha = 0.2f))
                            ),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subLabel: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier.height(180.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Text(
                text = value, 
                style = MaterialTheme.typography.displayMedium, 
                fontWeight = FontWeight.Black, 
                color = Color.White
            )
            
            Text(
                text = subLabel, 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun FooterSection(message: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Text(
            text = "¡Sigue así, Campeón!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
