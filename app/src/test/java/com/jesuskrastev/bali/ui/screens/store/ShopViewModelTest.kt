package com.jesuskrastev.bali.ui.screens.store

import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.RecoverStreakUseCase
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
}
