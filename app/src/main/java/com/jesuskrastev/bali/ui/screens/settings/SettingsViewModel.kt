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
 * the switches for each kind of notification.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val soundEffects: SoundEffects,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    /** What the preference switches show, gathered so the profile flows keep fitting one `combine`. */
    private data class Preferences(
        val soundsEnabled: Boolean,
        val disabledCategories: Set<NotificationCategory>,
        val notificationsBlocked: Boolean
    )

    private val notificationsAllowed = MutableStateFlow(notificationsRepository.isPermissionGranted())

    private val preferences = combine(
        soundEffects.isEnabled,
        notificationsRepository.disabledCategories,
        notificationsAllowed
    ) { soundsEnabled, disabled, allowed ->
        Preferences(soundsEnabled, disabled.orEmpty(), notificationsBlocked = !allowed)
    }

    /** Combines the local user profile, the live auth session and the preference switches into [SettingsUiState]. */
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
            disabledNotificationCategories = preferences.disabledCategories,
            notificationsBlocked = preferences.notificationsBlocked
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
     * Switches one kind of notification on or off. The choice reaches OneSignal through the tag
     * sync, which skips this user in the journeys of a category they turned off.
     *
     * @param category the kind of notification to change
     * @param enabled false to stop receiving it
     */
    fun setNotificationCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
        viewModelScope.launch {
            notificationsRepository.setCategoryEnabled(category, enabled)
            analyticsTracker.notificationCategoryChanged(category.key, enabled)
        }
    }

    /**
     * Re-reads whether Android lets the app show notifications. Called whenever the screen comes
     * back to the front, because the user may have changed it in the system settings.
     */
    fun refreshNotificationPermission() {
        notificationsAllowed.value = notificationsRepository.isPermissionGranted()
    }

    /**
     * Asks Android to let notifications through, taking the user to the system settings when
     * the dialog can no longer be shown.
     */
    fun requestNotificationPermission() {
        viewModelScope.launch {
            notificationsRepository.requestPermission(openSettingsIfBlocked = true)
            refreshNotificationPermission()
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
}
