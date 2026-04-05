package com.jesuskrastev.bali.data.remote.firestore.dao

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.data.remote.firestore.entities.LessonNodeFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreLessonNodeDao @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("env").document(BuildConfig.BUILD_TYPE).collection("users")

    fun getPathNodes(userId: String): Flow<List<LessonNodeFirestore>> {
        return collection.document(userId).collection("learning_path")
            .orderBy("orderIndex", Query.Direction.ASCENDING)
            .snapshots(MetadataChanges.INCLUDE)
            .filter { !it.metadata.isFromCache || it.documents.isNotEmpty() }
            .map { it.toObjects(LessonNodeFirestore::class.java) }
    }

    suspend fun insertNodes(userId: String, nodes: List<LessonNodeFirestore>) {
        val batch = firestore.batch()
        val pathCollection = collection.document(userId).collection("learning_path")
        
        nodes.forEach { node ->
            val docRef = pathCollection.document(node.id)
            batch.set(docRef, node, SetOptions.merge())
        }
        batch.commit().await()
    }

    suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int? = null) {
        val nodeRef = collection.document(userId).collection("learning_path").document(nodeId)
        val updates = mutableMapOf<String, Any>("status" to status)
        if (scorePercentage != null) {
            updates["scorePercentage"] = scorePercentage
        }
        nodeRef.update(updates).await()
    }
}
