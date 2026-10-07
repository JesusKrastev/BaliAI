package com.jesuskrastev.bali.ui.screens.settings

import com.jesuskrastev.bali.domain.model.NotificationCategory

/**
 * UI state for the Settings screen: the signed-in profile shown in its account section.
 *
 * @property userName display name shown next to the avatar
 * @property userEmail authenticated user's email, null when signed out or unavailable
 * @property profilePictureUrl authenticated user's avatar URL, null when signed out or unavailable
 * @property isLoggedIn whether a user session is currently active
 * @property soundsEnabled whether the answer sound effects are on (they are until the user turns them off)
 * @property disabledNotificationCategories the notification kinds the user switched off (all are on until they do)
 * @property notificationsEnabled whether pushes reach this install: Android allows the app's notifications
 *   and the user has not turned them off in the app (with the Settings switch or the onboarding's
 *   "Ahora no"). While false no category delivers anything, whatever its channel says
 */
data class SettingsUiState(
    val userName: String = "Futuro Conductor",
    val userEmail: String? = null,
    val profilePictureUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val soundsEnabled: Boolean = true,
    val disabledNotificationCategories: Set<NotificationCategory> = emptySet(),
    val notificationsEnabled: Boolean = true
)
