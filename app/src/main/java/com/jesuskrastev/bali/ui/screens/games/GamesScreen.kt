package com.jesuskrastev.bali.ui.screens.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.games.drive.BaliDriveCover

/** Shows Bali Drive's preview and invokes [onGameClick] when the player chooses to play. */
@Composable
fun GamesScreen(onGameClick: (GameType) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.drive_games), style = MaterialTheme.typography.headlineLarge)
            BaliDriveCover(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp)))
            Text("Bali Drive", style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.drive_description), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { onGameClick(GameType.DRIVE) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.drive_play))
            }
        }
    }
}
