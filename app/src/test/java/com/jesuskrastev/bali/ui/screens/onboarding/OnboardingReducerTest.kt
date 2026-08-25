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
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.FutureImpact, OnboardingData())).isFalse()
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
        OnboardingConfig.narratives.keys.forEach { step ->
            assertThat(reducer.updateMascotMessage(step, OnboardingData(name = "Jesús"))).isNotEmpty()
        }
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
            OnboardingStep.Motivation, OnboardingStep.TheoryBlocker, OnboardingStep.Concern,
            OnboardingStep.Experience, OnboardingStep.Readiness, OnboardingStep.FutureImpact,
            OnboardingStep.Empathy, OnboardingStep.LossTime, OnboardingStep.LossOpportunity,
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
    fun `the province confirmation names the province the user picked`() {
        val message = reducer.updateMascotMessage(
            OnboardingStep.ProvinceConfirmed,
            OnboardingData(province = "Almería")
        )

        assertThat(message).contains("Almería")
    }

    @Test
    fun `the province confirmation still reads without a province`() {
        val message = reducer.updateMascotMessage(
            OnboardingStep.ProvinceConfirmed,
            OnboardingData(province = null)
        )

        assertThat(message).isNotEmpty()
        assertThat(message).doesNotContain("null")
    }

    @Test
    fun `progress runs from zero to one across the flow`() {
        assertThat(reducer.calculateProgress(index = 0, totalSteps = 25)).isEqualTo(0f)
        assertThat(reducer.calculateProgress(index = 24, totalSteps = 25)).isEqualTo(1f)
    }
}
