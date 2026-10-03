package com.jesuskrastev.bali.ui.screens.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.ChestReward
import com.jesuskrastev.bali.domain.model.ChestRewardTable
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.model.StreakBet
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.RecoverStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    data object DismissFeedback : ShopEvent()
    data object DismissChest : ShopEvent()
}

/**
 * Coin prices and payout rules for the shop's products. The streak bet's own numbers live in
 * [StreakBet] and the recovery's in [DailyStreak.RECOVERY_COST_COINS].
 */
object ShopCatalog {
    const val STREAK_FREEZER_COST = 120
    const val HINT_COST = 30
    const val FIFTY_FIFTY_COST = 45
    const val SURPRISE_CHEST_COST = 60
    const val DOUBLE_XP_COST = 80
    const val DOUBLE_COINS_COST = 70

}

/** Shown when a purchase could not be completed, usually for lack of connection. */
private const val PURCHASE_FAILED_MESSAGE =
    "No se ha podido completar la compra. Comprueba tu conexión e inténtalo de nuevo."

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
 * @property hasActiveStreakBet true while a streak bet is running and not lost
 * @property streakBetDaysDone study days completed since the bet was placed, 0 without a bet
 * @property canBetOnStreak true when a bet could be placed now: no bet running and a streak to bet on
 * @property purchaseFeedback one-time message about a purchase that failed
 * @property chestReward reward granted by a paid surprise chest while its opening animation is on
 *   screen; null when no chest is being opened
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
    val streakBetDaysDone: Int = 0,
    val canBetOnStreak: Boolean = false,
    val purchaseFeedback: String? = null,
    val chestReward: ChestReward? = null
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val decrementCoinsUseCase: DecrementCoinsUseCase,
    private val recoverStreakUseCase: RecoverStreakUseCase
) : ViewModel() {

    private val _selectedItem = MutableStateFlow<ShopItem?>(null)
    private val _isProcessing = MutableStateFlow(false)
    private val _purchaseFeedback = MutableStateFlow<String?>(null)
    private val _chestReward = MutableStateFlow<ChestReward?>(null)

    val uiState: StateFlow<ShopUiState> = combine(
        userRepository.get(),
        _selectedItem,
        _isProcessing,
        _purchaseFeedback,
        _chestReward
    ) { user, selected, isProcessing, purchaseFeedback, chestReward ->
        user?.let {
            val now = System.currentTimeMillis()
            val settledStreak = DailyStreak.of(it).settledAt(now)
            val betRunning =
                it.streakBetTarget > 0 && !StreakBet.isLost(it.streakBetTarget, settledStreak.current)
            ShopUiState(
                coinsCount = it.coins,
                streakFreezes = it.streakFreezes,
                recoverableStreak = settledStreak.recoverableStreakAt(now),
                selectedItem = selected,
                isProcessing = isProcessing,
                hints = it.hints,
                fiftyFifties = it.fiftyFifties,
                doubleXpBoosts = it.doubleXpBoosts,
                doubleCoinBoosts = it.doubleCoinBoosts,
                hasActiveStreakBet = betRunning,
                streakBetDaysDone =
                    if (betRunning) StreakBet.daysDone(it.streakBetTarget, settledStreak.current) else 0,
                canBetOnStreak = !betRunning && StreakBet.canBetOn(settledStreak.current),
                purchaseFeedback = purchaseFeedback,
                chestReward = chestReward
            )
        } ?: ShopUiState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopUiState()
    )

    /** Reduces a shop interaction [event] into selection, feedback or one guarded purchase. */
    fun onEvent(event: ShopEvent) {
        when (event) {
            is ShopEvent.SelectItem -> {
                _selectedItem.value = event.item
            }
            ShopEvent.DismissSelection -> _selectedItem.value = null
            ShopEvent.PurchaseStreakFreezer -> purchaseStreakFreezer()
            ShopEvent.PurchaseStreakRecovery -> purchaseStreakRecovery()
            ShopEvent.ConfirmPurchase -> purchaseSelectedItem()
            ShopEvent.DismissFeedback -> _purchaseFeedback.value = null
            ShopEvent.DismissChest -> _chestReward.value = null
        }
    }

    /** Purchases the item currently selected in the confirmation sheet. */
    private fun purchaseSelectedItem() {
        when (_selectedItem.value) {
            ShopItem.StreakFreezer -> purchaseStreakFreezer()
            ShopItem.StreakRecovery -> purchaseStreakRecovery()
            ShopItem.StreakBet -> purchaseStreakBet()
            ShopItem.Hint -> purchaseInventory(ShopInventoryItem.HINT, ShopCatalog.HINT_COST)
            ShopItem.FiftyFifty -> purchaseInventory(ShopInventoryItem.FIFTY_FIFTY, ShopCatalog.FIFTY_FIFTY_COST)
            ShopItem.DoubleXp -> purchaseInventory(ShopInventoryItem.DOUBLE_XP, ShopCatalog.DOUBLE_XP_COST)
            ShopItem.DoubleCoins -> purchaseInventory(ShopInventoryItem.DOUBLE_COINS, ShopCatalog.DOUBLE_COINS_COST)
            ShopItem.SurpriseChest -> openSurpriseChest()
            null -> Unit
        }
    }

    /**
     * Runs one purchase with the guards every purchase needs: a second tap while one is in
     * flight is ignored (so nothing is charged twice), and a failure such as no connection
     * becomes a message instead of crashing the app.
     *
     * @param purchase the purchase itself; it closes the sheet when it worked
     */
    private fun runPurchase(purchase: suspend () -> Unit) {
        if (_isProcessing.value) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                purchase()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _purchaseFeedback.value = PURCHASE_FAILED_MESSAGE
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Charges [cost] and adds [item] to the user's synchronized inventory. */
    private fun purchaseInventory(item: ShopInventoryItem, cost: Int) = runPurchase {
        if (userRepository.purchaseInventoryItem(item, cost)) _selectedItem.value = null
    }

    /**
     * Opens a paid chest and keeps its prize in [uiState] until the UI has played the opening
     * animation and the user dismisses it with [ShopEvent.DismissChest].
     */
    private fun openSurpriseChest() = runPurchase {
        val reward = ChestRewardTable.roll()
        if (userRepository.openSurpriseChest(ShopCatalog.SURPRISE_CHEST_COST, reward)) {
            _selectedItem.value = null
            _chestReward.value = reward
        }
    }

    /**
     * Places the streak bet when there is a streak to bet on and no bet is running. A bet whose
     * streak was already lost is forgotten first, so the user can bet again.
     */
    private fun purchaseStreakBet() = runPurchase {
        val user = userRepository.get().first() ?: return@runPurchase
        val settledStreak = DailyStreak.of(user).settledAt(System.currentTimeMillis()).current
        if (!StreakBet.canBetOn(settledStreak)) return@runPurchase
        if (StreakBet.isLost(user.streakBetTarget, settledStreak)) {
            userRepository.clearStreakBet()
        } else if (user.streakBetTarget > 0) {
            return@runPurchase
        }
        if (userRepository.placeStreakBet(StreakBet.COST_COINS, StreakBet.targetFor(settledStreak))) {
            _selectedItem.value = null
        }
    }

    /**
     * Buys back the lost streak, closing the purchase sheet when it worked. Guarded like every
     * purchase, so a second tap cannot charge twice.
     */
    private fun purchaseStreakRecovery() = runPurchase {
        if (recoverStreakUseCase() != null) _selectedItem.value = null
    }

    /**
     * Buys a streak freezer for [ShopCatalog.STREAK_FREEZER_COST] coins, capped at
     * [DailyStreak.MAX_FREEZES] owned at once.
     *
     * Guarded by [runPurchase] so a second tap while a purchase is already in flight is ignored
     * instead of racing it: two concurrent calls could otherwise both pass the limit check before
     * either write lands, charging twice for one freezer.
     */
    private fun purchaseStreakFreezer() = runPurchase {
        val user = userRepository.get().first() ?: return@runPurchase
        if (user.streakFreezes >= DailyStreak.MAX_FREEZES) return@runPurchase

        if (decrementCoinsUseCase(ShopCatalog.STREAK_FREEZER_COST)) {
            userRepository.updateStreakFreezes(user.streakFreezes + 1)
            _selectedItem.value = null
        }
    }
}
