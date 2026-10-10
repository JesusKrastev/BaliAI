package com.jesuskrastev.bali.domain.exam

import com.jesuskrastev.bali.domain.model.Answer

/**
 * Works out which questions the student still gets wrong.
 *
 * A question stops counting as a mistake once the student answers it right, so a review does not
 * keep asking about something already learned.
 */
object PendingMistakes {

    /**
     * Lists the questions whose latest answer was wrong.
     *
     * @param answers every answer the student has given, in any order
     * @return the [Answer.resolvedQuestionId] of each question last answered wrongly
     */
    fun idsOf(answers: List<Answer>): Set<String> =
        answers
            .groupBy { it.resolvedQuestionId }
            .filterValues { history -> !history.maxBy { it.date }.isCorrect }
            .keys
}
