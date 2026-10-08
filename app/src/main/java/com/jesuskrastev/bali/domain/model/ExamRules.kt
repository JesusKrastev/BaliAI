package com.jesuskrastev.bali.domain.model

/**
 * The rules of the official DGT theory exam, shared by the exam screen that applies them and by
 * the progress statistics that predict them, so both always agree on what "passed" means.
 */
object ExamRules {
    /** [TestResult.category] stored for every official-exam attempt, win or lose. */
    const val OFFICIAL_EXAM_CATEGORY = "Examen Oficial"

    /** Questions in one exam. */
    const val QUESTION_COUNT = 30

    /** Mistakes the DGT allows before an exam is failed. */
    const val MAX_MISTAKES = 3

    /** Correct answers needed to pass an exam. */
    const val PASS_SCORE = QUESTION_COUNT - MAX_MISTAKES

    /**
     * Tells whether an exam score passes.
     *
     * @param correct number of correct answers in the exam
     * @return true when [correct] reaches [PASS_SCORE]
     */
    fun isPassed(correct: Int): Boolean = correct >= PASS_SCORE
}
