package com.jesuskrastev.bali.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.ChatRole
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.repository.ChatRepository
import com.jesuskrastev.bali.domain.usecase.AskDrivingTutorUseCase
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the AI tutor chat: streams the stored conversation into the UI and sends the
 * student's questions to Gemini through [AskDrivingTutorUseCase].
 *
 * The transcript is never held here as the source of truth — it is observed from
 * [ChatRepository], so a message written on another device (or restored after a
 * reinstall) shows up without any extra plumbing.
 *
 * The first answered question also completes a step of Home's first-steps bar, through
 * [CompleteFirstStepUseCase].
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val askDrivingTutorUseCase: AskDrivingTutorUseCase,
    private val completeFirstStepUseCase: CompleteFirstStepUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val reducer = ChatReducer()

    /** Guards [AnalyticsTracker.chatOpened] so it fires once per screen, not per emission. */
    private var hasTrackedOpen = false

    init {
        observeHistory()
    }

    /**
     * Handles a user action, delegating pure transitions to [ChatReducer] and running
     * the I/O ones itself.
     *
     * @param event the action the screen reported
     */
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SendDraft -> ask(_uiState.value.draft, fromSuggestion = false)
            is ChatEvent.SendSuggestion -> ask(event.question, fromSuggestion = true)
            ChatEvent.RetryFailed -> retryFailed()
            ChatEvent.ConfirmClear -> clearConversation()
            else -> _uiState.update { reducer.reduce(it, event) }
        }
    }

    /** Mirrors the persisted conversation into the state as it changes. */
    private fun observeHistory() {
        viewModelScope.launch {
            chatRepository.observeHistory()
                .catch { cause ->
                    FirebaseCrashlytics.getInstance().recordException(cause)
                    _uiState.update {
                        it.copy(isHistoryLoading = false, error = reducer.errorMessageFor(cause))
                    }
                }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages, isHistoryLoading = false) }
                    if (!hasTrackedOpen) {
                        hasTrackedOpen = true
                        analytics.chatOpened(hasHistory = messages.isNotEmpty())
                    }
                }
        }
    }

    /**
     * Sends a question to the tutor.
     *
     * @param question the text to ask; blank input and concurrent sends are ignored.
     * @param fromSuggestion true when it came from a suggested prompt rather than typing.
     */
    private fun ask(question: String, fromSuggestion: Boolean) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return

        val turnIndex = _uiState.value.messages.count { it.role == ChatRole.USER }
        _uiState.update { reducer.onSendStarted(it) }
        analytics.chatMessageSent(
            questionLength = trimmed.length,
            fromSuggestion = fromSuggestion,
            turnIndex = turnIndex
        )

        deliver(trimmed, persistQuestion = true)
    }

    /**
     * Re-asks the question whose answer failed. The question is already in the
     * transcript, so it is not stored again.
     */
    private fun retryFailed() {
        val question = _uiState.value.failedQuestion ?: return
        if (_uiState.value.isSending) return

        _uiState.update { reducer.onSendStarted(it) }
        deliver(question, persistQuestion = false)
    }

    /**
     * Runs the actual request and folds the outcome into the state.
     *
     * @param question the text to ask
     * @param persistQuestion false when the question is already stored (a retry)
     */
    private fun deliver(question: String, persistQuestion: Boolean) {
        viewModelScope.launch {
            askDrivingTutorUseCase(question, persistQuestion)
                .onSuccess {
                    _uiState.update { state -> reducer.onSendSucceeded(state) }
                    // A reply from Bali is the "Pregúntale una duda a Bali" step of Home's bar.
                    completeFirstStepUseCase(FirstStepTask.ASK_BALI)?.let(analytics::firstStepRewarded)
                }
                .onFailure { cause ->
                    if (cause is CancellationException) throw cause
                    analytics.chatMessageFailed(cause::class.simpleName ?: "Unknown")
                    _uiState.update { state -> reducer.onSendFailed(state, question, cause) }
                }
        }
    }

    /**
     * Wipes the conversation from the active data source and closes the dialog.
     * The repository moves the delete to [kotlinx.coroutines.Dispatchers.IO] itself, so
     * the state update lands back on the main dispatcher.
     */
    private fun clearConversation() {
        viewModelScope.launch {
            runCatching { chatRepository.clear() }
                .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
            analytics.chatCleared()
            _uiState.update {
                it.copy(showClearConfirmation = false, error = null, failedQuestion = null)
            }
        }
    }
}
