package com.jesuskrastev.bali.ui.screens.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

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
    val isRestoring: StateFlow<Boolean> = _isRestoring

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage

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

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }

    /** Tracks paywall dismissal and whether a purchase was completed. */
    fun onPaywallDismissed(purchased: Boolean) {
        analyticsTracker.paywallDismissed(purchased)
    }

    suspend fun checkPremiumNow(): Boolean {
        return subscriptionRepository.getCustomerInfo().fold(
            onSuccess = { customerInfo ->
                subscriptionRepository.hasPremiumEntitlement(customerInfo)
            },
            onFailure = { false }
        )
    }
}
