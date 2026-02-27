package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.data.mapper.toSuggestionFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreSuggestionDao
import com.jesuskrastev.bali.domain.model.Suggestion
import com.jesuskrastev.bali.domain.repository.SuggestionsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionsRepositoryImpl @Inject constructor(
    private val firestoreSuggestionDao: FirestoreSuggestionDao
) : SuggestionsRepository {

    override suspend fun sendSuggestion(suggestion: Suggestion): Result<Unit> {
        return try {
            firestoreSuggestionDao.insert(suggestion.toSuggestionFirestore())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
