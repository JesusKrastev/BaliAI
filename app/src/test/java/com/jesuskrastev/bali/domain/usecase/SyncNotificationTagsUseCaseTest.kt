package com.jesuskrastev.bali.domain.usecase

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.StudyRhythm
import com.jesuskrastev.bali.domain.model.StudySchedule
import com.jesuskrastev.bali.domain.model.StudySlot
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.jesuskrastev.bali.domain.usecase.SyncNotificationTagsUseCase.Companion.TAG_LAST_SESSION_AT
import com.jesuskrastev.bali.domain.usecase.SyncNotificationTagsUseCase.Companion.TAG_PREMIUM_SINCE
import com.jesuskrastev.bali.domain.usecase.SyncNotificationTagsUseCase.Companion.TAG_STREAK_DAYS
import com.jesuskrastev.bali.domain.usecase.SyncNotificationTagsUseCase.Companion.TAG_STUDY_RHYTHM
import com.jesuskrastev.bali.domain.usecase.SyncNotificationTagsUseCase.Companion.TAG_STUDY_SLOT
import com.jesuskrastev.bali.domain.usecase.SyncNotificationTagsUseCase.Companion.notificationTags
import com.jesuskrastev.bali.ui.screens.auth.FakeNotificationsRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

/** [AuthRepository] whose session can be changed during a test. */
private class SwitchableAuthRepository(userId: String?) : AuthRepository {
    val userId = MutableStateFlow(userId)
    override val isLoggedIn: Flow<Boolean> = this.userId.map { it != null }
    override val currentUserFlow: Flow<String?> = this.userId
    override suspend fun getGoogleIdTokenAndEmail(context: Context): Result<Pair<String, String>> =
        Result.failure(UnsupportedOperationException())
    override suspend fun signInWithGoogleCredential(idToken: String): Result<Unit> = Result.success(Unit)
    override suspend fun existsInAuth(email: String): Boolean = true
    override suspend fun signOut(context: Context) { userId.value = null }
    override suspend fun currentUser(): String? = userId.value
    override val currentUserPhotoUrlFlow: Flow<String?> = flowOf(null)
    override val currentUserEmailFlow: Flow<String?> = flowOf(null)
}

/**
 * [SubscriptionRepository] where RevenueCat answers with [premiumSince], or not at all when
 * [answers] is false, and pushes later updates through [updates].
 */
