package com.jesuskrastev.bali.ui.screens.games.drive

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import com.jesuskrastev.bali.ui.util.LightSystemBarIcons
import com.jesuskrastev.bali.ui.util.drawSafe
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import com.jesuskrastev.bali.ui.theme.RacingFont
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.sin
import kotlinx.coroutines.delay

/** A finger moving slower than this (dp per second) counts as held still, i.e. braking. */
private const val STILL_SPEED_DP = 90f

/** Lanes the car moves per lane-width of finger travel. */
private const val STEER_GAIN = 1.2f

/** Confetti colours of a well-handled situation. */
private val SUCCESS_COLORS = listOf(BaliPrimary, BaliAccentYellow, BaliAccentGreen, Color.White, Color(0xFF38BDF8))

/**
 * Bali Drive: drive Bali's car through a small town with one finger. Drag sideways to change
 * lane, hold the finger still to brake. Each run is a short route of situations (roadworks,
 * crosswalks, STOP, traffic lights, a car braking ahead, a ball rolling out) with stars between
 * them; handling situations in a row multiplies the score, and the personal best is what makes
 * the next run tempting.
 *
 * @param onExit leaves the game (back arrow, system back, "Salir")
 */
@Composable
fun BaliDriveScreen(onExit: () -> Unit, modifier: Modifier = Modifier, viewModel: BaliDriveViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasExited by remember { mutableStateOf(false) }
    val exitOnce: () -> Unit = {
        if (!hasExited) {
            hasExited = true
            onExit()
        }
    }
    LightSystemBarIcons()
    Box(modifier.fillMaxSize().background(DrivePalette.Grass)) {
        if (state.phase == DriveScreenPhase.LOADING) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.error) {
                    Text(stringResource(R.string.drive_load_error))
                    Button(onClick = viewModel::prepare) { Text(stringResource(R.string.drive_retry)) }
                    TextButton(onClick = exitOnce) { Text(stringResource(R.string.drive_exit)) }
                } else CircularProgressIndicator()
            }
        } else if (state.runId > 0) {
            key(state.runId) {
                DriveRun(state = state, viewModel = viewModel, onExit = exitOnce)
            }
        }
    }
}

/** Values the overlays show; a data class so unchanged frames do not recompose them. */
private data class HudState(
    val score: Int = 0,
    val multiplier: Int = 1,
    val combo: Int = 0,
    val progress: Float = 0f,
    val speedKmh: Int = 0,
    val braking: Boolean = false,
    val phase: DrivePhase = DrivePhase.COUNTDOWN,
    val countdown: Int = 3,
    val stars: Int = 0,
    val fault: DriveEvent.Faulted? = null,
    val faultShielded: Boolean = false,
    val newRecord: Boolean = false,
    val powers: List<Pair<PowerUpKind, Float>> = emptyList(),
    val speedLimit: Int? = null,
    val slowMotion: Boolean = false,
)

/** A floating text that rises and fades over the car. */
private data class Popup(val id: Long, val text: String, val detail: String?, val color: Color, val big: Boolean)

/** What Bali says in its corner, with a mood for its little jump. */
private data class BaliLine(val id: Long, val text: String, val happy: Boolean)

