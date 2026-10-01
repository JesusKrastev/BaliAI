package com.jesuskrastev.bali.ui.screens.streak

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.DailyStreak

/**
 * Page with the whole picture of daily streak momentum: its speedometer, the week, freezes and
 * the record.
 *
 * @param viewModel supplies the streak
 * @param onBackClick invoked by the back arrow
 * @param onShopClick opens the shop, where freezes are bought
 */
@Composable
fun StreakScreen(
    viewModel: StreakViewModel,
    onBackClick: () -> Unit,
    onShopClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StreakContent(uiState = uiState, onBackClick = onBackClick, onShopClick = onShopClick)
}

/**
 * Stateless body of [StreakScreen].
 *
 * @param uiState the streak to show
 * @param onBackClick invoked by the back arrow
 * @param onShopClick opens the shop
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakContent(
    uiState: StreakUiState,
    onBackClick: () -> Unit,
    onShopClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tu racha", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StreakHero(uiState)

            Section(title = "Actividad de esta semana") {
                StreakCard { StreakWeek(uiState.week) }
            }
            Section(title = "Congeladores") {
                FreezesCard(freezes = uiState.streakFreezes, onShopClick = onShopClick)
            }
            Section(title = "Tu récord") {
                RecordCard(current = uiState.currentStreak, highest = uiState.highestStreak)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * A titled block of the page.
 *
 * @param title the section title
 * @param content the section body
 */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StreakSectionLabel(title)
        content()
    }
}

/**
 * The speedometer, its current level and what the user can do today.
 *
 * @param uiState the streak to show
 */
@Composable
private fun StreakHero(uiState: StreakUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StreakSpeedometer(level = uiState.currentStreak)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ritmo de racha",
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (uiState.currentStreak == DailyStreak.MAX_LEVEL) {
                "¡Velocidad máxima alcanzada!"
            } else {
                "Un test al día suma velocidad."
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        val atRisk = uiState.currentStreak > 0 && !uiState.practicedToday
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (atRisk) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ) {
            Text(
                text = todayMessage(uiState),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (atRisk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * What today means for the streak.
 *
 * @param uiState the streak to describe
 * @return one line for the hero
 */
private fun todayMessage(uiState: StreakUiState): String = when {
    uiState.practicedToday -> "Hoy ya has acelerado. ¡Vuelve mañana!"
    uiState.currentStreak > 0 -> "Haz un test hoy para mantener el ritmo."
    else -> "Haz un test hoy y enciende el motor."
}

/**
 * The freezes left, what they do and a way to get more.
 *
 * @param freezes freezes still available
 * @param onShopClick opens the shop
 */
@Composable
private fun FreezesCard(freezes: Int, onShopClick: () -> Unit) {
    StreakCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.streak_freezer),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tienes $freezes de ${DailyStreak.MAX_FREEZES}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Si un día no estudias, se gasta uno y mantienes tu velocidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (freezes < DailyStreak.MAX_FREEZES) {
                    TextButton(onClick = onShopClick, contentPadding = PaddingValues(0.dp)) {
                        Text("Conseguir en la tienda →", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * The highest speed reached and how far the current momentum is from it.
 *
 * @param current current streak momentum
 * @param highest highest momentum ever reached
 */
@Composable
private fun RecordCard(current: Int, highest: Int) {
    StreakCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🏆", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Velocidad máxima",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$highest / ${DailyStreak.MAX_LEVEL}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = recordMessage(current, highest),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * How the current streak momentum compares with the record.
 *
 * @param current current streak momentum
 * @param highest highest momentum ever reached
 * @return one line under the record
 */
private fun recordMessage(current: Int, highest: Int): String = when {
    highest == 0 -> "Tu primer test pondrá el velocímetro en marcha."
    current >= highest && highest < DailyStreak.MAX_LEVEL -> "Estás en tu mejor ritmo: mañana subes otro nivel."
    current >= highest -> "Estás a velocidad máxima. ¡Mantén el ritmo!"
    else -> {
        val toMatch = highest - current
        "Te faltan $toMatch ${daysWord(toMatch)} para recuperar tu mejor velocidad."
    }
}
