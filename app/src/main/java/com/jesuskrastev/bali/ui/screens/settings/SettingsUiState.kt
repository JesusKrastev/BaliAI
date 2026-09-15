package com.jesuskrastev.bali.ui.screens.settings

/**
 * UI state for the Settings screen: the signed-in profile shown in its account section.
 *
 * @property userName display name shown next to the avatar
 * @property userEmail authenticated user's email, null when signed out or unavailable
 * @property profilePictureUrl authenticated user's avatar URL, null when signed out or unavailable
 * @property isLoggedIn whether a user session is currently active
 */
data class SettingsUiState(
    val userName: String = "Futuro Conductor",
    val userEmail: String? = null,
    val profilePictureUrl: String? = null,
    val isLoggedIn: Boolean = false
)
