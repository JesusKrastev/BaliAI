package com.jesuskrastev.bali.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnboardingNarrativesTest {

    private val arc = listOf(OnboardingStep.Pain, OnboardingStep.Gain)

    /** Every combination of the answers the arc reads, plus all of them unanswered. */
    private val profiles: List<OnboardingData> = buildList {
        add(OnboardingData())
        OnboardingConfig.motivations.forEach { motivation ->
            OnboardingConfig.theoryBlockers.forEach { blocker ->
                OnboardingConfig.experiences.forEach { experience ->
                    OnboardingConfig.concerns.forEach { concern ->
                        add(
                            OnboardingData(
                                motivation = motivation,
                                theoryBlocker = blocker,
                                experience = experience,
                                concern = concern
                            )
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `every screen of the arc has copy for every combination of answers`() {
        profiles.forEach { data ->
            arc.forEach { step ->
                val narrative = OnboardingNarratives.forStep(step, data)!!
                assertThat(narrative.headline).isNotEmpty()
                assertThat(narrative.content.body).isNotEmpty()
                assertThat(narrative.content.body).doesNotContain("null")
            }
        }
    }

    @Test
    fun `the arc changes with the answers`() {
        arc.forEach { step ->
            val bodies = profiles.map { OnboardingNarratives.forStep(step, it)!!.content.body }.toSet()
            assertThat(bodies.size).isGreaterThan(1)
        }
    }

    @Test
    fun `the pain names the blocker and the cost of the user's own reason`() {
        val pain = OnboardingNarratives.forStep(
            OnboardingStep.Pain,
            OnboardingData(
                motivation = OnboardingConfig.MOTIVATION_WORK,
                theoryBlocker = OnboardingConfig.BLOCKER_NO_START
            )
        )!!

        assertThat(pain.headline).contains("por dónde empezar")
        assertThat(pain.content.body).contains("trabajo")
    }

    @Test
    fun `the gain shows what the user's reason looks like with the licence`() {
        val gain = OnboardingNarratives.forStep(
            OnboardingStep.Gain,
            OnboardingData(motivation = OnboardingConfig.MOTIVATION_FREEDOM)
        )!!

        assertThat(gain.content.body).contains("escapada")
    }

    @Test
    fun `the copy reads the same whoever answers it`() {
        // Spanish words that change with the reader's gender, which this copy must avoid.
        val gendered = listOf("el único", "la única", "listo", "lista para", "preparado", "preparada", "eres el siguiente")

        profiles.forEach { data ->
            arc.forEach { step ->
                val narrative = OnboardingNarratives.forStep(step, data)!!
                val text = (narrative.headline + " " + narrative.content.body).lowercase()
                gendered.forEach { word -> assertThat(text).doesNotContain(word) }
            }
        }
    }

    @Test
    fun `the pain and the gain never show the same picture`() {
        profiles.forEach { data ->
            val pain = OnboardingNarratives.forStep(OnboardingStep.Pain, data)!!.content.animation
            val gain = OnboardingNarratives.forStep(OnboardingStep.Gain, data)!!.content.animation
            assertThat(pain).isNotEqualTo(gain)
        }
    }

    @Test
    fun `steps outside the arc have no narrative`() {
        assertThat(OnboardingNarratives.forStep(OnboardingStep.Name, OnboardingData())).isNull()
        assertThat(OnboardingNarratives.forStep(OnboardingStep.Quiz, OnboardingData())).isNull()
    }
}
