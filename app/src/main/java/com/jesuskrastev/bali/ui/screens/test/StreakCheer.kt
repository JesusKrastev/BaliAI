package com.jesuskrastev.bali.ui.screens.test

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.onboarding.components.SpeechBubbleShape
import kotlinx.coroutines.delay

/** How long Bali's cheer stays on screen before sliding away on its own. */
private const val CHEER_VISIBLE_MILLIS = 2_600L

/**
 * Whether Bali cheers on this run of correct answers: at three, at five, at ten and then every
 * five more. Spacing them out keeps the cheer a small surprise instead of noise on every answer.
 *
 * @param run consecutive correct answers in the session, this one included
 * @return true when this answer earns a cheer
 */
fun isCheerRun(run: Int): Boolean = run == 3 || run == 5 || (run >= 10 && run % 5 == 0)

/**
 * What Bali says for a run of correct answers. Short on purpose: it is read at a glance while the
 * user is already looking at the explanation.
 *
 * @param run consecutive correct answers in the session; one that passes [isCheerRun]
 * @return the line for the speech bubble
 */
fun cheerMessage(run: Int): String = when {
    run < 5 -> "¡Buen ritmo!"
    run < 10 -> "¡$run seguidas! Sigue así"
    run == 10 -> "¡10 seguidas! Imparable"
    else -> "¡$run seguidas! Vas lanzado"
}

/**
 * The mascot peeking in from the side with a short congratulation when the user strings together
 * a run of correct answers (see [isCheerRun]). It slides in only on the answer that reaches the
 * run, leaves by itself after [CHEER_VISIBLE_MILLIS] or as soon as the question changes, and never
 * takes touches, so it cannot get in the way of the explanation or the buttons.
 *
 * Only the moment the answer flips to checked counts: an answer that is already checked when the
 * screen appears (after a rotation, or when the exam goes back to an earlier question) does not
 * cheer again.
 *
 * @param currentIndex zero-based index of the question being answered
 * @param sessionStreak consecutive correct answers in this session
 * @param isAnswerChecked true once the current answer has been checked
 * @param modifier positions the cheer; it is usually aligned to the bottom start of the content
 */
@Composable
fun StreakCheer(
    currentIndex: Int,
    sessionStreak: Int,
    isAnswerChecked: Boolean,
    modifier: Modifier = Modifier
) {
    var lastSeen by remember { mutableStateOf(currentIndex to isAnswerChecked) }
    var visible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(currentIndex, isAnswerChecked) {
        val justChecked = isAnswerChecked && lastSeen == (currentIndex to false)
        lastSeen = currentIndex to isAnswerChecked
        if (!justChecked || !isCheerRun(sessionStreak)) {
            visible = false
            return@LaunchedEffect
        }
        message = cheerMessage(sessionStreak)
        visible = true
        delay(CHEER_VISIBLE_MILLIS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        ) { -it } + fadeIn(tween(durationMillis = 180)),
        exit = slideOutHorizontally(tween(durationMillis = 260)) { -it } + fadeOut(tween(durationMillis = 200))
    ) {
        CheerBubble(message = message)
    }
}

/**
 * The small happy mascot next to a speech bubble with [message].
 *
 * @param message what the mascot says
 */
@Composable
private fun CheerBubble(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.happy_bali),
            contentDescription = null,
            modifier = Modifier.size(44.dp)
        )
        Surface(
            shape = SpeechBubbleShape(cornerRadius = 14.dp, pointerSize = 8.dp, pointerOffset = 14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            shadowElevation = 3.dp,
            modifier = Modifier.widthIn(max = 220.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 18.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}
