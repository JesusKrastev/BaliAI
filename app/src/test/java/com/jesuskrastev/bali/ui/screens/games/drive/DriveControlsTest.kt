package com.jesuskrastev.bali.ui.screens.games.drive

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DriveControlsTest {
    /** Braking preserves the original short stationary-hold threshold. */
    @Test fun `stationary hold starts braking after 120 milliseconds`() {
        val input = DriveControls().apply { pressed = true }
        input.advance(0.1f, 0f, 90f)
        assertThat(input.braking).isFalse()
        input.advance(0.03f, 0f, 90f)
        assertThat(input.braking).isTrue()
    }

    /** Steering clears stationary time and movement is consumed once per frame. */
    @Test fun `moving finger steers and releases the brake`() {
        val input = DriveControls().apply { pressed = true; pendingDx = -20f; stillFor = 0.2f }
        val dx = input.takeDx()
        assertThat(dx).isEqualTo(-20f)
        assertThat(input.takeDx()).isEqualTo(0f)
        input.advance(0.016f, dx, 90f)
        assertThat(input.braking).isFalse()
    }

    /** Cancellation or lifecycle pause cannot leave braking or steering active. */
    @Test fun `reset clears all pending gesture state`() {
        val input = DriveControls().apply { pressed = true; stillFor = 1f; pendingDx = 50f }
        input.reset()
        assertThat(input.braking).isFalse()
        assertThat(input.takeDx()).isEqualTo(0f)
    }
}
