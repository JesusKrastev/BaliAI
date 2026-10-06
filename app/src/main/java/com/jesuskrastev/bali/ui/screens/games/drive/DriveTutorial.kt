package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliPrimary

/** Shows two legible control demonstrations and a safe practice area before the run starts. */
@Composable
internal fun DriveTutorial(state: DriveUiState, onEvent: (BaliDriveEvent) -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    val brakingStep = state.tutorialStep == 1
    val saving = state.phase == DriveScreenPhase.SAVING_TUTORIAL
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.drive_tutorial_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.drive_tutorial_step, state.tutorialStep + 1), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(if (brakingStep) R.string.drive_tutorial_brake else R.string.drive_tutorial_steer), style = MaterialTheme.typography.titleLarge)
            ControlDemonstration(brakingStep)
            if (brakingStep) Text(stringResource(R.string.drive_tutorial_release), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.drive_practice), style = MaterialTheme.typography.titleMedium)
            key(state.tutorialStep) { DrivePractice() }
            if (state.error) Text(stringResource(R.string.drive_tutorial_error), color = MaterialTheme.colorScheme.error)
            Button(onClick = { onEvent(if (brakingStep) BaliDriveEvent.CompleteTutorial else BaliDriveEvent.NextTutorialStep) },
                enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (saving) R.string.drive_saving else if (brakingStep) R.string.drive_tutorial_start else R.string.drive_next))
            }
            TextButton(onClick = onExit, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.drive_exit)) }
        }
    }
}

/** Animates a sliding finger or a stationary finger with a pulsing brake-pressure indicator. */
@Composable
private fun ControlDemonstration(braking: Boolean) {
    val motion = rememberInfiniteTransition(label = "drive_control_demo")
    val phase by motion.animateFloat(-1f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "finger")
    val color = if (braking) BaliAccentRed else BaliPrimary
    Box(Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(24.dp)) {
            if (braking) drawCircle(color.copy(alpha = 0.22f), (20f + (phase + 1) * 12f).dp.toPx(), center)
            else drawLine(color.copy(alpha = 0.4f), Offset(size.width * 0.2f, size.height / 2), Offset(size.width * 0.8f, size.height / 2), 6.dp.toPx(), StrokeCap.Round)
        }
        DriveIcon(DriveIconKind.FINGER, Modifier.size(60.dp).graphicsLayer { translationX = if (braking) 0f else phase * 70.dp.toPx() }, color)
    }
}

/** Practices the same steering and stationary-hold controls without traffic, score or run time. */
@Composable
private fun DrivePractice() {
    val controls = remember { DriveControls() }
    val density = LocalDensity.current
    var x by remember { mutableFloatStateOf(0f) }
    var braking by remember { mutableStateOf(false) }
    val stillSpeed = with(density) { 90.dp.toPx() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(controls, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = 0L
            try {
                while (true) withInfiniteAnimationFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(1f / 30f)
                    last = now
                    val dx = controls.takeDx()
                    controls.advance(dt, dx, stillSpeed)
                    x = (x + dx).coerceIn(-with(density) { 90.dp.toPx() }, with(density) { 90.dp.toPx() })
                    braking = controls.braking
                }
            } finally { controls.reset() }
        }
    }
    Box(Modifier.fillMaxWidth().height(150.dp).background(DrivePalette.Road, RoundedCornerShape(24.dp)).driveControls(controls), contentAlignment = Alignment.Center) {
        DriveIcon(DriveIconKind.CAR, Modifier.size(50.dp).graphicsLayer { translationX = x }, if (braking) BaliAccentRed else BaliPrimary)
        if (braking) Text(stringResource(R.string.drive_braking), color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}
