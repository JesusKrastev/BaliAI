package com.jesuskrastev.bali.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.PremiumSubscription
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** What [ManageSubscriptionScreen] shows in its plan card. */
sealed interface ManageSubscriptionUiState {
    data object Loading : ManageSubscriptionUiState

    /** Premium is active; [headline] is the status chip and [detail] the line under it. */
    data class Active(val headline: String, val detail: String, val isCancelled: Boolean) :
        ManageSubscriptionUiState

    data object Inactive : ManageSubscriptionUiState

    /** The status couldn't be read (offline); the user can still open the Customer Center. */
    data object Unavailable : ManageSubscriptionUiState
}

/**
 * Backs the "Gestionar suscripción" screen: reads the plan's status from RevenueCat, keeps it
 * live as the customer info changes and restores purchases on request. Cancelling or changing the
 * plan is delegated to the Customer Center (see [com.jesuskrastev.bali.ui.screens.paywall.CustomerCenterViewModel])
 * so the cancellation survey keeps working.
 */
@HiltViewModel
class ManageSubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManageSubscriptionUiState>(ManageSubscriptionUiState.Loading)
    val uiState: StateFlow<ManageSubscriptionUiState> = _uiState.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.getCustomerInfo().fold(
                onSuccess = ::publish,
                onFailure = { _uiState.value = ManageSubscriptionUiState.Unavailable }
            )
        }
        viewModelScope.launch {
            subscriptionRepository.customerInfoStream()
                .catch { }
                .collect(::publish)
        }
    }

    /** Restores previous purchases and reports the outcome through [message]. */
    fun restorePurchases() {
        viewModelScope.launch {
            _isRestoring.value = true
            subscriptionRepository.restorePurchases().fold(
                onSuccess = { customerInfo ->
                    publish(customerInfo)
                    _message.value = if (subscriptionRepository.hasPremiumEntitlement(customerInfo)) {
                        "Compras restauradas. Tu suscripción está activa."
                    } else {
                        "No hemos encontrado compras anteriores en esta cuenta de Google."
                    }
                },
                onFailure = { _message.value = "No se pudo restaurar. Revisa tu conexión e inténtalo de nuevo." }
            )
            _isRestoring.value = false
        }
    }

    /** Marks the current [message] as shown. */
    fun messageShown() {
        _message.value = null
    }

    private fun publish(customerInfo: CustomerInfo) {
        _uiState.value = describeSubscription(subscriptionRepository.premiumSubscription(customerInfo))
    }
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

/**
 * Turns the entitlement into the copy shown on the plan card.
 *
 * @param subscription the active premium entitlement, or null when there is none
 * @param zone zone used to print dates
 * @return [ManageSubscriptionUiState.Inactive] for null, otherwise an
 *   [ManageSubscriptionUiState.Active] whose wording depends on trial, cancellation and expiry
 */
internal fun describeSubscription(
    subscription: PremiumSubscription?,
    zone: ZoneId = ZoneId.systemDefault()
): ManageSubscriptionUiState {
    if (subscription == null) return ManageSubscriptionUiState.Inactive
    val date = subscription.endsAtMillis?.let { DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(zone)) }
    return when {
        date == null -> ManageSubscriptionUiState.Active("Activa", "Tu acceso a Bali no caduca.", false)
        !subscription.willRenew -> ManageSubscriptionUiState.Active(
            "Cancelada",
            "Seguirás teniendo acceso completo hasta el $date. No se te volverá a cobrar.",
            true
        )
        subscription.isTrial -> ManageSubscriptionUiState.Active(
            "Prueba gratis",
            "Tu prueba termina el $date y después empieza el cobro. Cancela antes si no quieres seguir.",
            false
        )
        else -> ManageSubscriptionUiState.Active("Activa", "Se renueva el $date.", false)
    }
}
