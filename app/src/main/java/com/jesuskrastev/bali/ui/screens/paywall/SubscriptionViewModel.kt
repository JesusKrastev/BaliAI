package com.jesuskrastev.bali.ui.screens.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jesuskrastev.bali.RobolectricDetector
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
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
 * Outcome of a paywall close attempt: either the screen should really exit (the user bought or
 * genuinely walked away), or a win-back offer should be shown in its place instead.
 */
sealed interface PaywallCloseOutcome {
    data class Exit(val hasPremium: Boolean) : PaywallCloseOutcome
    data object ShowWinback : PaywallCloseOutcome
}

/**
 * Owns the paywall's subscription state and its analytics.
 *
 * Besides the shown/closed/purchased/backgrounded events it receives the RevenueCat paywall's
 * own callbacks, relayed by [PaywallAnalyticsListener], and reports how far each visitor got:
 * tapped a plan, backed out of the Google Play sheet, hit an error, or bought. Every failure
 * along the way (a purchase or restore error, a failed premium check, a failed offering fetch)
 * is also sent to Crashlytics as a non-fatal via [recordPaywallError], so a broken paywall shows
 * up there instead of only as a drop in the funnel.
 *
 * When the user closes the paywall without buying, [onCloseAttempt] offers a one-time win-back
 * discount (see [winbackOffering]) instead of letting the screen exit immediately. A successful
 * purchase doesn't need the user to close anything at all: [PaywallScreen] observes [hasPremium]
 * and calls [onPremiumConfirmed] the moment it flips to true.
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

    /** Result of the fire-and-forget win-back prefetch kicked off in [init], if it resolved. */
    private var prefetchedWinback: Offering? = null

    /** True once the win-back offer has been shown, so it is never offered a second time. */
    private var winbackAlreadyOffered = false

    /** Monotonic timestamp of the win-back offer appearing; null until [onCloseAttempt] shows it. */
    private var winbackShownAtNanos: Long? = null

    /** Null shows the main/current offering, as before; non-null switches [PaywallScreen] to it. */
    private val _winbackOffering = MutableStateFlow<Offering?>(null)
    val winbackOffering: StateFlow<Offering?> = _winbackOffering.asStateFlow()

    init {
        analyticsTracker.paywallShown()
        viewModelScope.launch {
            prefetchedWinback = fetchWinbackOffering()
        }
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
                    recordPaywallError("restore purchases failed", error)
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
     * Tracks how the paywall was resolved, exactly once — a confirmed purchase or a final
     * decline. Called from [onCloseAttempt] (closing while premium is active, or declining with
     * no win-back left to offer) and from [onPremiumConfirmed] (a purchase detected without any
     * close attempt). A second call for the same paywall is a no-op either way.
     *
     * @param purchased true if the entitlement was active by the time this resolved
     */
    private fun onPaywallDismissed(purchased: Boolean) {
        if (isResolved) return
        isResolved = true
        if (purchased) analyticsTracker.paywallPurchased() else analyticsTracker.paywallClosed()
    }

    /**
     * Reports a purchase that RevenueCat confirmed on its own — [hasPremium] flipping to true —
     * without the user attempting to close the paywall first. Lets the screen advance the moment
     * a purchase completes instead of waiting for a manual close a happy buyer has no reason to
     * make; that gap is why [AnalyticsTracker.paywallPurchased] used to never fire in practice.
     * Exactly-once: a purchase already reported through [onCloseAttempt] (or a previous call
     * here) is a no-op, so a race between the two paths can't double-report analytics.
     */
    fun onPremiumConfirmed() {
        if (isResolved) return
        onPaywallDismissed(true)
        if (_winbackOffering.value != null) analyticsTracker.paywallWinbackPurchased(secondsOnWinback())
    }

    /**
     * Tracks that the paywall left the foreground before the user decided anything —
     * pressing home, switching apps or killing the app. Reports the win-back-specific event
     * instead of the generic one when that offer is what was on screen, so a visitor who
     * closes the app directly from the discount isn't folded into the main paywall's count.
     */
    fun onPaywallBackgrounded() {
        if (isResolved || isAway) return
        isAway = true
        if (_winbackOffering.value != null) {
            analyticsTracker.paywallWinbackBackgrounded()
        } else {
            analyticsTracker.paywallBackgrounded()
        }
    }

    /**
     * Tracks a return to a paywall that had been backgrounded, on whichever stage (main or
     * win-back) was on screen. Does nothing on the first foregrounding, which is simply the
     * screen opening.
     */
    fun onPaywallResumed() {
        if (!isAway) return
        isAway = false
        if (_winbackOffering.value != null) {
            analyticsTracker.paywallWinbackResumed()
        } else {
            analyticsTracker.paywallResumed()
        }
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
     * @param error the RevenueCat error; only its code is reported to analytics, never its message
     */
    fun onPurchaseError(error: PurchasesError) {
        analyticsTracker.paywallPurchaseFailed(error.code.name, pendingPlan, secondsOnPaywall())
        recordPaywallError("purchase failed: ${error.code} ${error.message}")
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
     * @param error the RevenueCat error; only its code is reported to analytics, never its message
     */
    fun onRestoreError(error: PurchasesError) {
        analyticsTracker.paywallRestoreFailed(error.code.name)
        recordPaywallError("restore failed: ${error.code} ${error.message}")
    }

    /** Returns the whole seconds elapsed since the paywall appeared. */
    private fun secondsOnPaywall(): Int =
        TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - shownAtNanos).toInt()

    /** Returns the whole seconds elapsed since the win-back offer appeared, or 0 if it never was. */
    private fun secondsOnWinback(): Int =
        winbackShownAtNanos?.let { TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - it).toInt() } ?: 0

    /**
     * Fetches the latest customer info and checks the premium entitlement.
     *
     * @return true only when RevenueCat confirms the premium entitlement; false on any failure
     */
    private suspend fun checkPremiumNow(): Boolean {
        return subscriptionRepository.getCustomerInfo().fold(
            onSuccess = { customerInfo ->
                subscriptionRepository.hasPremiumEntitlement(customerInfo)
            },
            onFailure = { error ->
                recordPaywallError("premium check failed", error)
                false
            }
        )
    }

    /**
     * Fetches the win-back offering, reporting a failure to Crashlytics instead of silently
     * dropping it — used both by the [init] prefetch and the [onCloseAttempt] fallback fetch.
     *
     * @return the offering, or null if it doesn't exist or the fetch failed
     */
    private suspend fun fetchWinbackOffering(): Offering? =
        subscriptionRepository.getOffering(WINBACK_OFFERING_ID)
            .onFailure { recordPaywallError("winback offering fetch failed", it) }
            .getOrNull()

    /**
     * Reports a paywall failure to Crashlytics as a non-fatal, tagged with [context] so distinct
     * paywall failures show up as distinct issues instead of being grouped by whatever generic
     * SDK exception caused them.
     *
     * @param context short, fixed label identifying which paywall step failed
     * @param cause the underlying error, if one was caught; omitted for RevenueCat's own
     *   [PurchasesError] callbacks, which aren't a [Throwable] and are folded into [context] instead
     */
    private fun recordPaywallError(context: String, cause: Throwable? = null) {
        // Crashlytics reaches into real Android/Play Services classes that the plain JVM unit
        // tests for this ViewModel don't mock, unlike the Robolectric-backed tests that do —
        // same guard used for OneSignal/Mixpanel/PostHog/RevenueCat init, see [RobolectricDetector].
        if (RobolectricDetector.isRobolectric()) return
        FirebaseCrashlytics.getInstance().recordException(Exception("Paywall: $context", cause))
    }

    /**
     * Decides what happens when the user tries to leave the paywall without having bought yet.
     *
     * The first attempt switches [winbackOffering] to a discounted offer instead of letting the
     * screen exit; the caller re-composes onto it and calls this again on the next attempt. Any
     * later attempt (or the first one, if no win-back offer is available) is the real exit.
     *
     * @return [PaywallCloseOutcome.ShowWinback] to stay on screen showing the win-back offer, or
     *   [PaywallCloseOutcome.Exit] once the caller should honor the close
     */
    suspend fun onCloseAttempt(): PaywallCloseOutcome {
        val isPremiumNow = checkPremiumNow()
        if (isPremiumNow) {
            onPremiumConfirmed()
            return PaywallCloseOutcome.Exit(true)
        }

        if (!winbackAlreadyOffered) {
            winbackAlreadyOffered = true
            val winback = prefetchedWinback ?: fetchWinbackOffering()
            if (winback != null) {
                _winbackOffering.value = winback
                winbackShownAtNanos = System.nanoTime()
                analyticsTracker.paywallWinbackShown()
                return PaywallCloseOutcome.ShowWinback
            }
        }

        if (_winbackOffering.value != null) analyticsTracker.paywallWinbackClosed(secondsOnWinback())
        onPaywallDismissed(false)
        return PaywallCloseOutcome.Exit(false)
    }
}

/** Store prices are reported in micro-units: 4.99 arrives as 4,990,000. */
private const val MICROS_PER_UNIT = 1_000_000.0

/** RevenueCat identifier of the discounted offering shown once a paywall is closed unbought. */
private const val WINBACK_OFFERING_ID = "winback_monthly_discount"

/** Maps a RevenueCat package to the non-identifying [AnalyticsTracker.PaywallPlan] sent with paywall events. */
private fun Package.toPaywallPlan() = AnalyticsTracker.PaywallPlan(
    packageId = identifier,
    productId = product.id,
    price = product.price.amountMicros / MICROS_PER_UNIT,
    currency = product.price.currencyCode,
    period = product.period?.iso8601,
    offeringId = presentedOfferingContext.offeringIdentifier
)
