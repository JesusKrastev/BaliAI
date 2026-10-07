package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.ui.screens.games.GamesScreen
import com.jesuskrastev.bali.ui.screens.games.GameType
import com.jesuskrastev.bali.ui.theme.BaliTheme
import com.jesuskrastev.bali.util.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h640dp")
/** Semantic and visual checks use the debug-only Compose test activity manifest. */
class BaliDriveScreenScreenshotTest {
    @get:Rule(order = 0) val mainDispatcherRule = MainDispatcherRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    /** The sole entry has a visible CTA which chooses Bali Drive. */
    @Test fun gamesOffersOnlyDrive() {
        var selected: GameType? = null
        compose.mainClock.autoAdvance = true
        compose.setContent { BaliTheme { GamesScreen(onGameClick = { selected = it }) } }
        compose.onNodeWithText("BALI DRIVE").assertIsDisplayed()
        compose.onNodeWithText("Jugar").assertIsDisplayed().performClick()
        assertThat(selected).isEqualTo(GameType.DRIVE)
        compose.onNodeWithText("Puntos del Carné").assertDoesNotExist()
        compose.onNodeWithText("Todos").assertDoesNotExist()
    }

    /** The coach card names the gesture being taught for each lesson. */
    @Test fun coachNamesEachGesture() {
        var step by mutableStateOf(CoachStep.STEER)
        compose.mainClock.autoAdvance = true
        compose.setContent { BaliTheme { Box { DriveCoachOverlay(step) } } }
        compose.onNodeWithText("¡DESLIZA!").assertExists()
        step = CoachStep.BRAKE
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("¡FRENA!").assertExists()
        step = CoachStep.DRIVE
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("¡FRENA!").assertDoesNotExist()
    }

    /** Pending rewards stay explicit, and both actions remain visible on a narrow display. */
    @Test fun pendingResultsDisableReplayAndExit() {
        compose.setContent { BaliTheme { DriveResultsDialog(
            DriveResult(DriveSummary(1000, 2, 3, listOf("STOP: detente"), 4, 7, 55), 2000), {}, {}, {}) } }
        compose.onNodeWithText("Otra vez").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Salir").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Guardando partida y recompensas…").performScrollTo().assertIsDisplayed()
    }

    /** Finished save failures unlock replay and exit without hiding fault explanations. */
    @Test fun resultActionsAndFaultsRemainAccessible() {
        var replayed = false
        var exited = false
        compose.setContent { BaliTheme { DriveResultsDialog(
            DriveResult(DriveSummary(1000, 2, 3, listOf("STOP: detente"), 4, 7, 55), 2000, rewardStatus = DriveRewardStatus.FAILED),
            { replayed = true }, { exited = true }, {}) } }
        compose.onNodeWithText("STOP: detente").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Otra vez").assertIsDisplayed().performClick()
        compose.onNodeWithText("Salir").assertIsDisplayed().performClick()
        assertThat(replayed).isTrue()
        assertThat(exited).isTrue()
    }
    /** Actual cancelled touch gestures cannot leave a held brake or pending steering movement. */
    @Test fun cancellingGestureClearsInput() {
        val input = DriveControls()
        compose.setContent { Box(Modifier.fillMaxSize().driveControls(input)) }
        compose.onRoot().performTouchInput { down(center); moveBy(Offset(40f, 0f)) }
        compose.runOnIdle { assertThat(input.pressed).isTrue(); assertThat(input.pendingDx).isGreaterThan(0f) }
        compose.onRoot().performTouchInput { cancel() }
        compose.runOnIdle { assertThat(input.pressed).isFalse(); assertThat(input.pendingDx).isEqualTo(0f) }
    }

    /** Large type on a narrow dark display keeps both modal actions outside the scroll area. */
    @Test fun resultActionsRemainVisibleWithLargeFonts() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                BaliTheme(darkTheme = true) { DriveResultsDialog(
                    DriveResult(DriveSummary(1000, 2, 3, emptyList(), 4, 7, 55), 2000, rewardStatus = DriveRewardStatus.FAILED), {}, {}, {}) }
            }
        }
        compose.onNodeWithText("Otra vez").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Salir").assertIsDisplayed().assertIsEnabled()
    }

}
