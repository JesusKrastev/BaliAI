package com.jesuskrastev.bali.ui.screens.main

import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.data.update.InAppUpdateManager
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import com.jesuskrastev.bali.domain.model.UpdateState
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Minimal [SubscriptionRepository] fake whose entitlement is fixed for the test. */
private class FakeSubscriptionRepository(private val hasPremium: Boolean) : SubscriptionRepository {
    private val customerInfo = mock<CustomerInfo>()
    override fun customerInfoStream(): Flow<CustomerInfo> = flowOf(customerInfo)
    override suspend fun getCustomerInfo(): Result<CustomerInfo> = Result.success(customerInfo)
    override suspend fun restorePurchases(): Result<CustomerInfo> = Result.success(customerInfo)
    override fun hasPremiumEntitlement(customerInfo: CustomerInfo): Boolean = hasPremium
    override suspend fun getOffering(identifier: String): Result<Offering?> = Result.success(null)
    override suspend fun identify(userId: String): Result<CustomerInfo> = Result.success(customerInfo)
}

/** No-op [FirestoreMigrationManager]; entry-point resolution never reaches it when signed out. */
private class FakeFirestoreMigrationManager : FirestoreMigrationManager {
    override suspend fun executePendingMigrations(userId: String) {}
    override suspend fun getCurrentSchemaVersion(userId: String): Int = 1
    override fun getTargetSchemaVersion(): Int = 1
}

/**
 * Covers [MainViewModel.entryPoint]'s gating: the hard paywall comes first, so nobody gets past it
 * without paying, signed in or not; only a paying, signed-in user reaches home; and onboarding
 * is never repeated once it is done.
 */
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeMigrationManager = FakeFirestoreMigrationManager()
    private val fakeUpdateManager: InAppUpdateManager = mock()

    init {
        whenever(fakeUpdateManager.updateState).thenReturn(MutableStateFlow(UpdateState.Idle).asStateFlow())
    }

    /**
     * Builds a [MainViewModel] for the given onboarding/session/entitlement combination and
     * resolves [MainViewModel.entryPoint] to its first real value.
     *
     * `entryPoint`'s `hasPremium` source is `flowOn(Dispatchers.IO)` in production, a real
     * dispatcher outside `runTest`'s virtual time, so reading `.value` right after subscribing
     * would still see the flow's initial `null` — awaiting the first non-null emission instead
     * genuinely suspends until that IO hop delivers it. The ViewModel's own scope is cancelled
     * before returning: `entryPoint` is `WhileSubscribed(5000)`, and left running it would keep
     * a real 5-second timer alive past this test, on a `Dispatchers.Main` that no longer exists
     * once [MainDispatcherRule] resets it.
     *
     * @param hasCompletedOnboarding whether a local profile already exists
     * @param isLoggedIn whether a Firebase session is active
     * @param hasPremium whether RevenueCat reports the premium entitlement
     */
    private suspend fun resolvedEntryPoint(
        hasCompletedOnboarding: Boolean,
        isLoggedIn: Boolean,
        hasPremium: Boolean
    ): AppEntryPoint {
        val viewModel = MainViewModel(
            userRepository = FakeUserRepository(hasCompletedOnboarding = hasCompletedOnboarding),
            // Kept signed out at the currentUser() level regardless of isLoggedIn: a non-null id
            // would drive MainViewModel's init block into OneSignal/FirebaseMessaging calls that
            // don't work in a plain JVM unit test. It also means the premium state is read
            // straight from the fake, without the RevenueCat account switch a real user gets.
            authRepository = FakeAuthRepository(isLoggedIn = isLoggedIn, currentUserId = null),
            subscriptionRepository = FakeSubscriptionRepository(hasPremium = hasPremium),
            migrationManager = fakeMigrationManager,
            inAppUpdateManager = fakeUpdateManager
        )
        val result = viewModel.entryPoint.filterNotNull().first()
        viewModel.viewModelScope.cancel()
        return result
    }

    @Test
    fun `onboarding not done sends the user to greetings regardless of login or premium`() = runTest {
        val entryPoint = resolvedEntryPoint(hasCompletedOnboarding = false, isLoggedIn = false, hasPremium = true)
        assertThat(entryPoint).isEqualTo(AppEntryPoint.GREETINGS)
    }

    @Test
    fun `reopening after seeing the paywall without buying or logging in never restarts onboarding`() = runTest {
        // Regression: onboarding is already persisted locally by the time the paywall first
        // shows, so quitting there and relaunching must not send the user through the
        // onboarding questions a second time. Where exactly it lands is variant-dependent
        // (debug skips the paywall gate entirely, see entryPoint's BuildConfig.DEBUG bypass),
        // but neither variant may ever repeat onboarding once it is done.
        val entryPoint = resolvedEntryPoint(hasCompletedOnboarding = true, isLoggedIn = false, hasPremium = false)
        assertThat(entryPoint).isNotEqualTo(AppEntryPoint.GREETINGS)
        assertThat(entryPoint).isEqualTo(if (BuildConfig.DEBUG) AppEntryPoint.LOGIN else AppEntryPoint.PAYWALL)
    }

    @Test
    fun `premium but not logged in requires signing in`() = runTest {
        val entryPoint = resolvedEntryPoint(hasCompletedOnboarding = true, isLoggedIn = false, hasPremium = true)
        assertThat(entryPoint).isEqualTo(AppEntryPoint.LOGIN)
    }

    @Test
    fun `logged in without premium is sent to the paywall, not home`() = runTest {
        val entryPoint = resolvedEntryPoint(hasCompletedOnboarding = true, isLoggedIn = true, hasPremium = false)
        // Debug builds treat the app as always entitled, see entryPoint's BuildConfig.DEBUG bypass.
        assertThat(entryPoint).isEqualTo(if (BuildConfig.DEBUG) AppEntryPoint.HOME else AppEntryPoint.PAYWALL)
    }

    @Test
    fun `logged in with premium lands on home`() = runTest {
        val entryPoint = resolvedEntryPoint(hasCompletedOnboarding = true, isLoggedIn = true, hasPremium = true)
        assertThat(entryPoint).isEqualTo(AppEntryPoint.HOME)
    }

    @Test
    fun `a signed in account has no local onboarding flag and still lands on home once it has paid`() = runTest {
        // Signing in clears the local profile, so hasCompletedOnboarding is false for an account.
        val entryPoint = resolvedEntryPoint(hasCompletedOnboarding = false, isLoggedIn = true, hasPremium = true)
        assertThat(entryPoint).isEqualTo(AppEntryPoint.HOME)
    }

    @Test
    fun `a signed in account without a local profile and without premium is sent to the paywall`() = runTest {
        val entryPoint = resolvedEntryPoint(hasCompletedOnboarding = false, isLoggedIn = true, hasPremium = false)
        assertThat(entryPoint).isEqualTo(if (BuildConfig.DEBUG) AppEntryPoint.HOME else AppEntryPoint.PAYWALL)
    }
}

