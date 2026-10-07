package com.jesuskrastev.bali.ui.screens.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.RecoverStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

sealed class ShopItem {
    data object StreakFreezer : ShopItem()
    data object StreakRecovery : ShopItem()
    data object StreakBet : ShopItem()
    data object Hint : ShopItem()
    data object FiftyFifty : ShopItem()
    data object SurpriseChest : ShopItem()
    data object DoubleXp : ShopItem()
    data object DoubleCoins : ShopItem()
}

sealed class ShopEvent {
    data class SelectItem(val item: ShopItem) : ShopEvent()
    data object DismissSelection : ShopEvent()
    data object PurchaseStreakFreezer : ShopEvent()
    data object PurchaseStreakRecovery : ShopEvent()
    data object ConfirmPurchase : ShopEvent()
    data object AdvanceChestOpening : ShopEvent()
    data object ChestOpeningAnimationFinished : ShopEvent()
}

/** The stages of the full-screen surprise-chest reveal. */
enum class ChestOpeningStep {
    AWAITING_OPEN,
    OPENING,
    AWAITING_REWARD,
    REWARD_REVEALED
}

/** A purchased chest's reward and the point reached in its reveal interaction. */
data class ChestOpening(
    val reward: Int,
    val step: ChestOpeningStep = ChestOpeningStep.AWAITING_OPEN
)

/** Coin prices and payout rules for the shop's consumables. */
object ShopCatalog {
    const val STREAK_FREEZER_COST = 120
    const val STREAK_BET_COST = 50
    const val STREAK_BET_PAYOUT = 100
    const val HINT_COST = 30
    const val FIFTY_FIFTY_COST = 45
    const val SURPRISE_CHEST_COST = 60
    const val DOUBLE_XP_COST = 80
    const val DOUBLE_COINS_COST = 70
    const val CHEST_MIN_REWARD = 30
    const val CHEST_MAX_REWARD = 120
}

/**
 * What the shop shows.
 *
 * @property coinsCount the user's coin balance
 * @property streakFreezes freezers owned
 * @property recoverableStreak days the streak recovery would bring back, 0 when there is no
 *   lost streak to recover
 * @property selectedItem the item whose purchase sheet is open
 * @property isProcessing true while a purchase is in flight
 * @property hints practice hints currently owned
 * @property fiftyFifties practice 50/50 aids currently owned
 * @property doubleXpBoosts double-XP rewards waiting for the next completed activity
 * @property doubleCoinBoosts double-coin rewards waiting for the next completed activity
 * @property hasActiveStreakBet true when the next new study day will pay the wager
 * @property chestOpening purchased chest currently being revealed, null outside that interaction
 */
