package com.jesuskrastev.bali.domain.model

/**
 * Milestones a finished test can reach, derived only from the results saved before it, so they
 * need no extra storage and survive a reinstall: the saved results come back from Firestore.
 */
object ResultMilestones {

    /** Accuracy, in percent, from which a lesson counts as a win: the bar that unlocks the next one. */
    const val WIN_ACCURACY = 70

    /**
     * Tells whether a saved result was a win: an official exam passed, or any other test at
     * [WIN_ACCURACY] or more.
     *
     * @param result a result saved by an earlier test or exam
     * @return true when it was a win
     */
    fun isWin(result: TestResult): Boolean =
        if (result.category == ExamRules.OFFICIAL_EXAM_CATEGORY) {
            ExamRules.isPassed(result.score)
        } else {
            result.total > 0 && result.score * 100 / result.total >= WIN_ACCURACY
        }

    /**
     * Tells whether the test being finished is the user's first win. Judged by wins, not by
     * tests: celebrating a first test that went badly would contradict its result, and the
     * celebration would then be gone for good.
     *
     * @param previousResults every result saved before this test, of any kind
     * @param isWin whether the test being finished is itself a win
     * @return true when it is a win and none of the earlier results was
     */
    fun isFirstWin(previousResults: List<TestResult>, isWin: Boolean): Boolean =
        isWin && previousResults.none { isWin(it) }

    /**
     * Finds the best official-exam score among earlier results.
     *
     * @param previousResults every result saved before this exam
     * @return the highest number of correct answers in an official exam, or null when the user had
     *   never finished one
     */
    fun bestExamScore(previousResults: List<TestResult>): Int? = previousResults
        .filter { it.category == ExamRules.OFFICIAL_EXAM_CATEGORY }
        .maxOfOrNull { it.score }

    /**
     * Tells whether an exam beats every earlier one. The very first exam is not a record: there is
     * nothing to beat yet, and calling it one would make the record mean nothing.
     *
     * @param score correct answers in the exam just finished
     * @param previousBest result of [bestExamScore] for the results before it
     * @return true when there was an earlier exam and [score] is strictly higher than all of them
     */
    fun isNewExamRecord(score: Int, previousBest: Int?): Boolean =
        previousBest != null && score > previousBest
}
