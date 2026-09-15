package com.jesuskrastev.bali.ui.screens.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliSecondary

/** Shows the arcade catalogue and routes a selected mini-game through [onGameClick]. */
@Composable
fun GamesScreen(onGameClick: (GameType) -> Unit, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var selectedDifficulty by remember { mutableStateOf<GameDifficulty?>(null) }
    val visibleGames = GameType.entries.filter { selectedDifficulty == null || GAME_DIFFICULTY[it] == selectedDifficulty }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ArcadeBackdrop(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GamesTitle()
            DifficultyFilterRow(
                selected = selectedDifficulty,
                available = AVAILABLE_DIFFICULTIES,
                onSelect = { selectedDifficulty = it },
            )
            GameCatalogueHeader()
            visibleGames.forEach { game ->
                val featured = game == GameType.PUNTOS_CARNE && selectedDifficulty == null
                GameCover(
                    game = game,
                    onClick = { onGameClick(game) },
                    modifier = Modifier.height(if (featured) 246.dp else 196.dp),
                    featured = featured,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Draws a subtle arcade-grid background behind the catalogue without competing with its cover art. */
@Composable
private fun ArcadeBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gridSpacing = 32.dp.toPx()
        val lineColor = BaliSecondary.copy(alpha = 0.035f)
        for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
            drawLine(lineColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
        }
        for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
            drawLine(lineColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
        }
        drawCircle(
            color = BaliPrimary.copy(alpha = 0.055f),
            radius = size.minDimension * 0.4f,
            center = Offset(size.width, 0f),
        )
    }
}

/** Bold page title introducing the mini-game catalogue. */
@Composable
private fun GamesTitle() {
    Text(
        text = "Juegos",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Black,
    )
}

/**
 * Horizontal, scrollable row of chips that filters the catalogue by [GameDifficulty] — the same
 * category-filter pattern most catalogue-style mobile screens (stores, streaming apps) use to let
 * players jump straight to the challenge level they want.
 *
 * @param selected difficulty currently applied, or null to show every game
 * @param available difficulties with at least one game, in display order
 * @param onSelect invoked with the tapped difficulty, or null when "Todos" is tapped
 */
@Composable
private fun DifficultyFilterRow(
    selected: GameDifficulty?,
    available: List<GameDifficulty>,
    onSelect: (GameDifficulty?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label = "Todos",
            selected = selected == null,
            selectedBackground = BaliPrimary,
            selectedContent = Color.White,
            onClick = { onSelect(null) },
        )
        available.forEach { difficulty ->
            FilterPill(
                label = difficulty.label,
                selected = selected == difficulty,
                selectedBackground = difficulty.background,
                selectedContent = difficulty.content,
                onClick = { onSelect(if (selected == difficulty) null else difficulty) },
            )
        }
    }
}

/**
 * Pill-shaped, selectable filter button using the same solid-color, bold-label treatment as
 * [StatPill] and [PlayCta], so the filter row reads as part of the arcade UI instead of a
 * generic Material chip.
 *
 * @param label text shown on the pill
 * @param selected whether this filter is the one currently applied
 * @param selectedBackground pill color while [selected] is true; a difficulty's own color when
 * the pill represents a [GameDifficulty], so the active filter echoes the badges on the cards below
 * @param selectedContent label color while [selected] is true
 * @param onClick invoked when the pill is tapped
 */
@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    selectedBackground: Color,
    selectedContent: Color,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        if (selected) selectedBackground else MaterialTheme.colorScheme.surfaceVariant,
        label = "filter_pill_background",
    )
    val content by animateColorAsState(
        if (selected) selectedContent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "filter_pill_content",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = background,
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            color = content,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 0.4.sp,
        )
    }
}

