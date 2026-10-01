package com.jesuskrastev.bali.ui.screens.paywall

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SubscriptionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = mock<AnalyticsTracker>()
    private val repository = FakeSubscriptionRepository()

    private fun viewModel() = SubscriptionViewModel(repository, analytics)

    private val monthlyPlan = AnalyticsTracker.PaywallPlan(
        packageId = "\$rc_monthly",
        productId = "bali_monthly",
        price = 4.99,
        currency = "EUR",
        period = "P1M",
        offeringId = "default"
    )

    private fun monthlyPackage(): Package {
        val storeProduct = mock<StoreProduct> {
            on { id } doReturn "bali_monthly"
            on { price } doReturn Price(formatted = "4,99 €", amountMicros = 4_990_000, currencyCode = "EUR")
            on { period } doReturn Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
        }
        return mock<Package> {
            on { identifier } doReturn "\$rc_monthly"
            on { product } doReturn storeProduct
            on { presentedOfferingContext } doReturn PresentedOfferingContext(offeringIdentifier = "default")
        }
    }

    @Test
    fun `opening the paywall is announced`() {
        viewModel()

        verify(analytics).paywallShown("onboarding")
    }

    @Test
    fun `tapping a plan reports the plan the store was asked to sell`() {
        viewModel().onPurchaseStarted(monthlyPackage())

        verify(analytics).paywallPurchaseStarted(eq(monthlyPlan), any(), eq("onboarding"))
    }

    @Test
    fun `backing out of the store sheet reports the plan that was in flight`() {
        val viewModel = viewModel()
        viewModel.onPurchaseStarted(monthlyPackage())

        viewModel.onPurchaseCancelled()

        verify(analytics).paywallPurchaseCancelled(eq(monthlyPlan), any(), eq("onboarding"))
    }

    @Test
    fun `a cancel with no purchase in flight reports no plan`() {
        viewModel().onPurchaseCancelled()

        verify(analytics).paywallPurchaseCancelled(isNull(), any(), eq("onboarding"))
    }

    @Test
    fun `a completed purchase reports the plan and forgets it`() {
        val viewModel = viewModel()
        viewModel.onPurchaseStarted(monthlyPackage())

        viewModel.onPurchaseCompleted()
        viewModel.onPurchaseCancelled()

        verify(analytics).paywallPurchaseCompleted(eq(monthlyPlan), any(), eq("onboarding"))
        verify(analytics).paywallPurchaseCancelled(isNull(), any(), eq("onboarding"))
    }

    @Test
    fun `a failed purchase reports only the error code`() {
        val viewModel = viewModel()
        viewModel.onPurchaseStarted(monthlyPackage())

        viewModel.onPurchaseError(PurchasesError(PurchasesErrorCode.StoreProblemError, "private detail"))

        verify(analytics).paywallPurchaseFailed(
            eq("StoreProblemError"),
            anyOrNull(),
            any(),
            eq("onboarding")
        )
    }

    @Test
    fun `a finished restore reports whether premium came back`() {
        repository.premium = true

        viewModel().onRestoreCompleted(mock<CustomerInfo>())

        verify(analytics).paywallRestoreCompleted(eq(true), eq("onboarding"))
    }

    @Test
    fun `a failed restore reports only the error code`() {
        viewModel().onRestoreError(PurchasesError(PurchasesErrorCode.NetworkError))

        verify(analytics).paywallRestoreFailed(eq("NetworkError"), eq("onboarding"))
    }

    @Test
    fun `closing the paywall for the first time offers the win-back discount instead of exiting`() = runTest {
        repository.winbackOffering = mock()
        val viewModel = viewModel()

        val outcome = viewModel.onCloseAttempt()

        assertThat(outcome).isEqualTo(PaywallCloseOutcome.ShowWinback)
        verify(analytics).paywallWinbackShown()
        verify(analytics, never()).paywallClosed(any())
    }

    @Test
    fun `closing the win-back offer too finally exits without showing it again`() = runTest {
        repository.winbackOffering = mock()
        val viewModel = viewModel()
        viewModel.onCloseAttempt() // first close: shows the win-back offer

        val outcome = viewModel.onCloseAttempt()

        assertThat(outcome).isEqualTo(PaywallCloseOutcome.Exit(false))
        verify(analytics).paywallWinbackClosed(any())
        verify(analytics).paywallClosed(any())
    }

    @Test
    fun `closing with no win-back offer available exits on the first attempt`() = runTest {
        val viewModel = viewModel() // repository.winbackOffering stays null

        val outcome = viewModel.onCloseAttempt()

        assertThat(outcome).isEqualTo(PaywallCloseOutcome.Exit(false))
        verify(analytics, never()).paywallWinbackShown()
        verify(analytics, never()).paywallWinbackClosed(any())
        verify(analytics).paywallClosed(any())
    }

    @Test
    fun `buying the win-back offer reports both the generic and the win-back purchase`() = runTest {
        repository.winbackOffering = mock()
        val viewModel = viewModel()
        viewModel.onCloseAttempt() // first close: shows the win-back offer
        repository.premium = true

        val outcome = viewModel.onCloseAttempt()

        assertThat(outcome).isEqualTo(PaywallCloseOutcome.Exit(true))
        verify(analytics).paywallWinbackPurchased(any())
        verify(analytics).paywallPurchased(any())
    }

    @Test
    fun `a purchase detected automatically is reported without waiting for a close attempt`() {
        val viewModel = viewModel()

        viewModel.onPremiumConfirmed()

        verify(analytics).paywallPurchased(any())
    }

    @Test
    fun `a purchase detected automatically after the win-back was shown reports both purchases`() = runTest {
        repository.winbackOffering = mock()
        val viewModel = viewModel()
        viewModel.onCloseAttempt() // first close: shows the win-back offer

        viewModel.onPremiumConfirmed()

        verify(analytics).paywallWinbackPurchased(any())
        verify(analytics).paywallPurchased(any())
    }

    @Test
    fun `a purchase already detected automatically is not reported again by a later close attempt`() = runTest {
        repository.premium = true
        val viewModel = viewModel()
        viewModel.onPremiumConfirmed()

        val outcome = viewModel.onCloseAttempt()

        assertThat(outcome).isEqualTo(PaywallCloseOutcome.Exit(true))
        verify(analytics).paywallPurchased(any())
    }

    @Test
    fun `a failed premium check during close is treated as not premium instead of crashing`() = runTest {
        repository.customerInfoFailure = IllegalStateException("network down")
        val viewModel = viewModel()

        val outcome = viewModel.onCloseAttempt()

        assertThat(outcome).isEqualTo(PaywallCloseOutcome.Exit(false))
        verify(analytics).paywallClosed(any())
    }

    @Test
    fun `a failed win-back fetch exits instead of crashing`() = runTest {
        repository.offeringFailure = IllegalStateException("offering not found")
        val viewModel = viewModel()

        val outcome = viewModel.onCloseAttempt()

        assertThat(outcome).isEqualTo(PaywallCloseOutcome.Exit(false))
        verify(analytics, never()).paywallWinbackShown()
    }

    @Test
    fun `backgrounding the main paywall reports the generic event`() {
        val viewModel = viewModel()

        viewModel.onPaywallBackgrounded()

        verify(analytics).paywallBackgrounded()
        verify(analytics, never()).paywallWinbackBackgrounded()
    }

    @Test
    fun `backgrounding after the win-back offer is shown reports the win-back event instead`() = runTest {
        repository.winbackOffering = mock()
        val viewModel = viewModel()
        viewModel.onCloseAttempt() // first close: shows the win-back offer

        viewModel.onPaywallBackgrounded()

        verify(analytics).paywallWinbackBackgrounded()
        verify(analytics, never()).paywallBackgrounded()
    }

    @Test
    fun `resuming after backgrounding the win-back offer reports the win-back event instead`() = runTest {
        repository.winbackOffering = mock()
        val viewModel = viewModel()
        viewModel.onCloseAttempt() // first close: shows the win-back offer
        viewModel.onPaywallBackgrounded()

        viewModel.onPaywallResumed()

        verify(analytics).paywallWinbackResumed()
        verify(analytics, never()).paywallResumed()
    }
}

private class FakeSubscriptionRepository : SubscriptionRepository {

    var premium = false
    var winbackOffering: Offering? = null
    var customerInfoFailure: Throwable? = null
    var offeringFailure: Throwable? = null

    private val customerInfo = mock<CustomerInfo>()

    override fun customerInfoStream(): Flow<CustomerInfo> = emptyFlow()

    override suspend fun getCustomerInfo(): Result<CustomerInfo> =
        customerInfoFailure?.let { Result.failure(it) } ?: Result.success(customerInfo)

    override suspend fun restorePurchases(): Result<CustomerInfo> =
        Result.failure(IllegalStateException("not needed by these tests"))

    override fun hasPremiumEntitlement(customerInfo: CustomerInfo): Boolean = premium

    override suspend fun getOffering(identifier: String): Result<Offering?> =
        offeringFailure?.let { Result.failure(it) } ?: Result.success(winbackOffering)
}
