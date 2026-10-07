package com.jesuskrastev.bali.ui.screens.test

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
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

    /** Verifies that only inventory aids available for the current question are rendered. */
    @Test
    fun practiceAidsOnlyShowItemsThatCanBeUsed() {
        composeTestRule.setContent {
            MaterialTheme {
                PracticeAids(
                    hints = 2,
                    fiftyFifties = 0,
                    isHintVisible = false,
                    isFiftyFiftyUsed = false,
                    onUseHint = {},
                    onUseFiftyFifty = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Usar pista; 2 disponibles").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Usar 50/50; 0 disponibles").assertDoesNotExist()
    }

    /** Verifies that the compact aid row is absent when the user owns no usable inventory. */
    @Test
    fun practiceAidsAreHiddenWithoutUsableInventory() {
        composeTestRule.setContent {
            MaterialTheme {
                PracticeAids(
                    hints = 0,
                    fiftyFifties = 0,
                    isHintVisible = false,
                    isFiftyFiftyUsed = false,
                    onUseHint = {},
                    onUseFiftyFifty = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Usar pista; 0 disponibles").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Usar 50/50; 0 disponibles").assertDoesNotExist()
    }
}
