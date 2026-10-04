package com.jesuskrastev.bali.ui.screens.settings

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.EnablePushesResult
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
import org.mockito.kotlin.never
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
    fun `a channel the user turned off in Android shows as off once Settings is opened again`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        fakeNotifications.disabledCategories.add(NotificationCategory.PROMOTIONS)
        viewModel.refreshNotificationChannels()

        assertThat(viewModel.uiState.value.disabledNotificationCategories)
            .containsExactly(NotificationCategory.PROMOTIONS)
        collectJob.cancel()
    }

    @Test
    fun `a channel turned back on stops showing as off`() = runTest {
        fakeNotifications.disabledCategories.add(NotificationCategory.STREAK)
        viewModel = SettingsViewModel(
            FakeUserRepository(), FakeAuthRepository(), analyticsTracker, fakeSoundEffects, fakeNotifications
        )
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        assertThat(viewModel.uiState.value.disabledNotificationCategories)
            .containsExactly(NotificationCategory.STREAK)

        fakeNotifications.disabledCategories.clear()
        viewModel.refreshNotificationChannels()

        assertThat(viewModel.uiState.value.disabledNotificationCategories).isEmpty()
        collectJob.cancel()
    }

    @Test
    fun `the switch follows Android and the opt-out without waiting for a refresh`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        assertThat(viewModel.uiState.value.notificationsEnabled).isTrue()

        fakeNotifications.pushesAllowed.value = false
        assertThat(viewModel.uiState.value.notificationsEnabled).isFalse()

        fakeNotifications.pushesAllowed.value = true
        assertThat(viewModel.uiState.value.notificationsEnabled).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `enabling notifications from Settings lets them through and logs it apart from onboarding`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        var settingsOpened = 0
        val eventsJob = launch(UnconfinedTestDispatcher()) {
            viewModel.openSystemNotificationSettings.collect { settingsOpened++ }
        }
        fakeNotifications.pushesAllowed.value = false

        viewModel.setNotificationsEnabled(true)

        assertThat(fakeNotifications.enableRequests).isEqualTo(1)
        assertThat(viewModel.uiState.value.notificationsEnabled).isTrue()
        assertThat(settingsOpened).isEqualTo(0)
        verify(analyticsTracker).notificationsPermissionAnswered("granted", null, "settings")
        eventsJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun `when Android will not ask again, Settings opens the system settings instead`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        var settingsOpened = 0
        val eventsJob = launch(UnconfinedTestDispatcher()) {
            viewModel.openSystemNotificationSettings.collect { settingsOpened++ }
        }
        fakeNotifications.pushesAllowed.value = false
        fakeNotifications.enableResult = EnablePushesResult.NEEDS_SYSTEM_SETTINGS

        viewModel.setNotificationsEnabled(true)

        assertThat(settingsOpened).isEqualTo(1)
        assertThat(viewModel.uiState.value.notificationsEnabled).isFalse()
        verify(analyticsTracker).notificationsPermissionAnswered("system_settings", null, "settings")
        eventsJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun `a no to the system dialog keeps notifications off and opens nothing else`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        var settingsOpened = 0
        val eventsJob = launch(UnconfinedTestDispatcher()) {
            viewModel.openSystemNotificationSettings.collect { settingsOpened++ }
        }
        fakeNotifications.pushesAllowed.value = false
        fakeNotifications.enableResult = EnablePushesResult.DENIED

        viewModel.setNotificationsEnabled(true)

        assertThat(settingsOpened).isEqualTo(0)
        assertThat(viewModel.uiState.value.notificationsEnabled).isFalse()
        verify(analyticsTracker).notificationsPermissionAnswered("denied", null, "settings")
        eventsJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun `switching notifications off opts the install out and logs it`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        viewModel.setNotificationsEnabled(false)

        assertThat(fakeNotifications.optedOut).isTrue()
        assertThat(viewModel.uiState.value.notificationsEnabled).isFalse()
        assertThat(fakeNotifications.enableRequests).isEqualTo(0)
        verify(analyticsTracker).notificationsSwitchedOff()
        collectJob.cancel()
    }

    @Test
    fun `switching notifications back on after turning them off lets them through again`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        viewModel.setNotificationsEnabled(false)

        viewModel.setNotificationsEnabled(true)

        assertThat(viewModel.uiState.value.notificationsEnabled).isTrue()
        verify(analyticsTracker).notificationsPermissionAnswered("granted", null, "settings")
        verify(analyticsTracker, never()).notificationsPermissionAnswered("denied", null, "settings")
        collectJob.cancel()
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
