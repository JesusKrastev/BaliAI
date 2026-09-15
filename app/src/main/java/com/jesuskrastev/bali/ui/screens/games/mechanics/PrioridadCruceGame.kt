package com.jesuskrastev.bali.ui.screens.games.mechanics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import com.jesuskrastev.bali.ui.screens.games.RoundFeedbackPanel
import com.jesuskrastev.bali.ui.screens.games.ShrinkingTimerBar
import com.jesuskrastev.bali.ui.screens.games.pickForRound
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val ROUND_TIME_MS = 4200L
private const val TICK_MS = 50L

private enum class Approach { NORTH, EAST, SOUTH, WEST }

/**
 * Returns the vehicle type label (e.g. "CAMIÓN") for this car's answer button, derived from its
 * tag. Only covers vehicle kinds that actually appear in the bundled crossing illustrations —
 * defaults to "COCHE" otherwise, since no generic vehicle emoji is present in that art.
 */
private fun CarAt.vehicleType(): String = when {
    tag?.contains("🚚") == true -> "CAMIÓN"
    tag?.contains("🚐") == true -> "FURGONETA"
    tag?.contains("🚌") == true -> "AUTOBÚS"
    tag?.contains("🏍️") == true -> "MOTO"
    tag?.contains("🚆") == true -> "TREN"
    else -> "COCHE"
}

/**
 * A vehicle in a crossing scenario. [colorName]/[colorEmoji] must match how this car is actually
 * painted in the scenario's illustration (see [CrossingScenario.imageKey]) — the answer button
 * displays them verbatim, so a mismatch here is visible to the player as a wrong-looking option.
 */
private data class CarAt(
    val approach: Approach,
    val tag: String? = null,
    val colorName: String = "ROJO",
    val colorEmoji: String = "🔴",
)

private data class CrossingScenario(
    val label: String,
    val cars: List<CarAt>,
    val priorityApproach: Approach,
    val explanation: String,
    val imageKey: String = "priority_urban",
)

/** Builds a crossing scenario and assigns one of the generated scene illustrations to it. */
private fun crossingScenario(
    label: String,
    cars: List<CarAt>,
    priorityApproach: Approach,
    explanation: String,
    imageKey: String = "priority_urban",
): CrossingScenario = CrossingScenario(label, cars, priorityApproach, explanation, imageKey)

/** Returns the bundled fallback resource for a generated priority scene. */
private fun priorityFallbackResource(imageKey: String): Int = when (imageKey) {
    "priority_stop" -> R.drawable.priority_stop
    "priority_roundabout" -> R.drawable.priority_roundabout
    "priority_main_road" -> R.drawable.priority_main_road
    "priority_tjunction" -> R.drawable.priority_tjunction
    "priority_multilane" -> R.drawable.priority_multilane
    "priority_motorway" -> R.drawable.priority_motorway
    "priority_mountain" -> R.drawable.priority_mountain
    "priority_railway" -> R.drawable.priority_railway
    "priority_vulnerable" -> R.drawable.priority_vulnerable
    else -> R.drawable.priority_urban
}

/**
 * Curated right-of-way situations. Each entry is deliberately faithful to its fixed illustration
 * ([CrossingScenario.imageKey], see [priorityFallbackResource]): only two vehicles compete per
 * scenario because that's what every image shows, their [CarAt.colorName]/[CarAt.colorEmoji]
 * match how they're actually painted, and only vehicle kinds genuinely drawn somewhere (car,
 * truck, van, bus, motorcycle, train) are used as tags — there is no image with an ambulance,
 * police car, tram, tractor, cyclist or pedestrian rendered as a selectable vehicle, so those
 * scenario types were dropped rather than left pointing at art that doesn't match them. Where an
 * illustration shows more than two vehicles (e.g. priority_multilane's bus/car/motorcycle scene),
 * multiple scenarios pick a different faithful pair from the same image.
 */
