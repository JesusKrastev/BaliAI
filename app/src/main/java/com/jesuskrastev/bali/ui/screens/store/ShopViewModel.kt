package com.jesuskrastev.bali.ui.screens.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ShopItem {
    data object StreakFreezer : ShopItem()
}

sealed class ShopEvent {
    data class SelectItem(val item: ShopItem) : ShopEvent()
    data object DismissSelection : ShopEvent()
    data object PurchaseStreakFreezer : ShopEvent()
}

data class ShopUiState(
    val coinsCount: Int = 0,
    val streakFreezes: Int = 0,
    val selectedItem: ShopItem? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val decrementCoinsUseCase: DecrementCoinsUseCase
) : ViewModel() {

    private val _selectedItem = MutableStateFlow<ShopItem?>(null)
    private val _isProcessing = MutableStateFlow(false)

    val uiState: StateFlow<ShopUiState> = combine(
        userRepository.get(),
        _selectedItem,
        _isProcessing
    ) { user, selected, isProcessing ->
        user?.let {
            ShopUiState(
                coinsCount = it.coins,
                streakFreezes = it.streakFreezes,
                selectedItem = selected,
                isProcessing = isProcessing
            )
        } ?: ShopUiState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopUiState()
    )

    fun onEvent(event: ShopEvent) {
        when (event) {
            is ShopEvent.SelectItem -> {
                _selectedItem.value = event.item
            }
            ShopEvent.DismissSelection -> _selectedItem.value = null
            ShopEvent.PurchaseStreakFreezer -> purchaseStreakFreezer()
        }
    }

    /**
     * Buys a streak freezer for 120 coins, capped at 2 owned at once.
     *
     * Guarded by [_isProcessing] so a second tap while a purchase is already in flight is
     * ignored instead of racing it — two concurrent calls could otherwise both pass the
     * `streakFreezes < 2` check before either write lands, charging twice for one freezer.
     */
    private fun purchaseStreakFreezer() {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val user = userRepository.get().first() ?: return@launch
                if (user.streakFreezes >= 2) return@launch

                val success = decrementCoinsUseCase(120)
                if (success) {
                    userRepository.updateStreakFreezes(user.streakFreezes + 1)
                    _selectedItem.value = null
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
