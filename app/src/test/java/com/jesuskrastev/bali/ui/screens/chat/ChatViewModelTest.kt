package com.jesuskrastev.bali.ui.screens.chat

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.ChatRole
import com.jesuskrastev.bali.domain.usecase.AskDrivingTutorUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = RecordingChatAnalytics()

    private fun viewModelWith(
        chatRepository: FakeChatRepository = FakeChatRepository(),
        tutor: FakeAiTutorRepository = FakeAiTutorRepository()
    ) = ChatViewModel(
        chatRepository = chatRepository,
        askDrivingTutorUseCase = AskDrivingTutorUseCase(
            chatRepository = chatRepository,
            aiTutorRepository = tutor,
            userRepository = FakeUserRepository()
        ),
        analytics = analytics
    )

    @Test
    fun `starts empty once the stored history has been read`() = runTest {
        val viewModel = viewModelWith()

        val state = viewModel.uiState.value
        assertThat(state.messages).isEmpty()
        assertThat(state.isHistoryLoading).isFalse()
        assertThat(state.isEmpty).isTrue()
    }

    @Test
    fun `sending a question appends both turns to the conversation`() = runTest {
        val chatRepository = FakeChatRepository()
        val viewModel = viewModelWith(chatRepository)

        viewModel.onEvent(ChatEvent.DraftChanged("¿Qué significa la señal R-1?"))
        viewModel.onEvent(ChatEvent.SendDraft)

        val messages = viewModel.uiState.value.messages
        assertThat(messages).hasSize(2)
        assertThat(messages[0].role).isEqualTo(ChatRole.USER)
        assertThat(messages[0].content).isEqualTo("¿Qué significa la señal R-1?")
        assertThat(messages[1].role).isEqualTo(ChatRole.ASSISTANT)
        assertThat(viewModel.uiState.value.draft).isEmpty()
        assertThat(viewModel.uiState.value.isSending).isFalse()
    }

    @Test
    fun `blank drafts are never sent`() = runTest {
        val tutor = FakeAiTutorRepository()
        val viewModel = viewModelWith(tutor = tutor)

        viewModel.onEvent(ChatEvent.DraftChanged("    "))
        viewModel.onEvent(ChatEvent.SendDraft)

        assertThat(tutor.questions).isEmpty()
        assertThat(viewModel.uiState.value.messages).isEmpty()
    }

    @Test
    fun `a suggested question is sent as typed and tracked as a suggestion`() = runTest {
        val tutor = FakeAiTutorRepository()
        val viewModel = viewModelWith(tutor = tutor)

        viewModel.onEvent(ChatEvent.SendSuggestion("¿Cuál es la tasa de alcohol permitida?"))

        assertThat(tutor.questions).containsExactly("¿Cuál es la tasa de alcohol permitida?")
        assertThat(analytics.sent).hasSize(1)
        assertThat(analytics.sent.first().second).isTrue()
    }

    @Test
    fun `a failed answer keeps the question and exposes an error`() = runTest {
        val chatRepository = FakeChatRepository()
        val viewModel = viewModelWith(
            chatRepository = chatRepository,
            tutor = FakeAiTutorRepository(failure = IOException("sin red"))
        )

        viewModel.onEvent(ChatEvent.DraftChanged("¿Y en una glorieta?"))
        viewModel.onEvent(ChatEvent.SendDraft)

        val state = viewModel.uiState.value
        assertThat(state.error).contains("conexión")
        assertThat(state.failedQuestion).isEqualTo("¿Y en una glorieta?")
        assertThat(state.isSending).isFalse()
        // The student's message survives the failure so retrying re-asks it.
        assertThat(chatRepository.saved.map { it.role }).containsExactly(ChatRole.USER)
        assertThat(analytics.failed).containsExactly("IOException")
    }

    @Test
    fun `retrying re-asks without storing the question twice`() = runTest {
        val chatRepository = FakeChatRepository()
        val tutor = FakeAiTutorRepository(failure = IOException("sin red"))
        val viewModel = viewModelWith(chatRepository, tutor)

        viewModel.onEvent(ChatEvent.DraftChanged("¿Y en una glorieta?"))
        viewModel.onEvent(ChatEvent.SendDraft)

        tutor.failure = null
        viewModel.onEvent(ChatEvent.RetryFailed)

        val messages = viewModel.uiState.value.messages
        assertThat(messages.count { it.role == ChatRole.USER }).isEqualTo(1)
        assertThat(messages.count { it.role == ChatRole.ASSISTANT }).isEqualTo(1)
        assertThat(viewModel.uiState.value.error).isNull()
        assertThat(tutor.questions).hasSize(2)
    }

    @Test
    fun `retry does nothing when no question failed`() = runTest {
        val tutor = FakeAiTutorRepository()
        val viewModel = viewModelWith(tutor = tutor)

        viewModel.onEvent(ChatEvent.RetryFailed)

        assertThat(tutor.questions).isEmpty()
    }

    @Test
    fun `confirming the clear wipes the conversation and closes the dialog`() = runTest {
        val chatRepository = FakeChatRepository(
            listOf(ChatMessage(id = "1", content = "hola", role = ChatRole.USER))
        )
        val viewModel = viewModelWith(chatRepository)

        viewModel.onEvent(ChatEvent.RequestClear)
        assertThat(viewModel.uiState.value.showClearConfirmation).isTrue()

        viewModel.onEvent(ChatEvent.ConfirmClear)

        assertThat(chatRepository.clearCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.messages).isEmpty()
        assertThat(viewModel.uiState.value.showClearConfirmation).isFalse()
        assertThat(analytics.clearedCount).isEqualTo(1)
    }

    @Test
    fun `opening a conversation with history is tracked as a return visit`() = runTest {
        val chatRepository = FakeChatRepository(
            listOf(ChatMessage(id = "1", content = "hola", role = ChatRole.USER))
        )

        viewModelWith(chatRepository)

        assertThat(analytics.opened).containsExactly(true)
    }
}