private val SCENARIOS = listOf(
    crossingScenario(
        "Cruce urbano sin señalizar",
        listOf(
            CarAt(Approach.NORTH, colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.WEST, colorName = "AZUL", colorEmoji = "🔵"),
        ),
        Approach.WEST,
        "Sin semáforos ni señales, tiene prioridad quien llega por tu derecha: el coche azul llega por la derecha del rojo.",
        "priority_urban",
    ),
    crossingScenario(
        "Cruce con señal de STOP",
        listOf(
            CarAt(Approach.SOUTH, tag = "🛑", colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.WEST, colorName = "AZUL", colorEmoji = "🔵"),
        ),
        Approach.WEST,
        "La señal de STOP obliga a detenerse y ceder el paso siempre, tenga o no prioridad a la derecha.",
        "priority_stop",
    ),
    crossingScenario(
        "Rotonda: uno ya está dentro",
        listOf(
            CarAt(Approach.EAST, colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.SOUTH, colorName = "AMARILLO", colorEmoji = "🟡"),
        ),
        Approach.EAST,
        "Quien ya circula por la rotonda tiene siempre prioridad sobre quien espera para entrar.",
        "priority_roundabout",
    ),
    crossingScenario(
        "Carretera con prioridad",
        listOf(
            CarAt(Approach.NORTH, tag = "♦️", colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.EAST, colorName = "AZUL", colorEmoji = "🔵"),
        ),
        Approach.NORTH,
        "La señal de carretera con prioridad da el paso al vehículo que circula por ella; el que se incorpora debe ceder.",
        "priority_main_road",
    ),
    crossingScenario(
        "Incorporación en un cruce en T",
        listOf(
            CarAt(Approach.EAST, tag = "🚐", colorName = "BLANCA", colorEmoji = "⚪"),
            CarAt(Approach.SOUTH, colorName = "AZUL", colorEmoji = "🔵"),
        ),
        Approach.EAST,
        "Quien se incorpora a la vía principal debe ceder el paso a los vehículos que ya circulan por ella.",
        "priority_tjunction",
    ),
    crossingScenario(
        "Paso a nivel con barreras",
        listOf(
            CarAt(Approach.WEST, tag = "🚆", colorName = "BLANCO", colorEmoji = "⚪"),
            CarAt(Approach.SOUTH, colorName = "ROJO", colorEmoji = "🔴"),
        ),
        Approach.WEST,
        "El tren tiene prioridad absoluta en el paso a nivel: con las barreras bajadas, el coche debe permanecer detenido.",
        "priority_railway",
    ),
    crossingScenario(
        "Incorporación a la autovía",
        listOf(
            CarAt(Approach.WEST, colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.NORTH, tag = "🚚", colorName = "NARANJA", colorEmoji = "🟠"),
        ),
        Approach.NORTH,
        "El vehículo que ya circula por la vía principal tiene prioridad sobre el que se incorpora por el carril de aceleración.",
        "priority_motorway",
    ),
    crossingScenario(
        "Puente estrecho de montaña",
        listOf(
            CarAt(Approach.NORTH, colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.SOUTH, tag = "🚚", colorName = "AMARILLO", colorEmoji = "🟡"),
        ),
        Approach.SOUTH,
        "En un tramo estrecho, el vehículo que ya ha entrado en él tiene prioridad; el otro debe esperar en el apartadero.",
        "priority_mountain",
    ),
    crossingScenario(
        "Avenida con semáforo en ámbar intermitente",
        listOf(
            CarAt(Approach.EAST, colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.NORTH, tag = "🚌", colorName = "AZUL", colorEmoji = "🔵"),
        ),
        Approach.NORTH,
        "Con el semáforo en ámbar intermitente se aplica la prioridad a la derecha: el autobús azul llega por la derecha del rojo.",
        "priority_multilane",
    ),
    crossingScenario(
        "Avenida con semáforo en ámbar intermitente",
        listOf(
            CarAt(Approach.EAST, colorName = "ROJO", colorEmoji = "🔴"),
            CarAt(Approach.SOUTH, tag = "🏍️", colorName = "BLANCA", colorEmoji = "⚪"),
        ),
        Approach.EAST,
        "Con el semáforo en ámbar intermitente se aplica la prioridad a la derecha: el coche rojo llega por la derecha de la moto.",
        "priority_multilane",
    ),
    crossingScenario(
        "Giro a la izquierda con tráfico de frente",
        listOf(
            CarAt(Approach.NORTH, tag = "↰", colorName = "NARANJA", colorEmoji = "🟠"),
            CarAt(Approach.SOUTH, colorName = "AZUL", colorEmoji = "🔵"),
        ),
        Approach.SOUTH,
        "Al girar a la izquierda debes ceder el paso al vehículo que viene de frente y continúa recto.",
        "priority_vulnerable",
    ),
)

/**
 * Right-of-way mini-game: a complete top-down traffic illustration appears and the player selects
 * the vehicle with priority by tapping its labeled button (vehicle type + color, matching the
 * illustration) before the timer runs out.
 *
 * @param sessionSeed shared across this session's rounds so the same scenario doesn't repeat
 *   before every other one in [SCENARIOS] has been shown (see [pickForRound])
 * @param roundIndex the current round number within the session (0-based)
 * @param onRoundResult called once with whether the round was won, after the player continues
 *   past the feedback panel
 */
@Composable
fun PrioridadCruceGame(sessionSeed: Long, roundIndex: Int, onRoundResult: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scenario = remember(sessionSeed, roundIndex) { pickForRound(SCENARIOS, sessionSeed, roundIndex) }
    var timeLeftMs by remember { mutableLongStateOf(ROUND_TIME_MS) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        while (isActive && timeLeftMs > 0 && outcome == null) {
            delay(TICK_MS)
            timeLeftMs = (timeLeftMs - TICK_MS).coerceAtLeast(0)
        }
        if (outcome == null) outcome = false
    }

    fun choose(approach: Approach) {
        if (outcome != null) return
        outcome = approach == scenario.priorityApproach
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShrinkingTimerBar(
            progress = timeLeftMs / ROUND_TIME_MS.toFloat(),
            remainingSeconds = ((timeLeftMs + 999L) / 1000L).toInt(),
        )
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(scenario.label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            "¿Qué vehículo tiene prioridad?",
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium,
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(LessonQuestionBank.gameAsset(scenario.imageKey))
                    .crossfade(true)
                    .build(),
                contentDescription = scenario.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                error = painterResource(priorityFallbackResource(scenario.imageKey)),
            )
        }
        if (outcome == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                scenario.cars.forEachIndexed { index, car ->
                    Surface(
                        modifier = Modifier.weight(1f).clickable { choose(car.approach) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (index == 0) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                        tonalElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("${car.vehicleType()} ${car.colorName}", fontWeight = FontWeight.Black)
                            Text(car.colorEmoji, style = MaterialTheme.typography.headlineSmall)
                            Text("Seleccionar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            RoundFeedbackPanel(
                isSuccess = outcome == true,
                title = if (outcome == true) "¡Bien visto! ⚡" else "Prioridad equivocada",
                explanation = scenario.explanation,
                onContinue = { onRoundResult(outcome == true) },
            )
        }
    }
}
