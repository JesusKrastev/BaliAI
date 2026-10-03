package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingQuiz
import com.jesuskrastev.bali.ui.screens.onboarding.QuizQuestion
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliTheme

private val OPTION_SHAPE = RoundedCornerShape(20.dp)

/**
 * One question of the onboarding mini-test, answered the way the real app answers: tap an
 * option, see right away whether it was right, and read why.
 *
 * The explanation does not dismiss itself: this is the moment the user sees what Bali does,
 * so the screen waits for "Siguiente".
 *
 * @param question the question on screen
 * @param index its place in the test, from 0
 * @param total questions in the test
 * @param selectedIndex the option tapped, or null while unanswered
 * @param onAnswer called with the option tapped
 * @param onNext moves on once answered
 * @param onSkip leaves the test; offered only on the first question, before answering
 */
@Composable
fun StepQuiz(
    question: QuizQuestion,
    index: Int,
    total: Int,
    selectedIndex: Int?,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val answered = selectedIndex != null
    val scroll = rememberScrollState()

    // Brings the explanation into view on short screens.
    LaunchedEffect(answered) {
        if (answered) scroll.animateScrollTo(scroll.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
        ) {
            Text(
                text = "PREGUNTA ${index + 1} DE $total · ESTILO EXAMEN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { optionIndex, option ->
                    QuizOption(
                        text = option,
                        letter = 'A' + optionIndex,
                        state = optionState(optionIndex, question.correctIndex, selectedIndex),
                        onClick = { onAnswer(optionIndex) }
                    )
                }
            }

            AnimatedVisibility(
                visible = answered,
                enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 3 }
            ) {
                ExplanationCard(
                    isCorrect = selectedIndex == question.correctIndex,
                    explanation = question.explanation,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (answered) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (index == total - 1) "Ver mi resultado →" else "Siguiente →",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (index == 0) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Saltar la prueba",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/** How an option looks: before answering, and after it as right, wrong or merely not chosen. */
private enum class OptionState { IDLE, CORRECT, WRONG, DIMMED }

/**
 * Works out how an option is drawn.
 *
 * @param optionIndex the option
 * @param correctIndex the right answer
 * @param selectedIndex the option tapped, or null while unanswered
 * @return its state
 */
private fun optionState(optionIndex: Int, correctIndex: Int, selectedIndex: Int?): OptionState = when {
    selectedIndex == null -> OptionState.IDLE
    optionIndex == correctIndex -> OptionState.CORRECT
    optionIndex == selectedIndex -> OptionState.WRONG
    else -> OptionState.DIMMED
}

/**
 * A tappable answer with its exam-style letter. Once the question is answered it shows the right
 * answer in green and a wrong pick in red, and stops reacting to taps.
 *
 * @param text the answer
 * @param letter A, B or C
 * @param state how to draw it
 * @param onClick called when tapped before answering
 */
@Composable
private fun QuizOption(text: String, letter: Char, state: OptionState, onClick: () -> Unit) {
    val accent = when (state) {
        OptionState.CORRECT -> BaliAccentGreen
        OptionState.WRONG -> BaliAccentRed
        else -> MaterialTheme.colorScheme.outline
    }
    val border by animateColorAsState(
        targetValue = if (state == OptionState.IDLE || state == OptionState.DIMMED) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        } else {
            accent
        },
        animationSpec = tween(200),
        label = "quiz_option_border"
    )
    val container = when (state) {
        OptionState.CORRECT -> BaliAccentGreen.copy(alpha = 0.12f)
        OptionState.WRONG -> BaliAccentRed.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = onClick,
        enabled = state == OptionState.IDLE,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        shape = OPTION_SHAPE,
        color = container,
        border = BorderStroke(if (state == OptionState.IDLE) 1.5.dp else 2.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = if (state == OptionState.CORRECT || state == OptionState.WRONG) accent
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    OptionState.CORRECT -> Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    OptionState.WRONG -> Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    else -> Text(
                        text = letter.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (state == OptionState.CORRECT || state == OptionState.WRONG) FontWeight.Bold
                else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (state == OptionState.DIMMED) 0.55f else 1f
                )
            )
        }
    }
}

/**
 * The explanation shown after answering, headed by whether the answer was right.
 *
 * @param isCorrect whether the user got it right
 * @param explanation the rule, the trap and a trick to remember it
 * @param modifier layout modifier
 */
@Composable
private fun ExplanationCard(isCorrect: Boolean, explanation: String, modifier: Modifier = Modifier) {
    val accent = if (isCorrect) BaliAccentGreen else BaliAccentRed
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (isCorrect) "✅ Bien visto" else "❌ Esta tenía trampa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun StepQuizAnsweredPreview() {
    BaliTheme {
        val question = OnboardingQuiz.questionsFor(null)[1]
        StepQuiz(
            question = question,
            index = 1,
            total = OnboardingQuiz.QUESTION_COUNT,
            selectedIndex = 0,
            onAnswer = {},
            onNext = {},
            onSkip = {}
        )
    }
}
