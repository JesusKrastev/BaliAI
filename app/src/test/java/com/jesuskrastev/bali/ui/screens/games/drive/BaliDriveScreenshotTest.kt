package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.repository.GameRecord
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.games.GamesScreen
import com.jesuskrastev.bali.ui.theme.BaliTheme
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures of Bali Drive at its key moments (one per situation, the HUD while driving, the results
 * card and the catalogue cover), saved under `build/bali-drive/` to review the look. The class
 * name ends in `ScreenshotTest` because the release build only keeps those (brain E-021).
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class BaliDriveScreenshotTest {

    @get:Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureRoadworks() = captureScene("1_roadworks", seed = 4) { e ->
        e.nextSituation() is RoadworksSituation && e.nextSituation()!!.y - e.car.y < 4.5f
    }

    @Test
    fun captureCrosswalk() = captureScene("2_crosswalk", seed = 4) { e ->
        (e.nextSituation() as? CrosswalkSituation)?.let { it.walkerOnRoad && it.walker.x in 1.2f..2.4f } == true
    }

    @Test
    fun captureStop() = captureScene("3_stop", seed = 5) { e ->
        (e.nextSituation() as? StopSituation)?.crossCar?.let { it.x in 0.8f..1.6f } == true
    }

    @Test
    fun captureTrafficLight() = captureScene("4_traffic_light", seed = 5) { e ->
        (e.nextSituation() as? TrafficLightSituation)?.let { it.light == LightColor.RED && e.car.speed < 0.5f } == true
    }

    @Test
    fun captureBall() = captureScene("5_ball", seed = 5) { e ->
        (e.nextSituation() as? BallSituation)?.child?.let { it.visible && it.x < 2.2f } == true
    }

    @Test
    fun captureLeadCar() = captureScene("6_lead_car", seed = 5) { e ->
        (e.nextSituation() as? LeadCarSituation)?.lead?.braking == true
    }

    @Test
    fun captureFinish() = captureScene("7_finish", seed = 5) { e -> e.finishY - e.car.y < 4f }

    @Test
    fun captureDrivingHud() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { BaliDriveScreen(onExit = {}, viewModel = viewModel(GameRecord(2350, 5))) } }
        composeTestRule.mainClock.advanceTimeBy(5_200)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/8_hud.png")
    }

    @Test
    fun captureCountdown() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { BaliDriveScreen(onExit = {}, viewModel = viewModel(GameRecord(0, 0))) } }
        composeTestRule.mainClock.advanceTimeBy(900)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/0_countdown.png")
    }

    @Test
    fun captureResults() {
        val vm = viewModel(GameRecord(2350, 5))
        vm.finishRun(DriveSummary(2780, 10, 11, listOf(RULE_STOP), 31, 44, 54))
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { BaliDriveScreen(onExit = {}, viewModel = vm) } }
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/9_results.png")
    }

    @Test
    fun captureCatalogue() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { GamesScreen(onGameClick = {}) } }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/catalogue.png")
    }

    /** Wraps [content] in the app theme over its background. */
    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        BaliTheme(darkTheme = false) { Surface(color = MaterialTheme.colorScheme.background) { content() } }
    }

    /** ViewModel over fakes, with [record] as the stored personal best. */
    private fun viewModel(record: GameRecord): BaliDriveViewModel {
        val users = FakeUserRepository()
        return BaliDriveViewModel(
            incrementXpUseCase = IncrementXpUseCase(users),
            incrementCoinsUseCase = IncrementCoinsUseCase(users),
            incrementStreakUseCase = IncrementStreakUseCase(users),
            completeFirstStepUseCase = CompleteFirstStepUseCase(users, PendingFirstStepRewards()),
            gameRecordRepository = FakeGameRecordRepository(record),
            soundEffects = FakeSoundEffects(),
            analyticsTracker = FakeAnalyticsTracker(mock(), mock()),
        )
    }

    /**
     * Drives a careful run until [moment] is true and draws that frame of the world.
     *
     * @param seed route to drive
     */
    private fun captureScene(name: String, seed: Long, moment: (DriveEngine) -> Boolean) {
        val engine = DriveEngine(seed)
        val fx = DriveFx()
        var elapsed = 0f
        while (!moment(engine) && elapsed < 200f) {
            val (steer, brake) = examinerInput(engine)
            engine.update(TEST_FRAME, steer, brake)
            engine.drainEvents().filterIsInstance<DriveEvent.Resolved>().forEach {
                fx.burst(engine.car.x, engine.car.y - CAR_LENGTH / 2, listOf(androidx.compose.ui.graphics.Color.Yellow), 20)
            }
            fx.update(TEST_FRAME)
            elapsed += TEST_FRAME
        }
        check(moment(engine)) { "moment $name never happened" }
        composeTestRule.setContent {
            val art = rememberDriveArt()
            Canvas(Modifier.fillMaxSize()) {
                drawDriveWorld(engine, fx, DriveProjection(size.width, size.height, engine.car.y), art, elapsed)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/$name.png")
    }
}
