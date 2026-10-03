package com.jesuskrastev.bali.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.audio.SoundEffects
import com.jesuskrastev.bali.domain.model.NotificationCategory
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.NotificationsRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Settings tab: the signed-in profile and sign-out action that used to live in Home's
 * side drawer, now surfaced from the persistent bottom navigation, the sound effects switch and
 * what the user allowed in Android for each kind of notification.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val soundEffects: SoundEffects,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    /** What Android currently lets the app show: the permission and which category channels are off. */
    private data class NotificationAccess(
        val blocked: Boolean,
        val disabledCategories: Set<NotificationCategory>
    )

    private val notificationAccess = MutableStateFlow(readNotificationAccess())

    /** What the preference rows show, gathered so the profile flows keep fitting one `combine`. */
    private data class Preferences(val soundsEnabled: Boolean, val notifications: NotificationAccess)

    private val preferences = combine(soundEffects.isEnabled, notificationAccess, ::Preferences)

    /** Combines the local user profile, the live auth session and the preference rows into [SettingsUiState]. */
    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.get(),
        authRepository.isLoggedIn,
        authRepository.currentUserEmailFlow,
        authRepository.currentUserPhotoUrlFlow,
        preferences
    ) { user, isLoggedIn, userEmail, profilePictureUrl, preferences ->
        SettingsUiState(
            userName = user?.name ?: "Futuro Conductor",
            userEmail = userEmail,
            profilePictureUrl = profilePictureUrl,
            isLoggedIn = isLoggedIn,
            soundsEnabled = preferences.soundsEnabled,
            disabledNotificationCategories = preferences.notifications.disabledCategories,
            notificationsBlocked = preferences.notifications.blocked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    /**
     * Turns the answer sound effects on or off. Switching them on plays the "correct" chime once,
     * so the user hears what they just enabled.
     *
     * @param enabled true to play sound effects, false to keep the app silent
     */
    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            soundEffects.setEnabled(enabled)
            if (enabled) soundEffects.playCorrect()
        }
    }

    /**
     * Re-reads the notification permission and every category's channel. Called whenever the
     * screen comes back to the front, because the user changes both in Android's settings.
     */
    fun refreshNotificationAccess() {
        notificationAccess.value = readNotificationAccess()
    }

    /**
     * Asks Android to let notifications through, taking the user to the system settings when
     * the dialog can no longer be shown.
     */
    fun requestNotificationPermission() {
        viewModelScope.launch {
            notificationsRepository.requestPermission(openSettingsIfBlocked = true)
            refreshNotificationAccess()
        }
    }

    /**
     * Signs the current user out and clears their identity from analytics.
     *
     * @param context used by [AuthRepository] to revoke the active Google sign-in session
     */
    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
            analyticsTracker.logout()
            analyticsTracker.resetUser()
        }
    }

    /**
     * Reads the permission and the channels from Android.
     *
     * @return what the app may show right now
     */
    private fun readNotificationAccess() = NotificationAccess(
        blocked = !notificationsRepository.isPermissionGranted(),
        disabledCategories = NotificationCategory.entries
            .filterNot(notificationsRepository::isCategoryEnabled)
            .toSet()
    )
}
