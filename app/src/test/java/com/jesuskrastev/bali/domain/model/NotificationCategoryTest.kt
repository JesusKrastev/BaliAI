package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationCategoryTest {

    @Test
    fun `the keys are the ones the OneSignal journeys filter on`() {
        // Renaming a key silently breaks the journeys in the dashboard: this fails first.
        assertThat(NotificationCategory.entries.map { it.key }).containsExactly("study", "promos").inOrder()
    }

    @Test
    fun `nothing switched off gives no tag value`() {
        assertThat(NotificationCategory.offTagValue(emptySet())).isNull()
    }

    @Test
    fun `the tag value does not depend on the order the categories were switched off`() {
        val both = setOf(NotificationCategory.PROMOTIONS, NotificationCategory.STUDY)

        assertThat(NotificationCategory.offTagValue(both)).isEqualTo("study,promos")
    }

    @Test
    fun `stored keys come back as categories and unknown ones are dropped`() {
        val read = NotificationCategory.fromKeys(setOf("promos", "from-a-newer-version"))

        assertThat(read).containsExactly(NotificationCategory.PROMOTIONS)
    }
}
