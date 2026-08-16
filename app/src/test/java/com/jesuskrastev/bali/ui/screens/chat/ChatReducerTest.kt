package com.jesuskrastev.bali.ui.screens.chat

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class ChatReducerTest {

    private val reducer = ChatReducer()

    @Test
    fun `DraftChanged stores the typed text`() {
        val state = reducer.reduce(ChatUiState(), ChatEvent.DraftChanged("¿Qué es la V-16?"))

        assertThat(state.draft).isEqualTo("¿Qué es la V-16?")
    }

    @Test
    fun `DismissError clears both the message and the retryable question`() {
        val failed = ChatUiState(error = "algo falló", failedQuestion = "¿Y de noche?")

        val state = reducer.reduce(failed, ChatEvent.DismissError)

        assertThat(state.error).isNull()
        assertThat(state.failedQuestion).isNull()
    }

    @Test
    fun `RequestClear and CancelClear toggle the confirmation dialog`() {
        val asked = reducer.reduce(ChatUiState(), ChatEvent.RequestClear)
        assertThat(asked.showClearConfirmation).isTrue()

        val cancelled = reducer.reduce(asked, ChatEvent.CancelClear)
        assertThat(cancelled.showClearConfirmation).isFalse()
    }

    @Test
    fun `onSendStarted empties the draft and drops the previous error`() {
        val state = reducer.onSendStarted(
            ChatUiState(draft = "¿Y de noche?", error = "algo falló", failedQuestion = "¿Y de noche?")
        )

        assertThat(state.draft).isEmpty()
        assertThat(state.isSending).isTrue()
        assertThat(state.error).isNull()
        assertThat(state.failedQuestion).isNull()
    }

    @Test
    fun `onSendFailed keeps the question so it can be retried`() {
        val state = reducer.onSendFailed(
            ChatUiState(isSending = true),
            question = "¿Quién tiene prioridad?",
            cause = IllegalStateException("boom")
        )

        assertThat(state.isSending).isFalse()
        assertThat(state.failedQuestion).isEqualTo("¿Quién tiene prioridad?")
        assertThat(state.error).isNotEmpty()
    }

    @Test
    fun `connectivity failures get their own message`() {
        val offline = reducer.errorMessageFor(UnknownHostException())
        val io = reducer.errorMessageFor(IOException())
        val other = reducer.errorMessageFor(IllegalStateException())

        assertThat(offline).contains("conexión")
        assertThat(io).isEqualTo(offline)
        assertThat(other).isNotEqualTo(offline)
    }

    @Test
    fun `canSend requires a non blank draft and no request in flight`() {
        assertThat(ChatUiState(draft = "  ").canSend).isFalse()
        assertThat(ChatUiState(draft = "hola", isSending = true).canSend).isFalse()
        assertThat(ChatUiState(draft = "hola").canSend).isTrue()
    }

    @Test
    fun `isEmpty stays false while the stored history is still loading`() {
        assertThat(ChatUiState(isHistoryLoading = true).isEmpty).isFalse()
        assertThat(ChatUiState(isHistoryLoading = false).isEmpty).isTrue()
    }

    @Test
    fun `isEmpty stays false while the first question is in flight`() {
        val sendingFirstQuestion = ChatUiState(isHistoryLoading = false, isSending = true)

        assertThat(sendingFirstQuestion.isEmpty).isFalse()
    }
}
