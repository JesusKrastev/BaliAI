package com.jesuskrastev.bali.ui.screens.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Owns the paywall's subscription state and its analytics.
 *
 * Besides the shown/closed/purchased/backgrounded events it receives the RevenueCat paywall's
 * own callbacks, relayed by [PaywallAnalyticsListener], and reports how far each visitor got:
 * tapped a plan, backed out of the Google Play sheet, hit an error, or bought.
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    /** Monotonic timestamp of the paywall appearing, so wall-clock changes can't skew durations. */
    private val shownAtNanos = System.nanoTime()

    /** Plan of the purchase in flight; the cancel, error and completion callbacks don't carry it. */
    private var pendingPlan: AnalyticsTracker.PaywallPlan? = null

    init {
        analyticsTracker.paywallShown()
    }

    // Emits true if the user has the 'premium' entitlement active
    val hasPremium: StateFlow<Boolean> = subscriptionRepository.customerInfoStream()
        .map { customerInfo ->
            subscriptionRepository.hasPremiumEntitlement(customerInfo)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    /** Restores previous purchases and publishes the outcome through [restoreMessage]. */
    fun restorePurchases() {
        viewModelScope.launch {
            _isRestoring.value = true
            _restoreMessage.value = null

            subscriptionRepository.restorePurchases().fold(
                onSuccess = { customerInfo ->
                    val isPremium = subscriptionRepository.hasPremiumEntitlement(customerInfo)
                    _restoreMessage.value = if (isPremium) {
                        "¡Compras restauradas con éxito!"
                    } else {
                        "No se encontraron compras anteriores para restaurar."
                    }
                },
                onFailure = { error ->
                    _restoreMessage.value = "Error al restaurar: ${error.localizedMessage ?: "Desconocido"}"
                }
            )

            _isRestoring.value = false
        }
    }

    /** Clears [restoreMessage] once it has been shown. */
    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }

    /** True once the user has either bought or closed, so backgrounding stops being tracked. */
    private var isResolved = false

    /** True while the user is away from a paywall they had not resolved yet. */
    private var isAway = false

    /**
     * Tracks how the user left the paywall.
     *
     * @param purchased true if the entitlement was active by the time they dismissed
     */
    fun onPaywallDismissed(purchased: Boolean) {
        isResolved = true
        if (purchased) analyticsTracker.paywallPurchased() else analyticsTracker.paywallClosed()
    }

    /**
     * Tracks that the paywall left the foreground before the user decided anything —
     * pressing home, switching apps or killing the app.
     */
    fun onPaywallBackgrounded() {
        if (isResolved || isAway) return
        isAway = true
        analyticsTracker.paywallBackgrounded()
    }

    /**
     * Tracks a return to a paywall that had been backgrounded. Does nothing on the first
     * foregrounding, which is simply the screen opening.
     */
    fun onPaywallResumed() {
        if (!isAway) return
        isAway = false
        analyticsTracker.paywallResumed()
    }

    /**
     * Tracks that the user tapped a plan and the purchase flow began, and remembers the plan
     * so the callbacks that don't carry it (cancel, error, completion) can still report it.
     *
     * @param rcPackage the RevenueCat package being purchased
     */
    fun onPurchaseStarted(rcPackage: Package) {
        val plan = rcPackage.toPaywallPlan()
        pendingPlan = plan
        analyticsTracker.paywallPurchaseStarted(plan, secondsOnPaywall())
    }

    /** Tracks that the store confirmed the purchase that [onPurchaseStarted] began. */
    fun onPurchaseCompleted() {
        analyticsTracker.paywallPurchaseCompleted(pendingPlan, secondsOnPaywall())
        pendingPlan = null
    }

    /** Tracks that the user backed out of the Google Play sheet without buying. */
    fun onPurchaseCancelled() {
        analyticsTracker.paywallPurchaseCancelled(pendingPlan, secondsOnPaywall())
        pendingPlan = null
    }

    /**
     * Tracks a purchase that failed for a reason other than the user cancelling.
     *
     * @param error the RevenueCat error; only its code is reported, never its message
     */
    fun onPurchaseError(error: PurchasesError) {
        analyticsTracker.paywallPurchaseFailed(error.code.name, pendingPlan, secondsOnPaywall())
        pendingPlan = null
    }

    /** Tracks that the user tapped restore purchases on the paywall. */
    fun onRestoreStarted() {
        analyticsTracker.paywallRestoreStarted()
    }

    /**
     * Tracks a restore that finished without error.
     *
     * @param customerInfo the customer info the restore returned
     */
    fun onRestoreCompleted(customerInfo: CustomerInfo) {
        analyticsTracker.paywallRestoreCompleted(subscriptionRepository.hasPremiumEntitlement(customerInfo))
    }

    /**
     * Tracks a restore that failed.
     *
     * @param error the RevenueCat error; only its code is reported, never its message
     */
    fun onRestoreError(error: PurchasesError) {
        analyticsTracker.paywallRestoreFailed(error.code.name)
    }

    /** Returns the whole seconds elapsed since the paywall appeared. */
    private fun secondsOnPaywall(): Int =
        TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - shownAtNanos).toInt()

    /**
     * Fetches the latest customer info and checks the premium entitlement.
     *
     * @return true only when RevenueCat confirms the premium entitlement; false on any failure
     */
    suspend fun checkPremiumNow(): Boolean {
        return subscriptionRepository.getCustomerInfo().fold(
            onSuccess = { customerInfo ->
                subscriptionRepository.hasPremiumEntitlement(customerInfo)
            },
            onFailure = { false }
        )
    }
}

/** Store prices are reported in micro-units: 4.99 arrives as 4,990,000. */
private const val MICROS_PER_UNIT = 1_000_000.0

/** Maps a RevenueCat package to the non-identifying [AnalyticsTracker.PaywallPlan] sent with paywall events. */
private fun Package.toPaywallPlan() = AnalyticsTracker.PaywallPlan(
    packageId = identifier,
    productId = product.id,
    price = product.price.amountMicros / MICROS_PER_UNIT,
    currency = product.price.currencyCode,
    period = product.period?.iso8601,
    offeringId = presentedOfferingContext.offeringIdentifier
)
