package com.jesuskrastev.bali.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.PremiumSubscription
import com.jesuskrastev.bali.domain.model.ProgressStats
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

/** The two steps of the cancellation flow. */
enum class CancelStep(val analyticsName: String) {
    /** What the user has achieved and would lose. */
    Progress("progress"),

    /** "¿Por qué quieres cancelar?", with an answer tailored to each reason. */
    Reason("reason")
}

/**
 * Answers to "¿Por qué quieres cancelar?".
 *
 * @property id stable name reported as `subscription_cancel_reason`
 * @property label the option as the user reads it
 */
enum class CancelReason(val id: String, val label: String) {
    PassedExam("passed_exam", "Ya he aprobado el examen"),
    TooExpensive("too_expensive", "Me parece caro"),
    NotUsing("not_using", "No lo uso lo suficiente"),
    MissingSomething("missing_something", "Le falta algo que necesito"),
    Other("other", "Otro motivo")
}

/**
 * What the progress step shows.
 *
 * @property stats the user's figures, or null while they load
 * @property daysToExam whole days left until the exam, or null when it is not booked or has passed
 * @property accessEndsOn the day access ends if they cancel, already formatted, or null when unknown
 */
data class CancelProgressUiState(
    val stats: ProgressStats? = null,
    val daysToExam: Long? = null,
    val accessEndsOn: String? = null
)

/**
 * Backs the in-app cancellation flow that replaced RevenueCat's Customer Center: shows what the
 * subscriber has built and would lose, asks why they leave, and only then hands them to Google
 * Play, where the cancellation actually happens. Staying is always one tap away, and so is going
 * on: the reason is optional, so the flow never blocks a cancellation.
 */
@HiltViewModel
class CancelSubscriptionViewModel @Inject constructor(
    testResultRepository: TestResultRepository,
    answerRepository: AnswerRepository,
    userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val calculateProgressStats: CalculateProgressStatsUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private val subscription = MutableStateFlow<PremiumSubscription?>(null)

    val progress: StateFlow<CancelProgressUiState> = combine(
        testResultRepository.get(),
        answerRepository.getAll(),
        userRepository.get(),
        subscription
    ) { results, answers, user, plan ->
        val now = System.currentTimeMillis()
        CancelProgressUiState(
            stats = calculateProgressStats(results, answers, user, now),
            daysToExam = user?.examDateMillis?.let { daysUntil(it, now) }?.takeIf { it > 0 },
            accessEndsOn = plan?.endsAtMillis?.let(::formatDay)
        )
    }
        .catch { emit(CancelProgressUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CancelProgressUiState())

    private val _step = MutableStateFlow(CancelStep.Progress)
    val step: StateFlow<CancelStep> = _step.asStateFlow()

    private val _reason = MutableStateFlow<CancelReason?>(null)
    val reason: StateFlow<CancelReason?> = _reason.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.getCustomerInfo().onSuccess {
                subscription.value = subscriptionRepository.premiumSubscription(it)
            }
        }
    }

    /** Moves from the progress step to the reason step. */
    fun continueCancelling() {
        _step.value = CancelStep.Reason
    }

    /** Goes back to the progress step from the reason step. */
    fun backToProgress() {
        _step.value = CancelStep.Progress
    }

    /**
     * Picks an answer to the survey.
     *
     * @param reason the answer tapped
     */
    fun selectReason(reason: CancelReason) {
        _reason.value = reason
    }

    /** Reports that the user chose to keep the plan on the current step. */
    fun keepPlan() {
        analytics.subscriptionCancelKept(_step.value.analyticsName, _reason.value?.id)
    }

    /**
     * Reports the reason and the hand-off to Google Play.
     *
     * @return the Play URL that manages this subscription
     */
    fun goToPlay(): String {
        analytics.subscriptionCancelReason(_reason.value?.id ?: "skipped")
        analytics.subscriptionManagementOpened()
        return playSubscriptionsUrl(subscription.value?.productId)
    }
}

private const val PACKAGE_NAME = "com.jesuskrastev.bali"

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "ES"))

private fun formatDay(millis: Long): String =
    DAY_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/**
 * Calendar days from today until [targetMillis], counted in the device's zone.
 *
 * @param targetMillis the day to count to
 * @param nowMillis the current time
 * @param zone zone used to find both days
 * @return the days left, negative once the day has passed
 */
internal fun daysUntil(targetMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(today, target)
}

/**
 * Google Play's page for managing this app's subscription, or the general subscriptions list when
 * the product is unknown.
 *
 * @param productId the store product, possibly suffixed with `:base-plan`, or null
 * @return the URL to open
 */
internal fun playSubscriptionsUrl(productId: String?): String {
    val sku = productId?.substringBefore(':')?.takeIf { it.isNotBlank() }
        ?: return "https://play.google.com/store/account/subscriptions"
    return "https://play.google.com/store/account/subscriptions?sku=$sku&package=$PACKAGE_NAME"
}
