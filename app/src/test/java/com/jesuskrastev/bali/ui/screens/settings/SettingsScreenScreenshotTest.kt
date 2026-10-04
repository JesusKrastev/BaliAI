package com.jesuskrastev.bali.ui.screens.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeNotificationsRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.theme.BaliTheme
import com.jesuskrastev.bali.util.FakeSoundEffects
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures of Settings with the notifications switch on (the channel rows below it) and off
 * (they fold away). Tall enough to show the subscription row above "Cerrar sesión". The class
 * name ends in `ScreenshotTest` because the release build only keeps those (brain E-021).
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h1900dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class SettingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureSettings_notificationsOn() = capture(pushesAllowed = true)

    @Test
    fun captureSettings_notificationsOff() = capture(pushesAllowed = false)

    /**
     * Renders Settings for a signed-in user over fake repositories.
     *
     * @param pushesAllowed whether notifications reach the install
     */
    private fun capture(pushesAllowed: Boolean) {
        val notifications = FakeNotificationsRepository().apply { this.pushesAllowed.value = pushesAllowed }
        val viewModel = SettingsViewModel(
            userRepository = FakeUserRepository(),
            authRepository = FakeAuthRepository(isLoggedIn = true),
            analyticsTracker = mock<AnalyticsTracker>(),
            soundEffects = FakeSoundEffects(),
            notificationsRepository = notifications
        )
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) { SettingsScreen(viewModel = viewModel) }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }
}
