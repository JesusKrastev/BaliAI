package com.jesuskrastev.bali.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jesuskrastev.bali.ui.screens.coins.CoinsGainedScreen
import com.jesuskrastev.bali.ui.screens.exam.ExamScreen
import com.jesuskrastev.bali.ui.screens.exam.ExamViewModel
import com.jesuskrastev.bali.ui.screens.greetings.GreetingsScreen
import com.jesuskrastev.bali.ui.screens.home.HomeScreen
import com.jesuskrastev.bali.ui.screens.home.HomeViewModel
import com.jesuskrastev.bali.ui.screens.mistakes.MistakesScreen
import com.jesuskrastev.bali.ui.screens.mistakes.MistakesViewModel
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingScreen
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingViewModel
import com.jesuskrastev.bali.ui.screens.store.ShopScreen
import com.jesuskrastev.bali.ui.screens.store.ShopViewModel
import com.jesuskrastev.bali.ui.screens.test.TestResultScreen
import com.jesuskrastev.bali.ui.screens.test.TestScreen
import com.jesuskrastev.bali.ui.screens.test.TestViewModel
import com.jesuskrastev.bali.ui.screens.topics.TopicsScreen
import com.jesuskrastev.bali.ui.screens.suggestions.SuggestionsScreen
import com.jesuskrastev.bali.ui.screens.suggestions.SuggestionsViewModel
import kotlinx.serialization.Serializable

@Serializable
object GreetingsRoute

@Serializable
object OnboardingRoute

@Serializable
object HomeRoute

@Serializable
data class TestRoute(
    val topic: String? = null,
    val nodeTitle: String? = null,
    val nodeDescription: String? = null,
    val nodeId: String? = null
)

@Serializable
object ExamRoute

@Serializable
object TopicsRoute

@Serializable
object MistakesRoute

@Serializable
object ShopRoute

@Serializable
object SuggestionsRoute

@Serializable
data class AuthRoute(val restrictNewAccounts: Boolean = false)

@Serializable
data class CoinsGainedRoute(val coins: Int)

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
    val coinsGained: Int = 0
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = GreetingsRoute
) {
    SharedTransitionLayout {
        NavHost(
            modifier = modifier,
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

                HomeScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    viewModel = viewModel,
                    onStudyClick = {
                        navController.navigate(TestRoute())
                    },
                    onNodeTestClick = { title, desc, id ->
                        navController.navigate(TestRoute(nodeTitle = title, nodeDescription = desc, nodeId = id))
                    },
                    onTopicsClick = {
                        navController.navigate(TopicsRoute)
                    },
                    onMistakesClick = {
                        navController.navigate(MistakesRoute)
                    },
                    onExamClick = {
                        viewModel.startExam {
                            navController.navigate(ExamRoute)
                        }
                    },
                    onShopClick = {
                        navController.navigate(ShopRoute)
                    },
                    onFeedbackClick = {
                        navController.navigate(SuggestionsRoute)
                    },
                    onAuthClick = {
                        navController.navigate(AuthRoute(restrictNewAccounts = false))
                    }
                )
            }

            composable<AuthRoute> { backStackEntry ->
                val route: AuthRoute = backStackEntry.toRoute()
                val viewModel: com.jesuskrastev.bali.ui.screens.auth.AuthViewModel = hiltViewModel()
                val isLoggingIn = viewModel.isLoggingIn.collectAsState().value
                val errorMsg = viewModel.errorMessage.collectAsState().value
                val errorEmail = viewModel.errorEmail.collectAsState().value
                
                com.jesuskrastev.bali.ui.screens.auth.AuthScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onBackClick = { navController.popBackStack() },
                    onLoginClick = { context -> 
                        viewModel.signInWithGoogle(context, route.restrictNewAccounts) {
                            // On success, go directly to Home
                            navController.navigate(HomeRoute) {
                                popUpTo(AuthRoute(route.restrictNewAccounts)) { inclusive = true }
                            }
                        }
                    },
                    isLoggingIn = isLoggingIn,
                    errorMessage = errorMsg,
                    errorEmail = errorEmail,
                    onErrorDismiss = { viewModel.clearError() }
                )
            }

            composable<TopicsRoute> {
                TopicsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onTopicClick = { topicName ->
                        navController.navigate(TestRoute(topic = topicName))
                    }
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

            composable<MistakesRoute> {
                val viewModel: MistakesViewModel = hiltViewModel()
                MistakesScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onFinishTest = { score, total, xp, duration, accuracy, baseXp, bPerfection, bFast, bStreak, leveledUp, coins ->
                        navController.navigate(
                            TestResultRoute(
                                score = score,
                                total = total,
                                xpGained = xp,
                                durationSeconds = duration,
                                accuracy = accuracy,
                                baseXp = baseXp,
                                bonusPerfection = bPerfection,
                                bonusFast = bFast,
                                bonusStreak = bStreak,
                                leveledUp = leveledUp,
                                coinsGained = coins
                            )
                        ) {
                            popUpTo(MistakesRoute) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable<TestRoute> { backStackEntry ->
                val route: TestRoute = backStackEntry.toRoute()
                val viewModel: TestViewModel = hiltViewModel()
                
                LaunchedEffect(route) {
                    if (route.nodeTitle != null) {
                        viewModel.setAiNodeParams(route.nodeTitle, route.nodeDescription, route.nodeId)
                    } else {
                        viewModel.setTopic(route.topic)
                    }
                }

                TestScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onFinishTest = { score, total, xp, duration, accuracy, baseXp, bPerfection, bFast, bStreak, leveledUp, coins ->
                        navController.navigate(
                            TestResultRoute(
                                score = score,
                                total = total,
                                xpGained = xp,
                                durationSeconds = duration,
                                accuracy = accuracy,
                                baseXp = baseXp,
                                bonusPerfection = bPerfection,
                                bonusFast = bFast,
                                bonusStreak = bStreak,
                                leveledUp = leveledUp,
                                coinsGained = coins
                            )
                        ) {
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
                    onFinishExam = { score, total, xp, duration, accuracy, baseXp, bPerfection, bFast, bStreak, leveledUp, coins ->
                        navController.navigate(
                            TestResultRoute(
                                score = score,
                                total = total,
                                xpGained = xp,
                                durationSeconds = duration,
                                accuracy = accuracy,
                                baseXp = baseXp,
                                bonusPerfection = bPerfection,
                                bonusFast = bFast,
                                bonusStreak = bStreak,
                                leveledUp = leveledUp,
                                coinsGained = coins
                            )
                        ) {
                            popUpTo(ExamRoute) { inclusive = true }
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable<TestResultRoute> { backStackEntry ->
                val route: TestResultRoute = backStackEntry.toRoute()
                TestResultScreen(
                    xpGained = route.xpGained,
                    baseXp = route.baseXp,
                    bonusPerfection = route.bonusPerfection,
                    bonusFast = route.bonusFast,
                    bonusStreak = route.bonusStreak,
                    leveledUp = route.leveledUp,
                    durationSeconds = route.durationSeconds,
                    accuracy = route.accuracy,
                    onContinueClick = {
                        navController.navigate(CoinsGainedRoute(route.coinsGained)) {
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
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<OnboardingRoute> {
                val viewModel: OnboardingViewModel = hiltViewModel()

                OnboardingScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    viewModel = viewModel,
                    onComplete = {
                        navController.navigate(HomeRoute) {
                            popUpTo(OnboardingRoute) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable<SuggestionsRoute> {
                val viewModel: SuggestionsViewModel = hiltViewModel()
                SuggestionsScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }
    }
}
