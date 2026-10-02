package com.jesuskrastev.bali.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures the app's bottom bar with Home selected and with the Chat tab selected. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class AppBottomBarScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Renders the bar under a navigation host sitting on [start] and captures it.
     *
     * @param name file name suffix of the capture
     * @param darkTheme whether to use the dark color scheme
     * @param start the destination the host opens on, which is the tab drawn as selected
     */
    private fun capture(name: String, darkTheme: Boolean, start: Any = HomeRoute) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val entry by navController.currentBackStackEntryAsState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    NavHost(navController = navController, startDestination = start) {
                        composable<HomeRoute> {}
                        composable<ChatRoute> {}
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        AppBottomBar(
                            currentDestination = entry?.destination,
                            onHomeClick = {},
                            onGamesClick = {},
                            onSettingsClick = {},
                            onChatClick = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/outputs/roborazzi/bottom_bar_$name.png")
    }

    @Test
    fun light() = capture("light", darkTheme = false)

    @Test
    fun dark() = capture("dark", darkTheme = true)

    @Test
    fun chatSelected() = capture("chat_light", darkTheme = false, start = ChatRoute)

    @Test
    fun chatSelectedDark() = capture("chat_dark", darkTheme = true, start = ChatRoute)
}
