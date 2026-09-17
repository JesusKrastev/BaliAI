package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingEvent
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingViewModel
import com.jesuskrastev.bali.ui.screens.onboarding.components.MascotHeader
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot test for the selector-step body (mascot header + [StepSelectorList]), reproducing
 * the layout [OnboardingScreen]'s `OnboardingBody` builds around it. Regression coverage for the
 * options opening a blank gap under the mascot bubble, and for long labels (with composed
 * emoji) being clipped by a fixed card height on a narrow 360dp phone.
 *
 * Run with: ./gradlew recordRoborazziDebug
 * Verify with: ./gradlew verifyRoborazziDebug
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
@RunWith(RobolectricTestRunner::class)
class StepSelectorListScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Captures the concern step, whose labels are long enough to wrap onto two lines. */
    @Test
    fun captureLongOptions() {
        captureSelectorStep(
            message = "¿Qué es lo que más te preocupa del examen?",
            options = OnboardingConfig.concerns
        )
    }

    /** Captures the blocker step, which includes a ZWJ emoji sequence (😵‍💫). */
    @Test
    fun captureComposedEmojiOptions() {
        captureSelectorStep(
            message = "¿Qué es lo que más te frena ahora mismo?",
            options = OnboardingConfig.theoryBlockers
        )
    }

    /**
     * Renders a selector step inside the same scaffolding the onboarding body uses and captures it.
     *
     * @param message question shown in the mascot bubble
     * @param options option labels rendered as cards
     */
    private fun captureSelectorStep(message: String, options: List<String>) {
        composeTestRule.setContent {
            BaliTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            MascotHeader(
                                message = message,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedVisibility,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                                StepSelectorList(
                                    options = options,
                                    viewModel = mock<OnboardingViewModel>(),
                                    onSelect = { _: String, _: OnboardingEvent -> }
                                )
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
