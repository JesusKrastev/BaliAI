package com.jesuskrastev.bali.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

@Composable
fun WeeklyStreakProgress(
    testsCompletedThisWeek: Int,
    weeklyGoal: Int = 3,
    macroStreakWeeks: Int,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary, // Use app primary color
    successColor: Color = Color(0xFF00E676)
) {
    val isGoalMet = testsCompletedThisWeek >= weeklyGoal
    val isOverachiever = testsCompletedThisWeek > weeklyGoal

    val targetProgress = min(testsCompletedThisWeek.toFloat() / weeklyGoal.toFloat(), 1f)

    // Fluid animation for the ring
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (isGoalMet) spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessLow
        ) else tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    // Color animation
    val animatedColor by animateColorAsState(
        targetValue = if (isGoalMet && !isOverachiever) successColor else primaryColor,
        animationSpec = tween(700),
        label = "color_animation"
    )

    // Infinite transitions for effects
    val infiniteTransition = rememberInfiniteTransition(label = "streak_effects")
    

    // Glow intensity animation
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    // Rotation for celebratory elements
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
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
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    )
                ),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = "Racha",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "$macroStreakWeeks SEMANAS EN RACHA",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Speedometer-style Circular Progress Bar
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width - strokeWidth * 3) / 2
                    val startAngle = -215f
                    val maxSweepAngle = 250f

                    // 1. Background Arc (Dark Track)
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = startAngle,
                        sweepAngle = maxSweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    if (animatedProgress > 0f) {
                        val currentSweep = maxSweepAngle * animatedProgress
                        
                        // 2. Beautiful Glow Effect (inspired by the image)
                        // Layered arcs to simulate a blur/glow
                        for (i in 1..4) {
                            val layerAlpha = (0.15f / i) * glowIntensity
                            val layerWidth = strokeWidth + (i * 8.dp.toPx())
                            drawArc(
                                color = animatedColor.copy(alpha = layerAlpha),
                                startAngle = startAngle,
                                sweepAngle = currentSweep,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = layerWidth, cap = StrokeCap.Round)
                            )
                        }

                        // 3. Main Progress Arc
                        drawArc(
                            color = animatedColor,
                            startAngle = startAngle,
                            sweepAngle = currentSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        
                        // 4. White Dot Indicator (exactly like the reference image)
                        val angleRad = (startAngle + currentSweep) * (PI / 180f).toFloat()
                        val dotX = center.x + radius * cos(angleRad)
                        val dotY = center.y + radius * sin(angleRad)
                        
                        // Outer dot glow
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                        // Main white dot
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                // Center Content
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isGoalMet) {
                        CelebrationContent(
                            animatedColor = animatedColor,
                            rotationAngle = rotationAngle,
                            isOverachiever = isOverachiever,
                            count = testsCompletedThisWeek,
                            goal = weeklyGoal
                        )
                    } else {
                        StandardProgressContent(
                            count = testsCompletedThisWeek,
                            goal = weeklyGoal,
                            labelColor = animatedColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationContent(
    animatedColor: Color,
    rotationAngle: Float,
    isOverachiever: Boolean,
    count: Int,
    goal: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Success Glow
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(animatedColor.copy(alpha = 0.1f), CircleShape)
            )
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = animatedColor,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "OBJETIVO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 2.sp
        )
        Text(
            text = "CUMPLIDO",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )
        
        if (isOverachiever) {
            Text(
                text = "+${count - goal} EXTRA",
                style = MaterialTheme.typography.labelMedium,
                color = animatedColor,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StandardProgressContent(
    count: Int,
    goal: Int,
    labelColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 84.sp,
                fontWeight = FontWeight.W900
            ),
            color = Color.White
        )
        Text(
            text = if (goal == 7) "DÍAS" else "TESTS", // Logic for tests/days
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = labelColor
        )
    }
}

private val SineHoverEasing = Easing { fraction ->
    sin(fraction * PI.toFloat() * 1).let { if (it < 0) -it else it }
    fraction
}

