package com.jesuskrastev.bali.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jesuskrastev.bali.ui.screens.auth.AuthScreen
import com.jesuskrastev.bali.ui.screens.auth.AuthViewModel
import com.jesuskrastev.bali.ui.screens.chat.ChatScreen
import com.jesuskrastev.bali.ui.screens.chat.ChatViewModel
import com.jesuskrastev.bali.ui.screens.coins.CoinsGainedScreen
import com.jesuskrastev.bali.ui.screens.exam.ExamScreen
import com.jesuskrastev.bali.ui.screens.exam.ExamViewModel
import com.jesuskrastev.bali.ui.screens.greetings.GreetingsScreen
import com.jesuskrastev.bali.ui.screens.home.HomeScreen
import com.jesuskrastev.bali.ui.screens.home.HomeViewModel
import com.jesuskrastev.bali.ui.screens.games.GameType
import com.jesuskrastev.bali.ui.screens.games.GamesScreen
import com.jesuskrastev.bali.ui.screens.games.drive.BaliDriveScreen
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingScreen
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingViewModel
import com.jesuskrastev.bali.ui.screens.subscription.CancelSubscriptionScreen
import com.jesuskrastev.bali.ui.screens.subscription.ManageSubscriptionScreen
import com.jesuskrastev.bali.ui.screens.paywall.PaywallScreen
import com.jesuskrastev.bali.ui.screens.store.ShopScreen
import com.jesuskrastev.bali.ui.screens.ranks.RankRewardsScreen
import com.jesuskrastev.bali.ui.screens.store.ShopViewModel
import com.jesuskrastev.bali.ui.screens.test.ResultSoundViewModel
import com.jesuskrastev.bali.ui.screens.test.TestResultScreen
import com.jesuskrastev.bali.ui.screens.test.TestScreen
import com.jesuskrastev.bali.ui.screens.test.TestSummary
import com.jesuskrastev.bali.ui.screens.test.TestViewModel
import com.jesuskrastev.bali.ui.screens.stats.StatsScreen
import com.jesuskrastev.bali.ui.screens.suggestions.SuggestionsScreen
import com.jesuskrastev.bali.ui.screens.suggestions.SuggestionsViewModel
import com.jesuskrastev.bali.ui.screens.streak.LessonStreakScreen
import com.jesuskrastev.bali.ui.screens.streak.StreakScreen
import com.jesuskrastev.bali.ui.screens.streak.StreakViewModel
import com.jesuskrastev.bali.ui.screens.settings.SettingsScreen

/**
 * Builds the result screen's route from a finished test or exam.
 *
 * @return the route carrying everything the result screen and the screens after it show
 */
private fun TestSummary.toResultRoute() = TestResultRoute(
    score = score,
    total = total,
    xpGained = xpGained,
    durationSeconds = durationSeconds,
    accuracy = accuracy,
    baseXp = baseXp,
    bonusPerfection = bonusPerfection,
    bonusFast = bonusFast,
    bonusStreak = bonusStreak,
    leveledUp = leveledUp,
    newLevel = newLevel,
    newTotalXp = newTotalXp,
    coinsGained = coinsGained,
    newStreakDays = newStreakDays,
    isFailedExam = isFailedExam,
    isPassedExam = isPassedExam,
    isFirstWin = isFirstWin,
    isNewRecord = isNewRecord,
    previousBestScore = previousBestScore
)

