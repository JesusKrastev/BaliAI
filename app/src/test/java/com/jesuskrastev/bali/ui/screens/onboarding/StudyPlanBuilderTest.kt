package com.jesuskrastev.bali.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class StudyPlanBuilderTest {

    /** A Monday at noon, so day arithmetic never lands on a daylight-saving change. */
    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 5, 12, 0, 0) }.timeInMillis

    private fun inDays(days: Int): Long =
        Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_MONTH, days) }.timeInMillis

    @Test
    fun `the path sections come from the learning path, without the mock exams block`() {
        val sections = StudyPlanBuilder.pathSections

        assertThat(sections).hasSize(9)
        assertThat(sections.first().second).isEqualTo("El Conductor")
        assertThat(sections.map { it.first }).isInOrder()
    }

    @Test
    fun `a far exam is told in weeks and ends with a final stretch`() {
        val plan = StudyPlanBuilder.build(now, inDays(28))

        assertThat(plan.blocks.map { it.title })
            .containsExactly("Semana 1", "Semana 2", "Semana 3", "Semana 4").inOrder()
        assertThat(plan.blocks.last().isFinalStretch).isTrue()
        assertThat(plan.blocks.last().sections).isEmpty()
        assertThat(plan.blocks.dropLast(1).none { it.isFinalStretch }).isTrue()
    }

    @Test
    fun `every section of the path is studied once, in path order`() {
        listOf(3, 9, 15, 28, 60, 200).forEach { days ->
            val plan = StudyPlanBuilder.build(now, inDays(days))

            assertThat(plan.blocks.flatMap { it.sections })
                .containsExactlyElementsIn(StudyPlanBuilder.pathSections.map { it.second })
                .inOrder()
        }
    }

    @Test
    fun `a close exam is told in three runs of days`() {
        val plan = StudyPlanBuilder.build(now, inDays(10))

        assertThat(plan.blocks.map { it.title })
            .containsExactly("Días 1–4", "Días 5–7", "Días 8–10").inOrder()
    }

    @Test
    fun `an exam tomorrow still gets a three-day plan`() {
        val plan = StudyPlanBuilder.build(now, inDays(1))

        assertThat(plan.days).isEqualTo(3)
        assertThat(plan.blocks.map { it.title }).containsExactly("Día 1", "Día 2", "Día 3").inOrder()
    }

    @Test
    fun `a far exam groups weeks so the list never runs long`() {
        val plan = StudyPlanBuilder.build(now, inDays(120))

        assertThat(plan.blocks).hasSize(6)
        assertThat(plan.blocks.first().title).startsWith("Semanas 1–")
    }

    @Test
    fun `the stretches follow each other without gaps`() {
        val plan = StudyPlanBuilder.build(now, inDays(30))

        plan.blocks.zipWithNext().forEach { (current, next) ->
            assertThat(next.startMillis).isGreaterThan(current.endMillis)
        }
        assertThat(plan.blocks.first().startMillis).isAtMost(now)
    }

    @Test
    fun `a missed topic is flagged in the stretch that studies it and again at the end`() {
        val alcohol = OnboardingQuiz.questionsFor(OnboardingConfig.CONCERN_SILLY_MISTAKES)
            .single { it.topic == "Alcohol" }

        val plan = StudyPlanBuilder.build(now, inDays(28), failed = listOf(alcohol))

        val withAlcohol = plan.blocks.filter { "Alcohol" in it.reinforce }
        assertThat(withAlcohol.map { it.isFinalStretch }).containsExactly(false, true).inOrder()
        assertThat(withAlcohol.first().sections).contains("El Conductor")
    }
}
