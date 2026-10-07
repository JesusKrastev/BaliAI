package com.jesuskrastev.bali.ui.screens.test

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.review.InAppReviewEffect
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.util.formatClock
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Pause between the experience finishing counting and the full-screen celebration on a plain result. */
private const val HEADLINE_DELAY_MS = 500L

/** Same pause after a passed exam's stamp, so the student can read the stamp first. */
private const val HEADLINE_AFTER_STAMP_MS = 1_500L

/** Time the screen is shown before the experience starts counting. */
private const val COUNT_START_DELAY_MS = 250L

/** Title of the message shown by the previous result, so two results in a row never repeat it. */
private var lastMessageTitle: String? = null

/** Saves a [ResultMessage] across rotation and process death, so the words never change under the user. */
private val ResultMessageSaver = listSaver<ResultMessage, String>(
    save = { listOf(it.title, it.subtitle) },
    restore = { ResultMessage(it[0], it[1]) }
)

/**
 * How long the experience takes to count up: longer for more experience, within limits so a
 * small result is not slow and a big one does not keep the student waiting.
 *
 * @param xpGained experience earned by the result
 * @return the duration in milliseconds, between 600 and 1500
 */
internal fun xpCountDurationMs(xpGained: Int): Int = (500 + xpGained * 20).coerceIn(600, 1_500)

/**
 * Celebrates a completed test in proportion to the result, with a message picked from
 * [ResultMessages] for its [ResultTier]. The screen plays in order, so nothing covers anything
 * else ([ResultPhase]): the "APROBADO" stamp of a passed exam lands, the experience counts up
 * and the level bar fills, then at most one full-screen celebration ([ResultHeadline]: first
 * win, new record or new level) is shown, skippable with a tap. Tests and mini-games at 70 % or
 * more get confetti. The Play review request waits until the whole sequence is over.
 *
 * @param xpGained total experience earned by the user
 * @param baseXp experience earned before bonuses
 * @param bonusPerfection optional bonus for a perfect result
 * @param bonusFast optional bonus for completing the test quickly
 * @param bonusStreak optional bonus for maintaining a streak
 * @param leveledUp whether the result increased the user's level
 * @param newLevel the level reached; 0 when unknown
 * @param newTotalXp the user's experience after this result, to fill the level bar; 0 hides the bar
 * @param durationSeconds time spent completing the test
 * @param accuracy percentage of correctly answered questions
 * @param score correct answers, shown under the stamp of a passed exam and on a failed one
 * @param total questions answered, shown with [score]
 * @param isFailedExam whether this is an official exam below the DGT pass mark (27/30); hides the
 *   confetti even when [accuracy] reaches 70
 * @param isPassedExam whether this is an official exam at or above the DGT pass mark; replaces
 *   the mascot and the motivational title with the "APROBADO" stamp
 * @param kind what was finished, which decides how the score is read; an exam when either exam
 *   flag is set, a lesson otherwise
 * @param isFirstWin whether this is the user's first win ever, which gets a one-time celebration
 * @param isNewRecord whether this exam beats every earlier one
 * @param previousBestScore best score of the earlier exams, shown with the record; negative when unknown
 * @param onResultShown invoked once, when the result appears, with the sound to play; the screen
 *   plays nothing itself so it stays testable
 * @param onContinueClick callback invoked when the user continues
 * @param secondaryActionLabel optional label for a secondary outlined action (e.g. "JUGAR OTRA VEZ"
 *   in the arcade mini-games); when null, only the primary continue button is shown
 * @param onSecondaryActionClick callback invoked when the secondary action is tapped
 */