/** One run: owns the engine, the frame loop, input and every overlay. */
@Composable
internal fun DriveRun(state: DriveUiState, viewModel: BaliDriveViewModel, onExit: () -> Unit) {
    val engine = remember(state.runId) { viewModel.session(state).engine }
    val fx = remember { DriveFx() }
    val input = remember { DriveControls() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = LocalView.current
    val density = LocalDensity.current
    val stillSpeedPx = with(density) { STILL_SPEED_DP.dp.toPx() }
    val art = rememberDriveArt()
    // The coach belongs to this run: finishing the lesson clears state.coached without rebuilding it.
    val coach = remember(state.runId) { DriveCoach(enabled = state.coached) }
    var coachStep by remember(state.runId) { mutableStateOf(coach.step) }

    var frame by remember { mutableLongStateOf(0L) }
    var clock by remember { mutableFloatStateOf(0f) }
    var headerHeight by remember { mutableIntStateOf(0) }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var hud by remember { mutableStateOf(HudState()) }
    var hint by remember { mutableStateOf<Pair<Long, SituationKind>?>(null) }
    var baliLine by remember { mutableStateOf<BaliLine?>(null) }
    var finishedAt by remember { mutableFloatStateOf(-1f) }
    var starPulse by remember { mutableIntStateOf(0) }
    val popups = remember { mutableStateListOf<Popup>() }
    var nextId by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = state.result == null) {
        viewModel.abandonRun(engine.situations.count { it.status != SituationStatus.UPCOMING })
        onExit()
    }

    /** Turns the engine's one-shot events into sound, haptics, confetti and words. */
    fun handle(event: DriveEvent) {
        val car = engine.car
        when (event) {
            is DriveEvent.Countdown -> viewModel.playCountdown(event.number)
            is DriveEvent.Hint -> hint = (nextId++) to event.kind
            is DriveEvent.Resolved -> {
                viewModel.playResolved()
                view.haptic(confirm = true)
                fx.burst(car.x, car.y - CAR_LENGTH / 2, SUCCESS_COLORS, 26)
                fx.ring(car.x, car.y - CAR_LENGTH / 2, if (event.multiplier >= 3) BaliAccentYellow else Color.White)
                popups += Popup(nextId++, "+${event.points}", event.bonusLabel ?: "×${event.multiplier}", BaliAccentYellow, big = true)
                baliLine = BaliLine(nextId++, event.praise, happy = true)
            }
            is DriveEvent.Faulted -> {
                viewModel.playFault()
                view.haptic(confirm = false)
                fx.shake = 1f
                fx.flash = 1f
                baliLine = BaliLine(nextId++, "¡Uy! Casi…", happy = false)
            }
            is DriveEvent.Pickup -> {
                viewModel.playPickup(event.streak)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                fx.burst(event.x, event.y, listOf(BaliAccentYellow, Color.White), 7, power = 1.2f)
                starPulse++
            }
            DriveEvent.NewRecord -> {
                viewModel.playResolved()
                popups += Popup(nextId++, "¡NUEVO RÉCORD!", "Sigue así", BaliPrimary, big = true)
                baliLine = BaliLine(nextId++, "¡Estás batiendo tu récord!", happy = true)
            }
            is DriveEvent.Finished -> {
                viewModel.playFinish(engine.summary().rating)
                view.haptic(confirm = true)
                fx.burst(car.x, car.y, SUCCESS_COLORS, 60, power = 3f)
                if (event.perfect) popups += Popup(nextId++, "¡RUTA PERFECTA!", "+300", BaliAccentGreen, big = true)
                baliLine = BaliLine(nextId++, if (event.perfect) "¡Sin un solo fallo!" else "¡Meta!", happy = true)
                finishedAt = clock
            }
            is DriveEvent.PowerUpCollected -> {
                viewModel.playPowerUp()
                view.haptic(confirm = true)
                val color = powerUpColor(event.kind)
                fx.burst(event.x, event.y, listOf(color, Color.White, BaliAccentYellow), 30, power = 2.6f)
                fx.ring(car.x, car.y - CAR_LENGTH / 2, color)
                popups += Popup(nextId++, event.kind.label, null, color, big = true)
                baliLine = BaliLine(
                    nextId++,
                    when (event.kind) {
                        PowerUpKind.SHIELD -> "¡Escudo! Tu racha está a salvo"
                        PowerUpKind.MAGNET -> "¡Imán de estrellas!"
                        PowerUpKind.DOUBLE -> "¡Todo vale el doble!"
                        PowerUpKind.SLOW_MOTION -> "Todo va más despacio…"
                    },
                    happy = true,
                )
            }
            DriveEvent.ShieldSaved -> fx.ring(car.x, car.y - CAR_LENGTH / 2, powerUpColor(PowerUpKind.SHIELD))
            DriveEvent.Siren -> {
                viewModel.playSiren()
                view.haptic(confirm = false)
                hint = (nextId++) to SituationKind.AMBULANCE
                baliLine = BaliLine(nextId++, "¡Ambulancia! Hazle sitio", happy = false)
            }
        }
    }

    LaunchedEffect(engine, lifecycle, state.result != null) {
        if (state.result != null) return@LaunchedEffect
        if (engine.phase == DrivePhase.FINISHED) {
            viewModel.finishRun(engine.summary(), state.runId)
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                var last = 0L
                while (true) {
                    withFrameNanos { now ->
                        val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(1f / 30f)
                        last = now
                        clock += dt
                        val dx = input.takeDx()
                        input.advance(dt, dx, stillSpeedPx)
                        val unitPx = canvasWidth / 5.6f
                        if (coach.step != CoachStep.DONE) {
                            val nextSituation = engine.situations.firstOrNull { it.status == SituationStatus.UPCOMING }
                            coachStep = coach.update(
                                driving = engine.phase == DrivePhase.DRIVING,
                                distanceToSituation = nextSituation?.let { it.layoutStart - engine.car.y } ?: Float.MAX_VALUE,
                                dx = dx,
                                laneWidthPx = unitPx,
                                braking = input.braking,
                                dt = dt,
                            )
                            if (coachStep == CoachStep.DONE) viewModel.completeCoach()
                        }
                        // While a gesture is being taught the road stands still, so there is no hurry.
                        if (!coach.freezesRoad) engine.update(dt, steer = dx / unitPx * STEER_GAIN, brake = input.braking)
                        fx.update(dt)
                        val car = engine.car
                        if (car.braking && car.speed > 1.4f) {
                            fx.skid(car.x - 0.2f, car.rear + 0.15f)
                            fx.skid(car.x + 0.2f, car.rear + 0.15f)
                        }
                        engine.drainEvents().forEach(::handle)
                        if (finishedAt >= 0f && clock - finishedAt > 1.3f) {
                            finishedAt = Float.MAX_VALUE
                            viewModel.finishRun(engine.summary(), state.runId)
                        }
                        hud = HudState(
                            score = engine.score,
                            multiplier = engine.multiplier,
                            combo = engine.combo,
                            progress = (engine.progress * 200).toInt() / 200f,
                            speedKmh = engine.speedKmh,
                            braking = car.braking && engine.phase == DrivePhase.DRIVING,
                            phase = engine.phase,
                            countdown = kotlin.math.ceil(engine.countdown).toInt(),
                            stars = engine.stars.count { it.collected },
                            fault = if (engine.phase == DrivePhase.FAULT) engine.lastFault else null,
                            faultShielded = engine.lastFaultShielded,
                            newRecord = state.bestScore > 0 && engine.score > state.bestScore,
                            powers = buildList {
                                if (engine.shield) add(PowerUpKind.SHIELD to 1f)
                                PowerUpKind.entries.filter { it.seconds > 0f && engine.isActive(it) }.forEach {
                                    add(it to (engine.remaining(it) / it.seconds * 20).toInt() / 20f)
                                }
                            },
                            speedLimit = engine.speedLimitKmh,
                            slowMotion = engine.isActive(PowerUpKind.SLOW_MOTION),
                        )
                        frame = now
                    }
                }
            } finally { input.reset() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasWidth = it.width.toFloat() }
                .graphicsLayer {
                    val shake = fx.shake * fx.shake
                    translationX = sin(frame / 18_000_000f) * shake * 16.dp.toPx()
                    translationY = sin(frame / 23_000_000f) * shake * 10.dp.toPx()
                }
                .driveControls(input, onPress = engine::resumeFromFault),
        ) {
            if (frame < 0L) return@Canvas // reading the frame makes every frame redraw
            val projection = DriveProjection(size.width, size.height, engine.car.y)
            drawDriveWorld(engine, fx, projection, art, clock)
            if (engine.isActive(PowerUpKind.SLOW_MOTION)) {
                drawRect(Brush.radialGradient(listOf(Color.Transparent, DrivePalette.Slow.copy(alpha = 0.4f)), radius = size.maxDimension * 0.7f))
            }
            if (fx.glow > 0f) {
                drawRect(Brush.radialGradient(listOf(Color.Transparent, BaliAccentYellow.copy(alpha = 0.25f * fx.glow)), radius = size.maxDimension * 0.7f))
            }
            if (fx.flash > 0f) {
                drawRect(Brush.radialGradient(listOf(Color.Transparent, BaliAccentRed.copy(alpha = 0.5f * fx.flash)), radius = size.maxDimension * 0.65f))
            }
        }

        DriveTopBar(
            hud = hud,
            bestScore = state.bestScore,
            starPulse = starPulse,
            onClose = {
                viewModel.abandonRun(engine.situations.count { it.status != SituationStatus.UPCOMING })
                onExit()
            },
            // Measured before the inset padding so headerHeight includes the status bar.
            modifier = Modifier.align(Alignment.TopCenter).onSizeChanged { headerHeight = it.height }
                .windowInsetsPadding(WindowInsets.drawSafe.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        )

        HintBanner(
            hint = hint,
            onDone = { hint = null },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = with(density) { headerHeight.toDp() } + 8.dp),
        )

        Box(Modifier.align(Alignment.Center).offset(y = 40.dp)) {
            popups.forEach { popup ->
                key(popup.id) { FloatingPopup(popup, onDone = { popups.remove(popup) }) }
            }
        }

        BaliCorner(
            line = baliLine,
            modifier = Modifier.align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.drawSafe.only(WindowInsetsSides.Bottom + WindowInsetsSides.Start))
                .padding(12.dp),
        )
        Speedometer(
            speedKmh = hud.speedKmh, braking = hud.braking, limit = hud.speedLimit,
            modifier = Modifier.align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.drawSafe.only(WindowInsetsSides.Bottom + WindowInsetsSides.End))
                .padding(12.dp),
        )

        if (hud.phase == DrivePhase.COUNTDOWN) CountdownOverlay(hud.countdown)
        DriveCoachOverlay(coachStep)
        hud.fault?.let { FaultOverlay(it, shielded = hud.faultShielded, onTap = { engine.resumeFromFault() }) }

        state.result?.let { result ->
            DriveResultsDialog(
                result = result,
                onReplay = viewModel::startRun,
                onExit = onExit,
                onStarShown = { viewModel.playPickup(it * 4) },
            )
        }
    }
}

