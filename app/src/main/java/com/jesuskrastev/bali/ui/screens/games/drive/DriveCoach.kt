package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import com.jesuskrastev.bali.ui.theme.RacingFont
import kotlin.math.abs

/** Lesson the first-run coach is currently teaching. */
internal enum class CoachStep { INTRO, STEER, DRIVE, BRAKE, DONE }

/** The coach waits to teach braking until the next situation is this close, in route units. */
private const val BRAKE_LESSON_DISTANCE = 7f

/** Seconds the finger must stay still before the braking lesson is passed. */
private const val BRAKE_HOLD_SECONDS = 0.6f

/** Sideways travel that passes the steering lesson, in lane widths. */
private const val STEER_LESSON_LANES = 0.9f

/**
 * First-run guide that freezes the road until the player performs each gesture: first a slide,
 * then, as the first situation approaches, a held-still finger. Pure so it can be tested without
 * a screen; [DriveRun] feeds it every frame and renders [step] with [DriveCoachOverlay].
 *
 * @param enabled false for players who already finished the lesson, which makes it start as [CoachStep.DONE]
 */
internal class DriveCoach(enabled: Boolean) {
    var step = if (enabled) CoachStep.INTRO else CoachStep.DONE
        private set
    private var travel = 0f
    private var held = 0f

    /** True while the road must stay frozen because the player has a gesture to perform. */
    val freezesRoad: Boolean get() = step == CoachStep.STEER || step == CoachStep.BRAKE

    /**
     * Advances the lessons by one frame and returns the step to show.
     *
     * @param driving whether the car is rolling (countdown over, no fault on screen)
     * @param distanceToSituation route units to the next situation, or infinity if none is left
     * @param dx horizontal finger movement this frame, in pixels
     * @param laneWidthPx width of one lane on screen, in pixels
     * @param braking whether the finger has been held still long enough to brake
     * @param dt seconds since the last frame
     * @return the lesson to display after this frame
     */
    fun update(driving: Boolean, distanceToSituation: Float, dx: Float, laneWidthPx: Float, braking: Boolean, dt: Float): CoachStep {
        when (step) {
            CoachStep.INTRO -> if (driving) step = CoachStep.STEER
            CoachStep.STEER -> {
                travel += abs(dx)
                if (travel >= laneWidthPx * STEER_LESSON_LANES) step = CoachStep.DRIVE
            }
            CoachStep.DRIVE -> if (driving && distanceToSituation < BRAKE_LESSON_DISTANCE) step = CoachStep.BRAKE
            CoachStep.BRAKE -> {
                held = if (braking) held + dt else 0f
                if (held >= BRAKE_HOLD_SECONDS) step = CoachStep.DONE
            }
            CoachStep.DONE -> Unit
        }
        return step
    }
}

/**
 * Lesson card with an animated hand demonstrating the gesture for [step]. It ignores touches so
 * the real controls underneath keep working.
 *
 * @param step the lesson being taught; nothing is drawn for the other steps
 */
@Composable
internal fun BoxScope.DriveCoachOverlay(step: CoachStep) {
    if (step != CoachStep.STEER && step != CoachStep.BRAKE) return
    val braking = step == CoachStep.BRAKE
    val motion = rememberInfiniteTransition(label = "drive_coach")
    val slide by motion.animateFloat(-1f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "coach_slide")
    val pulse by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(900)), label = "coach_pulse")
    val color = if (braking) BaliAccentRed else BaliPrimary

    Column(
        Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val mid = Offset(size.width / 2, size.height / 2)
                if (braking) {
                    // Rings that grow out of the finger read as "pressing and holding".
                    for (i in 0..1) {
                        val t = (pulse + i * 0.5f) % 1f
                        drawCircle(color.copy(alpha = 0.55f * (1f - t)), 34.dp.toPx() + t * 46.dp.toPx(), mid, style = Stroke(5.dp.toPx()))
                    }
                } else {
                    val reach = size.width * 0.3f
                    drawLine(Color.White.copy(alpha = 0.55f), Offset(mid.x - reach, mid.y), Offset(mid.x + reach, mid.y), 8.dp.toPx(), StrokeCap.Round)
                    for (side in listOf(-1f, 1f)) {
                        val tip = Offset(mid.x + side * (reach + 26.dp.toPx() + pulse * 6.dp.toPx()), mid.y)
                        drawLine(Color.White, tip, Offset(tip.x - side * 16.dp.toPx(), tip.y - 16.dp.toPx()), 7.dp.toPx(), StrokeCap.Round)
                        drawLine(Color.White, tip, Offset(tip.x - side * 16.dp.toPx(), tip.y + 16.dp.toPx()), 7.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            DriveIcon(
                DriveIconKind.FINGER,
                Modifier.size(76.dp).graphicsLayer {
                    translationX = if (braking) 0f else slide * size.width * 1.4f
                    scaleX = if (braking) 1f - 0.08f * pulse else 1f
                    scaleY = scaleX
                },
                color,
            )
        }
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = BaliSecondary.copy(alpha = 0.92f),
            border = BorderStroke(3.dp, BaliAccentYellow),
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(if (braking) R.string.drive_coach_brake_title else R.string.drive_coach_steer_title),
                    color = BaliAccentYellow, fontFamily = RacingFont, fontSize = 24.sp, textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(if (braking) R.string.drive_coach_brake_body else R.string.drive_coach_steer_body),
                    color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