/** Separates the introductory content from the selectable mini-game catalogue. */
@Composable
private fun GameCatalogueHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Elige tu reto",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Cada partida tiene $ROUNDS_PER_SESSION rondas rápidas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Maps every current mini-game to its dedicated illustrated cover. */
private val COVER_RESOURCES: Map<GameType, Int> = mapOf(
    GameType.PUNTOS_CARNE to R.drawable.game_puntos_carne_cover,
    GameType.SENAL to R.drawable.game_senal_relampago_cover,
    GameType.LEGAL_O_MULTA to R.drawable.game_legal_o_multa_cover,
    GameType.PELIGRO to R.drawable.game_peligro_cover,
    GameType.PRIORIDAD_CRUCE to R.drawable.game_prioridad_cruce_cover,
)

/** Per-game accent used by the emoji + gradient fallback cover for games without illustrated art yet. */
private val COVER_ACCENTS: Map<GameType, Color> = mapOf(
    GameType.PUNTOS_CARNE to Color(0xFFF59E0B),
    GameType.LEGAL_O_MULTA to Color(0xFF7C3AED),
    GameType.PELIGRO to Color(0xFFDC2626),
    GameType.PRIORIDAD_CRUCE to Color(0xFF0EA5E9),
)

/** Relative challenge level shown on a [GameCover], paired with the colors that convey it at a glance. */
private enum class GameDifficulty(val label: String, val background: Color, val content: Color) {
    FACIL("FÁCIL", BaliAccentGreen, Color.White),
    MEDIA("MEDIA", BaliAccentYellow, BaliSecondary),
    DIFICIL("DIFÍCIL", BaliAccentRed, Color.White),
}

/** Maps every mini-game to the difficulty pill shown on its catalogue card. */
private val GAME_DIFFICULTY: Map<GameType, GameDifficulty> = mapOf(
    GameType.PUNTOS_CARNE to GameDifficulty.MEDIA,
    GameType.SENAL to GameDifficulty.MEDIA,
    GameType.LEGAL_O_MULTA to GameDifficulty.MEDIA,
    GameType.PELIGRO to GameDifficulty.DIFICIL,
    GameType.PRIORIDAD_CRUCE to GameDifficulty.DIFICIL,
)

/** Difficulties that currently have at least one game, in ascending display order. */
private val AVAILABLE_DIFFICULTIES: List<GameDifficulty> = GameDifficulty.entries.filter { it in GAME_DIFFICULTY.values }

/**
 * Renders an illustrated game card with a high-contrast information panel and an explicit play CTA.
 *
 * @param game game represented by the card
 * @param onClick action that starts [game]
 * @param modifier layout customisation for the card
 * @param featured whether the card represents the highlighted game of the arcade
 */
@Composable
private fun GameCover(
    game: GameType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
) {
    val coverResource = COVER_RESOURCES[game]
    val difficulty = GAME_DIFFICULTY[game] ?: GameDifficulty.MEDIA
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "game_cover_press_scale")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale },
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp, pressedElevation = 2.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            if (coverResource != null) {
                Image(
                    painter = painterResource(coverResource),
                    contentDescription = "Portada 2D de ${game.title} con Bali",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                val accent = COVER_ACCENTS[game] ?: BaliPrimary
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(accent, Color(0xFF0F172A)))),
                )
                Text(
                    game.icon,
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).alpha(0.35f),
                    fontSize = 72.sp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.2f),
                            1f to Color.Black.copy(alpha = 0.95f),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatPill(text = difficulty.label, background = difficulty.background, content = difficulty.content)
                StatPill(
                    text = "$ROUNDS_PER_SESSION RONDAS",
                    background = Color.Black.copy(alpha = 0.38f),
                    content = Color.White,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        game.title,
                        style = if (featured) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        game.subtitle,
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    PlayCta()
                }
            }
        }
    }
}

/** Small pill used to surface one glance-able stat (difficulty, question count, featured badge) on a [GameCover]. */
@Composable
private fun StatPill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = background) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = content,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 0.35.sp,
        )
    }
}

/** Displays the consistent primary action used to start any [GameCover]. */
@Composable
private fun PlayCta(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = BaliPrimary, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                "JUGAR AHORA",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 0.4.sp,
            )
        }
    }
}
