package com.jesuskrastev.bali.ui.screens.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ShopItem {
    data object StreakFreezer : ShopItem()
    data object EnergyRefill : ShopItem()
}

sealed class ShopEvent {
    data class SelectItem(val item: ShopItem) : ShopEvent()
    data object DismissSelection : ShopEvent()
    data object PurchaseStreakFreezer : ShopEvent()
    data object PurchaseEnergyRefill : ShopEvent()
}

data class ShopUiState(
    val coinsCount: Int = 0,
    val energyCount: Int = 0,
    val streakFreezes: Int = 0,
    val selectedItem: ShopItem? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val decrementCoinsUseCase: DecrementCoinsUseCase,
    private val analyticsTracker: FirebaseAnalyticsTracker
) : ViewModel() {

    private val _selectedItem = MutableStateFlow<ShopItem?>(null)

    val uiState: StateFlow<ShopUiState> = combine(
        userRepository.get(),
        _selectedItem
    ) { user, selected ->
        user?.let {
            ShopUiState(
                coinsCount = it.coins,
                energyCount = it.energy,
                streakFreezes = it.streakFreezes,
                selectedItem = selected
            )
        } ?: ShopUiState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopUiState()
    )

    init {
        analyticsTracker.storeOpened()
    }

    fun onEvent(event: ShopEvent) {
        when (event) {
            is ShopEvent.SelectItem -> {
                _selectedItem.value = event.item
                analyticsTracker.storeItemViewed(event.item.javaClass.simpleName)
            }
            ShopEvent.DismissSelection -> _selectedItem.value = null
            ShopEvent.PurchaseStreakFreezer -> purchaseStreakFreezer()
            ShopEvent.PurchaseEnergyRefill -> purchaseEnergyRefill()
        }
    }

    private fun purchaseStreakFreezer() {
        viewModelScope.launch {
            val user = userRepository.get().first() ?: return@launch
            if (user.streakFreezes >= 2) return@launch

            val success = decrementCoinsUseCase(120)
            if (success) {
                userRepository.updateStreakFreezes(user.streakFreezes + 1)
                analyticsTracker.storeItemPurchased("StreakFreezer")
                _selectedItem.value = null
            }
        }
    }

    private fun purchaseEnergyRefill() {
        viewModelScope.launch {
            val user = userRepository.get().first() ?: return@launch
            if (user.energy >= 3) return@launch

            val success = decrementCoinsUseCase(50)
            if (success) {
                userRepository.updateEnergy(3)
                analyticsTracker.storeItemPurchased("EnergyRefill")
                _selectedItem.value = null
            }
        }
    }
}
