package com.jesuskrastev.bali.ui.screens.games.drive

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DriveCoachTest {
    private fun DriveCoach.tick(
        driving: Boolean = true,
        distance: Float = Float.MAX_VALUE,
        dx: Float = 0f,
        braking: Boolean = false,
        dt: Float = 0.016f,
    ) = update(driving, distance, dx, laneWidthPx = 100f, braking = braking, dt = dt)

    /** Players who already finished the lesson never see the coach or a frozen road. */
    @Test fun `disabled coach is done and never freezes`() {
        val coach = DriveCoach(enabled = false)
        assertThat(coach.step).isEqualTo(CoachStep.DONE)
        assertThat(coach.freezesRoad).isFalse()
    }

    /** The countdown is not coached; the steering lesson starts when the car rolls and freezes the road. */
    @Test fun `steering lesson starts after the countdown and waits for a slide`() {
        val coach = DriveCoach(enabled = true)
        assertThat(coach.tick(driving = false)).isEqualTo(CoachStep.INTRO)
        assertThat(coach.tick()).isEqualTo(CoachStep.STEER)
        assertThat(coach.freezesRoad).isTrue()
        assertThat(coach.tick(dx = 30f)).isEqualTo(CoachStep.STEER)
        assertThat(coach.tick(dx = -70f)).isEqualTo(CoachStep.DRIVE)
        assertThat(coach.freezesRoad).isFalse()
    }

    /** Braking is taught only near the first situation, and the finger must stay still long enough. */
    @Test fun `braking lesson needs a sustained hold and a release resets it`() {
        val coach = DriveCoach(enabled = true)
        coach.tick(); coach.tick(dx = 100f)
        assertThat(coach.tick(distance = 20f)).isEqualTo(CoachStep.DRIVE)
        assertThat(coach.tick(distance = 5f)).isEqualTo(CoachStep.BRAKE)
        assertThat(coach.freezesRoad).isTrue()
        coach.tick(braking = true, dt = 0.4f)
        coach.tick(braking = false)
        assertThat(coach.tick(braking = true, dt = 0.4f)).isEqualTo(CoachStep.BRAKE)
        assertThat(coach.tick(braking = true, dt = 0.4f)).isEqualTo(CoachStep.DONE)
        assertThat(coach.freezesRoad).isFalse()
    }
}