/** Loads the actor textures and measures the lettering once, for the game and the catalogue cover to share. */
@Composable
internal fun rememberDriveArt(): DriveArt {
    val textMeasurer = rememberTextMeasurer()
    val bali = ImageBitmap.imageResource(R.drawable.bali)
    val player = ImageBitmap.imageResource(R.drawable.drive_player)
    val traffic = ImageBitmap.imageResource(R.drawable.drive_traffic)
    val ambulance = ImageBitmap.imageResource(R.drawable.drive_ambulance)
    val pedestrian = ImageBitmap.imageResource(R.drawable.drive_pedestrian)
    val child = ImageBitmap.imageResource(R.drawable.drive_child)
    val scooter = ImageBitmap.imageResource(R.drawable.drive_scooter)
    val bicycle = ImageBitmap.imageResource(R.drawable.drive_bicycle)
    return remember(textMeasurer, bali, player, traffic, ambulance, pedestrian, child, scooter, bicycle) {
        val bold = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black)
        DriveArt(
            bali = bali,
            sprites = DriveSprites(player, traffic, ambulance, pedestrian, child, scooter, bicycle),
            stop = textMeasurer.measure("STOP", bold),
            finish = textMeasurer.measure("META", bold),
            alert = textMeasurer.measure("!", bold),
            thirty = textMeasurer.measure("30", bold),
            double = textMeasurer.measure("×2", bold),
        )
    }
}

