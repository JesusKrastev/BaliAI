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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Where the app must open on this launch.
 *
 * The hard paywall comes before everything else: only a user who has paid gets into the app,
 * and only with a session, since the account is what carries the subscription.
 */
enum class AppEntryPoint {
    /** Nothing collected yet: show the welcome screen and the onboarding. */
    GREETINGS,

    /**
     * Premium is not active: show the hard paywall. That covers a user who finished the
     * onboarding without paying and a signed-in one whose subscription lapsed or never existed.
     */
    PAYWALL,

    /** Paid but no session: the sign-in gate is the only way forward. */
    LOGIN,

    /** Signed in and premium is active: the app is fully usable. */
    HOME
}

/**
 * Decides which screen the app starts on.
 *
 * Whoever has not paid never gets past the paywall, signed in or not. A signed-in user is
 * checked before the onboarding flag because signing in clears the local Room profile, so an
 * account has no local onboarding flag and must still land on [AppEntryPoint.HOME] once it has
 * paid.
 *
 * @param hasCompletedOnboarding whether a local profile already exists
 * @param loggedIn whether a Firebase session is active
 * @param unlocked whether premium is active (debug builds count as always active, so the paywall
 *   never blocks manual testing)
 * @return the entry point to open the app on
 */
internal fun resolveEntryPoint(
    hasCompletedOnboarding: Boolean,
    loggedIn: Boolean,
    unlocked: Boolean
): AppEntryPoint = when {
    loggedIn -> if (unlocked) AppEntryPoint.HOME else AppEntryPoint.PAYWALL
    !hasCompletedOnboarding -> AppEntryPoint.GREETINGS
    unlocked -> AppEntryPoint.LOGIN
    else -> AppEntryPoint.PAYWALL
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
     * Whether a session is active and whether premium is active for whoever is using the app.
     *
     * @property loggedIn true when a Firebase session is active
     * @property premium true when the premium entitlement is active for that user
     */
    private data class Session(val loggedIn: Boolean, val premium: Boolean)

    /**
     * Emits whether premium is active for whoever is using the app, as soon as that is known,
     * and again on every change.
     *
     * For a signed-in user, RevenueCat is first switched to their account with
     * [SubscriptionRepository.identify], and nothing is emitted until that has answered (or
     * [IDENTIFY_TIMEOUT_MILLIS] has passed). Someone who subscribed on another device has no
     * entitlement on this device's anonymous customer, so trusting it would show them the
     * paywall for as long as the switch takes. If the switch fails, the entitlement already on
     * the device is used instead.
     *
     * @param loggedIn whether a Firebase session is active
     * @return a flow of the premium state, starting with its current value
     */
    private fun premiumStatus(loggedIn: Boolean): Flow<Boolean> = flow {
        val userId = if (loggedIn) authRepository.currentUser() else null
        val linkedCustomer = userId?.let {
            withTimeoutOrNull(IDENTIFY_TIMEOUT_MILLIS) { subscriptionRepository.identify(it).getOrNull() }
        }
        val customer = linkedCustomer ?: subscriptionRepository.getCustomerInfo().getOrNull()
        emit(customer?.let(subscriptionRepository::hasPremiumEntitlement) ?: false)
        emitAll(
            subscriptionRepository.customerInfoStream().map(
                subscriptionRepository::hasPremiumEntitlement
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * The screen the app has to start on, or null while it is still being resolved (the
     * splash screen stays up until then). See [resolveEntryPoint] for the rules.
     *
     * The session and its premium state are decided together, so when someone signs in the
     * previous screen stays up until their subscription is known, instead of flashing the
     * paywall at a subscriber whose account RevenueCat has not switched to yet.
     */
    val entryPoint: StateFlow<AppEntryPoint?> =
        combine(
            userRepository.hasCompletedOnboarding(),
            authRepository.isLoggedIn.distinctUntilChanged().flatMapLatest { loggedIn ->
                premiumStatus(loggedIn).map { premium -> Session(loggedIn, premium) }
            }
        ) { hasCompletedOnboarding, session ->
            // Debug builds treat the app as always entitled so the RevenueCat paywall never
            // blocks manual testing; release keeps the real hard-paywall check.
            resolveEntryPoint(
                hasCompletedOnboarding = hasCompletedOnboarding,
                loggedIn = session.loggedIn,
                unlocked = session.premium || BuildConfig.DEBUG
            )
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

    private companion object {
        /**
         * Longest the entry point waits for RevenueCat to switch to the signed-in account before
         * using the entitlement already on the device. The switch is one request, so this only
         * matters on a very slow connection.
         */
        const val IDENTIFY_TIMEOUT_MILLIS = 5_000L
    }
}
