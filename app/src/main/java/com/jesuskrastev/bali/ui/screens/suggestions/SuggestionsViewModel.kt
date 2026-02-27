package com.jesuskrastev.bali.ui.screens.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.Suggestion
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.SuggestionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestionsUiState(
    val suggestion: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val suggestionsRepository: SuggestionsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionsUiState())
    val uiState: StateFlow<SuggestionsUiState> = _uiState.asStateFlow()

    fun onSuggestionChange(text: String) {
        _uiState.update { it.copy(suggestion = text) }
    }

    fun sendSuggestion() {
        val text = _uiState.value.suggestion
        if (text.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser()
                val userEmail = authRepository.currentUserEmail()
                
                val result = suggestionsRepository.sendSuggestion(
                    Suggestion(
                        text = text,
                        userId = userId,
                        email = userEmail,
                        timestamp = System.currentTimeMillis()
                    )
                )

                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.localizedMessage) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
