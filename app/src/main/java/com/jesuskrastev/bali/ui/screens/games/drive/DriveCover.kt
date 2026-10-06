package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import kotlin.math.abs
import kotlin.math.min

/**
 * Live cover of Bali Drive for the arcade catalogue: the real renderer showing Bali's car on
 * autopilot, weaving between lanes to collect the stars of the route's first stretch, then
 * starting over before the first situation. It shows how the game plays before it is opened.
 */
@Composable
fun BaliDriveCover(modifier: Modifier = Modifier) {
    val art = rememberDriveArt()
    val fx = remember { DriveFx() }
    var seed by remember { mutableLongStateOf(7L) }
    var engine by remember { mutableStateOf(demoEngine(seed)) }
    var frame by remember { mutableLongStateOf(0L) }
    var clock by remember { mutableFloatStateOf(0f) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = 0L
            while (true) {
                // The infinite-animation clock lets UI tests stop a cover that would never go idle.
                withInfiniteAnimationFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(1f / 30f)
                    last = now
                    clock += dt
                    val car = engine.car
                    val target = engine.stars.firstOrNull { !it.collected && it.y > car.y - 0.2f && it.y < car.y + 2.5f }?.x ?: laneCenter(1)
                    engine.update(dt, steer = (target - car.targetX) * min(1f, dt * 5f), brake = false)
                    engine.drainEvents().filterIsInstance<DriveEvent.Pickup>().forEach {
                        fx.burst(it.x, it.y, listOf(BaliAccentYellow, Color.White), 7, power = 1.2f)
                    }
                    fx.update(dt)
                    val firstSituation = engine.situations.first().layoutStart
                    if (car.y > firstSituation - 2.5f || abs(car.speed) < 0.01f && engine.drivingTime > 2f) {
                        seed++
                        engine = demoEngine(seed)
                    }
                    frame = now
                }
            }
        }
    }

    Canvas(modifier) {
        if (frame < 0L) return@Canvas // reading the frame makes every frame redraw
        // Projected as on a phone screen, then framed on the car: a wide card would squash the road.
        val projection = DriveProjection(size.width, size.width * PHONE_ASPECT, engine.car.y, viewWidth = 8f, centerX = -0.6f)
        withTransform({ translate(top = size.height * 0.36f - projection.anchorScreenY) }) {
            drawDriveWorld(engine, fx, projection, art, clock)
        }
        // Darkens the lower half so the card's title and button stay readable over the scene.
        drawRect(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
    }
}

/** Height-to-width ratio of the phone screen the scene is designed for. */
private const val PHONE_ASPECT = 2.1f

/** A run already past its countdown, rolling at cruise speed from the start line. */
private fun demoEngine(seed: Long): DriveEngine = DriveEngine(seed).apply {
    update(COUNTDOWN_SECONDS, steer = 0f, brake = false)
    car.speed = car.cruise
    drainEvents()
}
