package com.jesuskrastev.bali.ui.review

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InAppReviewEffectTest {

    /** Verifies that an excellent score is eligible for an in-app review request. */
    @Test
    fun excellentResult_requestsReview() {
        assertThat(shouldRequestInAppReview(90)).isTrue()
        assertThat(shouldRequestInAppReview(100)).isTrue()
    }

    /** Verifies that ordinary or poor scores never interrupt the user with a review request. */
    @Test
    fun resultBelowExcellent_doesNotRequestReview() {
        assertThat(shouldRequestInAppReview(89)).isFalse()
        assertThat(shouldRequestInAppReview(0)).isFalse()
    }
}
