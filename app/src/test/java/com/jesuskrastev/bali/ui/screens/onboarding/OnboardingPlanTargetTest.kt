package com.jesuskrastev.bali.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class OnboardingPlanTargetTest {

    private val now = 1_790_000_000_000L

    @Test
    fun `a booked exam still ahead is the plan's date`() {
        val exam = now + TimeUnit.DAYS.toMillis(10)

        assertThat(OnboardingConfig.planTargetMillis(exam, OnboardingConfig.WEEKLY_STUDY_DAILY, now))
            .isEqualTo(exam)
    }

    @Test
    fun `without an exam the date follows the study rhythm`() {
        val daily = OnboardingConfig.planTargetMillis(null, OnboardingConfig.WEEKLY_STUDY_DAILY, now)
        val often = OnboardingConfig.planTargetMillis(null, OnboardingConfig.WEEKLY_STUDY_OFTEN, now)
        val whenever = OnboardingConfig.planTargetMillis(null, OnboardingConfig.WEEKLY_STUDY_WHENEVER, now)

        assertThat(TimeUnit.MILLISECONDS.toDays(daily - now)).isAtLeast(20L)
        assertThat(daily).isLessThan(often)
        assertThat(often).isLessThan(whenever)
    }

    @Test
    fun `an exam date already past is ignored`() {
        val pastExam = now - TimeUnit.DAYS.toMillis(1)

        assertThat(OnboardingConfig.planTargetMillis(pastExam, null, now)).isGreaterThan(now)
    }
}
