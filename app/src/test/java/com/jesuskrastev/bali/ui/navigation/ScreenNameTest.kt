package com.jesuskrastev.bali.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScreenNameTest {

    @Test
    fun `an object route becomes its bare name`() {
        assertThat(screenNameOf("com.jesuskrastev.bali.ui.navigation.HomeRoute")).isEqualTo("Home")
    }

    @Test
    fun `the statistics route is reported as Stats`() {
        assertThat(screenNameOf("com.jesuskrastev.bali.ui.navigation.StatsRoute")).isEqualTo("Stats")
    }

    @Test
    fun `query arguments are dropped`() {
        val route = "com.jesuskrastev.bali.ui.navigation.TestRoute?topic={topic}&nodeId={nodeId}"

        assertThat(screenNameOf(route)).isEqualTo("Test")
    }

    @Test
    fun `path arguments are dropped`() {
        val route = "com.jesuskrastev.bali.ui.navigation.GamePlayRoute/{gameId}"

        assertThat(screenNameOf(route)).isEqualTo("GamePlay")
    }

    @Test
    fun `a destination without a usable route reports nothing`() {
        assertThat(screenNameOf(null)).isNull()
        assertThat(screenNameOf("")).isNull()
        assertThat(screenNameOf("com.jesuskrastev.bali.ui.navigation.Route")).isNull()
    }
}
