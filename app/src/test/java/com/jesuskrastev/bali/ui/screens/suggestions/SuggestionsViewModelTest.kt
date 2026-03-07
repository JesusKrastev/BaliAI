package com.jesuskrastev.bali.ui.screens.suggestions

import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeSuggestionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class SuggestionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeSuggestionsRepository = FakeSuggestionsRepository()
    private val fakeAuthRepository = FakeAuthRepository()
    private lateinit var viewModel: SuggestionsViewModel

    @Before
    fun setup() {
        viewModel = SuggestionsViewModel(
            suggestionsRepository = fakeSuggestionsRepository,
            authRepository = fakeAuthRepository
        )
    }

    @Test
    fun `suggestions uiState initializes correctly`() = runTest {
        assertThat(viewModel.uiState.value).isNotNull()
        assertThat(viewModel.uiState.value.suggestion).isEmpty()
    }

    @Test
    fun `suggestion content updates correctly`() = runTest {
        viewModel.onSuggestionChange("New suggestion")
        assertThat(viewModel.uiState.value.suggestion).isEqualTo("New suggestion")
    }
}
