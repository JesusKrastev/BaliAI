package com.jesuskrastev.bali.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.ui.screens.onboarding.steps.DGT_THEORY_EXAMS_2025
import com.jesuskrastev.bali.ui.screens.onboarding.steps.DGT_THEORY_FAILS_2025
import org.junit.Test

class OnboardingQuizTest {

    private val everyConcern: List<String?> = OnboardingConfig.concerns + null

    @Test
    fun `every worry gets three exam-style questions with a valid answer key`() {
        everyConcern.forEach { concern ->
            val questions = OnboardingQuiz.questionsFor(concern)

            assertThat(questions).hasSize(OnboardingQuiz.QUESTION_COUNT)
            assertThat(questions.map { it.id }.toSet()).hasSize(OnboardingQuiz.QUESTION_COUNT)
            questions.forEach { question ->
                assertThat(question.options).hasSize(3)
                assertThat(question.correctIndex).isIn(question.options.indices)
                assertThat(question.explanation).contains("Truco")
            }
        }
    }

    @Test
    fun `the right answers are not always in the same place`() {
        val positions = everyConcern.flatMap { OnboardingQuiz.questionsFor(it) }.map { it.correctIndex }.toSet()

        assertThat(positions).containsExactly(0, 1, 2)
    }

    @Test
    fun `the questions for silly mistakes are the trap questions`() {
        assertThat(OnboardingQuiz.questionsFor(OnboardingConfig.CONCERN_SILLY_MISTAKES).map { it.id })
            .containsExactly("stop_always", "alcohol_novice", "urban_single_lane").inOrder()
    }

    @Test
    fun `every question points at a section of the learning path`() {
        val sections = StudyPlanBuilder.pathSections.map { it.first }.toSet()

        everyConcern.flatMap { OnboardingQuiz.questionsFor(it) }.forEach { question ->
            assertThat(question.sectionIndex).isIn(sections)
        }
    }

    @Test
    fun `the failed topics are the wrong answers, once each`() {
        val concern = OnboardingConfig.CONCERN_EXAM_MISMATCH
        val questions = OnboardingQuiz.questionsFor(concern)
        val answers = questions.map { QuizAnswer(it.id, it.correctIndex, isCorrect = it.topic == "Agentes") }

        assertThat(OnboardingQuiz.failedTopics(concern, answers)).containsExactly("Semáforos", "Prioridad").inOrder()
    }

    @Test
    fun `the DGT figures shown say half of the exams were failed`() {
        val failRate = DGT_THEORY_FAILS_2025.toDouble() / DGT_THEORY_EXAMS_2025

        assertThat(failRate).isWithin(0.001).of(0.497)
    }

    @Test
    fun `the exam timing tag keeps the old funnel buckets`() {
        val now = 1_790_000_000_000L
        val day = 86_400_000L

        assertThat(OnboardingConfig.examTimingTag(null, now)).isEqualTo("unbooked")
        assertThat(OnboardingConfig.examTimingTag(now + 2 * day, now)).isEqualTo("imminent")
        assertThat(OnboardingConfig.examTimingTag(now + 10 * day, now)).isEqualTo("soon")
        assertThat(OnboardingConfig.examTimingTag(now + 30 * day, now)).isEqualTo("later")
    }
}
