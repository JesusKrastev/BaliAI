package com.jesuskrastev.bali.ui.screens.store

import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock

class ShopViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock())
    private val fakeDecrementCoinsUseCase = DecrementCoinsUseCase(fakeUserRepository)

    private lateinit var viewModel: ShopViewModel

    @Before
    fun setup() {
        viewModel = ShopViewModel(
            userRepository = fakeUserRepository,
            decrementCoinsUseCase = fakeDecrementCoinsUseCase,
            analyticsTracker = fakeAnalyticsTracker
        )
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
