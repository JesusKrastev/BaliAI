package com.jesuskrastev.bali.ui.screens.main

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.data.update.InAppUpdateManager
import com.jesuskrastev.bali.domain.model.UpdateState
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val migrationManager: FirestoreMigrationManager,
    private val inAppUpdateManager: InAppUpdateManager
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean?> =
        combine(userRepository.hasCompletedOnboarding(), authRepository.isLoggedIn) { hasCompletedOnboarding, loggedIn ->
            hasCompletedOnboarding || loggedIn
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val updateState: StateFlow<UpdateState> = inAppUpdateManager.updateState

    private val _isMigrating = MutableStateFlow(true) // Start assuming migration might be needed if user is null initially until checked
    val isMigrating: StateFlow<Boolean> = _isMigrating.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private val _migrationError = MutableStateFlow<String?>(null)
    val migrationError: StateFlow<String?> = _migrationError.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            val currentUserUid = authRepository.currentUser()
            if (currentUserUid != null) {
                OneSignal.login(currentUserUid)
                runCatching {
                    val token = FirebaseMessaging.getInstance().token.await()
                    userRepository.updateFcmToken(token)
                }
                _isMigrating.value = true
                try {
                    migrationManager.executePendingMigrations(currentUserUid)
                } catch (e: Exception) {
                    _migrationError.value = e.message ?: "Error desconocido durante la inicialización de la base de datos."
                    e.printStackTrace()
                } finally {
                    _isMigrating.value = false
                }
            } else {
                _isMigrating.value = false
            }
        }
        inAppUpdateManager.checkForUpdate()
    }

    fun startFlexibleUpdate(activity: Activity) {
        inAppUpdateManager.startFlexibleUpdate(activity)
    }

    fun startImmediateUpdate(activity: Activity) {
        inAppUpdateManager.startImmediateUpdate(activity)
    }

    fun completeUpdate() {
        inAppUpdateManager.completeUpdate()
    }

    fun checkForDownloadedUpdate() {
        inAppUpdateManager.checkForDownloadedUpdate()
    }

    override fun onCleared() {
        super.onCleared()
        inAppUpdateManager.unregisterListener()
    }
}
