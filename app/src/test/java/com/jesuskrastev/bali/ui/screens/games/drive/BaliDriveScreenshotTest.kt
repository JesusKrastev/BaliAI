package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.repository.GameRecord
import com.jesuskrastev.bali.domain.usecase.PrepareGameUseCase
import com.jesuskrastev.bali.domain.usecase.CompleteGameTutorialUseCase
import com.jesuskrastev.bali.domain.usecase.SubmitGameRunUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
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
 * Captures of Bali Drive at its key moments (one per situation, the power-ups, the HUD while
 * driving, the results screens and the catalogue cover), saved under `build/bali-drive/` to review
 * the look. The class name ends in `ScreenshotTest` because the release build only keeps those
 * (brain E-021).
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
    fun captureLeadCarPullingOver() = captureScene("6b_lead_car_pulls_over", seed = 5) { e ->
        e.situations.filterIsInstance<LeadCarSituation>().first().lead?.let { it.drift > 0f && it.x in 2.0f..2.6f } == true
    }

    @Test
    fun captureFinish() = captureScene("7_finish", seed = 5) { e -> e.finishY - e.car.y < 4f }

    @Test
    fun captureScooter() = captureScene("10_scooter", seed = 5) { e ->
        (e.nextSituation() as? ScooterSituation)?.let { it.visible && it.riderY - e.car.y in 2.8f..3.6f } == true
    }

    @Test
    fun captureScooterTurningOff() = captureScene("10b_scooter_turns_off", seed = 5, driver = { examinerInput(it, overtakesScooters = false) }) { e ->
        e.situations.filterIsInstance<ScooterSituation>().first().let { it.visible && it.riderX > 2.4f }
    }

    @Test
    fun captureAmbulanceWarning() = captureScene("11_ambulance_warning", seed = 5) { e ->
        e.situations.filterIsInstance<AmbulanceSituation>().first().ambulance?.let { it.y - e.car.y in -4.4f..-4.1f } == true
    }

    @Test
    fun captureAmbulancePassing() = captureScene("11b_ambulance_passing", seed = 5) { e ->
        e.situations.filterIsInstance<AmbulanceSituation>().first().ambulance?.let { it.y - e.car.y in 1.4f..1.7f } == true
    }

    @Test
    fun captureZone30() = captureScene("12_zone_30", seed = 5) { e ->
        e.speedLimitKmh != null && e.situations.filterIsInstance<Zone30Situation>().first().let { e.car.y > it.y + 1.2f }
    }

    @Test
    fun capturePowerUpAhead() = captureScene("13_power_up_ahead", seed = 5, driver = { examinerFetching(it, it.powerUps.first()) }) { e ->
        e.powerUps.first().y - e.car.y in 3.2f..3.6f
    }

    @Test
    fun capturePowerUpActive() = captureScene("13b_power_up_active", seed = 5, driver = { examinerFetching(it, it.powerUps.first()) }) { e ->
        e.shield || PowerUpKind.entries.any { it.seconds > 0f && e.isActive(it) }
    }

    @Test
    fun captureDrivingHud() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { DrivePreviewScreen(viewModel(GameRecord(2350, 5))) } }
        composeTestRule.mainClock.advanceTimeBy(5_200)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/8_hud.png")
    }

    @Test
    fun captureCountdown() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { DrivePreviewScreen(viewModel(GameRecord(0, 0))) } }
        composeTestRule.mainClock.advanceTimeBy(900)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/0_countdown.png")
    }

    @Test
    fun captureResults() {
        val vm = viewModel(GameRecord(2350, 5))
        vm.finishRun(DriveSummary(2780, 10, 11, listOf(RULE_STOP), 31, 44, 54, faultKinds = listOf(SituationKind.STOP)))
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { ResultsPreview(vm.uiState.value.result!!) } }
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/9_results.png")
    }

    @Test
    fun captureResultsPerfect() {
        val vm = viewModel(GameRecord(2350, 5))
        vm.finishRun(DriveSummary(3420, 11, 11, emptyList(), 40, 44, 52))
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { ResultsPreview(vm.uiState.value.result!!) } }
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/9b_results_perfect.png")
    }

    @Test
    fun captureResultsRough() {
        val vm = viewModel(GameRecord(2350, 5))
        val kinds = listOf(SituationKind.STOP, SituationKind.CROSSWALK, SituationKind.BALL, SituationKind.SCOOTER, SituationKind.AMBULANCE)
        vm.finishRun(DriveSummary(900, 6, 11, listOf(RULE_STOP, RULE_CROSSWALK, RULE_BALL), 8, 44, 70, faultKinds = kinds))
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { ResultsPreview(vm.uiState.value.result!!) } }
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/9c_results_rough.png")
    }

    @Test
    fun captureCatalogue() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { GamesScreen(onGameClick = {}) } }
        composeTestRule.mainClock.advanceTimeBy(1_500)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/catalogue.png")
    }

    /** Records the dialog content in a narrow dark display with large text. */
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    @Test
    fun captureResultsLargeFonts() {
        val vm = viewModel(GameRecord(2350, 5))
        vm.finishRun(DriveSummary(2780, 10, 11, listOf(RULE_STOP), 31, 44, 54, faultKinds = listOf(SituationKind.STOP)))
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                BaliTheme(darkTheme = true) { ResultsPreview(vm.uiState.value.result!!) }
            }
        }
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/results_large_fonts.png")
    }

    /** Captures the steering lesson card over an empty road. */
    @Test
    fun captureSteeringCoach() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { Box(Modifier.fillMaxSize()) { DriveCoachOverlay(CoachStep.STEER) } } }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/coach_steer.png")
    }

    /** Captures the braking lesson card with its press-and-hold rings. */
    @Test
    fun captureBrakingCoach() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { Themed { Box(Modifier.fillMaxSize()) { DriveCoachOverlay(CoachStep.BRAKE) } } }
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.onRoot().captureRoboImage("build/bali-drive/coach_brake.png")
    }

    /** Captures the canonical dialog content; modal behavior is covered by semantic UI tests. */
    @Composable
    private fun ResultsPreview(result: DriveResult) {
        Surface(Modifier.fillMaxSize(), color = BaliSecondary.copy(alpha = 0.6f)) {
            Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                DriveResultsContent(result, {}, {}, {})
            }
        }
    }

    /** Presents a deterministic route seed while retaining the real screen overlays and loop. */
    @Composable
    private fun DrivePreviewScreen(viewModel: BaliDriveViewModel) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        DriveRun(state.copy(runSeed = 7L), viewModel, {})
    }

    /** Wraps [content] in the app theme over its background. */
    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        BaliTheme(darkTheme = false) { Surface(color = MaterialTheme.colorScheme.background) { content() } }
    }

    /** ViewModel over fakes, with [record] as the stored personal best. */
    private fun viewModel(record: GameRecord): BaliDriveViewModel {
        val users = FakeUserRepository()
        val records = FakeGameRecordRepository(record)
        return BaliDriveViewModel(
            incrementXpUseCase = IncrementXpUseCase(users),
            incrementCoinsUseCase = IncrementCoinsUseCase(users),
            incrementStreakUseCase = IncrementStreakUseCase(users),
            completeFirstStepUseCase = CompleteFirstStepUseCase(users, PendingFirstStepRewards()),
            prepareGame = PrepareGameUseCase(records, FakeAuthRepository()),
            completeTutorial = CompleteGameTutorialUseCase(records),
            submitRun = SubmitGameRunUseCase(records),
            soundEffects = FakeSoundEffects(),
            analyticsTracker = FakeAnalyticsTracker(mock(), mock()),
        )
    }

    /**
     * Drives a run until [moment] is true and draws that frame of the world.
     *
     * @param name file name of the capture, without extension
     * @param seed route to drive
     * @param driver steering and brake of the driver; the careful examiner by default
     * @param moment true on the frame to capture
     */
    private fun captureScene(
        name: String,
        seed: Long,
        driver: (DriveEngine) -> Pair<Float, Boolean> = { examinerInput(it) },
        moment: (DriveEngine) -> Boolean,
    ) {
        val engine = DriveEngine(seed)
        val fx = DriveFx()
        var elapsed = 0f
        while (!moment(engine) && elapsed < 200f) {
            val (steer, brake) = driver(engine)
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
