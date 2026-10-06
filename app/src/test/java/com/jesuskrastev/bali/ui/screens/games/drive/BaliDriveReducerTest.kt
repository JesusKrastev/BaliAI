package com.jesuskrastev.bali.ui.screens.games.drive

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BaliDriveReducerTest {
    /** Completion requires the second step, and subsequent actions cannot duplicate persistence. */
    @Test fun `completion requires both steps and ignores duplicates`() {
        val first = DriveUiState(phase = DriveScreenPhase.TUTORIAL)
        assertThat(BaliDriveReducer.reduce(first, BaliDriveEvent.CompleteTutorial)).isEqualTo(first)
        val second = BaliDriveReducer.reduce(first, BaliDriveEvent.NextTutorialStep)
        val saving = BaliDriveReducer.reduce(second, BaliDriveEvent.CompleteTutorial)
        assertThat(saving.phase).isEqualTo(DriveScreenPhase.SAVING_TUTORIAL)
        assertThat(BaliDriveReducer.reduce(saving, BaliDriveEvent.CompleteTutorial)).isEqualTo(saving)
    }

    /** Tutorial actions never affect an already running game. */
    @Test fun `tutorial events outside tutorial are ignored`() {
        val playing = DriveUiState(phase = DriveScreenPhase.PLAYING, runId = 3)
        assertThat(BaliDriveReducer.reduce(playing, BaliDriveEvent.NextTutorialStep)).isEqualTo(playing)
    }
}
