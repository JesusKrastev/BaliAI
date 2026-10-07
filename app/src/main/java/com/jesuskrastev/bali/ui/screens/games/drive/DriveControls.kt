package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/** Shared gesture state for practice and driving, preserving the 120 ms stationary brake delay. */
internal class DriveControls {
    var pressed = false
    var pendingDx = 0f
    var stillFor = 0f
    val braking: Boolean get() = pressed && stillFor >= 0.12f

    /** Consumes accumulated horizontal movement and returns pixels since the previous frame. */
    fun takeDx(): Float = pendingDx.also { pendingDx = 0f }

    /** Advances stationary time by [dt], resetting when [dx] exceeds [stillSpeedPx] per second. */
    fun advance(dt: Float, dx: Float, stillSpeedPx: Float) {
        stillFor = when {
            !pressed -> 0f
            dt > 0f && abs(dx) / dt > stillSpeedPx -> 0f
            else -> stillFor + dt
        }
    }

    /** Clears all input on release, cancellation or lifecycle pause; returns Unit. */
    fun reset() { pressed = false; pendingDx = 0f; stillFor = 0f }
}

/** Collects movement into [controls], invokes [onPress] on contact and clears all ended gestures. */
internal fun Modifier.driveControls(controls: DriveControls, onPress: (() -> Unit)? = null): Modifier = pointerInput(controls) {
    awaitEachGesture {
        try {
            val down = awaitFirstDown(requireUnconsumed = false)
            controls.pressed = true
            onPress?.invoke()
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                controls.pendingDx += change.positionChange().x
                change.consume()
            }
        } finally { controls.reset() }
    }
}
