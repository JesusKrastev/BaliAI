package com.jesuskrastev.bali.ui.screens.test

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

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class TestScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureTestScreen() {
        composeTestRule.setContent {
            Text("Test Screen Placeholder")
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureTestResultScreen() {
        composeTestRule.setContent {
            Text("Test Result Screen Placeholder")
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
