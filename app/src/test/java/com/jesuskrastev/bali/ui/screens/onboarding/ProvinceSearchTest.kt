package com.jesuskrastev.bali.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProvinceSearchTest {

    @Test
    fun `an empty query offers every province`() {
        assertThat(OnboardingConfig.provincesMatching("")).isEqualTo(OnboardingConfig.provinces)
        assertThat(OnboardingConfig.provincesMatching("   ")).isEqualTo(OnboardingConfig.provinces)
    }

    @Test
    fun `typing without accents still finds the province`() {
        // Nobody reaches for the long-press keyboard to type "Almería" into a search box.
        assertThat(OnboardingConfig.provincesMatching("almeria")).containsExactly("Almería")
        assertThat(OnboardingConfig.provincesMatching("avila")).containsExactly("Ávila")
        assertThat(OnboardingConfig.provincesMatching("caceres")).containsExactly("Cáceres")
        assertThat(OnboardingConfig.provincesMatching("jaen")).containsExactly("Jaén")
    }

    @Test
    fun `typing with accents also works`() {
        assertThat(OnboardingConfig.provincesMatching("Almería")).containsExactly("Almería")
    }

    @Test
    fun `the match looks anywhere in the name, not only at the start`() {
        assertThat(OnboardingConfig.provincesMatching("coruña")).containsExactly("La Coruña")
        assertThat(OnboardingConfig.provincesMatching("coruna")).containsExactly("La Coruña")
        assertThat(OnboardingConfig.provincesMatching("palmas")).containsExactly("Las Palmas")
    }

    @Test
    fun `an unknown query matches nothing instead of falling back to everything`() {
        assertThat(OnboardingConfig.provincesMatching("zzzz")).isEmpty()
    }

    @Test
    fun `accented initials are grouped under their plain letter`() {
        val sections = OnboardingConfig.provincesByInitial("")

        // Á and A are the same section for a reader scanning the list.
        assertThat(sections['A']).contains("Álava")
        assertThat(sections['A']).contains("Ávila")
        assertThat(sections['A']).contains("Albacete")
        assertThat(sections.keys).doesNotContain('Á')
    }

    @Test
    fun `every province lands in exactly one section`() {
        val sections = OnboardingConfig.provincesByInitial("")

        assertThat(sections.values.flatten()).containsExactlyElementsIn(OnboardingConfig.provinces)
    }
}
