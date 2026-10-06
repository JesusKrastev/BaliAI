package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliPrimary

/** Presents distance [progress] with Bali's rounded orange track and accessible progress semantics. */
@Composable
internal fun DriveRouteProgress(progress: Float) {
    val fraction = progress.coerceIn(0f, 1f)
    val label = stringResource(R.string.drive_route_progress)
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).semantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DriveIcon(DriveIconKind.CAR, Modifier.size(24.dp))
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(50)).background(BaliPrimary))
            }
            DriveIcon(DriveIconKind.FLAG, Modifier.size(24.dp))
        }
    }
}
