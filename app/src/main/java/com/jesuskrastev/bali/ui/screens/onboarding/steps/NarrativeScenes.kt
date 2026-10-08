package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * One problem the user named and what Bali does about it, for [NarrativeScene.Solved].
 *
 * @property problem the problem, in the words the user picked it in
 * @property fix what the app does about it; only things the app does today
 */
data class SolvedRow(val problem: String, val fix: String)

/**
 * Hand-drawn scenes of the onboarding's problem → risk → solution block, one per answer, so the
 * picture alone tells the user which answer the screen is about.
 */
sealed interface NarrativeScene {
    /** "No sé por dónde empezar": the topics drift around a question mark, none comes first. */
    data object TopicPile : NarrativeScene

    /** "Estudio pero no veo que avance": test after test fails the same topic and progress stalls. */
    data object SameMistake : NarrativeScene

    /** "Me falta un método": study days light up at random across the week and memory drains. */
    data object ScatteredWeek : NarrativeScene

    /** "Llegar sin estar preparado": the exam result flips between APTO and NO APTO, never settling. */
    data object CoinFlip : NarrativeScene

    /** "Que el examen no se parezca": what was studied lands on the exam question and does not fit. */
    data object Mismatch : NarrativeScene

    /** "Fallar por detalles tontos": the word that flips the question gets circled after the wrong pick. */
    data object TrapWord : NarrativeScene

    /** The answer: each problem the user named is struck out and turns into what Bali does about it. */
    data class Solved(val rows: List<SolvedRow>) : NarrativeScene
}

/**
 * Draws [scene] at the size of the narrative visual.
 *
 * @param scene the scene to play
 * @param modifier size and position of the scene
 */
@Composable
fun NarrativeSceneView(scene: NarrativeScene, modifier: Modifier = Modifier) {
    when (scene) {
        NarrativeScene.TopicPile -> TopicPile(modifier)
        NarrativeScene.SameMistake -> SameMistake(modifier)
        NarrativeScene.ScatteredWeek -> ScatteredWeek(modifier)
        NarrativeScene.CoinFlip -> CoinFlip(modifier)
        NarrativeScene.Mismatch -> Mismatch(modifier)
        NarrativeScene.TrapWord -> TrapWord(modifier)
        is NarrativeScene.Solved -> Solved(scene.rows, modifier)
    }
}

// ── El problema ─────────────────────────────────────────────────────────────

/** Topics of the theory exam, all asking to be first. */
private val TOPICS = listOf(
    "🚦" to "Señales", "⚠️" to "Prioridad", "🏎️" to "Velocidad",
    "🍺" to "Alcohol", "🛣️" to "Adelantar", "🅿️" to "Aparcar"
)

/**
 * Topic chips circle a pulsing question mark, each bobbing on its own beat, so none settles as the
 * first. They share one direction and stay evenly spaced, so they never run into each other.
 */
