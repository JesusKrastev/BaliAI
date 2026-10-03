package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingQuiz
import com.jesuskrastev.bali.ui.screens.onboarding.QuizAnswer
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliTheme

/**
 * The mini-test result: the score, one chip per question and what it means for the plan.
 *
 * Everything on it is true for this user. It does not extrapolate a pass or fail from three
 * questions; it states the real rule (30 questions, at most 3 mistakes) and what the plan will do
 * with the topics they missed.
 *
 * @param data the answers collected so far, including the test
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepQuizResult(data: OnboardingData) {
    val questions = data.quizQuestions()
    val answers = data.quizAnswers
    val score = data.quizScore()
    val failedTopics = data.failedQuizQuestions().map { it.topic }.distinct()

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(500)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .graphicsLayer { alpha = appear.value },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Has acertado",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$score de ${answers.size}",
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            questions.zip(answers).forEach { (question, answer) ->
                TopicChip(topic = question.topic, isCorrect = answer.isCorrect)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultLine(
                    "📝",
                    "En el examen son |${ExamRules.QUESTION_COUNT} preguntas| y solo puedes fallar |${ExamRules.MAX_MISTAKES}|."
                )
                ResultLine("💬", verdict(score, answers.size))
                if (data.experience == OnboardingConfig.EXPERIENCE_RETRY) {
                    ResultLine("🔁", "Esta vez vas a saber |dónde fallas| antes del examen.")
                }
                if (failedTopics.isNotEmpty()) {
                    ResultLine("🎯", "Lo apuntamos en tu plan para reforzarlo: |${failedTopics.joinToString(" y ")}|.")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * What the score means, without promising or predicting the real exam.
 *
 * @param score right answers
 * @param total questions answered
 * @return the line
 */
private fun verdict(score: Int, total: Int): String = when {
    score == total ->
        "Buen comienzo. Lo difícil es aguantar ${ExamRules.QUESTION_COUNT} seguidas con solo ${ExamRules.MAX_MISTAKES} fallos: |para eso están los simulacros|."
    score == total - 1 -> "Buena base. Ahora toca |pulir los detalles|."
    else -> "Normal al empezar: por eso vamos |tema a tema|."
}

/**
 * One question's topic, marked right or wrong.
 *
 * @param topic the subject of the question
 * @param isCorrect whether it was answered right
 */
@Composable
private fun TopicChip(topic: String, isCorrect: Boolean) {
    val accent = if (isCorrect) BaliAccentGreen else BaliAccentRed
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Text(
            text = "$topic ${if (isCorrect) "✅" else "❌"}",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * One line of the result card, with its emoji.
 *
 * @param emoji the symbol on the left
 * @param text the line; `|` pairs mark the words to highlight
 */
@Composable
private fun ResultLine(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = emoji, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
        Text(
            text = text.highlightPipes(MaterialTheme.colorScheme.primary),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun StepQuizResultPreview() {
    val questions = OnboardingQuiz.questionsFor(OnboardingConfig.CONCERN_SILLY_MISTAKES)
    BaliTheme {
        StepQuizResult(
            OnboardingData(
                concern = OnboardingConfig.CONCERN_SILLY_MISTAKES,
                experience = OnboardingConfig.EXPERIENCE_RETRY,
                quizAnswers = questions.mapIndexed { index, question ->
                    QuizAnswer(question.id, if (index == 1) 0 else question.correctIndex, index != 1)
                }
            )
        )
    }
}
