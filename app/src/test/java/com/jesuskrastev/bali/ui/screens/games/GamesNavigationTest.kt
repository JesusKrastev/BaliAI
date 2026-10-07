package com.jesuskrastev.bali.ui.screens.games

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GamesNavigationTest {
    /** All retained identifiers and invalid old links normalize to the only offered game. */
    @Test fun `retired and unknown routes open Bali Drive`() {
        (GameType.entries.map { it.id } + listOf("", "unknown")).forEach {
            assertThat(GameType.fromId(it)).isEqualTo(GameType.DRIVE)
        }
    }
}
