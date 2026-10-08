package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationCategoryTest {

    @Test
    fun `the channel ids are the ones the OneSignal messages name`() {
        // Renaming one silently sends that kind of push to a new, empty channel: this fails first.
        assertThat(NotificationCategory.entries.map { it.channelId })
            .containsExactly("reminders", "streak", "promotions").inOrder()
    }

    @Test
    fun `no two categories share a channel`() {
        val ids = NotificationCategory.entries.map { it.channelId }

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `the study channel keeps the id installs already have`() {
        // Existing installs created "reminders" before categories existed; keeping the id keeps their choice.
        assertThat(NotificationCategory.STUDY.channelId).isEqualTo("reminders")
    }
}
