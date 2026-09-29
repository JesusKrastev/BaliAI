package com.jesuskrastev.bali.ui.screens.auth

import androidx.lifecycle.SavedStateHandle
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

import org.mockito.kotlin.mock
import com.google.firebase.analytics.FirebaseAnalytics
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * [SubscriptionRepository] fake that records which accounts it was asked to link, and can be
 * told to fail the link.
 */
private class FakeSubscriptionRepository : SubscriptionRepository {
    val identifiedUserIds = mutableListOf<String>()
    var identifyFailure: Throwable? = null

    private val customerInfo = mock<CustomerInfo>()

    override fun customerInfoStream(): Flow<CustomerInfo> = emptyFlow()
    override suspend fun getCustomerInfo(): Result<CustomerInfo> = Result.success(customerInfo)
    override suspend fun restorePurchases(): Result<CustomerInfo> = Result.success(customerInfo)
    override fun hasPremiumEntitlement(customerInfo: CustomerInfo): Boolean = false
    override suspend fun getOffering(identifier: String): Result<Offering?> = Result.success(null)

    override suspend fun identify(userId: String): Result<CustomerInfo> {
        identifiedUserIds.add(userId)
        return identifyFailure?.let { Result.failure(it) } ?: Result.success(customerInfo)
    }
}

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeTestResultRepository = FakeTestResultRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock(), mock())
    private val fakeMigrationManager = FakeFirestoreMigrationManager()
    private val fakeSubscriptionRepository = FakeSubscriptionRepository()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        viewModel = AuthViewModel(
            userRepository = fakeUserRepository,
            testResultRepository = fakeTestResultRepository,
            answerRepository = fakeAnswerRepository,
            authRepository = fakeAuthRepository,
            analyticsTracker = fakeAnalyticsTracker,
            subscriptionRepository = fakeSubscriptionRepository,
            migrationManager = fakeMigrationManager
        )
    }


    @Test
    fun `clearError clears both error messages`() = runTest {
        viewModel.run {
            assertThat(errorMessage.value).isNull()
            assertThat(errorEmail.value).isNull()
            clearError()
            assertThat(errorMessage.value).isNull()
            assertThat(errorEmail.value).isNull()
        }
    }

    @Test
    fun `isLoggingIn starts as false`() = runTest {
        assertThat(viewModel.isLoggingIn.value).isFalse()
    }

    @Test
    fun `errorMessage is initially null`() = runTest {
        assertThat(viewModel.errorMessage.value).isNull()
    }

    @Test
    fun `errorEmail is initially null`() = runTest {
        assertThat(viewModel.errorEmail.value).isNull()
    }

    @Test
    fun `signing in links the subscription to the uid PostHog was identified with`() = runTest {
        var signedIn = false

        viewModel.signInWithGoogle(mock(), restrictNewAccounts = false) { signedIn = true }

        assertThat(signedIn).isTrue()
        assertThat(fakeAnalyticsTracker.identifiedUsers.map { it.first }).containsExactly("user_123")
        assertThat(fakeSubscriptionRepository.identifiedUserIds).containsExactly("user_123")
    }

    @Test
    fun `a failed subscription link never blocks the sign in`() = runTest {
        fakeSubscriptionRepository.identifyFailure = IllegalStateException("RevenueCat is down")
        var signedIn = false

        viewModel.signInWithGoogle(mock(), restrictNewAccounts = false) { signedIn = true }

        assertThat(signedIn).isTrue()
        assertThat(viewModel.errorMessage.value).isNull()
    }
}
