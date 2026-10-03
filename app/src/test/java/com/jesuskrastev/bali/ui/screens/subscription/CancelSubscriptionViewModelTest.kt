package com.jesuskrastev.bali.ui.screens.subscription

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneOffset

class CancelSubscriptionViewModelTest {

    @Test
    fun `a known product opens its own page in Google Play`() {
        assertThat(playSubscriptionsUrl("bali_monthly"))
            .isEqualTo("https://play.google.com/store/account/subscriptions?sku=bali_monthly&package=com.jesuskrastev.bali")
    }

    @Test
    fun `the base plan suffix is dropped from the sku`() {
        assertThat(playSubscriptionsUrl("bali_monthly:monthly-base"))
            .isEqualTo("https://play.google.com/store/account/subscriptions?sku=bali_monthly&package=com.jesuskrastev.bali")
    }

    @Test
    fun `an unknown product opens the subscriptions list`() {
        assertThat(playSubscriptionsUrl(null)).isEqualTo("https://play.google.com/store/account/subscriptions")
        assertThat(playSubscriptionsUrl("")).isEqualTo("https://play.google.com/store/account/subscriptions")
    }

    @Test
    fun `days to the exam count calendar days, not 24 hour blocks`() {
        val lateTonight = 1_798_847_940_000L // 2027-01-01T23:59:00Z
        val exam = 1_798_934_400_000L // 2027-01-03T00:00:00Z

        assertThat(daysUntil(exam, lateTonight, ZoneOffset.UTC)).isEqualTo(2)
        assertThat(daysUntil(lateTonight, exam, ZoneOffset.UTC)).isEqualTo(-2)
    }
}
