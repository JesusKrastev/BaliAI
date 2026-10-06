package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import java.text.NumberFormat
import java.util.Locale

/** Displays a frozen run's statistics with persistent actions and guarded pending rewards. */
@Composable
internal fun DriveResultsDialog(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val pending = result.rewardStatus == DriveRewardStatus.PENDING
    Dialog(onDismissRequest = { if (!pending) onExit() }, properties = DialogProperties(dismissOnClickOutside = false)) {
        DriveResultsContent(result, onReplay, onExit, onStarShown)
    }
}

/** Renders [result] within available height, keeping replay and exit below the scrollable stats. */
@Composable
internal fun DriveResultsContent(result: DriveResult, onReplay: () -> Unit, onExit: () -> Unit, onStarShown: (Int) -> Unit) {
    val summary = result.summary
    val pending = result.rewardStatus == DriveRewardStatus.PENDING
    LaunchedEffect(summary) { (1..summary.rating).forEach(onStarShown) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.9f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.drive_result_title), style = MaterialTheme.typography.headlineSmall)
                    val ratingLabel = stringResource(R.string.drive_rating, summary.rating)
                    Row(Modifier.semantics { contentDescription = ratingLabel }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 1..3) DriveIcon(DriveIconKind.STAR, Modifier.size(40.dp),
                            if (i <= summary.rating) BaliAccentYellow else MaterialTheme.colorScheme.outlineVariant)
                    }
                    ResultStat(DriveIconKind.POINTS, stringResource(R.string.drive_points), points(summary.score))
                    ResultStat(DriveIconKind.RECORD, stringResource(if (result.isNewRecord) R.string.drive_new_record else R.string.drive_record),
                        if (result.isNewRecord) points(summary.score) else points(result.previousBest))
                    if (result.previousBest > 0) {
                        val detail = if (result.isNewRecord) stringResource(R.string.drive_previous_record, points(result.previousBest))
                            else stringResource(R.string.drive_record_detail, points(result.previousBest), points(result.previousBest - summary.score))
                        Text(detail, style = MaterialTheme.typography.bodySmall)
                    }
                    ResultStat(DriveIconKind.CHECK, stringResource(R.string.drive_resolved), "${summary.resolved}/${summary.situations}")
                    ResultStat(DriveIconKind.STAR, stringResource(R.string.drive_collected), "${summary.starsCollected}/${summary.starsTotal}")
                    ResultStat(DriveIconKind.TIME, stringResource(R.string.drive_duration), stringResource(R.string.drive_seconds, summary.durationSeconds))
                    ResultStat(DriveIconKind.ACCURACY, stringResource(R.string.drive_accuracy), "${summary.resolved * 100 / summary.situations.coerceAtLeast(1)} %")
                    when (result.rewardStatus) {
                        DriveRewardStatus.PENDING -> Text(stringResource(R.string.drive_rewards_pending), style = MaterialTheme.typography.bodyMedium)
                        DriveRewardStatus.FAILED -> Text(stringResource(R.string.drive_rewards_failed), color = MaterialTheme.colorScheme.error)
                        DriveRewardStatus.COMPLETE -> result.rewards?.let { rewards ->
                            ResultStat(DriveIconKind.XP, stringResource(R.string.drive_xp), "+${rewards.xpEarned.xpGained}")
                            ResultStat(DriveIconKind.COIN, stringResource(R.string.drive_coins), "+${rewards.coinsGained}")
                            val xp = rewards.xpEarned
                            Text(stringResource(R.string.drive_xp_base, xp.baseXp), style = MaterialTheme.typography.bodySmall)
                            xp.bonusPerfection?.let { Text(stringResource(R.string.drive_xp_perfect, it), style = MaterialTheme.typography.bodySmall) }
                            xp.bonusFast?.let { Text(stringResource(R.string.drive_xp_fast, it), style = MaterialTheme.typography.bodySmall) }
                            xp.bonusStreak?.let { Text(stringResource(R.string.drive_xp_streak, it), style = MaterialTheme.typography.bodySmall) }
                            if (xp.levelUp) Text(stringResource(R.string.drive_level_up, xp.newLevel), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (summary.faults.isNotEmpty()) {
                        Text(stringResource(R.string.drive_faults, summary.faults.size), style = MaterialTheme.typography.titleMedium)
                        summary.faults.forEachIndexed { index, explanation ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DriveIcon(DriveIconKind.FAULT, color = BaliAccentRed)
                                Column(Modifier.weight(1f)) {
                                    summary.faultKinds.getOrNull(index)?.let { Text(it.hint, style = MaterialTheme.typography.labelLarge) }
                                    Text(explanation, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                Button(onClick = onReplay, enabled = !pending, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.drive_replay)) }
                OutlinedButton(onClick = onExit, enabled = !pending, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.drive_exit)) }
            }
        }
    }
}

/** Pairs a new decorative vector [kind] with a readable [label] and full [value]. */
@Composable
private fun ResultStat(kind: DriveIconKind, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DriveIcon(kind)
        if (LocalDensity.current.fontScale >= 1.3f) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Returns [value] formatted with Spanish thousands separators. */
internal fun points(value: Int): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-ES")).format(value)
