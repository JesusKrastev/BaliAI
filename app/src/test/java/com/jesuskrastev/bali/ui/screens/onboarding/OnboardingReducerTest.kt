package com.jesuskrastev.bali.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnboardingReducerTest {

    private val reducer = OnboardingReducer()

    @Test
    fun `informational steps always allow moving forward`() {
        val informationalSteps = listOf(
            OnboardingStep.Empathy,
            OnboardingStep.LossTime,
            OnboardingStep.LossOpportunity,
            OnboardingStep.LossAutonomy,
            OnboardingStep.GainFreedom,
            OnboardingStep.GainExperiences,
            OnboardingStep.GainLevelUp,
            OnboardingStep.MethodComparison,
            OnboardingStep.Comparison,
            OnboardingStep.PlanReveal,
            OnboardingStep.SocialProof
        )

        informationalSteps.forEach { step ->
            assertThat(reducer.shouldEnableNextButton(step, OnboardingData())).isTrue()
        }
    }

    @Test
    fun `selection steps do not enable the bottom button`() {
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Motivation, OnboardingData())).isFalse()
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Concern, OnboardingData())).isFalse()
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Quiz, OnboardingData())).isFalse()
    }

    @Test
    fun `name step requires a non blank alphabetic name`() {
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Name, OnboardingData(name = ""))).isFalse()
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Name, OnboardingData(name = "  "))).isFalse()
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Name, OnboardingData(name = "Jes4s"))).isFalse()
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Name, OnboardingData(name = "Jesús"))).isTrue()
    }

    @Test
    fun `province step requires a province`() {
        val empty = OnboardingData(province = null)
        val picked = OnboardingData(province = "Almería")

        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Province, empty)).isFalse()
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.Province, picked)).isTrue()
    }

    @Test
    fun `every narrative step has a headline in the mascot bubble`() {
        narrativeSteps.forEach { step ->
            assertThat(reducer.updateMascotMessage(step, OnboardingData(name = "Jesús"))).isNotEmpty()
        }
    }

    @Test
    fun `the narrative headline is the one written for the answers given`() {
        val data = OnboardingData(motivation = OnboardingConfig.MOTIVATION_WORK)

        assertThat(reducer.updateMascotMessage(OnboardingStep.LossTime, data))
            .isEqualTo(OnboardingNarratives.forStep(OnboardingStep.LossTime, data)!!.headline)
    }

    @Test
    fun `the mini-test headline follows the question on screen`() {
        val first = reducer.updateMascotMessage(OnboardingStep.Quiz, OnboardingData(), quizIndex = 0)
        val last = reducer.updateMascotMessage(OnboardingStep.Quiz, OnboardingData(), quizIndex = 2)

        assertThat(first).contains("3 preguntas")
        assertThat(last).isNotEqualTo(first)
    }

    @Test
    fun `mascot messages interpolate the user name`() {
        val message = reducer.updateMascotMessage(OnboardingStep.Pact, OnboardingData(name = "Jesús"))

        assertThat(message).contains("Jesús")
    }

    @Test
    fun `the screens shown before the name never leave a gap where it would go`() {
        // The name is asked at the start of the plan block, so everything from the "why"
        // through the whole emotional arc has to read without it.
        val beforeTheName = listOf(
            OnboardingStep.Motivation, OnboardingStep.TheoryBlocker, OnboardingStep.Experience,
            OnboardingStep.Readiness, OnboardingStep.Concern, OnboardingStep.Quiz,
            OnboardingStep.QuizResult, OnboardingStep.Empathy, OnboardingStep.LossTime, OnboardingStep.LossOpportunity,
            OnboardingStep.LossAutonomy, OnboardingStep.GainFreedom,
            OnboardingStep.GainExperiences, OnboardingStep.GainLevelUp, OnboardingStep.Name
        )

        beforeTheName.forEach { step ->
            val message = reducer.updateMascotMessage(step, OnboardingData(name = null))
            assertThat(message).isNotEmpty()
            assertThat(message).doesNotContain("null")
            assertThat(message).doesNotContain("  ")
            assertThat(message).doesNotContain(" ,")
            assertThat(message).doesNotContain(" .")
        }
    }

    @Test
    fun `the exam date question leaves room for having no date`() {
        val message = reducer.updateMascotMessage(OnboardingStep.ExamDate, OnboardingData())

        assertThat(message).contains("Si no, sin problema")
    }

    @Test
    fun `progress runs from zero to one across the flow`() {
        assertThat(reducer.calculateProgress(index = 0, totalSteps = 25)).isEqualTo(0f)
        assertThat(reducer.calculateProgress(index = 24, totalSteps = 25)).isEqualTo(1f)
    }

    private val narrativeSteps = listOf(
        OnboardingStep.Empathy,
        OnboardingStep.LossTime,
        OnboardingStep.LossOpportunity,
        OnboardingStep.LossAutonomy,
        OnboardingStep.GainFreedom,
        OnboardingStep.GainExperiences,
        OnboardingStep.GainLevelUp
    )

    private val shortFlow = listOf(
        OnboardingStep.Motivation,
        OnboardingStep.SocialProof,
        OnboardingStep.Processing,
        OnboardingStep.PlanReveal,
        OnboardingStep.Pact
    )

    @Test
    fun `back leads to the screen shown just before`() {
        assertThat(reducer.previousStep(shortFlow, OnboardingStep.SocialProof))
            .isEqualTo(OnboardingStep.Motivation)
        assertThat(reducer.previousStep(shortFlow, OnboardingStep.Pact))
            .isEqualTo(OnboardingStep.PlanReveal)
    }

    @Test
    fun `there is nothing to go back to from the first screen`() {
        assertThat(reducer.previousStep(shortFlow, OnboardingStep.Motivation)).isNull()
    }

    @Test
    fun `back from the plan reveal skips the plan being built`() {
        // The building screen advances on its own and has no controls: landing on it again
        // would leave the user stuck there.
        assertThat(reducer.previousStep(shortFlow, OnboardingStep.PlanReveal))
            .isEqualTo(OnboardingStep.SocialProof)
    }

    @Test
    fun `back jumps over a step left out of this run`() {
        val flow = listOf(OnboardingStep.Quiz, OnboardingStep.QuizResult, OnboardingStep.Empathy)

        assertThat(reducer.previousStep(flow, OnboardingStep.Empathy) { it == OnboardingStep.QuizResult })
            .isEqualTo(OnboardingStep.Quiz)
        assertThat(reducer.previousStep(flow, OnboardingStep.Empathy))
            .isEqualTo(OnboardingStep.QuizResult)
    }

    @Test
    fun `back is refused while the plan is being built`() {
        assertThat(reducer.previousStep(shortFlow, OnboardingStep.Processing)).isNull()
    }

    @Test
    fun `back is refused outside the flow`() {
        // The paywall and the finished state are not part of the order.
        assertThat(reducer.previousStep(shortFlow, OnboardingStep.PaywallPending)).isNull()
        assertThat(reducer.previousStep(shortFlow, OnboardingStep.Completed)).isNull()
    }
}