/**
 * Covers [resolveEntryPoint] directly, so the whole rule is checked whatever the build type:
 * `unlocked` is passed in, unlike through [MainViewModel] where debug builds force it on.
 */
class ResolveEntryPointTest {

    @Test
    fun `a signed in user who has paid lands on home, with or without a local profile`() {
        assertThat(resolveEntryPoint(hasCompletedOnboarding = true, loggedIn = true, unlocked = true))
            .isEqualTo(AppEntryPoint.HOME)
        assertThat(resolveEntryPoint(hasCompletedOnboarding = false, loggedIn = true, unlocked = true))
            .isEqualTo(AppEntryPoint.HOME)
    }

    @Test
    fun `a signed in user who has not paid never gets in and sees the paywall`() {
        assertThat(resolveEntryPoint(hasCompletedOnboarding = true, loggedIn = true, unlocked = false))
            .isEqualTo(AppEntryPoint.PAYWALL)
        assertThat(resolveEntryPoint(hasCompletedOnboarding = false, loggedIn = true, unlocked = false))
            .isEqualTo(AppEntryPoint.PAYWALL)
    }

    @Test
    fun `someone who paid but is not signed in must sign in`() {
        assertThat(resolveEntryPoint(hasCompletedOnboarding = true, loggedIn = false, unlocked = true))
            .isEqualTo(AppEntryPoint.LOGIN)
    }

    @Test
    fun `someone who finished onboarding without paying sees the paywall`() {
        assertThat(resolveEntryPoint(hasCompletedOnboarding = true, loggedIn = false, unlocked = false))
            .isEqualTo(AppEntryPoint.PAYWALL)
    }

    @Test
    fun `a new user without a session starts at the greetings, whatever the subscription`() {
        assertThat(resolveEntryPoint(hasCompletedOnboarding = false, loggedIn = false, unlocked = false))
            .isEqualTo(AppEntryPoint.GREETINGS)
        assertThat(resolveEntryPoint(hasCompletedOnboarding = false, loggedIn = false, unlocked = true))
            .isEqualTo(AppEntryPoint.GREETINGS)
    }
}
