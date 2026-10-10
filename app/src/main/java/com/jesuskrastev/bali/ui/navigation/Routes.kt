package com.jesuskrastev.bali.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe destinations of the app's NavHost. Each one is navigated to with
// `navController.navigate(SomeRoute(...))`; the arguments of a route are its constructor properties.

@Serializable
object GreetingsRoute

@Serializable
object OnboardingRoute

@Serializable
object PaywallRoute

@Serializable
object HomeRoute

@Serializable
object GamesRoute

@Serializable
object SettingsRoute

@Serializable
data class GamePlayRoute(val gameId: String)

@Serializable
data class TestRoute(
    val topic: String? = null,
    val nodeTitle: String? = null,
    val nodeDescription: String? = null,
    val nodeId: String? = null,
    val nodeType: String? = null
)

/**
 * An exam of the learning path.
 *
 * @property nodeId id of the path node being examined; it decides which lessons the exam asks about
 */
@Serializable
data class ExamRoute(val nodeId: String)

@Serializable
object ShopRoute

@Serializable
object RankRewardsRoute

@Serializable
object StatsRoute

@Serializable
object SuggestionsRoute

@Serializable
object ChatRoute

/** Plan status and subscription actions, opened from settings. */
@Serializable
object ManageSubscriptionRoute

/** The cancellation flow (progress, reason, Google Play), opened from the subscription screen. */
@Serializable
object CancelSubscriptionRoute

/**
 * Sign-in destination.
 *
 * @property restrictNewAccounts when true only Google accounts that already have a Bali
 *   profile are accepted, so "iniciar sesión" never silently creates a new account
 * @property isMandatory when true the screen cannot be dismissed: the session is required
 *   to use the app, so there is nowhere to go back to
 */
@Serializable
data class AuthRoute(
    val restrictNewAccounts: Boolean = false,
    val isMandatory: Boolean = false
)

@Serializable
data class CoinsGainedRoute(val coins: Int, val newStreakDays: Int)

/** Celebration after the first session of the day, the one that extends the streak. */
@Serializable
object StreakRoute

@Serializable
object MainStreakRoute

@Serializable
data class TestResultRoute(
    val score: Int,
    val total: Int,
    val xpGained: Int,
    val durationSeconds: Int,
    val accuracy: Int,
    val baseXp: Int,
    val bonusPerfection: Int? = null,
    val bonusFast: Int? = null,
    val bonusStreak: Int? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int = 0,
    val newTotalXp: Int = 0,
    val coinsGained: Int = 0,
    val newStreakDays: Int = -1,
    val isFailedExam: Boolean = false,
    val isPassedExam: Boolean = false,
    val isFirstWin: Boolean = false,
    val isNewRecord: Boolean = false,
    val previousBestScore: Int = -1
)
