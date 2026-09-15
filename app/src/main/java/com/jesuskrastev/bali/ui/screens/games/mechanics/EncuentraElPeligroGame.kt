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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val ROUND_TIME_MS = 10000L
private const val TICK_MS = 50L

private data class HazardScene(
    val imageKey: String,
    val fallbackResource: Int,
    val prompt: String,
    val answerOptions: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
)

/**
 * Fifty risk-perception situations with one correct danger to identify; the illustrations are
 * contextual Firebase Storage assets and the written scenario defines the expected answer.
 */
private val SCENES = listOf(
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Vas a pasar por esta calle. ¿Dónde está el peligro?", listOf("La pelota en la calzada", "El árbol de la acera", "El coche aparcado", "El paso de peatones"), 0, "Una pelota rodando suele significar que un niño puede salir corriendo detrás sin mirar."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "Te acercas a este cruce. ¿Cuál es la amenaza real?", listOf("El perro suelto", "La parada de autobús", "La tienda", "El vehículo estacionado"), 0, "Un perro suelto sin dueño a la vista puede cruzar la calzada de forma imprevisible."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Conduces por esta zona. ¿Qué deberías vigilar?", listOf("El niño junto al bordillo", "El autobús detenido", "Los árboles", "La bicicleta"), 0, "Un niño pegado al bordillo puede lanzarse a la calzada sin previo aviso."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Vas circulando y ves esta escena. ¿Qué es lo más peligroso?", listOf("El charco que oculta un bache", "La vivienda", "El semáforo", "La parada"), 0, "Un charco grande puede ocultar un bache o provocar aquaplaning si pasas rápido."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Te aproximas a este tramo. ¿Dónde está el riesgo?", listOf("La zona de obras y el trabajador oculto", "La bicicleta", "El árbol", "El semáforo"), 0, "Unas obras en la calzada pueden estrechar el carril o esconder a un trabajador."),
    HazardScene("game_hazard06", R.drawable.game_hazard06, "Miras hacia delante y ves esto. ¿Qué vigilarías primero?", listOf("El patinete que sale de la calle lateral", "El coche aparcado", "El árbol", "La parada"), 0, "Un patinete puede moverse mucho más rápido e imprevisible que un peatón."),
    HazardScene("game_hazard06", R.drawable.game_hazard06, "Un ciclista circula junto a una fila de coches. ¿Qué peligro anticipas?", listOf("La puerta de un coche que puede abrirse", "La señal de dirección", "La fachada", "La línea central"), 0, "Una puerta abierta de repente puede cortar la trayectoria del ciclista."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "Un autobús está detenido y no ves lo que ocurre delante. ¿Qué debes prever?", listOf("Un peatón que puede aparecer por delante del autobús", "La marquesina", "El cartel de la calle", "El coche que viene detrás"), 0, "Un vehículo grande oculta a peatones y obliga a reducir antes de continuar."),
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Hay coches aparcados junto a un colegio. ¿Qué riesgo requiere más atención?", listOf("Un niño que puede salir entre los vehículos", "La pintura de la fachada", "La papelera", "La señal de aparcamiento"), 0, "Los vehículos estacionados reducen la visibilidad de los menores que cruzan."),
    HazardScene("game_hazard06", R.drawable.game_hazard06, "Vas a girar a la derecha y un vehículo circula a tu lado. ¿Cuál es el peligro?", listOf("El ciclista situado en tu ángulo muerto", "El bordillo", "La farola", "La señal de ceda el paso"), 0, "Antes de girar debes comprobar el ángulo muerto y no cerrar la trayectoria del ciclista."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Una furgoneta inicia una maniobra marcha atrás. ¿Qué debes vigilar?", listOf("Una persona oculta detrás de la furgoneta", "La matrícula", "El escaparate", "La acera vacía"), 0, "El conductor puede no ver a un peatón o ciclista detrás del vehículo."),
    HazardScene("game_hazard06", R.drawable.game_hazard06, "Un camión se aproxima a una curva estrecha. ¿Qué amenaza puede aparecer?", listOf("La invasión parcial de tu carril por el vehículo largo", "La señal de población", "La cuneta", "El árbol lejano"), 0, "Los vehículos largos necesitan más espacio y pueden abrirse al tomar la curva."),
    HazardScene("game_hazard06", R.drawable.game_hazard06, "Un ciclista va a girar a la izquierda delante de ti. ¿Qué peligro hay?", listOf("Que cruce tu trayectoria sin que lo hayas previsto", "La sombra del árbol", "La marca del arcén", "La señal de orientación"), 0, "Debes anticipar la maniobra del ciclista, moderar la velocidad y mantener distancia."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Te acercas a un paso de peatones con una persona mayor al borde. ¿Qué riesgo existe?", listOf("Que empiece a cruzar lentamente", "El semáforo apagado de otra calle", "La papelera", "El vehículo estacionado lejos"), 0, "Una persona mayor puede necesitar más tiempo para cruzar; hay que ceder y esperar."),
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Sales de una zona escolar a la hora de entrada. ¿Cuál es el peligro principal?", listOf("Los niños que cruzan entre los adultos y los coches", "La señal de dirección", "La fachada del colegio", "El paso de peatones vacío"), 0, "En entornos escolares pueden aparecer menores de forma repentina y desde distintos puntos."),
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Pasas junto a un parque con la puerta abierta hacia la calle. ¿Qué debes esperar?", listOf("Un niño que salga corriendo del parque", "El banco", "La papelera", "El árbol"), 0, "La salida de un parque cerca de la calzada exige reducir y cubrir el freno."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "Encuentras un caballo junto a la carretera. ¿Qué peligroso comportamiento debes evitar?", listOf("Asustarlo con velocidad, claxon o maniobras bruscas", "Mantener distancia", "Reducir la velocidad", "Esperar una zona segura"), 0, "Los animales pueden reaccionar de forma imprevisible; hay que pasar despacio y dejando espacio."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "Ves un animal doméstico cerca del arcén. ¿Qué situación puede producirse?", listOf("Que entre de repente en la calzada", "Que cambie el semáforo", "Que se cierre la carretera", "Que desaparezca la línea central"), 0, "Un animal sin control puede cruzar inesperadamente; reduce y prepara una frenada suave."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "Una señal advierte de fauna salvaje. ¿Qué peligro debes anticipar de noche?", listOf("Un animal cruzando sin tiempo para reaccionar", "Un peatón en la acera urbana", "Una plaza de aparcamiento", "Una salida de garaje"), 0, "En zonas de fauna hay que moderar la velocidad y observar ambos márgenes."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Entras en un banco de niebla. ¿Qué amenaza aparece primero?", listOf("La pérdida de visibilidad y un vehículo detenido delante", "La señal que queda detrás", "El color de la cuneta", "La radio apagada"), 0, "La niebla reduce la distancia visible y puede ocultar obstáculos o vehículos."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "El sol bajo queda justo delante de ti. ¿Qué riesgo debes controlar?", listOf("El deslumbramiento que oculta peatones y señales", "La sombra detrás del coche", "La temperatura del motor", "La matrícula trasera"), 0, "El deslumbramiento puede ocultar un peligro; reduce la velocidad y limpia el parabrisas."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Comienza una lluvia intensa mientras circulas. ¿Cuál es el peligro?", listOf("Menor adherencia y mayor distancia de frenado", "El limpiaparabrisas del vehículo de detrás", "La señal de población", "El color de los edificios"), 0, "Con lluvia debes suavizar las maniobras, aumentar la distancia y adaptar la velocidad."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "La carretera parece húmeda tras una helada. ¿Qué debes prever?", listOf("Hielo y pérdida repentina de adherencia", "Más agarre en las curvas", "Que el motor frene mejor", "Que las señales pesen más"), 0, "El hielo puede ser invisible y alargar o desviar la trayectoria del vehículo."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Hay agua acumulada en un tramo de carretera. ¿Dónde está el riesgo?", listOf("El aquaplaning y la pérdida de control", "La pintura seca del arcén", "El vehículo aparcado legalmente", "La señal que ya has pasado"), 0, "El agua puede hacer que los neumáticos pierdan contacto con el asfalto."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Una rama grande ocupa parte del carril tras una tormenta. ¿Qué debes hacer?", listOf("Reducir y esquivarla solo cuando tengas visibilidad y espacio", "Acelerar para pasar antes", "Usar el móvil para fotografiarla", "Invadir el carril contrario sin mirar"), 0, "Un obstáculo en la calzada exige anticipación, velocidad reducida y una maniobra segura."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Ves piedras pequeñas en la calzada junto a un talud. ¿Qué peligro indican?", listOf("Un posible desprendimiento mayor", "Una zona de aparcamiento", "Un carril reversible", "Un paso exclusivo de peatones"), 0, "Las piedras pueden anunciar nuevos desprendimientos; aumenta la distancia y extrema la atención."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Un vehículo lleva una carga que sobresale y se mueve. ¿Qué riesgo hay?", listOf("Que la carga caiga o golpee a otros usuarios", "Que el vehículo consuma menos", "Que la vía se ensanche", "Que mejore la visibilidad"), 0, "Una carga mal asegurada puede desprenderse y convertirse en un obstáculo grave."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Llegas a una zona de obras con un carril cerrado. ¿Qué debes vigilar?", listOf("Los cambios de trayectoria y los trabajadores", "El edificio más alto", "La señal que queda lejos", "El color del cielo"), 0, "En obras pueden cambiar los carriles, aparecer operarios y reducirse el espacio de seguridad."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Un trabajador está junto a conos, pero no ves toda la calzada. ¿Cuál es el peligro?", listOf("Un vehículo o maquinaria que aparece oculto tras los conos", "La acera despejada", "El poste de luz", "La señal de destino"), 0, "Los conos no garantizan visibilidad; debes reducir y seguir las indicaciones provisionales."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "La circulación se estrecha por una obra y dos vehículos quieren pasar a la vez. ¿Qué riesgo existe?", listOf("Un roce o desplazamiento hacia la zona de trabajo", "Más espacio lateral", "Que desaparezca la obra", "Que el arcén se convierta en carril"), 0, "En un estrechamiento hay que ceder, mantener separación lateral y evitar competir por el paso."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Hay una retención y un vehículo de emergencia se aproxima por detrás. ¿Qué peligro debes evitar?", listOf("Bloquear el corredor de emergencia", "Dejar espacio", "Reducir progresivamente", "Observar los retrovisores"), 0, "Debes facilitar el paso al vehículo prioritario y no ocupar el corredor de emergencia."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "Te aproximas a una glorieta con vehículos esperando. ¿Qué riesgo anticipas?", listOf("Un vehículo que entra sin respetar la prioridad", "La señal de glorieta", "La isleta central", "El cielo despejado"), 0, "Observa a quienes se incorporan y prepara una frenada aunque tengas prioridad."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Un coche está detenido con las luces de emergencia en una curva. ¿Cuál es el peligro?", listOf("Que esté ocultando un obstáculo o una avería", "Que las luces creen más adherencia", "Que el coche acelere solo", "Que la curva desaparezca"), 0, "Un vehículo detenido en una curva puede ocultar personas, objetos o tráfico parado."),
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Una persona abre la puerta de un coche aparcado junto a tu carril. ¿Qué peligro aparece?", listOf("La puerta puede invadir tu trayectoria", "El coche puede ganar velocidad", "La acera puede moverse", "El retrovisor aumenta el carril"), 0, "Deja separación lateral y reduce al pasar junto a vehículos aparcados."),
    HazardScene("game_hazard05", R.drawable.game_hazard05, "Te acercas a un accidente reciente. ¿Qué peligro no debes olvidar?", listOf("Personas en la calzada y vehículos que frenan de repente", "La carretera completamente despejada", "El paisaje", "La señal de población"), 0, "En un accidente puede haber víctimas, restos y maniobras inesperadas de otros conductores."),
    HazardScene("game_hazard06", R.drawable.game_hazard06, "Un vehículo aparece circulando en sentido contrario. ¿Qué debes identificar?", listOf("El riesgo de colisión frontal", "Una incorporación normal", "Un vehículo estacionado", "Una plaza libre"), 0, "Un vehículo en sentido contrario exige reducir, mantenerse a la derecha y evitar movimientos bruscos."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "El semáforo acaba de ponerse verde, pero un peatón todavía está cruzando. ¿Cuál es el peligro?", listOf("Arrancar sin respetar al peatón que aún ocupa el paso", "El semáforo de la calle transversal", "El bordillo", "La señal de estacionamiento"), 0, "El peatón debe terminar de cruzar antes de iniciar la marcha."),
    HazardScene("game_hazard06", R.drawable.game_hazard06, "Vas a adelantar a una bicicleta y se aproxima una curva. ¿Dónde está el riesgo?", listOf("No disponer de visibilidad y separación suficientes", "La línea recta detrás de ti", "El arcén vacío", "La señal de destino"), 0, "No adelantes si no puedes garantizar visibilidad, distancia lateral y reincorporación segura."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Un autobús señaliza su salida de una parada en una calle urbana. ¿Qué peligro hay?", listOf("Que se incorpore y oculte a peatones", "Que la parada se desplace", "Que el autobús reduzca su tamaño", "Que la acera se convierta en carretera"), 0, "Facilita la salida del autobús y presta atención a los peatones que pueden cruzar delante."),
    HazardScene("game_hazard02", R.drawable.game_hazard02, "Escuchas una sirena, pero todavía no ves el vehículo prioritario. ¿Qué debes prever?", listOf("Que aparezca desde un cruce o detrás de otro vehículo", "Que la sirena indique que puedes acelerar", "Que la vía esté cerrada siempre", "Que no haya que mirar espejos"), 0, "Reduce, facilita el paso y busca el origen de la sirena antes de continuar."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Las barreras de un paso a nivel comienzan a bajar. ¿Cuál es el peligro?", listOf("Quedar atrapado sobre las vías", "Que el tren frene por ti", "Que el arcén sea más ancho", "Que la barrera permita aparcar"), 0, "Nunca entres en el paso a nivel si existe riesgo de quedar detenido sobre las vías."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Un tranvía se aproxima y varios peatones esperan junto a la vía. ¿Qué riesgo anticipas?", listOf("Un cruce apresurado de peatones delante del tranvía", "La señal de aparcamiento", "El edificio cercano", "La línea de fachada"), 0, "Hay que moderar la velocidad y observar a los peatones junto a las vías."),
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Un vehículo tapa parcialmente un cruce y quieres avanzar. ¿Qué peligro existe?", listOf("No ver al tráfico que cruza hasta estar demasiado cerca", "Tener más visibilidad", "Que el cruce se convierta en una rotonda", "Que el vehículo se vuelva transparente"), 0, "La falta de visibilidad en un cruce obliga a avanzar con extrema precaución."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Un coche está parado en el arcén con una persona fuera. ¿Qué debes vigilar?", listOf("La persona puede entrar en la calzada", "El color de la matrícula", "La señal que queda atrás", "El árbol del margen"), 0, "Al pasar junto a una avería debes dejar espacio y prever movimientos de la persona."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Notas que te cuesta mantener la atención después de muchas horas conduciendo. ¿Cuál es el peligro?", listOf("La fatiga y una reacción más lenta ante cualquier obstáculo", "Que los neumáticos se enfríen", "Que el tráfico desaparezca", "Que el coche tenga más potencia"), 0, "La fatiga reduce la atención y aumenta el tiempo de reacción; hay que descansar."),
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Un peatón camina mirando el móvil cerca del bordillo. ¿Qué peligro debes anticipar?", listOf("Que cambie de dirección y entre en la calzada sin mirar", "Que la acera se ensanche", "Que el móvil controle el tráfico", "Que el semáforo cambie por el peatón"), 0, "Un peatón distraído puede invadir la calzada; reduce y mantén una trayectoria prudente."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Un vehículo que viene de frente utiliza luces largas y te deslumbra. ¿Qué riesgo tienes?", listOf("Perder momentáneamente la visión de la calzada", "Ver mejor el arcén contrario", "Frenar con más eficacia", "Tener prioridad automática"), 0, "El deslumbramiento puede ocultar obstáculos; reduce y evita mirar directamente a los faros."),
    HazardScene("game_hazard04", R.drawable.game_hazard04, "Ves un bache profundo después de una curva. ¿Cuál es el peligro?", listOf("Dañar el vehículo o perder el control al pasarlo rápido", "Que el bache mejore la adherencia", "Que se detenga el tráfico contrario", "Que aumente la visibilidad"), 0, "Reduce antes del bache y evita maniobras bruscas o cambios de carril sin comprobar."),
    HazardScene("game_hazard03", R.drawable.game_hazard03, "Un autobús escolar está detenido con personas cerca de la calzada. ¿Qué peligro anticipas?", listOf("Que un menor cruce por delante o por detrás del autobús", "Que el autobús cambie de color", "Que la carretera se ensanche", "Que la señal de destino se mueva"), 0, "Los autobuses ocultan a los menores; reduce mucho y prepárate para detenerte."),
    HazardScene("game_hazard01", R.drawable.game_hazard01, "Es de noche y un peatón viste ropa oscura junto al arcén. ¿Dónde está el riesgo?", listOf("Que no lo detectes hasta tenerlo demasiado cerca", "Que sus reflejos iluminen la vía", "Que el arcén tenga prioridad", "Que la ropa cambie el límite de velocidad"), 0, "Con poca visibilidad debes moderar la velocidad y observar especialmente los márgenes."),
)

/** Randomizes the answer order while preserving which option is the real danger. */
private fun HazardScene.randomized(): HazardScene {
    val correctAnswer = answerOptions[correctAnswerIndex]
    val shuffledOptions = answerOptions.shuffled()
    return copy(
        answerOptions = shuffledOptions,
        correctAnswerIndex = shuffledOptions.indexOf(correctAnswer),
    )
}

/**
 * Risk-perception mini-game: an illustrated traffic scene appears and the player must identify
 * the real threat before the timer runs out.
 *
 * @param sessionSeed shared across this session's rounds so the same scene doesn't repeat before
 *   every other one in [SCENES] has been shown (see [pickForRound])
 * @param roundIndex the current round number within the session (0-based)
 * @param onRoundResult called once with whether the round was won, after the player continues
 *   past the feedback panel
 */
@Composable
fun EncuentraElPeligroGame(sessionSeed: Long, roundIndex: Int, onRoundResult: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scene = remember(sessionSeed, roundIndex) { pickForRound(SCENES, sessionSeed, roundIndex).randomized() }
    var timeLeftMs by remember { mutableLongStateOf(ROUND_TIME_MS) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        while (isActive && timeLeftMs > 0 && outcome == null) {
            delay(TICK_MS)
            timeLeftMs = (timeLeftMs - TICK_MS).coerceAtLeast(0)
        }
        if (outcome == null) outcome = false
    }

    /** Records the selected danger and reveals the answer once the player taps an option. */
    fun choose(index: Int) {
        if (outcome != null) return
        outcome = index == scene.correctAnswerIndex
    }

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ShrinkingTimerBar(
            progress = timeLeftMs / ROUND_TIME_MS.toFloat(),
            remainingSeconds = ((timeLeftMs + 999L) / 1000L).toInt(),
        )
        Text(scene.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(LessonQuestionBank.gameAsset(scene.imageKey))
                        .crossfade(true)
                        .build(),
                    contentDescription = scene.prompt,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(scene.fallbackResource),
                )
            }
        }
        if (outcome == null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scene.answerOptions.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEachIndexed { columnIndex, option ->
                            val index = rowIndex * 2 + columnIndex
                            Surface(
                                modifier = Modifier.weight(1f).clickable { choose(index) },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                            ) {
                                Text(
                                    option,
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            RoundFeedbackPanel(
                isSuccess = outcome == true,
                title = if (outcome == true) "¡Lo viste! 👀" else "El peligro real era otro",
                explanation = scene.explanation,
                onContinue = { onRoundResult(outcome == true) },
            )
        }
    }
}
