package com.jesuskrastev.bali.ui.review

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InAppReviewEffectTest {

    /** Verifies that a happy result after enough finished tests or games asks for a review. */
    @Test
    fun goodResultAfterSeveralCompletions_requestsReview() {
        assertThat(shouldRequestInAppReview(ReviewPromptState(completions = 3), accuracy = 70)).isTrue()
        assertThat(shouldRequestInAppReview(ReviewPromptState(completions = 12), accuracy = 100)).isTrue()
    }

    /** Verifies that new users are not interrupted before they have finished several tests or games. */
    @Test
    fun tooFewCompletions_doesNotRequestReview() {
        assertThat(shouldRequestInAppReview(ReviewPromptState(completions = 1), accuracy = 100)).isFalse()
        assertThat(shouldRequestInAppReview(ReviewPromptState(completions = 2), accuracy = 100)).isFalse()
    }

    /** Verifies that a poor result never interrupts the user, however experienced. */
    @Test
    fun poorResult_doesNotRequestReview() {
        assertThat(shouldRequestInAppReview(ReviewPromptState(completions = 10), accuracy = 69)).isFalse()
    }

    /** Verifies the spacing between requests and the lifetime cap. */
    @Test
    fun repeatedRequests_areSpacedAndCapped() {
        val asked = ReviewPromptState(completions = 5, requests = 1, completionsAtLastRequest = 3)
        assertThat(shouldRequestInAppReview(asked, accuracy = 100)).isFalse()
        assertThat(shouldRequestInAppReview(asked.copy(completions = 8), accuracy = 100)).isTrue()
        assertThat(shouldRequestInAppReview(ReviewPromptState(completions = 99, requests = 3), accuracy = 100)).isFalse()
    }
}
