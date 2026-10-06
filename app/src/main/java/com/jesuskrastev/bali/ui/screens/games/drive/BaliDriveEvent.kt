package com.jesuskrastev.bali.ui.screens.games.drive

/** Tutorial actions; engine events remain owned by DriveEngine. */
sealed interface BaliDriveEvent {
    data object NextTutorialStep : BaliDriveEvent
    data object CompleteTutorial : BaliDriveEvent
}