/** Light tap of the phone's vibrator: a confirm buzz for success, a reject buzz for a fault. */
private fun View.haptic(confirm: Boolean) {
    val constant = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> if (confirm) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        else -> if (confirm) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}


/** Close button, animated score, record, multiplier, stars and the route progress. */
@Composable
private fun DriveTopBar(hud: HudState, bestScore: Int, starPulse: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val shownScore by animateIntAsState(hud.score, tween(450), label = "drive_score")
    val multiplierScale by animateFloatAsState(
        if (hud.multiplier > 1) 1f else 0.9f,
        spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium),
        label = "drive_multiplier",
    )
    val starScale = remember { Animatable(1f) }
    LaunchedEffect(starPulse) {
        if (starPulse > 0) {
            starScale.snapTo(1.35f)
            starScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
        }
    }
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onClose, shape = CircleShape, color = Color.White.copy(alpha = 0.9f), shadowElevation = 2.dp) {
                Icon(Icons.Rounded.Close, contentDescription = "Salir", tint = BaliSecondary, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = BaliSecondary.copy(alpha = 0.88f), shadowElevation = 3.dp) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(points(shownScore), color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 26.sp)
                    Text(
                        if (hud.newRecord) "¡RÉCORD!" else if (bestScore > 0) "RÉCORD ${points(bestScore)}" else "PUNTOS",
                        color = if (hud.newRecord) BaliAccentYellow else Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier.scale(starScale.value),
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
            ) {
                Text("⭐ ${hud.stars}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Black, color = BaliSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier.scale(multiplierScale),
                shape = RoundedCornerShape(50),
                color = when {
                    hud.multiplier >= 4 -> BaliAccentRed
                    hud.multiplier >= 2 -> BaliPrimary
                    else -> Color.White.copy(alpha = 0.92f)
                },
                shadowElevation = 2.dp,
            ) {
                Text(
                    (if (hud.multiplier >= 3) "🔥 " else "") + "×${hud.multiplier}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    color = if (hud.multiplier >= 2) Color.White else BaliSecondary,
                )
            }
        }
        DriveRouteProgress(hud.progress)
        if (hud.powers.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { hud.powers.forEach { (kind, left) -> PowerChip(kind, left) } }
        }
    }
}

