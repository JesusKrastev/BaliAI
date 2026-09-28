package com.jesuskrastev.bali.ui.screens.paywall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.launch

/**
 * Displays the RevenueCat paywall and reports whether premium is active on dismissal.
 *
 * The caller owns the hard-paywall policy: this screen has no free-content fallback to fall
 * back to, so [onDismissResult] is only ever a real exit — either into the mandatory login gate
 * (true) or out of the app entirely (false), the same "no screen behind this one" rule
 * [com.jesuskrastev.bali.ui.screens.auth.AuthScreen] applies to its mandatory sign-in gate. A
 * close attempt (the paywall's own close button, or the system back gesture, both routed through
 * the same attempt) does not call [onDismissResult] straight away: [viewModel] switches this
 * composable to a one-time win-back offer first (see [SubscriptionViewModel.onCloseAttempt]),
 * and only a later close of that offer resolves to a real [onDismissResult] call. A successful
 * purchase does not wait for any of that: this screen also calls [onDismissResult] the moment
 * [SubscriptionViewModel.hasPremium] confirms it, so a buyer never has to close anything at all.
 *
 * The paywall's purchase and restore callbacks are relayed to [viewModel], so analytics can
 * tell who tapped a plan and who backed out at the Google Play sheet.
 *
 * @param onDismissResult receives true only when RevenueCat confirms the premium entitlement;
 * false means the win-back offer (if any) was already shown and declined, so the caller must
 * leave the app rather than leave this composable on screen with no way to progress
 * @param viewModel owner of subscription checks and paywall analytics
 */
@Composable
fun PaywallScreen(
    onDismissResult: (Boolean) -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val hasPremium by viewModel.hasPremium.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val restoreMessage by viewModel.restoreMessage.collectAsStateWithLifecycle()
    val winbackOffering by viewModel.winbackOffering.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val paywallListener = remember(viewModel) { PaywallAnalyticsListener(viewModel) }

    // Shared by the paywall's own close button and the system back gesture, so neither can
    // skip the win-back step (or the other's analytics) by taking a different way out.
    val attemptClose: () -> Unit = {
        scope.launch {
            when (val outcome = viewModel.onCloseAttempt()) {
                is PaywallCloseOutcome.Exit -> onDismissResult(outcome.hasPremium)
                PaywallCloseOutcome.ShowWinback -> Unit
            }
        }
    }
    BackHandler(onBack = attemptClose)

    // Advances the moment RevenueCat confirms the purchase, instead of waiting for the user to
    // also close the paywall afterward — a happy buyer has no reason to.
    LaunchedEffect(hasPremium) {
        if (hasPremium) {
            viewModel.onPremiumConfirmed()
            onDismissResult(true)
        }
    }

    // Tracks people who walk away from the paywall instead of deciding: without this, the
    // only signal is the absence of a dismissal, which is indistinguishable from a crash.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onPaywallBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onPaywallResumed()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Show restore messages in a snackbar
    LaunchedEffect(restoreMessage) {
        restoreMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearRestoreMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Using the default full-screen Paywall from Purchases UI. Keyed on the win-back
            // offering so switching to it remounts a fresh Paywall instance instead of relying
            // on the composable to react to an in-place options change.
            key(winbackOffering) {
                Paywall(
                    options = PaywallOptions.Builder(dismissRequest = attemptClose)
                        .setShouldDisplayDismissButton(true)
                        .setListener(paywallListener)
                        .apply { winbackOffering?.let { setOffering(it) } }
                        .build()
                )
            }
        }
    }
}

/**
 * Relays the RevenueCat paywall's purchase and restore callbacks to [viewModel], which reports
 * them to analytics. The paywall keeps the latest listener it is given, so the instance only
 * needs to live as long as [viewModel].
 *
 * @param viewModel owner of the paywall analytics
 */
internal class PaywallAnalyticsListener(
    private val viewModel: SubscriptionViewModel
) : PaywallListener {

    override fun onPurchaseStarted(rcPackage: Package) {
        viewModel.onPurchaseStarted(rcPackage)
    }

    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
        viewModel.onPurchaseCompleted()
    }

    override fun onPurchaseCancelled() {
        viewModel.onPurchaseCancelled()
    }

    override fun onPurchaseError(error: PurchasesError) {
        viewModel.onPurchaseError(error)
    }

    override fun onRestoreStarted() {
        viewModel.onRestoreStarted()
    }

    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
        viewModel.onRestoreCompleted(customerInfo)
    }

    override fun onRestoreError(error: PurchasesError) {
        viewModel.onRestoreError(error)
    }
}
