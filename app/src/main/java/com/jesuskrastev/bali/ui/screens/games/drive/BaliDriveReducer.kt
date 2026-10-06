package com.jesuskrastev.bali.ui.screens.games.drive

/** Pure tutorial transitions prevent duplicate completion while persistence is pending. */
object BaliDriveReducer {
    /** Returns the next screen state for [event], ignoring actions outside the tutorial. */
    fun reduce(state: DriveUiState, event: BaliDriveEvent): DriveUiState {
        if (state.phase != DriveScreenPhase.TUTORIAL) return state
        return when (event) {
            BaliDriveEvent.NextTutorialStep -> state.copy(tutorialStep = 1, error = false)
            BaliDriveEvent.CompleteTutorial -> if (state.tutorialStep == 1)
                state.copy(phase = DriveScreenPhase.SAVING_TUTORIAL, error = false) else state
        }
    }
}
