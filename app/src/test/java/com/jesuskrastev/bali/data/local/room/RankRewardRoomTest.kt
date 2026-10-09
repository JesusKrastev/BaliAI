package com.jesuskrastev.bali.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.domain.model.RankReward
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises the actual SQLite claim transaction for coin and inventory prizes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RankRewardRoomTest {
    private lateinit var database: BaliDatabase

    /** Opens a fresh in-memory database for each storage test; returns Unit. */
    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BaliDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    /** Closes the Room connection after each test; returns Unit. */
    @After
    fun closeDatabase() { database.close() }

    /** Grants all pack contents once without changing XP, streaks or existing claims. */
    @Test
    fun packIsAddedOnceAndPreservesExistingInventory(): Unit = runBlocking {
        val before = UserEntity(
            id = "u", xp = 1000, coins = 17, hints = 2, fiftyFifties = 3,
            doubleXpBoosts = 4, doubleCoinBoosts = 5, currentStreak = 9,
            claimedRankRewards = listOf("xp_50")
        )
        val reward = RankReward("pack", 600, coins = 10, hints = 2, fiftyFifties = 1, doubleXpBoosts = 2, doubleCoinBoosts = 3)
        val dao = database.userDao()
        dao.insert(before)

        assertThat(dao.claimRankReward(reward)).isTrue()
        assertThat(dao.claimRankReward(reward)).isFalse()
        assertThat(dao.getOnce()).isEqualTo(
            before.copy(
                coins = 27, hints = 4, fiftyFifties = 4, doubleXpBoosts = 6, doubleCoinBoosts = 8,
                claimedRankRewards = listOf("xp_50", "pack")
            )
        )
    }

    /** Locked prizes must not mark themselves claimed or modify any balance. */
    @Test
    fun lockedPackChangesNothing(): Unit = runBlocking {
        val before = UserEntity(id = "u", xp = 599, coins = 50)
        database.userDao().insert(before)
        assertThat(database.userDao().claimRankReward(RankProgression.rewardFor("practice_pack_600")!!)).isFalse()
        assertThat(database.userDao().getOnce()).isEqualTo(before)
    }

    /** Two overlapping requests must grant the inventory and claim id only once. */
    @Test
    fun overlappingClaimsCannotDoubleThePrize(): Unit = runBlocking {
        val dao = database.userDao()
        dao.insert(UserEntity(id = "u", xp = 600))
        val reward = RankProgression.rewardFor("practice_pack_600")!!
        val results = List(4) { async { dao.claimRankReward(reward) } }.awaitAll()
        assertThat(results.count { it }).isEqualTo(1)
        assertThat(dao.getOnce()!!.hints).isEqualTo(2)
        assertThat(dao.getOnce()!!.fiftyFifties).isEqualTo(1)
        assertThat(dao.getOnce()!!.claimedRankRewards).containsExactly(reward.id)
    }

    /** Checks every catalogue reward writes exactly its advertised contents to real storage. */
    @Test
    fun eachRewardGrantsItsAdvertisedContents(): Unit = runBlocking {
        val dao = database.userDao()
        RankProgression.rewards.forEach { reward ->
            dao.insert(UserEntity(id = "u", xp = reward.requiredXp))
            assertThat(dao.claimRankReward(reward)).isTrue()
            val after = dao.getOnce()!!
            assertThat(after.coins).isEqualTo(reward.coins)
            assertThat(after.hints).isEqualTo(reward.hints)
            assertThat(after.fiftyFifties).isEqualTo(reward.fiftyFifties)
            assertThat(after.doubleXpBoosts).isEqualTo(reward.doubleXpBoosts)
            assertThat(after.doubleCoinBoosts).isEqualTo(reward.doubleCoinBoosts)
        }
    }
}
