package com.jesuskrastev.bali.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Angle the speedometer arc starts at, in degrees. */
private const val ArcStartAngle = -215f

/** Total sweep of the speedometer arc, in degrees. */
private const val ArcMaxSweepAngle = 250f

/**
 * Speedometer-style ring showing how far the user is into their weekly test goal.
 *
 * Above the ring sits a badge with the number of consecutive weeks (the "macro" streak). Once the
 * goal is met the centre swaps to a celebration state and the ring turns green.
 *
 * @param testsCompletedThisWeek Tests already completed during the current week.
 * @param weeklyGoal Tests needed to keep the streak alive; must be greater than zero.
 * @param macroStreakWeeks Consecutive weeks the goal has been met.
 * @param modifier Modifier applied to the root container.
 * @param primaryColor Colour of the progress arc while the goal is still pending.
 * @param successColor Colour the arc animates to when the goal is exactly met.
 */
@Composable
fun WeeklyStreakProgress(
    testsCompletedThisWeek: Int,
    weeklyGoal: Int = 5,
    macroStreakWeeks: Int,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    successColor: Color = BaliAccentGreen
) {
    val isGoalMet = testsCompletedThisWeek >= weeklyGoal
    val isOverachiever = testsCompletedThisWeek > weeklyGoal

    val targetProgress = min(testsCompletedThisWeek.toFloat() / weeklyGoal.toFloat(), 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (isGoalMet) {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
        } else {
            tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        },
        label = "progress_animation"
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isGoalMet && !isOverachiever) successColor else primaryColor,
        animationSpec = tween(700),
        label = "color_animation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "streak_effects")
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val knobColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MacroStreakBadge(
                weeks = macroStreakWeeks,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width - strokeWidth * 3) / 2
                    val arcTopLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)

                    drawArc(
                        color = trackColor,
                        startAngle = ArcStartAngle,
                        sweepAngle = ArcMaxSweepAngle,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    if (animatedProgress > 0f) {
                        val currentSweep = ArcMaxSweepAngle * animatedProgress

                        // Layered wider arcs fake a blurred glow around the progress arc.
                        for (layer in 1..4) {
                            drawArc(
                                color = animatedColor.copy(alpha = (0.15f / layer) * glowIntensity),
                                startAngle = ArcStartAngle,
                                sweepAngle = currentSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = strokeWidth + (layer * 8.dp.toPx()),
                                    cap = StrokeCap.Round
                                )
                            )
                        }

                        drawArc(
                            color = animatedColor,
                            startAngle = ArcStartAngle,
                            sweepAngle = currentSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Knob riding the head of the arc.
                        val angleRad = (ArcStartAngle + currentSweep) * (PI / 180f).toFloat()
                        val knobCenter = Offset(
                            x = center.x + radius * cos(angleRad),
                            y = center.y + radius * sin(angleRad)
                        )
                        drawCircle(
                            color = knobColor.copy(alpha = 0.4f),
                            radius = 12.dp.toPx(),
                            center = knobCenter
                        )
                        drawCircle(
                            color = knobColor,
                            radius = 8.dp.toPx(),
                            center = knobCenter
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isGoalMet) {
                        CelebrationContent(
                            accentColor = animatedColor,
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

/**
 * Pill above the ring with the number of consecutive weeks on streak.
 *
 * @param weeks Consecutive weeks the weekly goal has been met.
 * @param modifier Modifier applied to the pill.
 */
@Composable
private fun MacroStreakBadge(
    weeks: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = "Racha",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "$weeks SEMANAS EN RACHA",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * Centre of the ring once the weekly goal has been reached.
 *
 * @param accentColor Colour of the check icon and the extra-sessions line.
 * @param isOverachiever Whether the user went past the goal.
 * @param count Tests completed this week.
 * @param goal Tests required this week.
 */
@Composable
private fun CelebrationContent(
    accentColor: Color,
    isOverachiever: Boolean,
    count: Int,
    goal: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape)
            )
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "OBJETIVO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )
        Text(
            text = "CUMPLIDO",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp
        )

        if (isOverachiever) {
            Text(
                text = "+${count - goal} EXTRA",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Centre of the ring while the weekly goal is still pending.
 *
 * @param count Tests completed this week.
 * @param goal Tests required this week; a goal of 7 is labelled in days instead of tests.
 * @param labelColor Colour of the unit label under the counter.
 */
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (goal == 7) "DÍAS" else "TESTS",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            ),
            color = labelColor
        )
    }
}
