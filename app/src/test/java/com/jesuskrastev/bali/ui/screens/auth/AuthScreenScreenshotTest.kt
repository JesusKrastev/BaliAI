package com.jesuskrastev.bali.ui.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
 * Screenshot test for AuthScreen using Roborazzi
 * Run with: ./gradlew recordRoborazziDebug
 * Verify with: ./gradlew verifyRoborazziDebug
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp")
@RunWith(RobolectricTestRunner::class)
class AuthScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureAuthScreen_optional() {
        composeTestRule.setContent {
            BaliTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        AuthScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onBackClick = {},
                            onLoginClick = {},
                            isLoggingIn = false,
                            errorMessage = null,
                            errorEmail = null,
                            onErrorDismiss = {},
                            isMandatory = false
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureAuthScreen_mandatory() {
        composeTestRule.setContent {
            BaliTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        AuthScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onBackClick = {},
                            onLoginClick = {},
                            isLoggingIn = false,
                            errorMessage = null,
                            errorEmail = null,
                            onErrorDismiss = {},
                            isMandatory = true
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureAuthScreen_darkTheme() {
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        AuthScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onBackClick = {},
                            onLoginClick = {},
                            isLoggingIn = false,
                            errorMessage = null,
                            errorEmail = null,
                            onErrorDismiss = {},
                            isMandatory = false
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureAuthScreen_unknownAccount() {
        composeTestRule.setContent {
            BaliTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        AuthScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onBackClick = {},
                            onLoginClick = {},
                            isLoggingIn = false,
                            errorMessage = null,
                            errorEmail = "conductor@example.com",
                            onErrorDismiss = {},
                            isMandatory = false
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