data class ShopUiState(
    val coinsCount: Int = 0,
    val streakFreezes: Int = 0,
    val recoverableStreak: Int = 0,
    val selectedItem: ShopItem? = null,
    val isProcessing: Boolean = false,
    val hints: Int = 0,
    val fiftyFifties: Int = 0,
    val doubleXpBoosts: Int = 0,
    val doubleCoinBoosts: Int = 0,
    val hasActiveStreakBet: Boolean = false,
    val chestOpening: ChestOpening? = null
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val decrementCoinsUseCase: DecrementCoinsUseCase,
    private val recoverStreakUseCase: RecoverStreakUseCase
) : ViewModel() {

    private val _selectedItem = MutableStateFlow<ShopItem?>(null)
    private val _isProcessing = MutableStateFlow(false)
    private val _chestOpening = MutableStateFlow<ChestOpening?>(null)

    val uiState: StateFlow<ShopUiState> = combine(
        userRepository.get(),
        _selectedItem,
        _isProcessing,
        _chestOpening
    ) { user, selected, isProcessing, chestOpening ->
        user?.let {
            val now = System.currentTimeMillis()
            ShopUiState(
                coinsCount = it.coins,
                streakFreezes = it.streakFreezes,
                recoverableStreak = DailyStreak.of(it).settledAt(now).recoverableStreakAt(now),
                selectedItem = selected,
                isProcessing = isProcessing,
                hints = it.hints,
                fiftyFifties = it.fiftyFifties,
                doubleXpBoosts = it.doubleXpBoosts,
                doubleCoinBoosts = it.doubleCoinBoosts,
                hasActiveStreakBet = it.activeStreakBet,
                chestOpening = chestOpening
            )
        } ?: ShopUiState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopUiState()
    )

    /**
     * Reduces a shop interaction [event] into selection, purchase, or chest-reveal state.
     *
     * @param event interaction received from the shop UI
     * @return Unit; state updates or a guarded purchase launch are performed as appropriate.
     */
    fun onEvent(event: ShopEvent) {
        when (event) {
            is ShopEvent.SelectItem -> {
                _selectedItem.value = event.item
            }
            ShopEvent.DismissSelection -> _selectedItem.value = null
            ShopEvent.PurchaseStreakFreezer -> purchaseStreakFreezer()
            ShopEvent.PurchaseStreakRecovery -> purchaseStreakRecovery()
            ShopEvent.ConfirmPurchase -> purchaseSelectedItem()
            ShopEvent.AdvanceChestOpening -> advanceChestOpening()
            ShopEvent.ChestOpeningAnimationFinished -> finishChestOpeningAnimation()
        }
    }

    /** Purchases the item currently selected in the confirmation sheet. */
    private fun purchaseSelectedItem() {
        when (_selectedItem.value) {
            ShopItem.StreakFreezer -> purchaseStreakFreezer()
            ShopItem.StreakRecovery -> purchaseStreakRecovery()
            ShopItem.StreakBet -> purchaseInventory(ShopInventoryItem.STREAK_BET, ShopCatalog.STREAK_BET_COST)
            ShopItem.Hint -> purchaseInventory(ShopInventoryItem.HINT, ShopCatalog.HINT_COST)
            ShopItem.FiftyFifty -> purchaseInventory(ShopInventoryItem.FIFTY_FIFTY, ShopCatalog.FIFTY_FIFTY_COST)
            ShopItem.DoubleXp -> purchaseInventory(ShopInventoryItem.DOUBLE_XP, ShopCatalog.DOUBLE_XP_COST)
            ShopItem.DoubleCoins -> purchaseInventory(ShopInventoryItem.DOUBLE_COINS, ShopCatalog.DOUBLE_COINS_COST)
            ShopItem.SurpriseChest -> openSurpriseChest()
            null -> Unit
        }
    }

    /** Charges [cost] and adds [item] to the user's synchronized inventory. */
    private fun purchaseInventory(item: ShopInventoryItem, cost: Int) {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                if (userRepository.purchaseInventoryItem(item, cost)) _selectedItem.value = null
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Purchases a chest, then keeps its reward private until the full-screen reveal is completed.
     *
     * @return Unit; the reveal state is emitted when the repository confirms the purchase.
     */
    private fun openSurpriseChest() {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val reward = Random.nextInt(ShopCatalog.CHEST_MIN_REWARD, ShopCatalog.CHEST_MAX_REWARD + 1)
                if (userRepository.openSurpriseChest(ShopCatalog.SURPRISE_CHEST_COST, reward)) {
                    _selectedItem.value = null
                    _chestOpening.value = ChestOpening(reward = reward)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Moves a chest reveal forward after a tap, ignoring taps while its opening animation runs.
     *
     * @return Unit; the state becomes opening, reward-revealed, or is cleared after the final tap.
     */
    private fun advanceChestOpening() {
        _chestOpening.update { chest ->
            when (chest?.step) {
                ChestOpeningStep.AWAITING_OPEN -> chest.copy(step = ChestOpeningStep.OPENING)
                ChestOpeningStep.AWAITING_REWARD -> chest.copy(step = ChestOpeningStep.REWARD_REVEALED)
                ChestOpeningStep.REWARD_REVEALED -> null
                ChestOpeningStep.OPENING, null -> chest
            }
        }
    }

    /**
     * Marks the opening animation as finished so the next tap can disclose the reward.
     *
     * @return Unit; no state changes when no chest animation is in progress.
     */
    private fun finishChestOpeningAnimation() {
        _chestOpening.update { chest ->
            if (chest?.step == ChestOpeningStep.OPENING) {
                chest.copy(step = ChestOpeningStep.AWAITING_REWARD)
            } else {
                chest
            }
        }
    }

    /**
     * Buys back the lost streak, closing the purchase sheet when it worked. Guarded by
     * [_isProcessing] like [purchaseStreakFreezer], so a second tap cannot charge twice.
     */
    private fun purchaseStreakRecovery() {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                if (recoverStreakUseCase() != null) _selectedItem.value = null
            } finally {
                _isProcessing.value = false
            }
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

                val success = decrementCoinsUseCase(ShopCatalog.STREAK_FREEZER_COST)
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
