package com.jesuskrastev.bali.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
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
 * Backs the Settings tab's account section: the signed-in profile and sign-out action that
 * used to live in Home's side drawer, now surfaced from the persistent bottom navigation.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    /** Combines the local user profile with the live auth session into [SettingsUiState]. */
    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.get(),
        authRepository.isLoggedIn,
        authRepository.currentUserEmailFlow,
        authRepository.currentUserPhotoUrlFlow
    ) { user, isLoggedIn, userEmail, profilePictureUrl ->
        SettingsUiState(
            userName = user?.name ?: "Futuro Conductor",
            userEmail = userEmail,
            profilePictureUrl = profilePictureUrl,
            isLoggedIn = isLoggedIn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

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
