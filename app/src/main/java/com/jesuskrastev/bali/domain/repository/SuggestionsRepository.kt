package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.Suggestion

interface SuggestionsRepository {
    suspend fun sendSuggestion(suggestion: Suggestion): Result<Unit>
}
