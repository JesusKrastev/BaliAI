package com.jesuskrastev.bali.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.repository.FeedbackPromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the green "tu opinión nos importa" banner on Home: result screens raise it through
 * [markPending] and Home shows it while [isPending] is true.
 */
@HiltViewModel
class FeedbackPromptViewModel @Inject constructor(
    private val repository: FeedbackPromptRepository
) : ViewModel() {

    /** True while the banner should be visible on Home. */
    val isPending: StateFlow<Boolean> = repository.isPending.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /** Raises the banner after an excellent result; ignored once it was answered. */
    fun markPending() {
        viewModelScope.launch { repository.markPending() }
    }

    /** Hides the banner for good, whether it was closed or the student tapped through. */
    fun dismiss() {
        viewModelScope.launch { repository.dismiss() }
    }
}
