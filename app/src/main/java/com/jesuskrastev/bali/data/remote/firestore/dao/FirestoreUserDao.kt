package com.jesuskrastev.bali.data.remote.firestore.dao

import android.util.Log
import com.jesuskrastev.bali.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.entities.AnswerFirestore
import com.jesuskrastev.bali.data.remote.firestore.entities.TestResultFirestore
import com.jesuskrastev.bali.data.remote.firestore.entities.UserFirestore
import com.jesuskrastev.bali.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserDao @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection =
        firestore.collection("env").document(BuildConfig.BUILD_TYPE).collection("users")

    companion object {
        private const val BATCH_LIMIT = 500
    }

    // --- User Operations ---
    suspend fun getSchemaVersion(userId: String): Int {
        val snapshot = collection.document(userId).get().await()
        if (!snapshot.exists()) return 1
        return snapshot.getLong("schemaVersion")?.toInt() ?: 1
    }

    fun getUser(userId: String): Flow<UserFirestore?> {
        return collection.document(userId).snapshots().map {
            it.toObject(UserFirestore::class.java)
        }
    }

    suspend fun updateUser(userId: String, user: User) {
        collection.document(userId).set(user.toFirestore(), SetOptions.merge()).await()
    }

    suspend fun updateFields(userId: String, updates: Map<String, Any>) {
        collection.document(userId).update(updates).await()
    }

    /**
     * Atomically adds [amount] to the user's coin balance using Firestore's server-side
     * increment operator — no read is involved, so two concurrent calls can't race each
     * other. `set(merge = true)` is used instead of `update` so this also works the very
     * first time, before the user's document exists yet, instead of throwing NOT_FOUND.
     */
    suspend fun incrementCoins(userId: String, amount: Int) {
        collection.document(userId)
            .set(mapOf("coins" to FieldValue.increment(amount.toLong())), SetOptions.merge())
            .await()
    }

    /**
     * Atomically subtracts [amount] from the user's coin balance inside a Firestore
     * transaction: the balance is read and checked, and the write only happens if it was
     * enough — all as one indivisible operation. If another write lands on the document
     * in between, Firestore retries this transaction against the fresh value automatically,
     * so a concurrent spend can't succeed twice off the same starting balance.
     *
     * @return true if the balance was sufficient and the subtraction applied, false otherwise.
     */
    suspend fun decrementCoinsIfEnough(userId: String, amount: Int): Boolean {
        val docRef = collection.document(userId)
        return firestore.runTransaction { transaction ->
            val current = transaction.get(docRef).getLong("coins") ?: 0L
            if (current < amount) {
                false
            } else {
                transaction.update(docRef, "coins", current - amount)
                true
            }
        }.await()
    }

    // --- Test Results Operations ---
    fun getTestResults(userId: String): Flow<List<TestResultFirestore>> {
        return collection.document(userId).collection("test_results")
            .orderBy("dateMillis", Query.Direction.DESCENDING).snapshots()
            .map { it.toObjects(TestResultFirestore::class.java) }
    }

    fun getAnswers(userId: String): Flow<List<AnswerFirestore>> {
        return collection.document(userId).collection("answers").snapshots()
            .map { it.toObjects(AnswerFirestore::class.java) }
    }

    suspend fun insertTestResult(userId: String, result: TestResultFirestore): String {
        val docRef = collection.document(userId).collection("test_results").document()
        docRef.set(result).await()
        return docRef.id
    }

    // --- Answers Operations ---
    fun getRecentMistakes(userId: String): Flow<List<AnswerFirestore>> {
        return collection.document(userId).collection("answers").whereEqualTo("correct", false)
            .whereEqualTo("corrected", false).snapshots()
            .map { it.toObjects(AnswerFirestore::class.java) }
    }

    suspend fun insertAnswer(userId: String, answer: AnswerFirestore) {
        collection.document(userId).collection("answers").add(answer).await()
    }

    suspend fun markAnswerAsCorrected(userId: String, questionText: String) {
        val snapshot = collection.document(userId).collection("answers")
            .whereEqualTo("questionText", questionText).get().await()

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.update(doc.reference, "corrected", true)
        }
        batch.commit().await()
    }

    fun exists(userId: String): Flow<Boolean> {
        return collection.document(userId).snapshots().map {
            it.exists()
        }
    }

    // --- Sync All ---
    suspend fun uploadAll(
        userId: String,
        user: UserFirestore,
        results: List<TestResultFirestore>,
        answers: List<AnswerFirestore>
    ) {
        val operations = mutableListOf<(WriteBatch) -> Unit>()

        operations.add { batch ->
            batch.set(collection.document(userId), user, SetOptions.merge())
        }

        results.forEach { result ->
            val testResultRef = collection.document(userId).collection("test_results").document()
            val firestoreTestId = testResultRef.id

            operations.add { batch ->
                batch.set(testResultRef, result)
            }

            answers.filter { it.testId == result.id }.forEach { answer ->
                val answerRef = collection.document(userId).collection("answers").document()
                operations.add { batch ->
                    batch.set(answerRef, answer.copy(testId = firestoreTestId))
                }
            }
        }

        operations.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { it(batch) }
            batch.commit().await()
        }
    }
}