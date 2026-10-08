package com.jesuskrastev.bali.ui.screens.subscription

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.ProgressStats
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.ReadinessResult
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

    private fun stats(total: Int, correct: Int, probability: Float?) = ProgressStats(
        readiness = ReadinessResult(
            level = ReadinessLevel.entries.first(),
            passProbability = probability,
            mocksTaken = 0,
            mocksMissing = 0,
            history = emptyList(),
            recent = emptyList(),
            passedInRecent = 0,
            averageScore = null,
            bestScore = null,
            trend = null
        ),
        topics = emptyList(),
        totalQuestions = total,
        correctQuestions = correct,
        practiceSessions = 0,
        week = emptyList(),
        studyDays = emptyList(),
        studyDaysCount = 0,
        currentStreak = 0,
        highestStreak = 0,
        level = 1,
        xp = 0
    )

    @Test
    fun `every reason but other has a reply and no reply offers a discount`() {
        CancelReason.entries.filter { it != CancelReason.Other }.forEach { reason ->
            val (title, body) = reasonReply(reason, stats(40, 30, 0.6f))!!
            assertThat(title + body).doesNotContain("descuento")
        }
        assertThat(reasonReply(CancelReason.Other, null)).isNull()
    }

    @Test
    fun `not working is answered with the user's own figures`() {
        val (_, body) = reasonReply(CancelReason.NotWorking, stats(40, 30, 0.6f))!!

        assertThat(body).contains("40 preguntas")
        assertThat(body).contains("75 %")
        assertThat(body).contains("60 de cada 100")
    }

    @Test
    fun `not working without progress points to a mock exam instead of inventing figures`() {
        val (_, body) = reasonReply(CancelReason.NotWorking, stats(0, 0, null))!!

        assertThat(body).contains("simulacro")
        assertThat(body).doesNotContain("preguntas")
    }

    @Test
    fun `reason ids are unique`() {
        assertThat(CancelReason.entries.map { it.id }).containsNoDuplicates()
    }
}
