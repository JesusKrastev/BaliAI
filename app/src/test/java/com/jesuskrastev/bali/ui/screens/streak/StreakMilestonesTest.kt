package com.jesuskrastev.bali.ui.screens.streak

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreakMilestonesTest {

    @Test
    fun `the first year has the listed milestones and no others`() {
        val milestones = (0..365).filter { isStreakMilestone(it) }

        assertThat(milestones).containsExactly(3, 7, 14, 30, 50, 100, 200, 365).inOrder()
    }

    @Test
    fun `an ordinary day is not a milestone`() {
        assertThat(isStreakMilestone(1)).isFalse()
        assertThat(isStreakMilestone(6)).isFalse()
        assertThat(isStreakMilestone(8)).isFalse()
        assertThat(isStreakMilestone(99)).isFalse()
    }

    @Test
    fun `after a year every hundred days and every full year count`() {
        assertThat(isStreakMilestone(366)).isFalse()
        assertThat(isStreakMilestone(400)).isTrue()
        assertThat(isStreakMilestone(730)).isTrue()
        assertThat(isStreakMilestone(1000)).isTrue()
        assertThat(isStreakMilestone(1095)).isTrue()
    }

    @Test
    fun `the next milestone is the first one strictly after the streak`() {
        assertThat(nextStreakMilestone(0)).isEqualTo(3)
        assertThat(nextStreakMilestone(3)).isEqualTo(7)
        assertThat(nextStreakMilestone(10)).isEqualTo(14)
        assertThat(nextStreakMilestone(365)).isEqualTo(400)
    }

    @Test
    fun `milestones read as round numbers in Spanish`() {
        assertThat(streakMilestoneTitle(3)).isEqualTo("¡3 días seguidos!")
        assertThat(streakMilestoneTitle(7)).isEqualTo("¡Una semana seguida!")
        assertThat(streakMilestoneTitle(14)).isEqualTo("¡Dos semanas seguidas!")
        assertThat(streakMilestoneTitle(30)).isEqualTo("¡Un mes seguido!")
        assertThat(streakMilestoneTitle(100)).isEqualTo("¡100 días seguidos!")
        assertThat(streakMilestoneTitle(365)).isEqualTo("¡Un año seguido!")
        assertThat(streakMilestoneTitle(730)).isEqualTo("¡2 años seguidos!")
    }

    @Test
    fun `the subtitle names the record and the next milestone`() {
        assertThat(streakMilestoneSubtitle(7, highest = 7)).isEqualTo("Es tu mejor racha. Próximo hito: 14 días.")
        assertThat(streakMilestoneSubtitle(7, highest = 20)).isEqualTo("Próximo hito: 14 días.")
    }
}
