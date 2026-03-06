package com.jesuskrastev.bali.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun WeeklyStreakProgress(
    testsCompletedThisWeek: Int,
    weeklyGoal: Int = 4,
    macroStreakWeeks: Int,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFFFF9800), // Vibrant orange
    successColor: Color = Color(0xFF4CAF50), // Bright green
    overachieverColor: Color = Color(0xFFFFC107) // Gold/glow
) {
    val isGoalMet = testsCompletedThisWeek >= weeklyGoal
    val isOverachiever = testsCompletedThisWeek > weeklyGoal

    val targetProgress = min(testsCompletedThisWeek.toFloat() / weeklyGoal.toFloat(), 1f)

    // Fluid animation for the ring
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (isGoalMet) spring(
            dampingRatio = 0.5f, // More bouncy
            stiffness = Spring.StiffnessLow
        ) else tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    // Color animation
    val animatedColor by animateColorAsState(
        targetValue = when {
            isOverachiever -> overachieverColor
            isGoalMet -> successColor
            else -> primaryColor
        },
        animationSpec = tween(500),
        label = "color_animation"
    )

    // Overachiever extra ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Macro Indicator (Fire badge)
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = "Racha",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Racha: $macroStreakWeeks semanas",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Circular Progress Bar
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 28.dp.toPx()
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width - strokeWidth) / 2
                    val startAngle = -210f
                    val maxSweepAngle = 240f

                    // Background Ring
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.15f),
                        startAngle = startAngle,
                        sweepAngle = maxSweepAngle,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Overachiever Glow / Pulse ring
                    if (isOverachiever) {
                        drawArc(
                            color = overachieverColor.copy(alpha = pulseAlpha),
                            startAngle = startAngle,
                            sweepAngle = maxSweepAngle,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - radius * pulseScale,
                                center.y - radius * pulseScale
                            ),
                            size = Size(radius * 2 * pulseScale, radius * 2 * pulseScale),
                            style = Stroke(width = strokeWidth * 0.4f, cap = StrokeCap.Round)
                        )
                    }

                    // Foreground Progress Ring
                    val sweepAngle = maxSweepAngle * animatedProgress
                    
                    if (animatedProgress > 0f) {
                        drawArc(
                            brush = Brush.horizontalGradient(
                                colors = listOf(animatedColor.copy(alpha = 0.8f), animatedColor)
                            ),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isGoalMet) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(animatedColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Completado",
                                tint = animatedColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¡Semana superada!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = animatedColor
                        )
                    } else {
                        Text(
                            text = testsCompletedThisWeek.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "/ $weeklyGoal tests",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            
            // Optional: extra text below if needed
            if (isOverachiever) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "¡Racha extendida (+${testsCompletedThisWeek - weeklyGoal})!",
                    style = MaterialTheme.typography.labelLarge,
                    color = overachieverColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
