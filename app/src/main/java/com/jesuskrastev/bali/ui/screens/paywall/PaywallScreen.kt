package com.jesuskrastev.bali.ui.screens.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(
    onDismissResult: (Boolean) -> Unit, // returns true if purchase successful
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val hasPremium by viewModel.hasPremium.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val restoreMessage by viewModel.restoreMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                    .build()
            )
        }
    }
}
