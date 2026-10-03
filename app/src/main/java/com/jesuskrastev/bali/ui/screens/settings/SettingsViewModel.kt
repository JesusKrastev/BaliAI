package com.jesuskrastev.bali.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.audio.SoundEffects
import com.jesuskrastev.bali.domain.model.EnablePushesResult
import com.jesuskrastev.bali.domain.model.NotificationCategory
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.NotificationsRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    /**
     * The categories whose channel is off. Android has no callback for channel changes, so this
     * is re-read by [refreshNotificationChannels] each time the screen comes back to the front.
     */
    private val disabledCategories = MutableStateFlow(readDisabledCategories())

    /** What the preference rows show, gathered so the profile flows keep fitting one `combine`. */
    private data class Preferences(
        val soundsEnabled: Boolean,
        val pushesAllowed: Boolean,
        val disabledCategories: Set<NotificationCategory>
    )

    private val preferences = combine(
        soundEffects.isEnabled,
        notificationsRepository.pushesAllowed,
        disabledCategories,
        ::Preferences
    )

    private val _openSystemNotificationSettings = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Fires when only Android's settings can turn notifications back on, so the screen opens them:
     * the view model holds no `Context` to start that activity itself.
     */
    val openSystemNotificationSettings: SharedFlow<Unit> = _openSystemNotificationSettings.asSharedFlow()

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
            disabledNotificationCategories = preferences.disabledCategories,
            notificationsBlocked = !preferences.pushesAllowed
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
     * Re-reads every category's channel. Called whenever the screen comes back to the front,
     * because the user switches channels in Android's settings. The permission needs no
     * refresh: [NotificationsRepository.pushesAllowed] follows it by itself.
     */
    fun refreshNotificationChannels() {
        disabledCategories.value = readDisabledCategories()
    }

    /**
     * Lets notifications through again: opts back in, shows the system dialog, or, when Android
     * will not show it any more, asks the screen to open the system settings. The outcome is
     * logged with `source = settings`, apart from the onboarding's answers.
     */
    fun enableNotifications() {
        viewModelScope.launch {
            val result = notificationsRepository.enablePushes()
            analyticsTracker.notificationsPermissionAnswered(
                result = result.analyticsValue,
                studySlot = null,
                source = NOTIFICATIONS_SOURCE_SETTINGS
            )
            if (result == EnablePushesResult.NEEDS_SYSTEM_SETTINGS) {
                _openSystemNotificationSettings.tryEmit(Unit)
            }
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
     * Reads the channels from Android.
     *
     * @return the categories whose channel the user switched off
     */
    private fun readDisabledCategories(): Set<NotificationCategory> = NotificationCategory.entries
        .filterNot(notificationsRepository::isCategoryEnabled)
        .toSet()

    private companion object {
        /** `source` of `notifications_permission_result` when the answer comes from Settings. */
        const val NOTIFICATIONS_SOURCE_SETTINGS = "settings"
    }
}
