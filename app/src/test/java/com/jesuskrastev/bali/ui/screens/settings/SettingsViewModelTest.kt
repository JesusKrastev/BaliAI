package com.jesuskrastev.bali.ui.screens.settings

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeSoundEffects = FakeSoundEffects()

    private lateinit var viewModel: SettingsViewModel

    /** Built here, not as a property: `viewModelScope` needs the rule's `Dispatchers.Main` to be set already. */
    @Before
    fun setup() {
        viewModel = SettingsViewModel(
            userRepository = FakeUserRepository(),
            authRepository = FakeAuthRepository(),
            analyticsTracker = mock<AnalyticsTracker>(),
            soundEffects = fakeSoundEffects
        )
    }

    @Test
    fun `sounds are on by default`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.soundsEnabled).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `turning sounds off is stored and shown, and plays nothing`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        viewModel.setSoundsEnabled(false)

        assertThat(fakeSoundEffects.isEnabled.first()).isFalse()
        assertThat(viewModel.uiState.value.soundsEnabled).isFalse()
        assertThat(fakeSoundEffects.correctPlays).isEqualTo(0)
        collectJob.cancel()
    }

    @Test
    fun `turning sounds back on plays the correct chime once as a preview`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        viewModel.setSoundsEnabled(false)
        viewModel.setSoundsEnabled(true)

        assertThat(viewModel.uiState.value.soundsEnabled).isTrue()
        assertThat(fakeSoundEffects.correctPlays).isEqualTo(1)
        collectJob.cancel()
    }
}
