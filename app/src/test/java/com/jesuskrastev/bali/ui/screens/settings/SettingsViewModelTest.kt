package com.jesuskrastev.bali.ui.screens.settings

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.NotificationCategory
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeNotificationsRepository
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
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeSoundEffects = FakeSoundEffects()
    private val fakeNotifications = FakeNotificationsRepository()
    private val analyticsTracker = mock<AnalyticsTracker>()

    private lateinit var viewModel: SettingsViewModel

    /** Built here, not as a property: `viewModelScope` needs the rule's `Dispatchers.Main` to be set already. */
    @Before
    fun setup() {
        viewModel = SettingsViewModel(
            userRepository = FakeUserRepository(),
            authRepository = FakeAuthRepository(),
            analyticsTracker = analyticsTracker,
            soundEffects = fakeSoundEffects,
            notificationsRepository = fakeNotifications
        )
    }

    @Test
    fun `every notification category is on until the user turns it off`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.disabledNotificationCategories).isEmpty()
        collectJob.cancel()
    }

    @Test
    fun `turning a category off is stored, shown and tracked, and leaves the others on`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        viewModel.setNotificationCategoryEnabled(NotificationCategory.PROMOTIONS, enabled = false)

        assertThat(viewModel.uiState.value.disabledNotificationCategories)
            .containsExactly(NotificationCategory.PROMOTIONS)
        verify(analyticsTracker).notificationCategoryChanged("promos", false)
        collectJob.cancel()
    }

    @Test
    fun `turning a category back on removes it from the disabled ones`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        viewModel.setNotificationCategoryEnabled(NotificationCategory.STUDY, enabled = false)
        viewModel.setNotificationCategoryEnabled(NotificationCategory.STUDY, enabled = true)

        assertThat(viewModel.uiState.value.disabledNotificationCategories).isEmpty()
        verify(analyticsTracker).notificationCategoryChanged("study", true)
        collectJob.cancel()
    }

    @Test
    fun `notifications are flagged as blocked when Android blocks them and unflagged when it lets them through`() = runTest {
        fakeNotifications.permissionGranted = false
        viewModel = SettingsViewModel(
            FakeUserRepository(), FakeAuthRepository(), analyticsTracker, fakeSoundEffects, fakeNotifications
        )
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        assertThat(viewModel.uiState.value.notificationsBlocked).isTrue()

        fakeNotifications.permissionGranted = true
        viewModel.refreshNotificationPermission()

        assertThat(viewModel.uiState.value.notificationsBlocked).isFalse()
        collectJob.cancel()
    }

    @Test
    fun `asking for permission from Settings falls back to the system settings`() = runTest {
        viewModel.requestNotificationPermission()

        assertThat(fakeNotifications.permissionRequests).isEqualTo(1)
        assertThat(fakeNotifications.settingsFallbacks).containsExactly(true)
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
