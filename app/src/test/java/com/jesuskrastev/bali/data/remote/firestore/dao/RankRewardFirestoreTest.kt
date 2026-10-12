package com.jesuskrastev.bali.data.remote.firestore.dao

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.model.RankReward
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Verifies the remote SDK receives one atomic grant, using mocks only for Firebase types. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RankRewardFirestoreTest {
    private val firestore = mock<FirebaseFirestore>()
    private val environment = mock<CollectionReference>()
    private val environmentDocument = mock<DocumentReference>()
    private val users = mock<CollectionReference>()
    private val userDocument = mock<DocumentReference>()
    private val transaction = mock<Transaction>()
    private val snapshot = mock<DocumentSnapshot>()
    private lateinit var dao: FirestoreUserDao

    /** Configures an immediate successful Firebase transaction for each test; returns Unit. */
    @Before
    fun setUp() {
        whenever(firestore.collection("env")).thenReturn(environment)
        whenever(environment.document(BuildConfig.BUILD_TYPE)).thenReturn(environmentDocument)
        whenever(environmentDocument.collection("users")).thenReturn(users)
        whenever(users.document("u")).thenReturn(userDocument)
        whenever(transaction.get(userDocument)).thenReturn(snapshot)
        whenever(firestore.runTransaction(any<Transaction.Function<Boolean>>())).thenAnswer {
            Tasks.forResult(it.getArgument<Transaction.Function<Boolean>>(0).apply(transaction))
        }
        dao = FirestoreUserDao(firestore)
    }

    /** Contents and claim id are merged together, preserving existing inventory and claims. */
    @Test
    fun allPackContentsAreGrantedInOneWrite(): Unit = runBlocking {
        whenever(snapshot.getLong("xp")).thenReturn(600L)
        whenever(snapshot.getLong("coins")).thenReturn(10L)
        whenever(snapshot.getLong("hints")).thenReturn(3L)
        whenever(snapshot.getLong("fiftyFifties")).thenReturn(4L)
        whenever(snapshot.getLong("doubleXpBoosts")).thenReturn(5L)
        whenever(snapshot.getLong("doubleCoinBoosts")).thenReturn(6L)
        whenever(snapshot.get("claimedRankRewards")).thenReturn(listOf("xp_50"))
        val prize = RankReward("pack", 600, coins = 20, hints = 2, fiftyFifties = 1, doubleXpBoosts = 2, doubleCoinBoosts = 3)

        assertThat(dao.claimRankReward("u", prize)).isTrue()
        val payload = argumentCaptor<Map<String, Any>>()
        verify(transaction).set(eq(userDocument), payload.capture(), any<SetOptions>())
        assertThat(payload.firstValue).containsExactly(
            "coins", 30L, "hints", 5L, "fiftyFifties", 5L, "doubleXpBoosts", 7L,
            "doubleCoinBoosts", 9L, "claimedRankRewards", listOf("xp_50", "pack")
        )
    }

    /** Existing profiles lacking optional inventory counters start from zero. */
    @Test
    fun missingOptionalCountersAreTreatedAsZero(): Unit = runBlocking {
        whenever(snapshot.getLong("xp")).thenReturn(600L)
        assertThat(dao.claimRankReward("u", RankReward("hint", 600, hints = 2))).isTrue()
        val payload = argumentCaptor<Map<String, Any>>()
        verify(transaction).set(eq(userDocument), payload.capture(), any<SetOptions>())
        assertThat(payload.firstValue["hints"]).isEqualTo(2L)
        assertThat(payload.firstValue["coins"]).isEqualTo(0L)
    }

    /** Locked prizes cannot update the remote profile. */
    @Test
    fun insufficientXpDoesNotWrite(): Unit = runBlocking {
        whenever(snapshot.getLong("xp")).thenReturn(599L)
        assertThat(dao.claimRankReward("u", RankReward("hint", 600, hints = 2))).isFalse()
        verify(transaction, never()).set(any<DocumentReference>(), any<Map<String, Any>>(), any<SetOptions>())
    }

    /** A retry of a previously collected prize cannot pay its inventory a second time. */
    @Test
    fun previouslyClaimedPrizeDoesNotWrite(): Unit = runBlocking {
        whenever(snapshot.getLong("xp")).thenReturn(600L)
        whenever(snapshot.get("claimedRankRewards")).thenReturn(listOf("hint"))
        assertThat(dao.claimRankReward("u", RankReward("hint", 600, hints = 2))).isFalse()
        verify(transaction, never()).set(any<DocumentReference>(), any<Map<String, Any>>(), any<SetOptions>())
    }
}
