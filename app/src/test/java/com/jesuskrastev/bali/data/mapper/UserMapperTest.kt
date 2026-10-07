package com.jesuskrastev.bali.data.mapper

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.remote.firestore.entities.UserFirestore
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.domain.model.User
import org.junit.Test

class UserMapperTest {

    @Test
    fun `first steps survive the trip to Firestore and back`() {
        val user = User(
            id = "user_123",
            firstSteps = FirstStepsProgress(
                startedAtMillis = 5_000L,
                completed = setOf(FirstStepTask.FIRST_TEST, FirstStepTask.PLAY_GAME),
                dismissed = true
            )
        )

        assertThat(user.toFirestore().toDomain().firstSteps).isEqualTo(user.firstSteps)
    }

    @Test
    fun `first steps are stored under the ids and fields the transaction reads`() {
        val firestore = User(
            firstSteps = FirstStepsProgress(startedAtMillis = 5_000L, completed = setOf(FirstStepTask.ASK_BALI))
        ).toFirestore()

        assertThat(firestore.firstStepsStartedAt).isEqualTo(5_000L)
        assertThat(firestore.firstStepsDone).containsExactly("ask_bali")
        assertThat(firestore.firstStepsDismissed).isFalse()
    }

    @Test
    fun `a document written before the feature counts as never enrolled`() {
        val progress = UserFirestore().toDomain().firstSteps

        assertThat(progress).isEqualTo(FirstStepsProgress())
        assertThat(progress.isEnrolled).isFalse()
    }

    @Test
    fun `task ids this build does not know are ignored`() {
        val progress = UserFirestore(
            firstStepsStartedAt = 1L,
            firstStepsDone = listOf("first_test", "a_task_from_the_future")
        ).toDomain().firstSteps

        assertThat(progress.completed).containsExactly(FirstStepTask.FIRST_TEST)
    }

    @Test
    fun `the local copy of the user never carries first steps`() {
        val user = User(id = "user_123", firstSteps = FirstStepsProgress.startingAt(5_000L))

        assertThat(user.toEntity().toDomain().firstSteps).isEqualTo(FirstStepsProgress())
    }
}
