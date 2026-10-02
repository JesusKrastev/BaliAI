package com.jesuskrastev.bali.data.remote.firestore.dao

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jesuskrastev.bali.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.entities.AnswerFirestore
import com.jesuskrastev.bali.data.remote.firestore.entities.TestResultFirestore
import com.jesuskrastev.bali.data.remote.firestore.entities.UserFirestore
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import kotlinx.coroutines.CancellationException
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

    /**
     * Runs [block] as one Firestore transaction, reporting a failure to Crashlytics before
     * rethrowing it. Shop writes are transactions because they read the balance or the stock
     * they change; unlike [incrementCoins] they need a connection.
     *
     * @param block the reads and writes of the transaction
     * @return what [block] returned
     */
    private suspend fun <T> shopTransaction(block: (Transaction) -> T): T =
        try {
            firestore.runTransaction { transaction -> block(transaction) }.await()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            FirebaseCrashlytics.getInstance().recordException(error)
            throw error
        }

    /**
     * Purchases an inventory [item] for [cost] within one Firestore transaction.
     *
     * @param userId document owner
     * @param item consumable to add
     * @param cost coins to charge
     * @return true only when the balance permits the purchase
     */
    suspend fun purchaseInventoryItem(userId: String, item: ShopInventoryItem, cost: Int): Boolean {
        val docRef = collection.document(userId)
        val field = item.firestoreField
        return shopTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val coins = snapshot.getLong("coins") ?: 0L
            if (coins < cost) {
                false
            } else {
                val owned = snapshot.getLong(field) ?: 0L
                transaction.set(
                    docRef,
                    mapOf("coins" to coins - cost, field to owned + 1),
                    SetOptions.merge()
                )
                true
            }
        }
    }

    /**
     * Charges a surprise chest and credits its coin [reward] atomically.
     *
     * @param userId document owner
     * @param cost coins paid to open the chest
     * @param reward random coin reward, picked by the caller
     * @return true if the chest opened, false when the user cannot afford it
     */
    suspend fun openSurpriseChest(userId: String, cost: Int, reward: Int): Boolean {
        val docRef = collection.document(userId)
        return shopTransaction { transaction ->
            val coins = transaction.get(docRef).getLong("coins") ?: 0L
            if (coins < cost) {
                false
            } else {
                transaction.set(docRef, mapOf("coins" to coins - cost + reward), SetOptions.merge())
                true
            }
        }
    }

    /**
     * Removes one owned [item] atomically.
     *
     * @param userId document owner
     * @param item consumable to use
     * @return true only when an item was available
     */
    suspend fun consumeInventoryItem(userId: String, item: ShopInventoryItem): Boolean {
        val field = item.firestoreField
        val docRef = collection.document(userId)
        return shopTransaction { transaction ->
            val count = transaction.get(docRef).getLong(field) ?: 0L
            if (count <= 0) {
                false
            } else {
                transaction.set(docRef, mapOf(field to count - 1), SetOptions.merge())
                true
            }
        }
    }

    /**
     * Charges the streak bet's [cost] and records its [target] in one transaction.
     *
     * @param userId document owner
     * @param cost coins to charge
     * @param target streak length that wins the bet
     * @return true if the bet was placed, false for insufficient coins or a bet already active
     */
    suspend fun placeStreakBet(userId: String, cost: Int, target: Int): Boolean {
        val docRef = collection.document(userId)
        return shopTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val coins = snapshot.getLong("coins") ?: 0L
            val hasBet = (snapshot.getLong("streakBetTarget") ?: 0L) > 0
            if (coins < cost || hasBet) {
                false
            } else {
                transaction.set(
                    docRef,
                    mapOf("coins" to coins - cost, "streakBetTarget" to target),
                    SetOptions.merge()
                )
                true
            }
        }
    }

    /**
     * Credits the won streak bet and clears it in one transaction, so it pays only once.
     *
     * @param userId document owner
     * @param payout coins paid
     * @return true if a bet was claimed, false if none was active
     */
    suspend fun claimStreakBet(userId: String, payout: Int): Boolean {
        val docRef = collection.document(userId)
        return shopTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if ((snapshot.getLong("streakBetTarget") ?: 0L) <= 0) {
                false
            } else {
                val coins = snapshot.getLong("coins") ?: 0L
                transaction.set(
                    docRef,
                    mapOf("coins" to coins + payout, "streakBetTarget" to 0),
                    SetOptions.merge()
                )
                true
            }
        }
    }

    /**
     * Forgets the streak bet without paying it.
     *
     * @param userId document owner
     */
    suspend fun clearStreakBet(userId: String) {
        collection.document(userId)
            .set(mapOf("streakBetTarget" to 0), SetOptions.merge())
            .await()
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

/** Returns the Firestore counter field associated with this consumable item. */
private val ShopInventoryItem.firestoreField: String
    get() = when (this) {
        ShopInventoryItem.HINT -> "hints"
        ShopInventoryItem.FIFTY_FIFTY -> "fiftyFifties"
        ShopInventoryItem.DOUBLE_XP -> "doubleXpBoosts"
        ShopInventoryItem.DOUBLE_COINS -> "doubleCoinBoosts"
    }
