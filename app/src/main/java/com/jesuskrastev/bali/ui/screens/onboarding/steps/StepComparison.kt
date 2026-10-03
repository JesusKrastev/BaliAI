package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * Theory exams for the B licence sat in Spain in 2025, from the DGT's monthly exam microdata
 * (dgt.es, DGT en cifras, microdatos de exámenes por autoescuela: `PRUEBA TEÓRICA`, permiso B,
 * January to December 2025). Counted on 2026-10-03.
 */
internal const val DGT_THEORY_EXAMS_2025 = 1_069_167

/** Of [DGT_THEORY_EXAMS_2025], the ones marked "no apto". */
internal const val DGT_THEORY_FAILS_2025 = 531_286

/** Sheets per row in the picture: one row passed, one row failed, which is what 49.7 % looks like. */
private const val SHEETS_PER_ROW = 5

/** Delay between one sheet and the next as they land. */
private const val SHEET_STAGGER_MS = 70

private val SPANISH = Locale("es", "ES")

/**
 * The real stakes, from the DGT's own figures: in 2025 half of the theory exams for the B licence
 * ended in a fail. Ten exam sheets make the number visible at a glance, and the title answers
 * back the attempt the user just told us about.
 *
 * It replaced a chart that put a made-up 89 % "with Bali" next to an unsourced average: every
 * number on this screen can be checked.
 *
 * @param data the answers collected so far, used for the title and the closing line
 */
@Composable
fun StepComparison(data: OnboardingData) {
    val primary = MaterialTheme.colorScheme.primary
    val failRate = DGT_THEORY_FAILS_2025.toDouble() / DGT_THEORY_EXAMS_2025
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(SHEETS_PER_ROW * 2 * SHEET_STAGGER_MS + 400)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildTitle(data).highlightPipes(primary),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetRow(passed = true, rowIndex = 0, progress = { appear.value })
                SheetRow(passed = false, rowIndex = 1, progress = { appear.value })
                Spacer(modifier = Modifier.height(4.dp))
                Legend(color = BaliAccentGreen, text = "${percent(1 - failRate)} aprobados")
                Legend(color = BaliAccentRed, text = "${percent(failRate)} suspensos")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Fuente: DGT. Exámenes teóricos del permiso B en 2025 " +
                "(${NumberFormat.getIntegerInstance(SPANISH).format(DGT_THEORY_EXAMS_2025)} exámenes).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = buildHeadline(data).highlightPipes(primary),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

/**
 * Formats a rate the Spanish way, with one decimal: "49,7 %".
 *
 * @param rate a fraction between 0 and 1
 * @return the percentage
 */
private fun percent(rate: Double): String = String.format(SPANISH, "%.1f %%", rate * 100)

/**
 * A row of exam sheets that land one after another.
 *
 * @param passed whether the row shows passed (green tick) or failed (red cross) sheets
 * @param rowIndex 0 for the first row, so the second one lands after it
 * @param progress the entrance, from 0 to 1, shared by both rows
 */
@Composable
private fun SheetRow(passed: Boolean, rowIndex: Int, progress: () -> Float) {
    val color = if (passed) BaliAccentGreen else BaliAccentRed
    val total = SHEETS_PER_ROW * 2
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(SHEETS_PER_ROW) { column ->
            val order = rowIndex * SHEETS_PER_ROW + column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.78f)
                    .graphicsLayer {
                        val start = order / total.toFloat()
                        val local = ((progress() - start) * total).coerceIn(0f, 1f)
                        alpha = local
                        scaleX = 0.7f + 0.3f * local
                        scaleY = 0.7f + 0.3f * local
                    }
                    .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (passed) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * One line of the legend under the sheets.
 *
 * @param color the colour of the sheets it names
 * @param text what they stand for
 */
@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Text(
            text = text,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Picks the title, which answers back the attempt the user just told us about.
 *
 * @param data the answers collected during the onboarding flow
 * @return the title, with the words to highlight wrapped in pipes
 */
private fun buildTitle(data: OnboardingData): String = when (data.experience) {
    OnboardingConfig.EXPERIENCE_FIRST_TIME ->
        "En 2025, |1 de cada 2| exámenes teóricos acabó en suspenso. No tiene que ser tu caso."
    OnboardingConfig.EXPERIENCE_RETRY ->
        "Ya sabes lo que se siente: en 2025, |1 de cada 2| exámenes teóricos acabó en suspenso. Esta vez, no."
    else -> "En 2025, |1 de cada 2| exámenes teóricos acabó en suspenso."
}

/**
 * Picks the closing line under the picture, which turns the statistic into what the user's own
 * plan does about it.
 *
 * @param data the answers collected during the onboarding flow
 * @return the closing line, with the words to highlight wrapped in pipes
 */
private fun buildHeadline(data: OnboardingData): String = when (data.experience) {
    OnboardingConfig.EXPERIENCE_FIRST_TIME ->
        "Llegar al examen con un método desde el día uno es tu |mayor ventaja|."
    OnboardingConfig.EXPERIENCE_RETRY ->
        "La segunda vez se aprueba |con método|, no repitiendo lo mismo."
    else -> "Con un método, |no tienes por qué estar en esa mitad|."
}

@Preview(showBackground = true)
@Composable
private fun StepComparisonPreview() {
    BaliTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StepComparison(data = OnboardingData(experience = OnboardingConfig.EXPERIENCE_FIRST_TIME))
        }
    }
}
