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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

/** A finger moving slower than this (dp per second) counts as held still, i.e. braking. */
private const val STILL_SPEED_DP = 90f

/** A finger held still this long starts braking; shorter pauses while steering do not. */
private const val BRAKE_DELAY_SECONDS = 0.12f

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
    Box(modifier.fillMaxSize().background(DrivePalette.Grass)) {
        if (state.runId > 0) {
            key(state.runId) {
                DriveRun(state = state, viewModel = viewModel, onExit = onExit)
            }
        }
    }
}

/** Finger state shared between the gesture detector and the frame loop. */
private class FingerInput {
    var pressed = false
    var pendingDx = 0f
    var stillFor = 0f
    var taps = 0

    /** Returns the horizontal drag since the last frame and forgets it. */
    fun takeDx(): Float = pendingDx.also { pendingDx = 0f }
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
    val newRecord: Boolean = false,
)

/** A floating text that rises and fades over the car. */
private data class Popup(val id: Long, val text: String, val detail: String?, val color: Color, val big: Boolean)

/** What Bali says in its corner, with a mood for its little jump. */
private data class BaliLine(val id: Long, val text: String, val happy: Boolean)

/** One run: owns the engine, the frame loop, input and every overlay. */
@Composable
private fun DriveRun(state: DriveUiState, viewModel: BaliDriveViewModel, onExit: () -> Unit) {
    val engine = remember { DriveEngine(state.runSeed, state.bestScore, state.showHints) }
    val fx = remember { DriveFx() }
    val input = remember { FingerInput() }
    val view = LocalView.current
    val density = LocalDensity.current
    val stillSpeedPx = with(density) { STILL_SPEED_DP.dp.toPx() }
    val art = rememberDriveArt()

    var frame by remember { mutableLongStateOf(0L) }
    var clock by remember { mutableFloatStateOf(0f) }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var hud by remember { mutableStateOf(HudState()) }
    var hint by remember { mutableStateOf<Pair<Long, SituationKind>?>(null) }
    var baliLine by remember { mutableStateOf<BaliLine?>(null) }
    var finishedAt by remember { mutableFloatStateOf(-1f) }
    var starPulse by remember { mutableIntStateOf(0) }
    val popups = remember { mutableStateListOf<Popup>() }
    var nextId by remember { mutableLongStateOf(0L) }

    BackHandler {
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
        }
    }

    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(1f / 30f)
                last = now
                clock += dt
                val dx = input.takeDx()
                input.stillFor = when {
                    !input.pressed -> 0f
                    dt > 0f && abs(dx) / dt > stillSpeedPx -> 0f
                    else -> input.stillFor + dt
                }
                if (input.taps > 0) {
                    input.taps = 0
                    engine.resumeFromFault()
                }
                val unitPx = canvasWidth / 5.6f
                engine.update(dt, steer = dx / unitPx * STEER_GAIN, brake = input.stillFor >= BRAKE_DELAY_SECONDS)
                fx.update(dt)
                val car = engine.car
                if (car.braking && car.speed > 1.4f) {
                    fx.skid(car.x - 0.2f, car.rear + 0.15f)
                    fx.skid(car.x + 0.2f, car.rear + 0.15f)
                }
                engine.drainEvents().forEach(::handle)
                if (finishedAt >= 0f && clock - finishedAt > 1.3f) {
                    finishedAt = Float.MAX_VALUE
                    viewModel.finishRun(engine.summary())
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
                    newRecord = state.bestScore > 0 && engine.score > state.bestScore,
                )
                frame = now
            }
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
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        input.pressed = true
                        input.taps++
                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull { it.pressed }?.also { pointerId = it.id }
                                ?: break
                            if (!change.pressed) break
                            input.pendingDx += change.positionChange().x
                            change.consume()
                        }
                        input.pressed = false
                    }
                },
        ) {
            if (frame < 0L) return@Canvas // reading the frame makes every frame redraw
            val projection = DriveProjection(size.width, size.height, engine.car.y)
            drawDriveWorld(engine, fx, projection, art, clock)
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
            modifier = Modifier.align(Alignment.TopCenter),
        )

        HintBanner(hint = hint, onDone = { hint = null }, modifier = Modifier.align(Alignment.TopCenter).padding(top = 116.dp))

        Box(Modifier.align(Alignment.Center).offset(y = 40.dp)) {
            popups.forEach { popup ->
                key(popup.id) { FloatingPopup(popup, onDone = { popups.remove(popup) }) }
            }
        }

        BaliCorner(line = baliLine, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
        Speedometer(speedKmh = hud.speedKmh, braking = hud.braking, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))

        if (hud.phase == DrivePhase.COUNTDOWN) CountdownOverlay(hud.countdown)
        hud.fault?.let { FaultOverlay(it, onTap = { engine.resumeFromFault() }) }

        state.result?.let { result ->
            DriveResultsOverlay(
                result = result,
                onReplay = viewModel::startRun,
                onExit = onExit,
                onStarShown = { viewModel.playPickup(it * 4) },
            )
        }
    }
}