/** Active power-up in the HUD: its colour, its name and a bar emptying as it runs out. */
@Composable
private fun PowerChip(kind: PowerUpKind, left: Float) {
    val color = powerUpColor(kind)
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(color).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(kind.chipLabel, color = if (kind == PowerUpKind.DOUBLE) BaliSecondary else Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
        if (kind.seconds > 0f) {
            Box(Modifier.width(44.dp).height(3.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.35f))) {
                Box(Modifier.fillMaxWidth(left.coerceIn(0f, 1f)).height(3.dp).background(Color.White))
            }
        }
    }
}

/** Banner announcing the next situation during the first runs. */
@Composable
private fun HintBanner(hint: Pair<Long, SituationKind>?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(hint?.first) {
        if (hint != null) {
            delay(2300)
            onDone()
        }
    }
    AnimatedVisibility(
        visible = hint != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        val kind = hint?.second ?: return@AnimatedVisibility
        Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 6.dp, modifier = Modifier.border(2.dp, BaliPrimary, RoundedCornerShape(50))) {
            Text(
                "${kind.icon}  ${kind.hint}",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                fontWeight = FontWeight.Black,
                color = BaliSecondary,
            )
        }
    }
}

/** "+200 ×2" style text that pops, rises and fades above the car. */
@Composable
private fun FloatingPopup(popup: Popup, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(popup.id) {
        progress.animateTo(1f, tween(1100, easing = LinearEasing))
        onDone()
    }
    val t = progress.value
    val pop = if (t < 0.15f) 0.6f + t / 0.15f * 0.55f else 1.15f - ((t - 0.15f) * 0.6f).coerceAtMost(0.15f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            translationY = -t * 90.dp.toPx()
            alpha = if (t < 0.7f) 1f else 1f - (t - 0.7f) / 0.3f
            scaleX = pop
            scaleY = pop
        },
    ) {
        Text(
            popup.text,
            color = popup.color,
            fontWeight = FontWeight.Black,
            fontSize = if (popup.big) 36.sp else 22.sp,
            style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 4f), 8f)),
        )
        popup.detail?.let {
            Text(
                it,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 3f), 6f)),
            )
        }
    }
}

