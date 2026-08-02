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
    fun `difficult topics step requires at least one topic`() {
        val empty = OnboardingData(difficultTopics = emptySet())
        val picked = OnboardingData(difficultTopics = setOf("🛑 Señales y marcas"))

        assertThat(reducer.shouldEnableNextButton(OnboardingStep.DifficultTopics, empty)).isFalse()
        assertThat(reducer.shouldEnableNextButton(OnboardingStep.DifficultTopics, picked)).isTrue()
    }

    @Test
    fun `every narrative step has a headline in the mascot bubble`() {
        OnboardingConfig.narratives.keys.forEach { step ->
            assertThat(reducer.updateMascotMessage(step, OnboardingData(name = "Jesús"))).isNotEmpty()
        }
    }

    @Test
    fun `mascot messages interpolate the user name`() {
        val message = reducer.updateMascotMessage(OnboardingStep.Motivation, OnboardingData(name = "Jesús"))

        assertThat(message).contains("Jesús")
    }

    @Test
    fun `repeat candidates get the failure statistic instead of a generic reaction`() {
        val data = OnboardingData(experience = OnboardingConfig.EXPERIENCE_RETRY)

        val reaction = reducer.updateMascotMessage(OnboardingStep.DialogueExperience, data)

        assertThat(reaction).contains("58%")
    }

    @Test
    fun `every experience option produces a reaction`() {
        OnboardingConfig.experiences.forEach { experience ->
            val reaction = reducer.updateMascotMessage(
                OnboardingStep.DialogueExperience,
                OnboardingData(experience = experience)
            )
            assertThat(reaction).isNotEmpty()
        }
    }

    @Test
    fun `progress runs from zero to one across the flow`() {
        assertThat(reducer.calculateProgress(index = 0, totalSteps = 25)).isEqualTo(0f)
        assertThat(reducer.calculateProgress(index = 24, totalSteps = 25)).isEqualTo(1f)
    }
}