/** Measures the lettering and loads the mascot bitmap the renderer stamps on the scene. */
@Composable
internal fun rememberDriveArt(): DriveArt {
    val textMeasurer = rememberTextMeasurer()
    val bali = ImageBitmap.imageResource(R.drawable.bali)
    return remember(textMeasurer, bali) {
        val bold = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black)
        DriveArt(bali, textMeasurer.measure("STOP", bold), textMeasurer.measure("META", bold), textMeasurer.measure("!", bold))
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

/** Formats points with Spanish thousands separators (1.234). */
private fun points(value: Int): String = NumberFormat.getIntegerInstance(Locale("es", "ES")).format(value)

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
        RouteProgress(hud.progress)
    }
}

/** The route as a bar: Bali's car slides from the start towards the chequered flag. */
@Composable
private fun RouteProgress(progress: Float) {
    Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().padding(end = 22.dp).height(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.65f))) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(50)).background(BaliPrimary))
        }
        Text("🏁", modifier = Modifier.align(Alignment.CenterEnd), fontSize = 16.sp)
        Box(Modifier.fillMaxWidth(progress.coerceIn(0.04f, 0.96f)).padding(end = 22.dp), contentAlignment = Alignment.CenterEnd) {
            Text("🚗", fontSize = 15.sp)
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
private fun Speedometer(speedKmh: Int, braking: Boolean, modifier: Modifier = Modifier) {
    val sweep by animateFloatAsState((speedKmh / 55f).coerceIn(0f, 1f), tween(120), label = "speedo")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(BaliSecondary.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(64.dp)) {
                val stroke = 7.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(Color.White.copy(alpha = 0.18f), 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(listOf(BaliAccentGreen, BaliAccentYellow, BaliPrimary, BaliAccentGreen)),
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

/** "3, 2, 1, ¡YA!" with the two controls explained underneath. */
@Composable
private fun BoxScope.CountdownOverlay(number: Int) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(number) {
        pop.snapTo(0f)
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    Box(Modifier.matchParentSize().background(BaliSecondary.copy(alpha = 0.35f)))
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Text(
            if (number <= 0) "¡YA!" else "$number",
            modifier = Modifier.graphicsLayer { scaleX = 0.4f + pop.value * 0.6f; scaleY = 0.4f + pop.value * 0.6f; alpha = pop.value.coerceIn(0f, 1f) },
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(BaliPrimary, Offset(0f, 8f), 0f)),
        )
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlRow("↔️", "Desliza para cambiar de carril")
                ControlRow("✋", "Deja el dedo quieto para frenar")
                ControlRow("⭐", "Encadena aciertos: ×2, ×3, ×4…")
            }
        }
    }
}

/** One line of the controls card. */
@Composable
private fun ControlRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
        Text(text, fontWeight = FontWeight.Bold, color = BaliSecondary, fontSize = 15.sp)
    }
}

