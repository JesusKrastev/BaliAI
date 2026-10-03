package com.jesuskrastev.bali.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.jesuskrastev.bali.ui.theme.BaliGrayMedium
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.White

/**
 * Renders the primary app navigation: Home, the AI tutor chat, the statistics, Games and
 * Settings. The chat and the statistics are tabs like the others, so the bar stays visible
 * while they are open.
 *
 * @param currentDestination back stack entry's destination, used to highlight the active tab
 * @param onHomeClick navigates to [HomeRoute]
 * @param onGamesClick navigates to [GamesRoute]
 * @param onSettingsClick navigates to [SettingsRoute]
 * @param onChatClick navigates to [ChatRoute]
 * @param onStatsClick navigates to [StatsRoute]
 */
@Composable
internal fun AppBottomBar(
    currentDestination: NavDestination?,
    onHomeClick: () -> Unit,
    onGamesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onChatClick: () -> Unit,
    onStatsClick: () -> Unit,
) {
    // The bar's background is drawn flush to the true bottom edge (behind the system nav bar,
    // matching edge-to-edge), but the tappable row is lifted above it by the real nav bar inset
    // so icons/labels are never obscured or partially unreachable on gesture or 3-button nav.
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp + navBarInset),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp + navBarInset)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {}
        // No fixed height here: the row must grow with navBarInset (via the bottom padding
        // below) or the fixed-height tabs get squeezed shorter than their content on every
        // device with a system nav bar, which is what caused the icons/labels to overlap.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp + navBarInset),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BaliBottomBarItem(
                label = "Inicio",
                icon = Icons.Rounded.Home,
                selected = currentDestination?.hasRoute<HomeRoute>() == true,
                onClick = onHomeClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Chat",
                icon = Icons.Rounded.ChatBubble,
                selected = currentDestination?.hasRoute<ChatRoute>() == true,
                onClick = onChatClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Progreso", // "Estadísticas" no cabe en una pestaña de cinco y se parte en dos líneas
                icon = Icons.Rounded.BarChart,
                selected = currentDestination?.hasRoute<StatsRoute>() == true,
                onClick = onStatsClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Juegos",
                icon = Icons.Rounded.SportsEsports,
                selected = currentDestination?.hasRoute<GamesRoute>() == true,
                onClick = onGamesClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Ajustes",
                icon = Icons.Rounded.Settings,
                selected = currentDestination?.hasRoute<SettingsRoute>() == true,
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Draws one primary-navigation tab using the raised orange selection from the visual reference.
 *
 * @param label accessible text shown below the icon
 * @param icon Material icon associated with the destination
 * @param selected whether this is the route currently displayed
 * @param onClick action that navigates to the destination
 * @param modifier layout modifier applied to the tab's touch target
 */
@Composable
private fun BaliBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) BaliPrimary else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "bottom_bar_indicator_color",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) White else BaliGrayMedium,
        animationSpec = tween(durationMillis = 180),
        label = "bottom_bar_icon_color",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) BaliPrimary else BaliGrayMedium,
        animationSpec = tween(durationMillis = 180),
        label = "bottom_bar_label_color",
    )
    val indicatorElevation by animateDpAsState(
        targetValue = if (selected) 7.dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_indicator_elevation",
    )
    val indicatorOffset by animateDpAsState(
        targetValue = if (selected) (-8).dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_indicator_offset",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 44.dp else 36.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_indicator_width",
    )
    val underlineWidth by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "bottom_bar_underline_width",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 0.94f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_icon_scale",
    )

    Column(
        modifier = modifier
            .height(70.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .offset(y = indicatorOffset)
                .shadow(elevation = indicatorElevation, shape = RoundedCornerShape(13.dp))
                .clip(RoundedCornerShape(13.dp))
                .background(indicatorColor)
                .width(indicatorWidth)
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(underlineWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(BaliPrimary),
        )
    }
}