@Composable
fun TestResultScreen(
    xpGained: Int,
    baseXp: Int,
    bonusPerfection: Int? = null,
    bonusFast: Int? = null,
    bonusStreak: Int? = null,
    leveledUp: Boolean = false,
    newLevel: Int = 0,
    newTotalXp: Int = 0,
    durationSeconds: Int,
    accuracy: Int,
    score: Int = 0,
    total: Int = 0,
    isFailedExam: Boolean = false,
    isPassedExam: Boolean = false,
    kind: ResultKind = if (isFailedExam || isPassedExam) ResultKind.EXAM else ResultKind.LESSON,
    isFirstWin: Boolean = false,
    isNewRecord: Boolean = false,
    previousBestScore: Int = -1,
    onResultShown: (ResultSound) -> Unit = {},
    onContinueClick: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: () -> Unit = {},
) {

    val headline = remember { ResultHeadline.of(isFirstWin, isNewRecord, leveledUp) }
    val tier = remember { ResultTier.of(kind, accuracy, score, total) }
    // Saved, so a rotation keeps the words, the step and the fact that the sound already played.
    val message = rememberSaveable(saver = ResultMessageSaver) {
        ResultMessages.pick(tier, ResultPace.of(durationSeconds, total), avoid = lastMessageTitle)
            .also { lastMessageTitle = it.title }
    }
    var phase by rememberSaveable { mutableStateOf(ResultPhase.first(isPassedExam)) }
    var soundPlayed by rememberSaveable { mutableStateOf(false) }

    val count = remember {
        Animatable(if (phase.ordinal > ResultPhase.COUNTING.ordinal) 1f else 0f)
    }
    val xpShown by remember { derivedStateOf { (xpGained * count.value).roundToInt() } }

    LaunchedEffect(phase) {
        // After a stamp the sound waits for the impact; any other result opens with it.
        if (phase != ResultPhase.STAMP && !soundPlayed) {
            soundPlayed = true
            onResultShown(tier.sound)
        }
        if (phase != ResultPhase.COUNTING) return@LaunchedEffect
        delay(COUNT_START_DELAY_MS)
        count.animateTo(1f, tween(durationMillis = xpCountDurationMs(xpGained), easing = FastOutSlowInEasing))
        if (headline != null) delay(if (isPassedExam) HEADLINE_AFTER_STAMP_MS else HEADLINE_DELAY_MS)
        phase = phase.next(headline)
    }

    InAppReviewEffect(accuracy = accuracy, enabled = phase == ResultPhase.DONE)

    // A passed exam's confetti waits for the stamp to land, and rests under a full-screen celebration.
    val showConfetti = accuracy >= 70 && !isFailedExam &&
        phase != ResultPhase.STAMP && phase != ResultPhase.HEADLINE
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = showConfetti,
        iterations = LottieConstants.IterateForever
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onContinueClick,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("CONTINUAR", fontWeight = FontWeight.Black)
                    }
                    if (secondaryActionLabel != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(secondaryActionLabel, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (showConfetti) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Scrolls when the content is taller than the screen (small phones, big fonts);
                // otherwise the min height keeps it centred as before.
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = maxHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (leveledUp || isNewRecord) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (leveledUp) LevelUpBadge(newLevel = newLevel)
                                if (isNewRecord) RecordBadge()
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (isPassedExam) {
                            PassedExamStamp(
                                score = score,
                                total = total,
                                landed = phase != ResultPhase.STAMP,
                                onLanded = { if (phase == ResultPhase.STAMP) phase = phase.next(headline) },
                                message = message
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.bali),
                                contentDescription = "Bali",
                                modifier = Modifier.size(120.dp),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = message.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = message.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            if (kind == ResultKind.EXAM && total > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = examScoreLine(score, total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Stats Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ResultStatCard(
                                modifier = Modifier.weight(1f),
                                label = "EXP Total",
                                value = "+$xpShown",
                                icon = Icons.Rounded.Bolt,
                                color = Color(0xFFFACC15),
                            )

                            ResultStatCard(
                                modifier = Modifier.weight(1f),
                                label = "Aciertos",
                                value = "$accuracy%",
                                icon = Icons.Rounded.CheckCircle,
                                color = Color(0xFF22C55E)
                            )

                            ResultStatCard(
                                modifier = Modifier.weight(1f),
                                label = "Tiempo",
                                value = formatClock(durationSeconds),
                                icon = Icons.Rounded.Timer,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (newTotalXp > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LevelProgressCard(
                                startXp = (newTotalXp - xpGained).coerceAtLeast(0),
                                xpGained = xpGained,
                                countProgress = { count.value }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // XP Breakdown
                        XpBreakdownCard(baseXp, bonusPerfection, bonusFast, bonusStreak)
                    }
                }
            }
        }

        // Above the Scaffold, so it also covers the continue button.
        AnimatedVisibility(visible = phase == ResultPhase.HEADLINE, enter = fadeIn(), exit = fadeOut()) {
            val dismiss = { phase = ResultPhase.DONE }
            when (headline) {
                ResultHeadline.FIRST_WIN -> FirstWinOverlay(onDismiss = dismiss)
                ResultHeadline.NEW_RECORD -> NewRecordOverlay(
                    score = score,
                    total = total,
                    previousBest = previousBestScore,
                    passed = isPassedExam,
                    onDismiss = dismiss
                )
                ResultHeadline.LEVEL_UP -> LevelUpOverlay(newLevel = newLevel, onDismiss = dismiss)
                null -> Unit
            }
        }
    }
}

/**
 * The level bar of the result: it starts where the user was before the result and fills as the
 * experience counts up, starting again from empty if a level is crossed on the way.
 *
 * @param startXp the user's experience before the result
 * @param xpGained experience the result added
 * @param countProgress how much of [xpGained] has been counted so far, from 0 to 1; read only in
 *   this card, so the frames of the count recompose the card and nothing else
 */
@Composable
private fun LevelProgressCard(startXp: Int, xpGained: Int, countProgress: () -> Float) {
    val level = LevelProgress.at(startXp + xpGained * countProgress())
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nivel ${level.level}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${level.xpToNextLevel} XP para el nivel ${level.level + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { level.progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                color = BaliAccentYellow,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/** Static gold pill that keeps a new exam record in sight once its full-screen celebration has closed. */
@Composable
fun RecordBadge() {
    Surface(color = BaliAccentYellow, shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "¡NUEVO RÉCORD!",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
    }
}

@Composable
fun XpRow(label: String, value: Int, icon: String? = null, isBonus: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Text(text = icon, modifier = Modifier.padding(end = 8.dp), fontSize = 14.sp)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = "+$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Pill that keeps the new level in sight on the result screen once its overlay has closed.
 *
 * @param newLevel the level reached; 0 when unknown, which shows a generic label instead
 */
@Composable
fun LevelUpBadge(newLevel: Int = 0) {
    val infiniteTransition = rememberInfiniteTransition(label = "levelup")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(50),
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (newLevel > 0) "¡NIVEL $newLevel!" else "¡SUBISTE DE NIVEL!",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun XpBreakdownCard(baseXp: Int, perfection: Int?, fast: Int?, streak: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Desglose de Experiencia",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            XpRow(label = "Base de la lección", value = baseXp)

            if (perfection != null) {
                XpRow(label = "Bono: Perfección", value = perfection, icon = "🎯", isBonus = true)
            }
            if (fast != null) {
                XpRow(label = "Bono: Velocidad", value = fast, icon = "⚡", isBonus = true)
            }
            if (streak != null) {
                XpRow(label = "Bono: Racha", value = streak, icon = "🔥", isBonus = true)
            }
        }
    }
}

@Composable
fun ResultStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
