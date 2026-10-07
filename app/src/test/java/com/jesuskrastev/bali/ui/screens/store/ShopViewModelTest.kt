package com.jesuskrastev.bali.ui.screens.store

import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.RecoverStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class ShopViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeDecrementCoinsUseCase = DecrementCoinsUseCase(fakeUserRepository)

    private lateinit var viewModel: ShopViewModel

    @Before
    fun setup() {
        viewModel = ShopViewModel(
            userRepository = fakeUserRepository,
            decrementCoinsUseCase = fakeDecrementCoinsUseCase,
            recoverStreakUseCase = RecoverStreakUseCase(fakeUserRepository, fakeDecrementCoinsUseCase)
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

    @Test
    fun `buying a hint charges coins and adds it to the practice inventory`() = runTest {
        fakeUserRepository.insert(User(coins = 100))

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.Hint))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)

        val user = fakeUserRepository.get().first { it?.hints == 1 }!!
        assertThat(user.coins).isEqualTo(100 - ShopCatalog.HINT_COST)
        assertThat(viewModel.uiState.value.selectedItem).isNull()
    }

    /** Verifies that purchasing a chest begins its interactive reveal without exposing its reward. */
    @Test
    fun `buying a chest starts its full screen reveal`() = runTest {
        fakeUserRepository.insert(User(coins = 100))

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)

        val chest = viewModel.uiState.first { it.chestOpening != null }.chestOpening!!
        val user = fakeUserRepository.get().first()!!
        assertThat(chest.reward).isIn(ShopCatalog.CHEST_MIN_REWARD..ShopCatalog.CHEST_MAX_REWARD)
        assertThat(chest.step).isEqualTo(ChestOpeningStep.AWAITING_OPEN)
        assertThat(user.coins).isEqualTo(100 - ShopCatalog.SURPRISE_CHEST_COST + chest.reward)
        assertThat(viewModel.uiState.value.selectedItem).isNull()
    }

    /** Verifies the chest only reveals its reward after the opening animation has completed. */
    @Test
    fun `chest reveal follows open animation reward and return stages`() = runTest {
        fakeUserRepository.insert(User(coins = 100))
        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)
        viewModel.uiState.first { it.chestOpening != null }

        viewModel.onEvent(ShopEvent.AdvanceChestOpening)
        assertThat(viewModel.uiState.value.chestOpening?.step).isEqualTo(ChestOpeningStep.OPENING)

        viewModel.onEvent(ShopEvent.AdvanceChestOpening)
        assertThat(viewModel.uiState.value.chestOpening?.step).isEqualTo(ChestOpeningStep.OPENING)

        viewModel.onEvent(ShopEvent.ChestOpeningAnimationFinished)
        viewModel.onEvent(ShopEvent.AdvanceChestOpening)
        assertThat(viewModel.uiState.value.chestOpening?.step).isEqualTo(ChestOpeningStep.REWARD_REVEALED)

        viewModel.onEvent(ShopEvent.AdvanceChestOpening)
        assertThat(viewModel.uiState.value.chestOpening).isNull()
    }

    @Test
    fun `a streak bet pays once after the next new study day`() = runTest {
        val today = DailyStreak.startOfDayMillis(DailyStreak.epochDay(System.currentTimeMillis()))
        fakeUserRepository.insert(
            User(
                coins = 100,
                currentStreak = 2,
                highestStreak = 2,
                lastPracticeTimestamp = today - 24 * 60 * 60 * 1000L,
                practiceDays = listOf(today - 24 * 60 * 60 * 1000L)
            )
        )

        viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakBet))
        viewModel.onEvent(ShopEvent.ConfirmPurchase)
        IncrementStreakUseCase(fakeUserRepository)()

        val user = fakeUserRepository.get().first { it?.activeStreakBet == false && it.coins == 150 }!!
        assertThat(user.currentStreak).isEqualTo(3)
    }
}
