package com.jesuskrastev.bali.ui.screens.main

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.data.update.InAppUpdateManager
import com.jesuskrastev.bali.domain.model.UpdateState
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.usecase.ExecuteFirestoreMigrationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val executeFirestoreMigrationsUseCase: ExecuteFirestoreMigrationsUseCase,
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
                _migrationError.value = null
                _isMigrating.value = true
                try {
                    executeFirestoreMigrationsUseCase(currentUserUid)
                } catch (e: Exception) {
                    _migrationError.value = e.message ?: "Error desconocido durante la inicializaciÃ³n de la base de datos."
                    e.printStackTrace()
                } finally {
                    _isMigrating.value = false
                }
            } else {
                // Not logged in: nothing to migrate or initialize, allow visual entry.
                _isMigrating.value = false
                _migrationError.value = null
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
