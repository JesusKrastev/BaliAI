package com.jesuskrastev.bali.ui.screens.store

import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.model.ChestReward
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.model.StreakBet
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.RecoverStreakUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class ShopViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeDecrementCoinsUseCase = DecrementCoinsUseCase(fakeUserRepository)

    private val fakeSoundEffects = FakeSoundEffects()

    private lateinit var viewModel: ShopViewModel

    @Before
    fun setup() {
        viewModel = ShopViewModel(
            userRepository = fakeUserRepository,
            decrementCoinsUseCase = fakeDecrementCoinsUseCase,
            recoverStreakUseCase = RecoverStreakUseCase(fakeUserRepository, fakeDecrementCoinsUseCase),
            soundEffects = fakeSoundEffects
        )
    }

    /**
     * Puts the user in the day after a missed one: a streak of [days] lost yesterday.
     *
     * @param days length of the lost streak
     * @param coins the balance to start with
     */
    private suspend fun loseStreakYesterday(days: Int, coins: Int) {
        val twoDaysAgo = DailyStreak.startOfDayMillis(DailyStreak.epochDay(System.currentTimeMillis()) - 2)
        fakeUserRepository.insert(
            User(
                name = "Jesus",
                coins = coins,
                currentStreak = days,
                highestStreak = days,
                lastPracticeTimestamp = twoDaysAgo,
                practiceDays = listOf(twoDaysAgo)
            )
        )
    }

    @Test
    fun `the shop offers the streak lost yesterday`() = runTest {
        loseStreakYesterday(days = 9, coins = 500)

        val state = viewModel.uiState.first { it.recoverableStreak > 0 }

        assertThat(state.recoverableStreak).isEqualTo(9)
    }

    @Test
    fun `buying the recovery restores the streak and charges its price`() = runTest {
        loseStreakYesterday(days = 9, coins = 500)
        viewModel.uiState.first { it.recoverableStreak > 0 }

        viewModel.onEvent(ShopEvent.PurchaseStreakRecovery)

        val user = fakeUserRepository.get().first()!!
        assertThat(user.currentStreak).isEqualTo(9)
        assertThat(user.frozenDays).hasSize(1)
        assertThat(user.coins).isEqualTo(500 - DailyStreak.RECOVERY_COST_COINS)
        assertThat(viewModel.uiState.first { it.recoverableStreak == 0 }.selectedItem).isNull()
    }

    @Test
    fun `the recovery is not sold when the balance falls short`() = runTest {
        loseStreakYesterday(days = 9, coins = DailyStreak.RECOVERY_COST_COINS - 1)
        viewModel.uiState.first { it.recoverableStreak > 0 }

        viewModel.onEvent(ShopEvent.PurchaseStreakRecovery)

        val user = fakeUserRepository.get().first()!!
        assertThat(user.frozenDays).isEmpty()
        assertThat(user.coins).isEqualTo(DailyStreak.RECOVERY_COST_COINS - 1)
        assertThat(viewModel.uiState.value.recoverableStreak).isEqualTo(9)
    }

    @Test
    fun `shop items are loaded correctly`() = runTest {
        assertThat(viewModel.uiState.value).isNotNull()
    }

    @Test
    fun `user coins are displayed`() = runTest {
        assertThat(viewModel.uiState.value.coinsCount).isAtLeast(0)
    }

    /** Verifies a successful inventory purchase emits a celebratory confirmation. */
    @Test
    fun `buying a hint charges coins and adds it to the practice inventory`() = runTest {
        fakeUserRepository.insert(User(coins = 100))

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.Hint))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)

        val user = fakeUserRepository.get().first { it?.hints == 1 }!!
        assertThat(user.coins).isEqualTo(100 - ShopCatalog.HINT_COST)
        assertThat(viewModel.uiState.value.selectedItem).isNull()
        val feedback = viewModel.uiState.first { it.purchaseFeedback?.isSuccess == true }.purchaseFeedback!!
        assertThat(feedback.message).contains("Pista conseguida")
    }

    /** A profile with a streak of [days] days that includes today, so a bet can be placed. */
    private suspend fun studiedToday(days: Int, coins: Int) {
        val today = DailyStreak.startOfDayMillis(DailyStreak.epochDay(System.currentTimeMillis()))
        fakeUserRepository.insert(
            User(
                coins = coins,
                currentStreak = days,
                highestStreak = days,
                lastPracticeTimestamp = today,
                practiceDays = listOf(today)
            )
        )
    }

    @Test
    fun `a streak bet charges its stake and aims seven days past the current streak`() = runTest {
        studiedToday(days = 3, coins = 100)

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakBet))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)

        val user = fakeUserRepository.get().first()!!
        assertThat(user.coins).isEqualTo(100 - StreakBet.COST_COINS)
        assertThat(user.streakBetTarget).isEqualTo(3 + StreakBet.DAYS)
        val state = viewModel.uiState.first { it.hasActiveStreakBet }
        assertThat(state.streakBetDaysDone).isEqualTo(0)
        assertThat(state.canBetOnStreak).isFalse()
    }

    @Test
    fun `a streak bet is not sold without a running streak`() = runTest {
        fakeUserRepository.insert(User(coins = 100))

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakBet))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)

        val user = fakeUserRepository.get().first()!!
        assertThat(user.coins).isEqualTo(100)
        assertThat(user.streakBetTarget).isEqualTo(0)
        assertThat(viewModel.uiState.first { it.coinsCount == 100 }.canBetOnStreak).isFalse()
    }

    @Test
    fun `a second streak bet is not sold while one is running`() = runTest {
        studiedToday(days = 3, coins = 200)

        repeat(2) {
            viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakBet))
            viewModel.onEvent(ShopEvent.ConfirmPurchase)
        }

        assertThat(fakeUserRepository.get().first()!!.coins).isEqualTo(200 - StreakBet.COST_COINS)
    }

    @Test
    fun `a streak bet whose streak was lost is no longer shown as running`() = runTest {
        val today = DailyStreak.startOfDayMillis(DailyStreak.epochDay(System.currentTimeMillis()))
        val longAgo = today - 10 * 24 * 60 * 60 * 1000L
        // A bet placed at a streak of 5, which was then lost: the streak is 0 today.
        fakeUserRepository.insert(
            User(
                coins = 100,
                currentStreak = 5,
                highestStreak = 5,
                streakBetTarget = 5 + StreakBet.DAYS,
                lastPracticeTimestamp = longAgo,
                practiceDays = listOf(longAgo)
            )
        )
        val state = viewModel.uiState.first { it.coinsCount == 100 }
        assertThat(state.hasActiveStreakBet).isFalse()
        // Nothing to bet on until the streak starts again.
        assertThat(state.canBetOnStreak).isFalse()
    }

    /** Verifies the chest sound plays when the lid opens, not when the chest is bought. */
    @Test
    fun `the chest sound plays only when the overlay reports the lid opening`() = runTest {
        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest))
        assertThat(fakeSoundEffects.chestOpenPlays).isEqualTo(0)

        viewModel.onEvent(ShopEvent.ChestOpening)

        assertThat(fakeSoundEffects.chestOpenPlays).isEqualTo(1)
    }

    /** Verifies one chest purchase charges once and persists whichever weighted reward was rolled. */
    @Test
    fun `opening the surprise chest charges its price and grants its reward once`() = runTest {
        fakeUserRepository.insert(User(coins = 100))

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)

        val state = viewModel.uiState.first { it.chestReward != null }
        val reward = state.chestReward!!
        val user = fakeUserRepository.get().first()!!
        when (reward) {
            is ChestReward.Coins ->
                assertThat(user.coins).isEqualTo(100 - ShopCatalog.SURPRISE_CHEST_COST + reward.amount)
            is ChestReward.Inventory -> {
                assertThat(user.coins).isEqualTo(100 - ShopCatalog.SURPRISE_CHEST_COST)
                val owned = when (reward.item) {
                    ShopInventoryItem.HINT -> user.hints
                    ShopInventoryItem.FIFTY_FIFTY -> user.fiftyFifties
                    ShopInventoryItem.DOUBLE_XP -> user.doubleXpBoosts
                    ShopInventoryItem.DOUBLE_COINS -> user.doubleCoinBoosts
                }
                assertThat(owned).isEqualTo(reward.quantity)
            }
        }
        assertThat(state.purchaseFeedback).isNull()
        assertThat(state.selectedItem).isNull()
    }

    @Test
    fun `the chest prize stays on screen until the animation is dismissed`() = runTest {
        fakeUserRepository.insert(User(coins = 100))
        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)
        assertThat(viewModel.uiState.first { it.chestReward != null }.chestReward).isNotNull()

        viewModel.onEvent(ShopEvent.DismissChest)

        assertThat(viewModel.uiState.first { it.chestReward == null }.chestReward).isNull()
    }

    @Test
    fun `a chest the user cannot afford shows no animation and charges nothing`() = runTest {
        fakeUserRepository.insert(User(coins = ShopCatalog.SURPRISE_CHEST_COST - 1))

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)

        val state = viewModel.uiState.first()
        assertThat(state.chestReward).isNull()
        assertThat(state.coinsCount).isEqualTo(ShopCatalog.SURPRISE_CHEST_COST - 1)
    }

    @Test
    fun `a purchase that cannot reach the server reports it instead of crashing`() = runTest {
        fakeUserRepository.insert(User(coins = 100))
        val offline = object : UserRepository by fakeUserRepository {
            override suspend fun purchaseInventoryItem(item: ShopInventoryItem, cost: Int): Boolean =
                throw IOException("offline")
        }
        val offlineViewModel = ShopViewModel(
            userRepository = offline,
            decrementCoinsUseCase = fakeDecrementCoinsUseCase,
            recoverStreakUseCase = RecoverStreakUseCase(offline, fakeDecrementCoinsUseCase),
            soundEffects = fakeSoundEffects
        )

        offlineViewModel.onEvent(ShopEvent.SelectItem(ShopItem.Hint))
        offlineViewModel.onEvent(ShopEvent.ConfirmPurchase)

        val state = offlineViewModel.uiState.first { it.purchaseFeedback != null }
        assertThat(state.purchaseFeedback?.message).contains("No se ha podido completar la compra")
        assertThat(state.purchaseFeedback?.isSuccess).isFalse()
        assertThat(state.isProcessing).isFalse()
        assertThat(fakeUserRepository.get().first()!!.coins).isEqualTo(100)
    }
}