/** Bali in the corner: hops when something goes well, droops on a fault, and says a few words. */
@Composable
private fun BaliCorner(line: BaliLine?, modifier: Modifier = Modifier) {
    val jump = remember { Animatable(0f) }
    var visibleLine by remember { mutableStateOf<BaliLine?>(null) }
    LaunchedEffect(line?.id) {
        line ?: return@LaunchedEffect
        visibleLine = line
        jump.snapTo(0f)
        jump.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        jump.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        delay(1200)
        if (visibleLine?.id == line.id) visibleLine = null
    }
    val idle = rememberInfiniteTransition(label = "bali_idle")
    val breathe by idle.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "bali_breathe")
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Image(
            painter = painterResource(R.drawable.bali),
            contentDescription = null,
            modifier = Modifier.size(64.dp).graphicsLayer {
                val happy = visibleLine?.happy != false
                translationY = -jump.value * (if (happy) 22.dp else 6.dp).toPx()
                rotationZ = if (happy) 0f else -jump.value * 12f
                scaleX = breathe
                scaleY = breathe
            },
        )
        AnimatedVisibility(visible = visibleLine != null, enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)) + fadeIn(), exit = fadeOut()) {
            val text = visibleLine?.text.orEmpty()
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(start = 4.dp, bottom = 40.dp).widthIn(max = 170.dp),
            ) {
                Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, color = BaliSecondary, fontSize = 14.sp)
            }
        }
    }
}

/** Round speedometer with the km/h and a brake lamp that lights while the finger holds still. */
@Composable
private fun Speedometer(speedKmh: Int, braking: Boolean, limit: Int?, modifier: Modifier = Modifier) {
    val sweep by animateFloatAsState((speedKmh / 55f).coerceIn(0f, 1f), tween(120), label = "speedo")
    val over = limit != null && speedKmh > limit
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (limit != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White).border(5.dp, BaliAccentRed, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$limit", color = BaliSecondary, fontWeight = FontWeight.Black, fontSize = 15.sp) }
        }
        Box(Modifier.size(78.dp).clip(CircleShape).background(BaliSecondary.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(64.dp)) {
                val stroke = 7.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(Color.White.copy(alpha = 0.18f), 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    if (over) Brush.sweepGradient(listOf(BaliAccentRed, BaliAccentRed)) else Brush.sweepGradient(listOf(BaliAccentGreen, BaliAccentYellow, BaliPrimary, BaliAccentGreen)),
                    135f, 270f * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$speedKmh", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 20.sp)
                Text("km/h", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Surface(shape = RoundedCornerShape(50), color = if (braking) BaliAccentRed else Color.White.copy(alpha = 0.85f), shadowElevation = if (braking) 6.dp else 1.dp) {
            Text(
                "✋ FRENO",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = if (braking) Color.White else BaliSecondary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
            )
        }
    }
}

/** Shows only the countdown; the first-run tutorial explains controls before it starts. */
@Composable
private fun BoxScope.CountdownOverlay(number: Int) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(number) {
        pop.snapTo(0f)
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Text(
            if (number <= 0) "¡YA!" else "$number",
            modifier = Modifier.graphicsLayer { scaleX = 0.4f + pop.value * 0.6f; scaleY = 0.4f + pop.value * 0.6f; alpha = pop.value.coerceIn(0f, 1f) },
            color = Color.White,
            fontFamily = RacingFont,
            fontSize = 96.sp,
            style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(BaliPrimary, Offset(0f, 8f), 0f)),
        )
    }
}

/** The frozen moment after a broken rule: the rule in one sentence, then a tap to drive on. */
@Composable
private fun BoxScope.FaultOverlay(fault: DriveEvent.Faulted, shielded: Boolean, onTap: () -> Unit) {
    val countdown = remember { Animatable(0f) }
    LaunchedEffect(fault) { countdown.animateTo(1f, tween((FAULT_PAUSE_SECONDS * 1000).toInt(), easing = LinearEasing)) }
    Box(Modifier.matchParentSize().background(BaliSecondary.copy(alpha = 0.3f)).clickable(onClick = onTap))
    Surface(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(fault.kind.icon, fontSize = 44.sp)
            Surface(shape = RoundedCornerShape(50), color = if (shielded) powerUpColor(PowerUpKind.SHIELD) else BaliAccentRed) {
                Text(if (shielded) "¡FALTA! · El escudo salva tu racha" else "¡FALTA! · Pierdes la racha", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Text(fault.message, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = BaliSecondary, fontSize = 17.sp, lineHeight = 22.sp)
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE2E8F0))) {
                Box(Modifier.fillMaxWidth(countdown.value).height(5.dp).background(BaliPrimary))
            }
            Text("Toca para seguir", color = BaliSecondary.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
