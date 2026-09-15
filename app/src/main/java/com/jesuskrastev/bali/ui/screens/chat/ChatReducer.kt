package com.jesuskrastev.bali.ui.screens.chat

import java.io.IOException
import java.net.UnknownHostException

/**
 * Pure state transitions for the AI tutor chat.
 *
 * Every function here maps state (plus an input) to new state with no side effects, so
 * the ViewModel is left with orchestration only: persisting turns, calling the model and
 * tracking analytics.
 */
class ChatReducer {

    /**
     * Applies the synchronous part of an event.
     *
     * Events that need I/O ([ChatEvent.SendDraft], [ChatEvent.SendSuggestion],
     * [ChatEvent.RetryFailed], [ChatEvent.ConfirmClear]) are handled by the ViewModel,
     * which calls [onSendStarted] once the work is actually under way.
     *
     * @param state the current state
     * @param event the user action
     * @return the state to render, unchanged for events this reducer does not own
     */
    fun reduce(state: ChatUiState, event: ChatEvent): ChatUiState = when (event) {
        is ChatEvent.DraftChanged -> state.copy(draft = event.text.take(MAX_QUESTION_CHARS))
        ChatEvent.DismissError -> state.copy(error = null, failedQuestion = null)
        ChatEvent.RequestClear -> state.copy(showClearConfirmation = true)
        ChatEvent.CancelClear -> state.copy(showClearConfirmation = false)
        else -> state
    }

    companion object {
        /**
         * Hard cap on the question the student can type. Enough for a genuinely detailed
         * driving doubt while stopping a pasted essay from turning one chat turn into a
         * disproportionately expensive prompt — every character here is billed on every
         * request, since [com.jesuskrastev.bali.data.repository.GeminiTutorRepository]
         * sends the current question in full, unlike past turns which it already trims.
         */
        private const val MAX_QUESTION_CHARS = 600
    }

    /**
     * Marks a question as in flight: the input empties immediately so the field is ready
     * for the next one, and any previous error is cleared.
     *
     * @param state the current state
     * @return the state while waiting for the tutor's answer
     */
    fun onSendStarted(state: ChatUiState): ChatUiState =
        state.copy(draft = "", isSending = true, error = null, failedQuestion = null)

    /**
     * Clears the in-flight flag after a successful answer. The message itself arrives
     * through the repository flow, not from here.
     *
     * @param state the current state
     * @return the state with the typing indicator hidden
     */
    fun onSendSucceeded(state: ChatUiState): ChatUiState = state.copy(isSending = false)

    /**
     * Records a failed question so the screen can offer a retry.
     *
     * @param state the current state
     * @param question the question that went unanswered
     * @param cause what stopped it
     * @return the state showing the error banner
     */
    fun onSendFailed(state: ChatUiState, question: String, cause: Throwable): ChatUiState =
        state.copy(
            isSending = false,
            error = errorMessageFor(cause),
            failedQuestion = question
        )

    /**
     * Turns a failure into copy the student can act on. Connectivity problems are worth
     * distinguishing because the fix is theirs; everything else reads the same.
     *
     * @param cause the failure raised while asking the tutor
     * @return a Spanish, user-facing message
     */
    fun errorMessageFor(cause: Throwable): String = when (cause) {
        is UnknownHostException, is IOException ->
            "No hay conexión. Comprueba tu internet y vuelve a intentarlo."
        else ->
            "Bali no ha podido responder ahora mismo. Inténtalo de nuevo en unos segundos."
    }
}
