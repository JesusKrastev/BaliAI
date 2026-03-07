package com.jesuskrastev.bali.ui.screens.streak

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
class StreakScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureStreakScreen() {
        composeTestRule.setContent {
            Text("Streak Screen Placeholder")
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureLessonStreakScreen() {
        composeTestRule.setContent {
            Text("Lesson Streak Screen Placeholder")
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
