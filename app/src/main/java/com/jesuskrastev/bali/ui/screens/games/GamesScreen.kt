package com.jesuskrastev.bali.ui.screens.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.games.drive.BaliDriveCover
import com.jesuskrastev.bali.ui.screens.games.drive.DriveIcon
import com.jesuskrastev.bali.ui.screens.games.drive.DriveIconKind
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import com.jesuskrastev.bali.ui.theme.RacingFont
import com.jesuskrastev.bali.ui.util.LightSystemBarIcons
import com.jesuskrastev.bali.ui.util.drawSafe

/**
 * Fills the Games tab with Bali Drive's live world, painted under the status bar; the title stays
 * below it and the Jugar button sits above the app's bottom bar.
 * @param onGameClick opens the selected game when Jugar is pressed
 * @param modifier lays out the preview inside the app's bottom-bar scaffold
 * @return the full Games tab content
 */
@Composable
fun GamesScreen(onGameClick: (GameType) -> Unit, modifier: Modifier = Modifier) {
    LightSystemBarIcons()
    Box(modifier.fillMaxSize()) {
        BaliDriveCover(Modifier.fillMaxSize(), fullScreen = true)
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.drawSafe.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArcadeTag(stringResource(R.string.drive_games).uppercase())
            Text(
                "BALI DRIVE",
                modifier = Modifier.padding(top = 10.dp),
                color = Color.White,
                style = TextStyle(
                    fontFamily = RacingFont,
                    fontSize = 46.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 2.sp,
                    shadow = Shadow(BaliPrimary, Offset(0f, 7f), blurRadius = 0f),
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.drive_description),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(shadow = Shadow(Color.Black, Offset(0f, 2f), 6f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.drive_cover_prompt),
                color = BaliAccentYellow,
                fontFamily = RacingFont,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.titleMedium.copy(shadow = Shadow(Color.Black, Offset(0f, 2f), 6f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            PlayButton(onClick = { onGameClick(GameType.DRIVE) })
            Row(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DriveIcon(DriveIconKind.FINGER, Modifier.size(18.dp), Color.White)
                Text(
                    stringResource(R.string.drive_cover_controls),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelMedium.copy(shadow = Shadow(Color.Black, Offset(0f, 2f), 6f)),
                    fontFamily = RacingFont,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

/** Small arcade-cabinet marquee with [label]; returns the pill above the game's title. */
@Composable
private fun ArcadeTag(label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(BaliSecondary.copy(alpha = 0.88f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DriveIcon(DriveIconKind.FLAG, Modifier.size(16.dp), BaliAccentYellow)
        Text(label, color = BaliAccentYellow, fontFamily = RacingFont, fontSize = 12.sp, letterSpacing = 3.sp)
        DriveIcon(DriveIconKind.FLAG, Modifier.size(16.dp), BaliAccentYellow)
    }
}

/**
 * Big arcade "start" button that breathes gently to invite the tap.
 * @param onClick starts the game
 */
@Composable
private fun PlayButton(onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "drive_play_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "drive_play_scale",
    )
    Box(Modifier.fillMaxWidth().scale(pulse)) {
        // A solid slab under the button gives the chunky, pressable arcade look.
        Box(
            Modifier.matchParentSize().padding(top = 6.dp).background(Color(0xFFB8400F), RoundedCornerShape(26.dp)),
        )
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            shape = RoundedCornerShape(26.dp),
            color = BaliPrimary,
            contentColor = Color.White,
            border = BorderStroke(3.dp, BaliAccentYellow),
            shadowElevation = 12.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(36.dp))
                Text(
                    stringResource(R.string.drive_play),
                    modifier = Modifier.padding(start = 6.dp),
                    fontFamily = RacingFont,
                    fontSize = 28.sp,
                    letterSpacing = 3.sp,
                )
            }
        }
    }
}
