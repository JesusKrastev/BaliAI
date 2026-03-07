package com.jesuskrastev.bali.ui.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot test for AuthScreen using Roborazzi
 * Run with: ./gradlew recordRoborazziDebug
 * Verify with: ./gradlew verifyRoborazziDebug
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
@RunWith(RobolectricTestRunner::class)
class AuthScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureAuthScreen() {
        composeTestRule.setContent {
            // Simple placeholder - put actual AuthScreen here
            Text("Auth Screen Placeholder")
        }

        composeTestRule.onRoot()
            .captureRoboImage()
    }
}
