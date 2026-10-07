package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Firestore rejects write batches with more than this many operations. */
private const val BATCH_LIMIT = 500

/**
 * Deletes every document of this collection, in batches that respect Firestore's size limit.
 *
 * @param firestore used to open the write batches
 */
internal suspend fun CollectionReference.deleteAllDocuments(firestore: FirebaseFirestore) {
    get().await().documents.chunked(BATCH_LIMIT).forEach { chunk ->
        val batch = firestore.batch()
        chunk.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
