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

private const val ROUND_TIME_MS = 8000L
private const val TICK_MS = 50L

private enum class SignCategory(val label: String) {
    PELIGRO("Peligro"),
    PROHIBICION("Prohibición"),
    OBLIGACION("Obligación"),
    INFORMACION("Información"),
    PRIORIDAD("Prioridad"),
}

private data class SignEntry(
    val fileName: String,
    val code: String,
    val name: String,
    val category: SignCategory,
    val isNewCatalogue: Boolean = false,
    val fallbackResource: Int? = null,
    val isRare: Boolean = false,
)

/**
 * DGT signs used by the game, including the current catalogue update effective since July 2025
 * and therefore applicable in 2026. New entries and their official names follow DGT's published
 * catalogue material at revista.dgt.es/es/reportajes/2023/06JUNIO/0620-Nuevas-senales.shtml.
 * The rare-sign entries come from DGT's educational report on signs most often missed in exams.
 */
private val SIGN_POOL = listOf(
    SignEntry("p1", "P-1", "Intersección con prioridad", SignCategory.PELIGRO),
    SignEntry("p3", "P-3", "Proximidad de semáforos", SignCategory.PELIGRO),
    SignEntry("p20a", "P-20a", "Proximidad de un paso de peatones", SignCategory.PELIGRO),
    SignEntry("r100", "R-100", "Prohibido el paso a toda clase de vehículos", SignCategory.PROHIBICION),
    SignEntry("r101", "R-101", "Dirección prohibida", SignCategory.PROHIBICION),
    SignEntry("r102", "R-102", "Prohibido el paso a vehículos de motor", SignCategory.PROHIBICION),
    SignEntry("r301-50", "R-301/50", "Velocidad máxima 50 km/h", SignCategory.PROHIBICION),
    SignEntry("r301-80", "R-301/80", "Velocidad máxima 80 km/h", SignCategory.PROHIBICION),
    SignEntry("r301-110", "R-301/110", "Velocidad máxima 110 km/h", SignCategory.PROHIBICION),
    SignEntry("r305", "R-305", "Prohibido adelantar", SignCategory.PROHIBICION),
    SignEntry("s17", "S-17", "Zona de estacionamiento", SignCategory.INFORMACION),
    SignEntry("s1", "S-1", "Comienzo de autopista", SignCategory.INFORMACION),
    SignEntry("s2", "S-2", "Fin de autopista", SignCategory.INFORMACION),
    SignEntry("r2", "R-2", "STOP: detención obligatoria", SignCategory.PRIORIDAD),
    SignEntry("r1", "R-1", "Ceda el paso", SignCategory.PRIORIDAD),
    SignEntry("p1e", "P-1e", "Tramo con accesos directos", SignCategory.PELIGRO, true, R.drawable.sign_p1e),
    SignEntry("p20c", "P-20c", "Paso para peatones y ciclistas", SignCategory.PELIGRO, true, R.drawable.sign_p20c),
    SignEntry("p24a", "P-24a", "Paso de animales en libertad (jabalíes)", SignCategory.PELIGRO, true, R.drawable.sign_p24a),
    SignEntry("p33", "P-33", "Visibilidad reducida", SignCategory.PELIGRO, true, R.drawable.sign_p33),
    SignEntry("p35", "P-35", "Trenzado", SignCategory.PELIGRO, true, R.drawable.sign_p35),
    SignEntry("r118", "R-118", "Entrada prohibida a vehículos de movilidad personal", SignCategory.PROHIBICION, true, R.drawable.sign_r118),
    SignEntry("r120", "R-120", "Entrada prohibida según distintivo ambiental", SignCategory.PROHIBICION, true, R.drawable.sign_r120),
    SignEntry("s1c", "S-1c", "Carretera 2+1", SignCategory.INFORMACION, true, R.drawable.sign_s1c),
    SignEntry("s47", "S-47", "Zona de coexistencia", SignCategory.INFORMACION, true, R.drawable.sign_s47),
    SignEntry("s51b", "S-51b", "Carril reservado para vehículos con alta ocupación", SignCategory.INFORMACION, true, R.drawable.sign_s51b),
    SignEntry("s892", "S-892", "Vigilancia por medios automáticos", SignCategory.INFORMACION, true, R.drawable.sign_s892),
    SignEntry("r200", "R-200", "Prohibido pasar sin detenerse", SignCategory.PROHIBICION, true, R.drawable.sign_r200, true),
    SignEntry("r405", "R-405", "Calzada obligatoria para motocicletas de dos ruedas", SignCategory.OBLIGACION, true, R.drawable.sign_r405, true),
    SignEntry("s33", "S-33", "Senda ciclable", SignCategory.INFORMACION, true, R.drawable.sign_s33, true),
    SignEntry("r407b", "R-407b", "Vía reservada y obligatoria para ciclomotores", SignCategory.OBLIGACION, true, R.drawable.sign_r407b, true),
    SignEntry("r404", "R-404", "Calzada obligatoria para automóviles, excepto motocicletas de dos ruedas", SignCategory.OBLIGACION, true, R.drawable.sign_r404, true),
    SignEntry("s8", "S-8", "Fin de velocidad máxima aconsejada", SignCategory.INFORMACION, true, R.drawable.sign_s8, true),
    SignEntry("r102", "R-102", "Entrada prohibida a vehículos de motor", SignCategory.PROHIBICION, true, R.drawable.sign_r102, true),
    SignEntry("r105", "R-105", "Entrada prohibida a ciclomotores", SignCategory.PROHIBICION, true, R.drawable.sign_r105, true),
)

