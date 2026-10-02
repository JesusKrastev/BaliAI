package com.jesuskrastev.bali.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.audio.SoundEffects
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Settings tab: the signed-in profile and sign-out action that used to live in Home's
 * side drawer, now surfaced from the persistent bottom navigation, and the sound effects switch.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val soundEffects: SoundEffects
) : ViewModel() {

    /** Combines the local user profile, the live auth session and the sound switch into [SettingsUiState]. */
    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.get(),
        authRepository.isLoggedIn,
        authRepository.currentUserEmailFlow,
        authRepository.currentUserPhotoUrlFlow,
        soundEffects.isEnabled
    ) { user, isLoggedIn, userEmail, profilePictureUrl, soundsEnabled ->
        SettingsUiState(
            userName = user?.name ?: "Futuro Conductor",
            userEmail = userEmail,
            profilePictureUrl = profilePictureUrl,
            isLoggedIn = isLoggedIn,
            soundsEnabled = soundsEnabled
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