/**
 * Hosts every app destination, applies the hard-paywall-to-login handoff and reports each
 * destination change to analytics as a screen view.
 *
 * @param modifier modifier applied to the navigation host
 * @param navController controller used for typed navigation
 * @param startDestination destination selected from onboarding and authentication state
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = GreetingsRoute
) {
    val screenTracking: ScreenTrackingViewModel = hiltViewModel()
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            screenTracking.onDestinationChanged(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.let { destination ->
        destination.hasRoute<HomeRoute>() ||
            destination.hasRoute<ChatRoute>() ||
            destination.hasRoute<StatsRoute>() ||
            destination.hasRoute<GamesRoute>() ||
            destination.hasRoute<SettingsRoute>()
    } == true

    // Games and the Bali Drive run paint under the system bars, so they take no top inset and the
    // run takes no bottom one either: each of them places its own controls clear of the bars.
    val isGamePlay = currentDestination?.hasRoute<GamePlayRoute>() == true
    val isImmersive = isGamePlay || currentDestination?.hasRoute<GamesRoute>() == true

    SharedTransitionLayout {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    AppBottomBar(
                        currentDestination = currentDestination,
                        onHomeClick = { navController.navigateTopLevel(HomeRoute) },
                        onGamesClick = { navController.navigateTopLevel(GamesRoute) },
                        onSettingsClick = { navController.navigateTopLevel(SettingsRoute) },
                        onChatClick = { navController.navigateTopLevel(ChatRoute) },
                        onStatsClick = { navController.navigateTopLevel(StatsRoute) },
                    )
                }
            },
        ) { contentPadding ->
            // consumeWindowInsets is what stops each screen's own Scaffold/TopAppBar/bottom bar
            // from applying the status and navigation bar insets a second time on top of this padding.
            val hostPadding = when {
                isGamePlay -> PaddingValues()
                isImmersive -> PaddingValues(bottom = contentPadding.calculateBottomPadding())
                else -> contentPadding
            }
            NavHost(
                modifier = modifier
                    .padding(hostPadding)
                    .consumeWindowInsets(hostPadding),
                navController = navController,
                startDestination = startDestination
            ) {
            composable<GreetingsRoute> {
                GreetingsScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onStartClick = {
                        navController.navigate(OnboardingRoute) {
                            popUpTo(GreetingsRoute) {
                                inclusive = true
                            }
                        }
                    },
                    onAuthClick = {
                        navController.navigate(AuthRoute(restrictNewAccounts = true))
                    }
                )
            }

            composable<HomeRoute> {
                val viewModel: HomeViewModel = hiltViewModel()
                // Mock exams are free for subscribers (idea 015): no coin check, from a path
                // node or from the first-steps bar alike.
                val openExam = { navController.navigate(ExamRoute) }

                HomeScreen(
                    viewModel = viewModel,
                    onNodeTestClick = { title, desc, id, type ->
                        if (type == "EXAM") {
                            openExam()
                        } else {
                            navController.navigate(TestRoute(nodeTitle = title, nodeDescription = desc, nodeId = id, nodeType = type))
                        }
                    },
                    onShopClick = {
                        navController.navigate(ShopRoute)
                    },
                    onRanksClick = { navController.navigate(RankRewardsRoute) },
                    onStreakClick = {
                        navController.navigate(MainStreakRoute)
                    },
                    // The first-steps bar's chat task opens the Chat tab, as the bottom bar does (D-028).
                    onChatClick = {
                        navController.navigateTopLevel(ChatRoute)
                    },
                    onPlayGameClick = {
                        // The first-steps bar opens Bali Drive; every exit returns to Juegos.
                        navController.openBaliDrive()
                    },
                    onExamClick = openExam,
                    onSeePlanClick = {
                        navController.navigateTopLevel(StatsRoute)
                    }
                )
            }

            composable<GamesRoute> {
                GamesScreen(onGameClick = { navController.openBaliDrive() })
            }

            composable<GamePlayRoute> {
                // Retired routes retain their serializable signature and open Bali Drive.
                BaliDriveScreen(onExit = {
                    if (!navController.popBackStack<GamesRoute>(inclusive = false)) {
                        navController.navigateTopLevel(GamesRoute)
                    }
                })
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onAuthClick = {
                        navController.navigate(AuthRoute(restrictNewAccounts = false))
                    },
                    onFeedbackClick = {
                        navController.navigate(SuggestionsRoute)
                    },
                    onManageSubscriptionClick = {
                        navController.navigate(ManageSubscriptionRoute)
                    }
                )
            }

            composable<ManageSubscriptionRoute> {
                ManageSubscriptionScreen(
                    onBackClick = { navController.popBackStack() },
                    onCancelClick = { navController.navigate(CancelSubscriptionRoute) }
                )
            }

            composable<CancelSubscriptionRoute> {
                CancelSubscriptionScreen(
                    onClose = { navController.popBackStack() },
                    onSuggestionsClick = { navController.navigate(SuggestionsRoute) }
                )
            }

            composable<AuthRoute> { backStackEntry ->
                val route: AuthRoute = backStackEntry.toRoute()
                val viewModel: AuthViewModel = hiltViewModel()
                val isLoggingIn = viewModel.isLoggingIn.collectAsStateWithLifecycle().value
                val errorMsg = viewModel.errorMessage.collectAsStateWithLifecycle().value
                val errorEmail = viewModel.errorEmail.collectAsStateWithLifecycle().value
                
                AuthScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBackClick = { navController.popBackStack() },
                    onLoginClick = { context -> 
                        viewModel.signInWithGoogle(context, route.restrictNewAccounts) {
                            // On success, go directly to Home
                            navController.navigate(HomeRoute) {
                                if (route.isMandatory) {
                                    // The onboarding and the paywall are behind us: wipe the
                                    // whole stack so back cannot return to the gate.
                                    popUpTo(0) { inclusive = true }
                                } else {
                                    popUpTo(HomeRoute) { inclusive = true }
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    isLoggingIn = isLoggingIn,
                    errorMessage = errorMsg,
                    errorEmail = errorEmail,
                    onErrorDismiss = { viewModel.clearError() },
                    isMandatory = route.isMandatory
                )
            }

            composable<ShopRoute> {
                val viewModel: ShopViewModel = hiltViewModel()
                ShopScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }

            composable<RankRewardsRoute> {
                RankRewardsScreen(onBackClick = { navController.popBackStack() })
            }

            composable<TestRoute> { backStackEntry ->
                val route: TestRoute = backStackEntry.toRoute()
                val viewModel: TestViewModel = hiltViewModel()
                
                LaunchedEffect(route) {
                    if (route.nodeTitle != null) {
                        viewModel.setAiNodeParams(route.nodeTitle, route.nodeDescription, route.nodeId, route.nodeType)
                    } else {
                        viewModel.setTopic(route.topic)
                    }
                }

                TestScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onFinishTest = { result ->
                        navController.navigate(result.toResultRoute()) {
                            popUpTo<TestRoute> { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable<ExamRoute> {
                val viewModel: ExamViewModel = hiltViewModel()
                ExamScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onFinishExam = { result ->
                        navController.navigate(result.toResultRoute()) {
                            popUpTo(ExamRoute) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable<TestResultRoute> { backStackEntry ->
                val route: TestResultRoute = backStackEntry.toRoute()
                val resultSound: ResultSoundViewModel = hiltViewModel()
                TestResultScreen(
                    xpGained = route.xpGained,
                    baseXp = route.baseXp,
                    bonusPerfection = route.bonusPerfection,
                    bonusFast = route.bonusFast,
                    bonusStreak = route.bonusStreak,
                    leveledUp = route.leveledUp,
                    durationSeconds = route.durationSeconds,
                    accuracy = route.accuracy,
                    newLevel = route.newLevel,
                    newTotalXp = route.newTotalXp,
                    score = route.score,
                    total = route.total,
                    isFailedExam = route.isFailedExam,
                    isPassedExam = route.isPassedExam,
                    isFirstWin = route.isFirstWin,
                    isNewRecord = route.isNewRecord,
                    previousBestScore = route.previousBestScore,
                    onResultShown = resultSound::play,
                    onContinueClick = {
                        navController.navigate(CoinsGainedRoute(route.coinsGained, route.newStreakDays)) {
                            popUpTo(HomeRoute) { inclusive = false }
                        }
                    }
                )
            }

            composable<CoinsGainedRoute> { backStackEntry ->
                val route: CoinsGainedRoute = backStackEntry.toRoute()
                CoinsGainedScreen(
                    coinsGained = route.coins,
                    onContinueClick = {
                        if (route.newStreakDays > 0) {
                            navController.navigate(StreakRoute) {
                                popUpTo(HomeRoute) { inclusive = false }
                            }
                        } else {
                            navController.navigate(HomeRoute) {
                                popUpTo(HomeRoute) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<StreakRoute> {
                LessonStreakScreen(
                    viewModel = hiltViewModel<StreakViewModel>(),
                    onContinueClick = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<MainStreakRoute> {
                StreakScreen(
                    viewModel = hiltViewModel<StreakViewModel>(),
                    onBackClick = { navController.popBackStack() },
                    onShopClick = { navController.navigate(ShopRoute) }
                )
            }

            composable<OnboardingRoute> {
                val viewModel: OnboardingViewModel = hiltViewModel()

                OnboardingScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    viewModel = viewModel,
                    onComplete = {
                        // Paying is not enough: the account is what activates the access,
                        // so the paywall hands over straight to the sign-in gate.
                        navController.navigate(AuthRoute(isMandatory = true)) {
                            popUpTo(OnboardingRoute) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<PaywallRoute> {
                // PaywallRoute is only ever an app start destination (returning users who
                // completed onboarding but never unlocked premium): there is no screen behind
                // it to fall back to, so a decline — the win-back offer already shown and
                // turned down — leaves the app instead of stranding the user on a paywall
                // whose close button no longer does anything.
                val activity = LocalActivity.current
                PaywallScreen(
                    onDismissResult = { hasPremium ->
                        if (hasPremium) {
                            navController.navigate(AuthRoute(isMandatory = true)) {
                                popUpTo(PaywallRoute) { inclusive = true }
                            }
                        } else {
                            activity?.finish()
                        }
                    }
                )
            }

            composable<StatsRoute> {
                StatsScreen()
            }

            composable<SuggestionsRoute> {
                val viewModel: SuggestionsViewModel = hiltViewModel()
                SuggestionsScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }

            composable<ChatRoute> {
                val viewModel: ChatViewModel = hiltViewModel()
                ChatScreen(viewModel = viewModel)
            }
            }
        }
    }
}

/** Opens Bali Drive once, even when its entry button receives rapid repeated taps. */
private fun NavHostController.openBaliDrive() {
    if (currentDestination?.hasRoute<GamePlayRoute>() == true) return
    navigate(GamePlayRoute(GameType.DRIVE.id)) { launchSingleTop = true }
}

/** Switches primary tabs while preserving their state and avoiding duplicate destinations. */
private inline fun <reified T : Any> NavHostController.navigateTopLevel(route: T) {
    navigate(route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
