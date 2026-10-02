package com.jesuskrastev.bali.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.jesuskrastev.bali.ui.screens.chat.ChatScreen
import com.jesuskrastev.bali.ui.screens.chat.ChatViewModel
import com.jesuskrastev.bali.ui.screens.coins.CoinsGainedScreen
import com.jesuskrastev.bali.ui.screens.exam.ExamScreen
import com.jesuskrastev.bali.ui.screens.exam.ExamViewModel
import com.jesuskrastev.bali.ui.screens.greetings.GreetingsScreen
import com.jesuskrastev.bali.ui.screens.home.HomeScreen
import com.jesuskrastev.bali.ui.screens.home.HomeViewModel
import com.jesuskrastev.bali.ui.screens.games.GamePlayScreen
import com.jesuskrastev.bali.ui.screens.games.GameType
import com.jesuskrastev.bali.ui.screens.games.GamesScreen
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingScreen
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingViewModel
import com.jesuskrastev.bali.ui.screens.paywall.CustomerCenterLauncher
import com.jesuskrastev.bali.ui.screens.paywall.CustomerCenterViewModel
import com.jesuskrastev.bali.ui.screens.paywall.PaywallScreen
import com.jesuskrastev.bali.ui.screens.store.ShopScreen
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
import com.jesuskrastev.bali.ui.theme.BaliGrayMedium
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.White
import kotlinx.serialization.Serializable

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

@Serializable
object ExamRoute

@Serializable
object ShopRoute

@Serializable
object StatsRoute

@Serializable
object SuggestionsRoute

@Serializable
object ChatRoute

/** RevenueCat's Customer Center, opened from settings to manage or cancel the subscription. */
@Serializable
object CustomerCenterRoute

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
            NavHost(
                modifier = modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
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
                    onStreakClick = {
                        navController.navigate(MainStreakRoute)
                    },
                    // The first-steps bar's chat task opens the Chat tab, as the bottom bar does (D-028).
                    onChatClick = {
                        navController.navigateTopLevel(ChatRoute)
                    },
                    onPlayGameClick = {
                        // The first-steps bar's game task goes straight into a game; back returns to Home.
                        navController.navigate(GamePlayRoute(GameType.entries.first().id))
                    },
                    onExamClick = openExam,
                    onSeePlanClick = {
                        navController.navigateTopLevel(StatsRoute)
                    }
                )
            }

            composable<GamesRoute> {
                GamesScreen(onGameClick = { game ->
                    navController.navigate(GamePlayRoute(game.id))
                })
            }

            composable<GamePlayRoute> { backStackEntry ->
                val route: GamePlayRoute = backStackEntry.toRoute()
                GamePlayScreen(
                    game = GameType.fromId(route.gameId),
                    onBackClick = { navController.popBackStack() },
                )
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
                        navController.navigate(CustomerCenterRoute)
                    }
                )
            }

            composable<CustomerCenterRoute> {
                val viewModel: CustomerCenterViewModel = hiltViewModel()
                CustomerCenterLauncher(
                    listener = viewModel.listener,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable<AuthRoute> { backStackEntry ->
                val route: AuthRoute = backStackEntry.toRoute()
                val viewModel: com.jesuskrastev.bali.ui.screens.auth.AuthViewModel = hiltViewModel()
                val isLoggingIn = viewModel.isLoggingIn.collectAsStateWithLifecycle().value
                val errorMsg = viewModel.errorMessage.collectAsStateWithLifecycle().value
                val errorEmail = viewModel.errorEmail.collectAsStateWithLifecycle().value
                
                com.jesuskrastev.bali.ui.screens.auth.AuthScreen(
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
                            popUpTo(TestRoute(null)) { inclusive = true }
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

/**
 * Renders the primary app navigation: Home, the AI tutor chat, the statistics, Games and
 * Settings. The chat and the statistics are tabs like the others, so the bar stays visible
 * while they are open.
 *
 * @param currentDestination back stack entry's destination, used to highlight the active tab
 * @param onHomeClick navigates to [HomeRoute]
 * @param onGamesClick navigates to [GamesRoute]
 * @param onSettingsClick navigates to [SettingsRoute]
 * @param onChatClick navigates to [ChatRoute]
 * @param onStatsClick navigates to [StatsRoute]
 */
@Composable
internal fun AppBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    onHomeClick: () -> Unit,
    onGamesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onChatClick: () -> Unit,
    onStatsClick: () -> Unit,
) {
    // The bar's background is drawn flush to the true bottom edge (behind the system nav bar,
    // matching edge-to-edge), but the tappable row is lifted above it by the real nav bar inset
    // so icons/labels are never obscured or partially unreachable on gesture or 3-button nav.
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp + navBarInset),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp + navBarInset)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {}
        // No fixed height here: the row must grow with navBarInset (via the bottom padding
        // below) or the fixed-height tabs get squeezed shorter than their content on every
        // device with a system nav bar, which is what caused the icons/labels to overlap.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp + navBarInset),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BaliBottomBarItem(
                label = "Inicio",
                icon = Icons.Rounded.Home,
                selected = currentDestination?.hasRoute<HomeRoute>() == true,
                onClick = onHomeClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Chat",
                icon = Icons.Rounded.ChatBubble,
                selected = currentDestination?.hasRoute<ChatRoute>() == true,
                onClick = onChatClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Progreso", // "Estadísticas" no cabe en una pestaña de cinco y se parte en dos líneas
                icon = Icons.Rounded.BarChart,
                selected = currentDestination?.hasRoute<StatsRoute>() == true,
                onClick = onStatsClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Juegos",
                icon = Icons.Rounded.SportsEsports,
                selected = currentDestination?.hasRoute<GamesRoute>() == true,
                onClick = onGamesClick,
                modifier = Modifier.weight(1f),
            )
            BaliBottomBarItem(
                label = "Ajustes",
                icon = Icons.Rounded.Settings,
                selected = currentDestination?.hasRoute<SettingsRoute>() == true,
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Draws one primary-navigation tab using the raised orange selection from the visual reference.
 *
 * @param label accessible text shown below the icon
 * @param icon Material icon associated with the destination
 * @param selected whether this is the route currently displayed
 * @param onClick action that navigates to the destination
 * @param modifier layout modifier applied to the tab's touch target
 */
@Composable
private fun BaliBottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) BaliPrimary else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "bottom_bar_indicator_color",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) White else BaliGrayMedium,
        animationSpec = tween(durationMillis = 180),
        label = "bottom_bar_icon_color",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) BaliPrimary else BaliGrayMedium,
        animationSpec = tween(durationMillis = 180),
        label = "bottom_bar_label_color",
    )
    val indicatorElevation by animateDpAsState(
        targetValue = if (selected) 7.dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_indicator_elevation",
    )
    val indicatorOffset by animateDpAsState(
        targetValue = if (selected) (-8).dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_indicator_offset",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 44.dp else 36.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_indicator_width",
    )
    val underlineWidth by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "bottom_bar_underline_width",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 0.94f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bottom_bar_icon_scale",
    )

    Column(
        modifier = modifier
            .height(70.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .offset(y = indicatorOffset)
                .shadow(elevation = indicatorElevation, shape = RoundedCornerShape(13.dp))
                .clip(RoundedCornerShape(13.dp))
                .background(indicatorColor)
                .width(indicatorWidth)
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(underlineWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(BaliPrimary),
        )
    }
}

/** Switches primary tabs while preserving their state and avoiding duplicate destinations. */
private inline fun <reified T : Any> NavHostController.navigateTopLevel(route: T) {
    navigate(route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
