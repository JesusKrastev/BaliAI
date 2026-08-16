package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.ChatRole
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.chat.FakeAiTutorRepository
import com.jesuskrastev.bali.ui.screens.chat.FakeChatRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class AskDrivingTutorUseCaseTest {

    private fun useCaseWith(
        chatRepository: FakeChatRepository = FakeChatRepository(),
        tutor: FakeAiTutorRepository = FakeAiTutorRepository()
    ) = AskDrivingTutorUseCase(
        chatRepository = chatRepository,
        aiTutorRepository = tutor,
        userRepository = FakeUserRepository()
    )

    @Test
    fun `stores the question and the answer in order`() = runTest {
        val chatRepository = FakeChatRepository()

        val result = useCaseWith(chatRepository)("¿Qué significa la señal R-1?")

        assertThat(result.isSuccess).isTrue()
        assertThat(chatRepository.saved.map { it.role })
            .containsExactly(ChatRole.USER, ChatRole.ASSISTANT)
            .inOrder()
    }

    @Test
    fun `trims the question before storing and asking`() = runTest {
        val chatRepository = FakeChatRepository()
        val tutor = FakeAiTutorRepository()

        useCaseWith(chatRepository, tutor)("   ¿Y de noche?   ")

        assertThat(tutor.questions).containsExactly("¿Y de noche?")
        assertThat(chatRepository.saved.first().content).isEqualTo("¿Y de noche?")
    }

    @Test
    fun `rejects a blank question without touching the tutor`() = runTest {
        val tutor = FakeAiTutorRepository()

        val result = useCaseWith(tutor = tutor)("   ")

        assertThat(result.isFailure).isTrue()
        assertThat(tutor.questions).isEmpty()
    }

    @Test
    fun `keeps the question stored when the tutor fails`() = runTest {
        val chatRepository = FakeChatRepository()

        val result = useCaseWith(
            chatRepository,
            FakeAiTutorRepository(failure = IOException("sin red"))
        )("¿Y de noche?")

        assertThat(result.isFailure).isTrue()
        assertThat(chatRepository.saved.map { it.role }).containsExactly(ChatRole.USER)
    }

    @Test
    fun `a retry does not store the question a second time`() = runTest {
        val chatRepository = FakeChatRepository(
            listOf(ChatMessage(id = "1", content = "¿Y de noche?", role = ChatRole.USER))
        )

        useCaseWith(chatRepository)("¿Y de noche?", persistQuestion = false)

        assertThat(chatRepository.saved.count { it.role == ChatRole.USER }).isEqualTo(1)
        assertThat(chatRepository.saved.count { it.role == ChatRole.ASSISTANT }).isEqualTo(1)
    }

    @Test
    fun `the new question is not duplicated inside the history sent to the tutor`() = runTest {
        val chatRepository = FakeChatRepository(
            listOf(ChatMessage(id = "1", content = "¿Qué es la V-16?", role = ChatRole.USER))
        )
        val tutor = FakeAiTutorRepository()

        useCaseWith(chatRepository, tutor)("¿Y desde cuándo es obligatoria?")

        assertThat(tutor.lastHistory.map { it.content }).containsExactly("¿Qué es la V-16?")
    }

    @Test
    fun `only the most recent turns travel with the question`() = runTest {
        val longHistory = (1..30).map {
            ChatMessage(id = "$it", content = "mensaje $it", role = ChatRole.USER)
        }
        val tutor = FakeAiTutorRepository()

        useCaseWith(FakeChatRepository(longHistory), tutor)("¿Y ahora?")

        assertThat(tutor.lastHistory).hasSize(AskDrivingTutorUseCase.MAX_HISTORY_TURNS)
        assertThat(tutor.lastHistory.last().content).isEqualTo("mensaje 30")
    }
}
