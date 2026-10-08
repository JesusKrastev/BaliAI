package com.jesuskrastev.bali.ui.screens.onboarding

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.ui.screens.onboarding.steps.NarrativeScene
import org.junit.Test

class OnboardingNarrativesTest {

    private val block = listOf(OnboardingStep.Problem, OnboardingStep.Risk, OnboardingStep.Solution)

    /** Every combination of the answers the block reads, plus all of them unanswered. */
    private val profiles: List<OnboardingData> = buildList {
        add(OnboardingData())
        OnboardingConfig.motivations.forEach { motivation ->
            OnboardingConfig.theoryBlockers.forEach { blocker ->
                OnboardingConfig.experiences.forEach { experience ->
                    OnboardingConfig.concerns.forEach { concern ->
                        add(
                            OnboardingData(
                                motivation = motivation,
                                theoryBlocker = blocker,
                                experience = experience,
                                concern = concern
                            )
                        )
                    }
                }
            }
        }
    }

    /** All answers in, plus a mini-test with the first question right and the other two wrong. */
    private fun withQuiz(concern: String): OnboardingData {
        val questions = OnboardingQuiz.questionsFor(concern)
        return OnboardingData(
            motivation = OnboardingConfig.MOTIVATION_INDEPENDENCE,
            theoryBlocker = OnboardingConfig.BLOCKER_NO_PROGRESS,
            experience = OnboardingConfig.EXPERIENCE_FIRST_TIME,
            concern = concern,
            quizAnswers = questions.mapIndexed { i, q ->
                val right = i == 0
                QuizAnswer(q.id, if (right) q.correctIndex else (q.correctIndex + 1) % q.options.size, right)
            }
        )
    }

    @Test
    fun `every screen of the block has copy and a scene for every combination of answers`() {
        profiles.forEach { data ->
            block.forEach { step ->
                val narrative = OnboardingNarratives.forStep(step, data)!!
                assertThat(narrative.headline).isNotEmpty()
                assertThat(narrative.content.body).isNotEmpty()
                assertThat(narrative.content.body).doesNotContain("null")
                assertThat(narrative.content.scene).isNotNull()
                assertThat(narrative.content.animation).isNull()
            }
        }
    }

    @Test
    fun `the problem and the risk show a different scene for each answer`() {
        val problemScenes = OnboardingConfig.theoryBlockers.map {
            OnboardingNarratives.forStep(OnboardingStep.Problem, OnboardingData(theoryBlocker = it))!!.content.scene
        }
        val riskScenes = OnboardingConfig.concerns.map {
            OnboardingNarratives.forStep(OnboardingStep.Risk, OnboardingData(concern = it))!!.content.scene
        }

        assertThat(problemScenes.toSet()).hasSize(OnboardingConfig.theoryBlockers.size)
        assertThat(riskScenes.toSet()).hasSize(OnboardingConfig.concerns.size)
    }

    @Test
    fun `the problem mirrors the blocker and names a second attempt`() {
        val first = OnboardingNarratives.forStep(
            OnboardingStep.Problem,
            OnboardingData(theoryBlocker = OnboardingConfig.BLOCKER_NO_START, experience = OnboardingConfig.EXPERIENCE_FIRST_TIME)
        )!!
        val retry = OnboardingNarratives.forStep(
            OnboardingStep.Problem,
            OnboardingData(theoryBlocker = OnboardingConfig.BLOCKER_NO_PROGRESS, experience = OnboardingConfig.EXPERIENCE_RETRY)
        )!!

        assertThat(first.headline).contains("por dónde empezar")
        assertThat(retry.headline).contains("suspendiste")
        assertThat(retry.headline).contains("sin ver avance")
    }

    @Test
    fun `the risk quotes the mini-test score`() {
        val notReady = OnboardingNarratives.forStep(OnboardingStep.Risk, withQuiz(OnboardingConfig.CONCERN_NOT_READY))!!
        val silly = OnboardingNarratives.forStep(OnboardingStep.Risk, withQuiz(OnboardingConfig.CONCERN_SILLY_MISTAKES))!!

        assertThat(notReady.content.body).contains("|1 de 3|")
        assertThat(silly.content.body).contains("|2 de 3|")
    }

    @Test
    fun `the risk does without the score when the test was skipped`() {
        val risk = OnboardingNarratives.forStep(OnboardingStep.Risk, OnboardingData(concern = OnboardingConfig.CONCERN_NOT_READY))!!

        assertThat(risk.content.body).doesNotContain(" de 0")
        assertThat(risk.content.body).doesNotContain("Hoy")
    }

    @Test
    fun `the solution answers each problem named, and the failed topics of the test`() {
        val data = withQuiz(OnboardingConfig.CONCERN_EXAM_MISMATCH)
        val scene = OnboardingNarratives.forStep(OnboardingStep.Solution, data)!!.content.scene as NarrativeScene.Solved
        val failedTopics = data.failedQuizQuestions().map { it.topic }.distinct()

        assertThat(scene.rows).hasSize(3)
        assertThat(scene.rows[0].problem).isEqualTo("Estudio y no avanzo")
        assertThat(scene.rows[1].fix).contains("Simulacros de 30 preguntas")
        failedTopics.forEach { assertThat(scene.rows[2].problem).contains(it) }
    }

    @Test
    fun `the solution has no test row when nothing was failed`() {
        val scene = OnboardingNarratives.forStep(OnboardingStep.Solution, OnboardingData())!!.content.scene as NarrativeScene.Solved

        assertThat(scene.rows).hasSize(2)
    }

    @Test
    fun `the licence's why only closes the solution`() {
        val work = OnboardingData(motivation = OnboardingConfig.MOTIVATION_WORK)

        assertThat(OnboardingNarratives.forStep(OnboardingStep.Solution, work)!!.content.body).contains("puertas")
        listOf(OnboardingStep.Problem, OnboardingStep.Risk).forEach { step ->
            val text = OnboardingNarratives.forStep(step, work)!!.let { it.headline + it.content.body }.lowercase()
            assertThat(text).doesNotContain("trabajo")
            assertThat(text).doesNotContain("te lleven")
        }
    }

    @Test
    fun `the copy reads the same whoever answers it`() {
        // Spanish words that change with the reader's gender, which this copy must avoid.
        val gendered = listOf("el único", "la única", "listo", "lista para", "preparado", "preparada", "eres el siguiente")

        profiles.forEach { data ->
            block.forEach { step ->
                val narrative = OnboardingNarratives.forStep(step, data)!!
                val rows = (narrative.content.scene as? NarrativeScene.Solved)?.rows.orEmpty()
                    .joinToString(" ") { it.problem + " " + it.fix }
                val text = (narrative.headline + " " + narrative.content.body + " " + rows).lowercase()
                gendered.forEach { word -> assertThat(text).doesNotContain(word) }
            }
        }
    }

    @Test
    fun `steps outside the block have no narrative`() {
        assertThat(OnboardingNarratives.forStep(OnboardingStep.Name, OnboardingData())).isNull()
        assertThat(OnboardingNarratives.forStep(OnboardingStep.Quiz, OnboardingData())).isNull()
    }
}
