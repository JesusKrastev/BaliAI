package com.jesuskrastev.bali.ui.screens.test

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures of the "preparing your questions" screen in both themes. The class name ends in
 * `ScreenshotTest` because the release build only keeps those (brain E-021).
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class LoadingViewScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureLoadingView_light() {
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) { LoadingView() }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureLoadingView_dark() {
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) { LoadingView() }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