@Composable
private fun TopicPile(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "topic_pile")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "topic_pile_turn"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "topic_pile_pulse"
    )
    val density = LocalDensity.current

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val radiusX = with(density) { (maxWidth * 0.36f).toPx() }
        val radiusY = with(density) { (maxHeight * 0.38f).toPx() }
        val bobHeight = with(density) { 6.dp.toPx() }
        TOPICS.forEachIndexed { i, (emoji, label) ->
            val angle = 2 * PI * i / TOPICS.size + 2 * PI * turn
            // Each chip bobs and tilts on its own beat: a crowd, not a carousel.
            val beat = sin(2 * PI * (turn * 6 + i / 3.0))
            TopicChip(
                emoji = emoji,
                label = label,
                modifier = Modifier.graphicsLayer {
                    translationX = (cos(angle) * radiusX).toFloat()
                    translationY = (sin(angle) * radiusY + beat * bobHeight).toFloat()
                    rotationZ = (5 * beat).toFloat()
                }
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("?", fontSize = 40.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** A topic as a small pill: emoji and name. */
@Composable
private fun TopicChip(emoji: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.sp)
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Three tests arrive one by one and each fails the same topic, while the progress bar under them
 * creeps up and slips back. Loops.
 */
@Composable
private fun SameMistake(modifier: Modifier) {
    val rows = remember { List(3) { Animatable(0f) } }
    val progress = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        while (true) {
            rows.forEach { it.snapTo(0f) }
            for (row in rows) {
                launch { row.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
                progress.animateTo(0.42f, tween(260, easing = FastOutSlowInEasing))
                progress.animateTo(0.3f, tween(520, easing = FastOutSlowInEasing))
                delay(120)
            }
            delay(1_300)
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        SceneCard(label = "TUS ÚLTIMOS TESTS") {
            rows.forEachIndexed { i, appear ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .graphicsLayer {
                            alpha = appear.value.coerceIn(0f, 1f)
                            translationX = (1f - appear.value) * 40.dp.toPx()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Test ${i + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Rotondas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Mark(ok = false)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Tu avance",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Bar(fraction = progress.value, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Days of the week, Monday first, as Spanish calendars write them. */
private val WEEK = listOf("L", "M", "X", "J", "V", "S", "D")

/** Days studied in each loop of [ScatteredWeek]: no two weeks alike. */
private val STUDY_PATTERNS = listOf(listOf(0, 4), listOf(2), listOf(1, 5, 6), listOf(3))

/**
 * A week where study days light up at random and go dark again, and a memory bar that jumps a little
 * with each day studied and drains between them. Loops through different weeks.
 */
@Composable
private fun ScatteredWeek(modifier: Modifier) {
    var pattern by remember { mutableIntStateOf(0) }
    val days = remember { List(WEEK.size) { Animatable(0f) } }
    val memory = remember { Animatable(0.15f) }
    LaunchedEffect(Unit) {
        while (true) {
            for (day in STUDY_PATTERNS[pattern]) {
                launch { days[day].animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) }
                memory.animateTo((memory.value + 0.3f).coerceAtMost(0.7f), tween(260))
                memory.animateTo(0.15f, tween(900, easing = LinearEasing))
            }
            delay(500)
            days.forEach { launch { it.animateTo(0f, tween(300)) } }
            delay(400)
            pattern = (pattern + 1) % STUDY_PATTERNS.size
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        SceneCard(label = "TU SEMANA") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WEEK.forEachIndexed { i, letter ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            letter,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val lit = days[i].value
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        scaleX = lit
                                        scaleY = lit
                                        alpha = lit.coerceIn(0f, 1f)
                                    }
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Lo que recuerdas",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Bar(fraction = memory.value, color = BaliAccentRed)
        }
    }
}

// ── El riesgo ───────────────────────────────────────────────────────────────

/**
 * An exam result card spinning like a coin between "APTO" and "NO APTO", never landing: without
 * knowing whether you are ready, the exam is a toss.
 */
@Composable
private fun CoinFlip(modifier: Modifier) {
    val spin by rememberInfiniteTransition(label = "coin_flip").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2_400
                0f at 0
                0f at 500
                180f at 1_000 using FastOutSlowInEasing
                180f at 1_700
                360f at 2_200 using FastOutSlowInEasing
            }
        ),
        label = "coin_flip_spin"
    )
    val backSide = spin % 360f in 90f..270f
    val color = if (backSide) BaliAccentRed else BaliAccentGreen

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(
            modifier = Modifier
                .width(230.dp)
                .graphicsLayer {
                    rotationY = spin
                    cameraDistance = 14f * density
                },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            border = BorderStroke(2.dp, color)
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 22.dp, horizontal = 16.dp)
                    // The back face is drawn mirrored; turn it round so "NO APTO" reads the right way.
                    .graphicsLayer { rotationY = if (backSide) 180f else 0f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "RESULTADO DEL EXAMEN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (backSide) "NO APTO ✗" else "APTO ✓",
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "¿Hoy aprobarías?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The STOP sign the user studied drops onto the exam question, which adds an officer to it: it does
 * not fit, the card shakes and a red "OTRA TRAMPA" stamp lands. Loops.
 */
@Composable
private fun Mismatch(modifier: Modifier) {
    val drop = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val stamp = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            drop.snapTo(0f)
            stamp.snapTo(0f)
            delay(500)
            drop.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
            launch { stamp.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
            shake.animateTo(0f, keyframes {
                durationMillis = 420
                -1f at 60
                1f at 140
                -0.8f at 220
                0.6f at 300
                0f at 420
            })
            drop.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
            delay(1_400)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        QuestionCard(
            label = "LO QUE ESTUDIASTE",
            emoji = "🛑",
            title = "Señal de STOP",
            subtitle = "Te la sabes de memoria",
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.graphicsLayer {
                translationY = drop.value * 12.dp.toPx()
                translationX = shake.value * 10.dp.toPx()
                rotationZ = shake.value * 3f
            }
        )
        Spacer(Modifier.height(14.dp))
        QuestionCard(
            label = "LO QUE SALE EN EL EXAMEN",
            emoji = "👮",
            title = "STOP y un agente",
            subtitle = "¿A quién haces caso?",
            borderColor = if (stamp.value > 0.5f) BaliAccentRed else MaterialTheme.colorScheme.outlineVariant
        ) {
            if (stamp.value > 0f) {
                Stamp(
                    "OTRA TRAMPA",
                    BaliAccentRed,
                    rotation = -10f,
                    modifier = Modifier.graphicsLayer {
                        val s = 2.2f - 1.2f * stamp.value
                        scaleX = s
                        scaleY = s
                        alpha = stamp.value
                    }
                )
            }
        }
    }
}

/**
 * An exam question with a "NO" in it: the user's pick turns red, then a magnifier finds the "NO" and
 * circles it. Loops.
 */
@Composable
private fun TrapWord(modifier: Modifier) {
    val picked = remember { Animatable(0f) }
    val circle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            picked.snapTo(0f)
            circle.snapTo(0f)
            delay(600)
            picked.animateTo(1f, tween(250))
            delay(350)
            circle.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            delay(1_700)
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        SceneCard(label = "PREGUNTA DEL EXAMEN") {
            Text("¿Cuál de estas maniobras", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "NO",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = if (circle.value > 0f) BaliAccentRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                    val sweep = circle.value * 360f
                    Canvas(Modifier.matchParentSize()) {
                        drawArc(
                            color = BaliAccentRed,
                            startAngle = -110f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(1.dp.toPx(), 0f),
                            size = size.copy(width = size.width - 2.dp.toPx()),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        "🔍",
                        fontSize = 22.sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (circle.value in 0.01f..0.99f) 1f else 0f
                            translationX = 18.dp.toPx() * cos(circle.value * 2 * PI).toFloat()
                            translationY = 14.dp.toPx() * sin(circle.value * 2 * PI).toFloat() + 6.dp.toPx()
                        }
                    )
                }
                Text(" está permitida?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            listOf("A", "B", "C").forEachIndexed { i, letter ->
                val wrong = i == 0 && picked.value > 0.5f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(
                            if (wrong) BaliAccentRed.copy(alpha = 0.12f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                if (wrong) BaliAccentRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (wrong) "✗" else letter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (wrong) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    PlaceholderLine(widthFraction = listOf(0.8f, 0.65f, 0.72f)[i])
                }
            }
        }
    }
}

// ── La solución ─────────────────────────────────────────────────────────────

/**
 * Each problem appears in red, gets struck out and turns into what Bali does about it, in green,
 * one row after the other. Plays once and stays solved.
 */
@Composable
private fun Solved(rows: List<SolvedRow>, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        rows.forEachIndexed { i, row -> SolvedRowView(row, startDelay = 300L + i * 750L) }
    }
}

/** One row of [Solved]: the problem fades in, is struck out, and gives way to the fix. */
@Composable
private fun SolvedRowView(row: SolvedRow, startDelay: Long) {
    val appear = remember { Animatable(0f) }
    val strike = remember { Animatable(0f) }
    val solved = remember { Animatable(0f) }
    LaunchedEffect(row) {
        delay(startDelay)
        appear.animateTo(1f, tween(220))
        strike.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        delay(120)
        solved.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    val done = solved.value > 0.5f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = appear.value },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (done) 6.dp else 2.dp,
        border = BorderStroke(if (done) 2.dp else 1.dp, if (done) BaliAccentGreen else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Mark(ok = done)
            Spacer(Modifier.width(12.dp))
            // Both lines are laid out from the start, so the row never jumps when the fix lands;
            // the struck-out problem stays above it, so the finished row still reads "this → that".
            Column(Modifier.weight(1f)) {
                Text(
                    row.problem,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.drawWithContent {
                        drawContent()
                        val y = size.height / 2
                        drawLine(
                            color = BaliAccentRed,
                            start = Offset(0f, y),
                            end = Offset(size.width * strike.value, y),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                )
                Text(
                    row.fix,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        alpha = solved.value.coerceIn(0f, 1f)
                        translationY = (1f - solved.value) * 12.dp.toPx()
                    }
                )
            }
        }
    }
}

// ── Piezas comunes ──────────────────────────────────────────────────────────

/** The card every scene is drawn on: the app's surface, its outline and a small orange label. */
@Composable
private fun SceneCard(label: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.width(270.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * A question as a small card: label, icon, title and a hint, then two placeholder lines where
 * [overlay] (the stamp) lands, so it never hides what the question asks.
 */
@Composable
private fun QuestionCard(
    label: String,
    emoji: String,
    title: String,
    subtitle: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {}
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier.width(250.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(if (borderColor == outline) 1.dp else 2.dp, borderColor)
    ) {
        Box {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 20.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PlaceholderLine(widthFraction = 1f)
                    PlaceholderLine(widthFraction = 0.7f)
                }
            }
            Box(
                Modifier
                    .matchParentSize()
                    .padding(top = 66.dp),
                contentAlignment = Alignment.Center
            ) { overlay() }
        }
    }
}

/** A round ✓ in green or ✗ in red. */
@Composable
private fun Mark(ok: Boolean) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(if (ok) BaliAccentGreen else BaliAccentRed, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(if (ok) "✓" else "✗", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

/** A rounded progress bar filled to [fraction]. */
@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}

/** A grey line standing in for text nobody needs to read. */
@Composable
private fun PlaceholderLine(widthFraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(7.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
    )
}

/** A rubber stamp: bold text in a rounded double border, tilted. */
@Composable
private fun Stamp(text: String, color: Color, rotation: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .border(3.dp, color, RoundedCornerShape(10.dp))
            .padding(3.dp)
            .border(1.5.dp, color.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.2.sp)
    }
}