/**
 * Sign-recognition mini-game: a real DGT sign appears and the player must identify its exact
 * code and meaning before a shrinking timer runs out.
 *
 * @param sessionSeed shared across this session's rounds so the same sign doesn't repeat before
 *   every other one in [SIGN_POOL] has been shown (see [pickForRound])
 * @param roundIndex the current round number within the session (0-based)
 * @param onRoundResult called once with whether the round was won, after the player continues
 *   past the feedback panel
 */
@Composable
fun SenalRelampagoGame(sessionSeed: Long, roundIndex: Int, onRoundResult: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val sign = remember(sessionSeed, roundIndex) { pickForRound(SIGN_POOL, sessionSeed, roundIndex) }
    val answerOptions = remember(sign) {
        (listOf(sign) + SIGN_POOL.filter { it != sign }.shuffled().take(3)).shuffled()
    }
    var timeLeftMs by remember { mutableLongStateOf(ROUND_TIME_MS) }
    var outcome by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        while (isActive && timeLeftMs > 0 && outcome == null) {
            delay(TICK_MS)
            timeLeftMs = (timeLeftMs - TICK_MS).coerceAtLeast(0)
        }
        if (outcome == null) outcome = false
    }

    /** Records whether [option] identifies the displayed sign before the timer expires. */
    fun choose(option: SignEntry) {
        if (outcome != null) return
        outcome = option == sign
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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            if (sign.isNewCatalogue) {
                                LessonQuestionBank.gameAsset("sign_${sign.fileName}")
                            } else {
                                LessonQuestionBank.signImage(sign.fileName)
                            },
                        )
                        .crossfade(true)
                        .build(),
                    contentDescription = "Señal ${sign.code}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    error = sign.fallbackResource?.let { painterResource(it) },
                )
            }
        }
        if (outcome == null) {
            Text(
                "¿Qué señal es?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                answerOptions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            Surface(
                                modifier = Modifier.weight(1f).clickable { choose(option) },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                            ) {
                                Text(
                                    "${option.code}\n${option.name}",
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                title = if (outcome == true) "¡Correcto! ⚡" else "Esta era ${sign.code}",
                explanation = "${sign.code}: ${sign.name}. Categoría: ${sign.category.label}${when { sign.isRare -> " · Señal poco conocida"; sign.isNewCatalogue -> " · Catálogo actualizado DGT"; else -> "" }}.",
                onContinue = { onRoundResult(outcome == true) },
            )
        }
    }
}
