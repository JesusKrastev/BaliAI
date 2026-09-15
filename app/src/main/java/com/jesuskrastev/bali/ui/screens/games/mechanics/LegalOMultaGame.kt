package com.jesuskrastev.bali.ui.screens.games.mechanics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import com.jesuskrastev.bali.ui.screens.games.RoundFeedbackPanel
import com.jesuskrastev.bali.ui.screens.games.ShrinkingTimerBar
import com.jesuskrastev.bali.ui.screens.games.pickForRound
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val ROUND_TIME_MS = 4000L
private const val TICK_MS = 50L

private data class ParkingScenario(
    val imageKey: String,
    val fallbackResource: Int,
    val description: String,
    val isLegal: Boolean,
    val penalty: String?,
)

/**
 * Parking and stopping situations based on the current Reglamento General de Circulación,
 * especially articles 93 and 94; municipal ordinances can add local restrictions.
 */
private val SCENARIOS = listOf(
    ParkingScenario("game_legal01", R.drawable.game_legal01, "Aparcado justo delante de un vado permanente, tapándolo por completo.", false, "Multa aprox. 200 € y posible retirada con grúa."),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Aparcado en línea, dentro de las marcas, sin tapar ningún paso ni vado.", true, null),
    ParkingScenario("game_legal03", R.drawable.game_legal03, "Parado sobre el carril bus para esperar a que baje un pasajero, con el carril en uso.", false, "Multa aprox. 200 €, sin puntos."),
    ParkingScenario("game_legal04", R.drawable.game_legal04, "Parado justo encima de un paso de peatones para dejar bajar a alguien.", false, "Multa aprox. 200 €, sin puntos."),
    ParkingScenario("game_legal05", R.drawable.game_legal05, "Aparcado en zona azul (ORA) con el tique de pago visible en el salpicadero.", true, null),
    ParkingScenario("game_legal06", R.drawable.game_legal06, "Aparcado en doble fila junto a otros coches, bloqueando un carril de circulación.", false, "Multa aprox. 200 €, sin puntos."),
    ParkingScenario("game_legal07", R.drawable.game_legal07, "Aparcado en batería, dentro de las líneas, en una calle donde está permitido.", true, null),
    ParkingScenario("game_legal08", R.drawable.game_legal08, "Aparcado a menos de 5 metros de una esquina, tapando la visibilidad del cruce.", false, "Multa aprox. 200 €, sin puntos."),
    ParkingScenario("game_legal09", R.drawable.game_legal09, "Aparcado sobre un carril bici pintado en la calzada.", false, "Multa aprox. 200 €, sin puntos."),
    ParkingScenario("game_legal10", R.drawable.game_legal10, "Aparcado dentro de una plaza de un parking público, entre las líneas.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Detenido ante un semáforo en rojo, sin rebasar la línea de detención.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Parado junto al bordillo para dejar bajar a un pasajero, sin obstaculizar la circulación.", true, null),
    ParkingScenario("game_legal05", R.drawable.game_legal05, "Detenido en una zona de carga y descarga dentro del horario autorizado para descargar mercancía.", true, null),
    ParkingScenario("game_legal10", R.drawable.game_legal10, "Aparcado en una plaza para personas con movilidad reducida con la tarjeta visible y válida.", true, null),
    ParkingScenario("game_legal05", R.drawable.game_legal05, "Aparcado en una plaza reservada para recarga de vehículos eléctricos mientras está recargando.", true, null),
    ParkingScenario("game_legal07", R.drawable.game_legal07, "Motocicleta aparcada dentro de una plaza señalizada exclusivamente para motos.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Aparcado en una plaza urbana señalizada, en el sentido de la marcha y sin invadir la calzada.", true, null),
    ParkingScenario("game_legal10", R.drawable.game_legal10, "Aparcado en una plaza delimitada de un garaje público, sin bloquear accesos ni salidas.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Detenido porque un agente de tráfico lo ordena, aunque la zona tenga una prohibición ordinaria.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Parado en un tramo donde la señalización lo permite, junto al borde derecho y sin crear peligro.", true, null),
    ParkingScenario("game_legal04", R.drawable.game_legal04, "Aparcado sobre la acera, dejando solo un pequeño espacio para que pasen los peatones.", false, "Infracción: está prohibido estacionar en aceras y zonas peatonales; puede retirarse el vehículo."),
    ParkingScenario("game_legal10", R.drawable.game_legal10, "Aparcado en una plaza PMR reservada, pero sin llevar tarjeta de estacionamiento habilitante.", false, "Infracción: plaza reservada para PMR sin autorización; posible retirada."),
    ParkingScenario("game_legal05", R.drawable.game_legal05, "Aparcado en una zona de carga y descarga fuera del horario permitido.", false, "Infracción: uso de zona de carga y descarga fuera de horario; la cuantía depende de la ordenanza."),
    ParkingScenario("game_legal03", R.drawable.game_legal03, "Aparcado en una parada de autobús señalizada, aunque el autobús todavía no haya llegado.", false, "Infracción: parada o estacionamiento en zona reservada para autobuses."),
    ParkingScenario("game_legal03", R.drawable.game_legal03, "Aparcado en una plaza reservada para vehículos de emergencia sin estar prestando ese servicio.", false, "Infracción: plaza reservada a servicios de emergencia; posible retirada."),
    ParkingScenario("game_legal06", R.drawable.game_legal06, "Parado en mitad de la calzada, obligando a los demás vehículos a esquivarlo.", false, "Infracción: detenerse creando peligro u obstaculizando gravemente la circulación."),
    ParkingScenario("game_legal06", R.drawable.game_legal06, "Aparcado sobre una isleta, mediana o separador que divide los sentidos de circulación.", false, "Infracción: está prohibido estacionar en isletas, medianas y separadores."),
    ParkingScenario("game_legal08", R.drawable.game_legal08, "Parado en una curva sin visibilidad suficiente para los vehículos que se aproximan.", false, "Infracción: parada en curva o cambio de rasante con visibilidad insuficiente."),
    ParkingScenario("game_legal08", R.drawable.game_legal08, "Parado en una intersección, bloqueando el giro de los vehículos que tienen preferencia.", false, "Infracción: detención en intersección obstaculizando la circulación transversal."),
    ParkingScenario("game_legal04", R.drawable.game_legal04, "Parado dentro de un túnel, paso inferior o tramo cubierto sin una emergencia.", false, "Infracción: parada o estacionamiento en túnel o paso inferior."),
    ParkingScenario("game_legal04", R.drawable.game_legal04, "Parado sobre un paso a nivel, impidiendo que otros crucen las vías con seguridad.", false, "Infracción: parada o estacionamiento en paso a nivel."),
    ParkingScenario("game_legal04", R.drawable.game_legal04, "Parado sobre un paso para ciclistas señalizado.", false, "Infracción: parada o estacionamiento en paso destinado a ciclistas."),
    ParkingScenario("game_legal09", R.drawable.game_legal09, "Parado sobre los raíles de un tranvía, bloqueando su recorrido.", false, "Infracción: parada o estacionamiento sobre vías de tranvía."),
    ParkingScenario("game_legal06", R.drawable.game_legal06, "Parado en el arcén de una autopista para hacer una llamada, sin avería ni emergencia.", false, "Infracción: no se puede parar en autopista salvo en lugares autorizados o por emergencia."),
    ParkingScenario("game_legal06", R.drawable.game_legal06, "Aparcado en un carril de una autovía para esperar a otra persona.", false, "Infracción: estacionamiento en autovía fuera de los lugares habilitados."),
    ParkingScenario("game_legal01", R.drawable.game_legal01, "Aparcado delante de una entrada de garaje correctamente señalizada con vado.", false, "Infracción: obstrucción de un acceso de vehículos señalizado; posible retirada."),
    ParkingScenario("game_legal08", R.drawable.game_legal08, "Aparcado junto a una línea amarilla continua que prohíbe la parada.", false, "Infracción: la marca amarilla continua prohíbe parar y estacionar."),
    ParkingScenario("game_legal06", R.drawable.game_legal06, "Aparcado en una zona marcada con cebreado y reservada para un uso especial.", false, "Infracción: estacionamiento en zona reservada y señalizada con marcas especiales."),
    ParkingScenario("game_legal03", R.drawable.game_legal03, "Aparcado en un carril reservado para autobuses durante su periodo de funcionamiento.", false, "Infracción: ocupación de carril reservado al transporte público."),
    ParkingScenario("game_legal09", R.drawable.game_legal09, "Aparcado en un carril reservado exclusivamente a bicicletas.", false, "Infracción: ocupación de carril reservado a bicicletas."),
    ParkingScenario("game_legal03", R.drawable.game_legal03, "Aparcado en una parada de taxi señalizada, sin ser un taxi en servicio.", false, "Infracción: estacionamiento en parada o reserva de taxi."),
    ParkingScenario("game_legal10", R.drawable.game_legal10, "Aparcado delante de un bordillo rebajado para facilitar el paso de una silla de ruedas.", false, "Infracción: se obstruye un acceso peatonal adaptado; posible retirada."),
    ParkingScenario("game_legal04", R.drawable.game_legal04, "Aparcado invadiendo el itinerario peatonal y obligando a caminar por la calzada.", false, "Infracción: obstrucción de la zona de paso peatonal."),
    ParkingScenario("game_legal06", R.drawable.game_legal06, "En doble fila y con las luces de emergencia encendidas, pero dejando el vehículo sin vigilancia.", false, "Infracción: las luces de emergencia no autorizan el estacionamiento en doble fila."),
    ParkingScenario("game_legal05", R.drawable.game_legal05, "Aparcado en zona azul sin tique ni autorización de residente.", false, "Infracción: estacionamiento en zona regulada sin autorización."),
    ParkingScenario("game_legal05", R.drawable.game_legal05, "Aparcado en zona azul con el tique caducado y superando el tiempo autorizado.", false, "Infracción: exceso del tiempo autorizado en zona regulada."),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Detenido en el arcén derecho de una carretera, fuera de la calzada y sin bloquear el paso.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Detenido por una avería en el lugar más seguro posible y señalizando correctamente el vehículo.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Vehículo de auxilio detenido el tiempo imprescindible para retirar un vehículo averiado.", true, null),
    ParkingScenario("game_legal02", R.drawable.game_legal02, "Parado para cumplir una obligación del tráfico, dejando libre el paso y siguiendo las indicaciones.", true, null),
)

