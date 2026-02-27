package com.jesuskrastev.bali.data.remote.firestore.dao

import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.data.remote.firestore.entities.SuggestionFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSuggestionDao @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("env").document(BuildConfig.BUILD_TYPE).collection("suggestions")

    suspend fun insert(suggestion: SuggestionFirestore) {
        collection.add(suggestion).await()
    }
}
