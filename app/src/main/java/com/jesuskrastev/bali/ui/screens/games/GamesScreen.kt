package com.jesuskrastev.bali.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.games.drive.BaliDriveCover
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary

/**
 * Fills the Games tab with Bali Drive's live world.
 * @param onGameClick opens the selected game when Jugar is pressed
 * @param modifier lays out the preview inside the app's bottom-bar scaffold
 * @return the full Games tab content
 */
@Composable
fun GamesScreen(onGameClick: (GameType) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        BaliDriveCover(Modifier.fillMaxSize(), fullScreen = true)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.drive_games).uppercase(),
                color = BaliAccentYellow,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )
            Text(
                "BALI DRIVE",
                color = Color.White,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.drive_description),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.drive_cover_prompt),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Surface(
                onClick = { onGameClick(GameType.DRIVE) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = BaliPrimary,
                contentColor = Color.White,
                border = BorderStroke(3.dp, BaliAccentYellow),
                shadowElevation = 10.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 24.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
                    Text(
                        stringResource(R.string.drive_play),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                }
            }
            Text(
                stringResource(R.string.drive_cover_controls),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