/**
 * Parking-rules mini-game: a short scene appears and the player must decide LEGAL or
 * INFRACCIÓN before the timer runs out, then sees the real fine tied to the answer.
 *
 * @param sessionSeed shared across this session's rounds so the same scenario doesn't repeat
 *   before every other one in [SCENARIOS] has been shown (see [pickForRound])
 * @param roundIndex the current round number within the session (0-based)
 * @param onRoundResult called once with whether the round was won, after the player continues
 *   past the feedback panel
 */
@Composable
fun LegalOMultaGame(sessionSeed: Long, roundIndex: Int, onRoundResult: (Boolean) -> Unit, modifier: Modifier = Modifier) {
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

    /** Records the player's legal/infracción decision while the round is still active. */
    fun choose(saidLegal: Boolean) {
        if (outcome != null) return
        outcome = saidLegal == scenario.isLegal
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
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(LessonQuestionBank.gameAsset(scenario.imageKey))
                            .crossfade(true)
                            .build(),
                        contentDescription = scenario.description,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentScale = ContentScale.Crop,
                        error = painterResource(scenario.fallbackResource),
                    )
                    Text(
                        scenario.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (outcome == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.weight(1f).clickable { choose(saidLegal = true) },
                    shape = RoundedCornerShape(18.dp),
                    color = BaliAccentGreen,
                ) {
                    Text("LEGAL", modifier = Modifier.fillMaxWidth().padding(18.dp), fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                }
                Surface(
                    modifier = Modifier.weight(1f).clickable { choose(saidLegal = false) },
                    shape = RoundedCornerShape(18.dp),
                    color = BaliAccentRed,
                ) {
                    Text("INFRACCIÓN", modifier = Modifier.fillMaxWidth().padding(18.dp), fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                }
            }
        } else {
            RoundFeedbackPanel(
                isSuccess = outcome == true,
                title = if (scenario.isLegal) "Era legal ✅" else "Era infracción 🚫",
                explanation = scenario.penalty ?: "Aparcamiento correcto: no hay sanción.",
                onContinue = { onRoundResult(outcome == true) },
            )
        }
    }
}
