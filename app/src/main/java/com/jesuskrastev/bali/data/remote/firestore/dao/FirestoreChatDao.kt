package com.jesuskrastev.bali.data.remote.firestore.dao

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.data.remote.firestore.entities.ChatMessageFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore access to the AI tutor conversation, stored as a `chat_messages`
 * subcollection of the user document.
 */
@Singleton
class FirestoreChatDao @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection =
        firestore.collection("env").document(BuildConfig.BUILD_TYPE).collection("users")

    private companion object {
        const val CHAT_MESSAGES = "chat_messages"

        /** Firestore rejects batches above 500 operations. */
        const val BATCH_LIMIT = 500
    }

    /**
     * Emits the user's conversation ordered oldest first, re-emitting on every change.
     *
     * @param userId the authenticated user's UID.
     */
    fun getMessages(userId: String): Flow<List<ChatMessageFirestore>> =
        collection.document(userId).collection(CHAT_MESSAGES)
            .orderBy("timestampMillis", Query.Direction.ASCENDING)
            .snapshots()
            .map { it.toObjects(ChatMessageFirestore::class.java) }

    /**
     * Writes one message under its own id, so a retried save overwrites the same
     * document rather than duplicating the turn.
     *
     * @param userId the authenticated user's UID.
     * @param message the turn to store.
     */
    suspend fun insert(userId: String, message: ChatMessageFirestore) {
        try {
            collection.document(userId).collection(CHAT_MESSAGES)
                .document(message.id)
                .set(message, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    /**
     * Deletes the user's whole conversation in batches.
     *
     * @param userId the authenticated user's UID.
     */
    suspend fun clear(userId: String) {
        try {
            val snapshot = collection.document(userId).collection(CHAT_MESSAGES).get().await()
            snapshot.documents.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }
}
