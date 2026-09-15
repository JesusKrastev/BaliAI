package com.jesuskrastev.bali.ui.screens.games.mechanics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.games.RoundFeedbackPanel
import com.jesuskrastev.bali.ui.screens.games.ShrinkingTimerBar
import com.jesuskrastev.bali.ui.screens.games.pickForRound
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val ROUND_TIME_MS = 7000L
private const val TICK_MS = 50L

/** The only point values the Spanish driving licence's point system actually uses. */
private val POINT_OPTIONS = listOf(0, 2, 3, 4, 6)

private data class InfractionScenario(
    val imageRes: Int,
    val description: String,
    val points: Int,
    val explanation: String,
)

/**
 * Point deductions verified against the official DGT infraction catalogue (Anexo II/IV of the
 * Real Decreto Legislativo 6/2015) as of September 2026. Numeric blood-alcohol thresholds are
 * deliberately avoided: the exact mg/l boundary between the 4- and 6-point bands is reported
 * inconsistently even across DGT-adjacent sources, so both alcohol/drugs scenarios below use only
 * well-established, undisputed framings instead of a specific reading.
 */
private val SCENARIOS = listOf(
    InfractionScenario(R.drawable.game_points_mobile_hand, "Sujetas el móvil con la mano para hablar mientras conduces.", 6, "Sujetar y manejar el móvil con la mano al volante resta 6 puntos desde 2022 (antes eran 3): es de las infracciones más penalizadas."),
    InfractionScenario(R.drawable.game_points_breath_test_refusal, "Un agente te pide la prueba de alcoholemia y te niegas a hacerla.", 6, "Negarte a las pruebas de alcohol o drogas se sanciona igual que dar positivo: 6 puntos."),
    InfractionScenario(R.drawable.game_points_wrong_way, "Circulas en sentido contrario al establecido en una vía.", 6, "Circular en sentido contrario es una de las infracciones más graves del reglamento: 6 puntos."),
    InfractionScenario(R.drawable.game_points_speed_110, "Un radar te capta a 110 km/h en una calle con límite de 50 km/h.", 6, "En vía urbana, superar el límite en más de 50 km/h resta 6 puntos."),
    InfractionScenario(R.drawable.game_points_no_seatbelt, "No llevas puesto el cinturón de seguridad mientras conduces.", 4, "No usar el cinturón de seguridad resta 4 puntos, seas conductor o pasajero."),
    InfractionScenario(R.drawable.game_points_red_light, "Te saltas un semáforo en rojo.", 4, "Saltarse un semáforo en rojo o una señal de STOP resta 4 puntos."),
    InfractionScenario(R.drawable.game_points_tailgating, "Circulas muy pegado al coche de delante, sin distancia de seguridad.", 4, "No mantener la distancia de seguridad resta 4 puntos: es una de las causas más comunes de alcance."),
    InfractionScenario(R.drawable.game_points_no_helmet, "Circulas en moto sin casco.", 4, "No llevar casco en moto o ciclomotor resta 4 puntos, además del riesgo real que supone."),
    InfractionScenario(R.drawable.game_points_unsafe_overtake, "Adelantas a otro vehículo sin visibilidad suficiente, con riesgo real.", 4, "Un adelantamiento antirreglamentario con riesgo resta 4 puntos."),
    InfractionScenario(R.drawable.game_points_speed_85, "Un radar te capta a 85 km/h en una calle con límite de 50 km/h.", 4, "En vía urbana, superar el límite entre 31 y 40 km/h resta 4 puntos."),
    InfractionScenario(R.drawable.game_points_phone_mount, "Manipulas el móvil con la mano aunque lo llevas puesto en un soporte.", 3, "Tocar el móvil con la mano resta 3 puntos incluso si está en un soporte homologado; si lo sujetas para hablar, son 6."),
    InfractionScenario(R.drawable.game_points_headphones, "Conduces con auriculares o cascos que te tapan los oídos.", 3, "Usar auriculares al volante resta 3 puntos: te impiden oír sirenas, cláxones u otros avisos."),
    InfractionScenario(R.drawable.game_points_speed_75, "Un radar te capta a 75 km/h en una calle con límite de 50 km/h.", 2, "En vía urbana, superar el límite entre 21 y 30 km/h resta 2 puntos."),
    InfractionScenario(R.drawable.game_points_double_parking, "Aparcas en doble fila junto a otros coches, bloqueando un carril.", 0, "Hay infracción y multa, pero al ser una falta de estacionamiento no resta puntos del carné."),
    InfractionScenario(R.drawable.game_points_bus_lane_stop, "Paras sobre el carril bus para que baje un pasajero, con el carril en uso.", 0, "Hay infracción y multa, pero al ser una falta de parada no resta puntos del carné."),
    InfractionScenario(R.drawable.game_points_corner_parking, "Aparcas a menos de 5 metros de una esquina, tapando la visibilidad del cruce.", 0, "Hay infracción y multa, pero al ser una falta de estacionamiento no resta puntos del carné."),
)

/**
 * Point-deduction mini-game: a real infraction appears and the player must pick how many points
 * of the licence it costs — 0, 2, 3, 4 or 6, the only values the Spanish system uses — before the
 * timer runs out.
 *
 * @param sessionSeed shared across this session's rounds so the same infraction doesn't repeat
 *   before every other one in [SCENARIOS] has been shown (see [pickForRound])
 * @param roundIndex the current round number within the session (0-based)
 * @param onRoundResult called once with whether the round was won, after the player continues
 *   past the feedback panel
 */
@Composable
fun PuntosCarneGame(sessionSeed: Long, roundIndex: Int, onRoundResult: (Boolean) -> Unit, modifier: Modifier = Modifier) {
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

    /** Records the chosen point value while the round is still active. */
    fun choose(points: Int) {
        if (outcome != null) return
        outcome = points == scenario.points
    }

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ShrinkingTimerBar(
            progress = timeLeftMs / ROUND_TIME_MS.toFloat(),
            remainingSeconds = ((timeLeftMs + 999L) / 1000L).toInt(),
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(scenario.imageRes),
                    contentDescription = "Ilustración: ${scenario.description}",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    scenario.description,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (outcome == null) {
            Text(
                "¿Cuántos puntos del carné pierdes?",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                POINT_OPTIONS.forEach { points ->
                    Surface(
                        modifier = Modifier.weight(1f).clickable { choose(points) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                    ) {
                        Text(
                            "$points",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            RoundFeedbackPanel(
                isSuccess = outcome == true,
                title = if (outcome == true) "¡Correcto! ⚡" else "Eran ${scenario.points} puntos",
                explanation = scenario.explanation,
                onContinue = { onRoundResult(outcome == true) },
            )
        }
    }
}
