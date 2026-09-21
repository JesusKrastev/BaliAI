package com.jesuskrastev.bali.ui.screens.main

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.data.update.InAppUpdateManager
import com.jesuskrastev.bali.domain.model.UpdateState
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Where the app must open on this launch.
 *
 * The session is not optional: paying unlocks the content, but the account is what carries
 * it, so a user who finished the onboarding without signing in is sent to the gate instead
 * of into the app.
 */
enum class AppEntryPoint {
    /** Nothing collected yet: show the welcome screen and the onboarding. */
    GREETINGS,

    /** Onboarding saved but premium is not active: show the hard paywall again. */
    PAYWALL,

    /** Onboarding done but no session: the sign-in gate is the only way forward. */
    LOGIN,

    /** Signed in: the app is fully usable. */
    HOME
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val migrationManager: FirestoreMigrationManager,
    private val inAppUpdateManager: InAppUpdateManager
) : ViewModel() {

    /**
     * The screen the app has to start on, or null while it is still being resolved (the
     * splash screen stays up until then).
     *
     * Signing in clears the local Room profile, so the session is checked first: a logged-in
     * user has no local onboarding flag and must still land on [AppEntryPoint.HOME].
     */
    private val hasPremium = flow {
        val initialStatus = subscriptionRepository.getCustomerInfo().fold(
            onSuccess = subscriptionRepository::hasPremiumEntitlement,
            onFailure = { false }
        )
        emit(initialStatus)
        emitAll(
            subscriptionRepository.customerInfoStream().map(
                subscriptionRepository::hasPremiumEntitlement
            )
        )
    }.flowOn(Dispatchers.IO)

    val entryPoint: StateFlow<AppEntryPoint?> =
        combine(
            userRepository.hasCompletedOnboarding(),
            authRepository.isLoggedIn,
            hasPremium
        ) { hasCompletedOnboarding, loggedIn, premium ->
            // Debug builds treat the app as always entitled so the RevenueCat paywall never
            // blocks manual testing; release keeps the real hard-paywall check.
            val unlocked = premium || BuildConfig.DEBUG
            when {
                loggedIn -> AppEntryPoint.HOME
                hasCompletedOnboarding && !unlocked -> AppEntryPoint.PAYWALL
                hasCompletedOnboarding -> AppEntryPoint.LOGIN
                else -> AppEntryPoint.GREETINGS
            }
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
                    FirebaseCrashlytics.getInstance().recordException(e)
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