private class ControllableSubscriptionRepository(
    var premiumSince: Long? = null,
    private val answers: Boolean = true
) : SubscriptionRepository {
    private val customerInfo = mock<CustomerInfo>()
    val updates = MutableSharedFlow<CustomerInfo>()

    /** Makes RevenueCat push a new state, as after a purchase or an expiry. */
    suspend fun push(premiumSince: Long?) {
        this.premiumSince = premiumSince
        updates.emit(customerInfo)
    }

    override fun customerInfoStream(): Flow<CustomerInfo> = updates
    override suspend fun getCustomerInfo(): Result<CustomerInfo> =
        if (answers) Result.success(customerInfo) else Result.failure(IllegalStateException("offline"))
    override suspend fun restorePurchases(): Result<CustomerInfo> = Result.success(customerInfo)
    override fun hasPremiumEntitlement(customerInfo: CustomerInfo): Boolean = premiumSince != null
    override fun premiumSinceMillis(customerInfo: CustomerInfo): Long? = premiumSince
    override fun premiumSubscription(customerInfo: CustomerInfo): com.jesuskrastev.bali.domain.model.PremiumSubscription? = null
    override suspend fun getOffering(identifier: String): Result<Offering?> = Result.success(null)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SyncNotificationTagsUseCaseTest {

    private val schedule = StudySchedule(StudySlot.NIGHT, StudyRhythm.OFTEN)

    // ── Which tags are written ──────────────────────────────────────────────

    @Test
    fun `the study moment and the streak are sent as tags`() {
        val tags = notificationTags(
            user = User(currentStreak = 3, lastPracticeTimestamp = 1_700_000_000_123),
            premiumSinceMillis = null,
            premiumKnown = false,
            schedule = schedule
        )

        assertThat(tags).containsExactly(
            TAG_STUDY_SLOT, "night",
            TAG_STUDY_RHYTHM, "often",
            TAG_LAST_SESSION_AT, "1700000000",
            TAG_STREAK_DAYS, "3"
        )
    }

    @Test
    fun `times are sent in whole seconds, as OneSignal's time filters expect`() {
        val tags = notificationTags(null, premiumSinceMillis = 1_700_000_000_999, premiumKnown = true, schedule = null)

        assertThat(tags[TAG_PREMIUM_SINCE]).isEqualTo("1700000000")
    }

    @Test
    fun `a subscription that ended removes the subscriber tag`() {
        val tags = notificationTags(null, premiumSinceMillis = null, premiumKnown = true, schedule = null)

        assertThat(tags).containsEntry(TAG_PREMIUM_SINCE, null)
    }

    @Test
    fun `an unanswered RevenueCat call leaves the subscriber tag alone`() {
        val tags = notificationTags(null, premiumSinceMillis = null, premiumKnown = false, schedule = null)

        assertThat(tags).doesNotContainKey(TAG_PREMIUM_SINCE)
    }

    @Test
    fun `a device without an answer keeps the study moment the account already has`() {
        val tags = notificationTags(User(), premiumSinceMillis = null, premiumKnown = false, schedule = null)

        assertThat(tags).doesNotContainKey(TAG_STUDY_SLOT)
        assertThat(tags).doesNotContainKey(TAG_STUDY_RHYTHM)
    }

    @Test
    fun `someone who never studied gets no last session`() {
        val tags = notificationTags(User(lastPracticeTimestamp = 0), null, premiumKnown = false, schedule = null)

        assertThat(tags).doesNotContainKey(TAG_LAST_SESSION_AT)
    }

    // ── Linking the account and sending ─────────────────────────────────────

    private val notifications = FakeNotificationsRepository()
    private val users = FakeUserRepository()

    /**
     * Starts the sync in the background of the test, running eagerly so every change to the
     * fakes is mirrored before the next line of the test.
     */
    private fun TestScope.startSync(
        auth: AuthRepository,
        subscriptions: SubscriptionRepository = ControllableSubscriptionRepository()
    ) {
        val useCase = SyncNotificationTagsUseCase(auth, users, subscriptions, notifications)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase() }
    }

    @Test
    fun `a signed-in user is linked before any tag is sent`() = runTest {
        startSync(SwitchableAuthRepository("uid_1"))

        assertThat(notifications.calls.first()).isEqualTo("identify:uid_1")
        assertThat(notifications.calls).contains("tags")
    }

    @Test
    fun `a signed-out start does not unlink anything`() = runTest {
        startSync(SwitchableAuthRepository(null))

        assertThat(notifications.identifiedUsers).isEmpty()
        assertThat(notifications.sentTags).isNotEmpty()
    }

    @Test
    fun `signing in links the account and sends the tags again`() = runTest {
        // Linking to an account that already exists in OneSignal drops the tags set before.
        val auth = SwitchableAuthRepository(null)
        startSync(auth)
        notifications.calls.clear()

        auth.userId.value = "uid_1"

        assertThat(notifications.calls).containsExactly("identify:uid_1", "tags").inOrder()
    }

    @Test
    fun `signing out unlinks the device`() = runTest {
        val auth = SwitchableAuthRepository("uid_1")
        startSync(auth)

        auth.userId.value = null

        assertThat(notifications.identifiedUsers).containsExactly("uid_1", null).inOrder()
    }

    @Test
    fun `the study moment is sent as soon as it is saved`() = runTest {
        startSync(SwitchableAuthRepository(null))

        notifications.saveStudySchedule(schedule)

        assertThat(notifications.sentTags.last()).containsEntry(TAG_STUDY_SLOT, "night")
    }

    @Test
    fun `a purchase marks the user as a subscriber`() = runTest {
        val subscriptions = ControllableSubscriptionRepository(premiumSince = null)
        startSync(SwitchableAuthRepository("uid_1"), subscriptions)
        assertThat(notifications.sentTags.last()).containsEntry(TAG_PREMIUM_SINCE, null)

        subscriptions.push(premiumSince = 1_700_000_000_000)

        assertThat(notifications.sentTags.last()).containsEntry(TAG_PREMIUM_SINCE, "1700000000")
    }

    @Test
    fun `offline, the subscriber tag is never touched`() = runTest {
        startSync(SwitchableAuthRepository("uid_1"), ControllableSubscriptionRepository(answers = false))

        notifications.sentTags.forEach { assertThat(it).doesNotContainKey(TAG_PREMIUM_SINCE) }
    }

    @Test
    fun `the same tags are not sent twice`() = runTest {
        startSync(SwitchableAuthRepository("uid_1"))
        val sentBefore = notifications.sentTags.size

        // Coins change the profile but none of the tags.
        users.incrementCoins(10)

        assertThat(notifications.sentTags).hasSize(sentBefore)
    }
}