/** The frozen moment after a broken rule: the rule in one sentence, then a tap to drive on. */
@Composable
private fun BoxScope.FaultOverlay(fault: DriveEvent.Faulted, onTap: () -> Unit) {
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
            Surface(shape = RoundedCornerShape(50), color = BaliAccentRed) {
                Text("¡FALTA! · Pierdes la racha", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Text(fault.message, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = BaliSecondary, fontSize = 17.sp, lineHeight = 22.sp)
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE2E8F0))) {
                Box(Modifier.fillMaxWidth(countdown.value).height(5.dp).background(BaliPrimary))
            }
            Text("Toca para seguir", color = BaliSecondary.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

/**
 * End-of-run card: stars pop one by one, the score counts up, the record is celebrated (or the
 * gap to it shown, the "so close" that makes the next run tempting), rewards, what to revise, and
 * a big replay button.
 *
 * @param onStarShown called as each rating star appears, with its 1-based index (for the sound)
 */
@Composable
private fun BoxScope.DriveResultsOverlay(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val summary = result.summary
    var starsShown by remember { mutableIntStateOf(0) }
    val score = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250)
        for (i in 1..summary.rating) {
            delay(330)
            starsShown = i
            onStarShown(i)
        }
        score.animateTo(summary.score.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    Box(Modifier.matchParentSize().background(BaliSecondary.copy(alpha = 0.6f)).clickable(enabled = false) {})
    if (result.isNewRecord) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
        val lottieProgress by animateLottieCompositionAsState(composition, iterations = 1)
        LottieAnimation(composition, { lottieProgress }, Modifier.matchParentSize())
    }
    Surface(
        modifier = Modifier.align(Alignment.Center).padding(20.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                when (summary.rating) {
                    3 -> "¡Conducción perfecta!"
                    2 -> "¡Muy bien conducido!"
                    else -> "¡Has llegado!"
                },
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                for (i in 1..3) {
                    val shown = i <= starsShown
                    val scale by animateFloatAsState(if (shown) 1f else 0.7f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMediumLow), label = "result_star_$i")
                    Text(
                        "★",
                        modifier = Modifier.scale(scale).offset(y = if (i == 2) (-8).dp else 0.dp),
                        fontSize = if (i == 2) 58.sp else 46.sp,
                        color = if (shown) BaliAccentYellow else MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
            Text(points(score.value.toInt()), fontWeight = FontWeight.Black, fontSize = 44.sp, color = BaliPrimary)
            RecordLine(result)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("✅ ${summary.resolved}/${summary.situations}")
                StatChip("⭐ ${summary.starsCollected}/${summary.starsTotal}")
                StatChip("⏱ ${summary.durationSeconds} s")
            }
            RewardsLine(result)
            if (summary.faults.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Para la próxima", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    summary.faults.distinct().take(3).forEach {
                        Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
            Button(
                onClick = onReplay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BaliPrimary),
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("OTRA VEZ", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            TextButton(onClick = onExit) { Text("Salir", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Record badge, or how far the run fell from it when it was close. */
@Composable
private fun RecordLine(result: DriveResult) {
    val gap = result.previousBest - result.summary.score
    when {
        result.isNewRecord -> {
            val pulse = rememberInfiniteTransition(label = "record_pulse")
            val scale by pulse.animateFloat(0.95f, 1.07f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "record_scale")
            Surface(Modifier.scale(scale), shape = RoundedCornerShape(50), color = BaliAccentYellow) {
                Text(
                    if (result.previousBest > 0) "🏆 ¡NUEVO RÉCORD! (+${points(-gap)})" else "🏆 ¡TU PRIMER RÉCORD!",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    color = BaliSecondary,
                )
            }
        }
        gap in 1..(result.previousBest / 4).coerceAtLeast(1) -> Text(
            "¡A solo ${points(gap)} puntos de tu récord!",
            fontWeight = FontWeight.Black,
            color = BaliPrimary,
        )
        else -> Text("Récord: ${points(result.previousBest)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** XP and coins earned, with the level-up if there was one. */
@Composable
private fun RewardsLine(result: DriveResult) {
    val rewards = result.rewards
    if (rewards == null) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip("+${rewards.xpEarned.xpGained} XP", BaliPrimary.copy(alpha = 0.15f))
        StatChip("+${rewards.coinsGained} monedas", BaliAccentYellow.copy(alpha = 0.3f))
    }
    if (rewards.xpEarned.levelUp) {
        Text("¡Subes a nivel ${rewards.xpEarned.newLevel}!", fontWeight = FontWeight.Black, color = BaliAccentGreen)
    }
}

/** Small rounded figure on the results card. */
@Composable
private fun StatChip(text: String, background: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
