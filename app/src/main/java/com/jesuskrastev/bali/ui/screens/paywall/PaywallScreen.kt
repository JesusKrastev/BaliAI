package com.jesuskrastev.bali.ui.screens.paywall

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
 * The caller owns the hard-paywall policy: a false result must keep this composable on
 * screen, while a true result may advance to the mandatory login gate.
 *
 * The paywall's purchase and restore callbacks are relayed to [viewModel], so analytics can
 * tell who tapped a plan and who backed out at the Google Play sheet.
 *
 * @param onDismissResult receives true only when RevenueCat confirms the premium entitlement
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val paywallListener = remember(viewModel) { PaywallAnalyticsListener(viewModel) }

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
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearRestoreMessage()
            }
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
            // Using the default full-screen Paywall from Purchases UI
            Paywall(
                options = PaywallOptions.Builder(
                    dismissRequest = {
                        scope.launch {
                            val isPremiumNow = viewModel.checkPremiumNow()
                            viewModel.onPaywallDismissed(isPremiumNow)
                            onDismissResult(isPremiumNow)
                        }
                    }
                )
                    .setShouldDisplayDismissButton(true)
                    .setListener(paywallListener)
                    .build()
            )
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
