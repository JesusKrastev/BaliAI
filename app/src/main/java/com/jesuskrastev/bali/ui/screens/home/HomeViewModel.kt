package com.jesuskrastev.bali.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.PathRepository
import com.jesuskrastev.bali.domain.usecase.GenerateInitialPathUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateNextPathNodesUseCase
import com.jesuskrastev.bali.domain.usecase.SettleStreakUseCase
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.data.remote.RemoteConfigProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val testResultRepository: TestResultRepository,
    private val answerRepository: AnswerRepository,
    private val authRepository: AuthRepository,
    private val pathRepository: PathRepository,
    private val generateNextPathNodesUseCase: GenerateNextPathNodesUseCase,
    private val generateInitialPathUseCase: GenerateInitialPathUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val settleStreak: SettleStreakUseCase,
    private val pendingFirstStepRewards: PendingFirstStepRewards,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isPathLoading = MutableStateFlow(false)
    private val _pathError = MutableStateFlow<String?>(null)

    private val _dailyTip = MutableStateFlow("")

    /** Guards [AnalyticsTracker.firstStepsShown] so it fires once per Home, not per recomposition. */
    private var hasTrackedFirstStepsShown = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _pathNodes: StateFlow<List<LessonNode>?> = authRepository.currentUserFlow
        .flatMapLatest { userId -> 
             pathRepository.getPathNodes(userId ?: "") 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadDailyTip()
        observeAndAutoGeneratePath()
        // Days missed since the last visit spend freezes or end the streak before it is shown.
        viewModelScope.launch { settleStreak() }
        viewModelScope.launch {
            remoteConfigProvider.fetchAndActivate()
        }
    }

    private fun observeAndAutoGeneratePath() {
        viewModelScope.launch {
            _pathNodes
                .filterNotNull()
                .distinctUntilChanged { old, new -> old.isNotEmpty() == new.isNotEmpty() }
                .collect { nodes ->
                    if (nodes.isEmpty() && !_isPathLoading.value) {
                        generateInitialPath()
                    }
                }
        }
    }

    private fun generateInitialPath() {
        viewModelScope.launch {
            _isPathLoading.value = true
            _pathError.value = null
            try {
                generateInitialPathUseCase()
            } catch (e: Exception) {
                _pathError.value = e.localizedMessage
            } finally {
                _isPathLoading.value = false
            }
        }
    }

    private fun loadDailyTip() {
        try {
            val jsonString = context.resources.openRawResource(R.raw.tips).bufferedReader().use { it.readText() }
            val tips = Json.decodeFromString<List<String>>(jsonString)
            if (tips.isNotEmpty()) {
                _dailyTip.value = tips.random()
            }
        } catch (e: Exception) {
            _dailyTip.value = "Conduce con precaución y respeta las señales."
        }
    }

    /** What Home shows about the student's results, gathered so the flows fit typed `combine`s. */
    private data class ResultsSummary(
        val totalTests: Int,
        val mistakesCount: Int,
        val avgScore: Int,
        val hasTakenExam: Boolean
    )

    /** The learning path as Home needs it: its nodes and whether more are being generated. */
    private data class PathSummary(
        val nodes: List<LessonNode>,
        val isLoading: Boolean,
        val error: String?
    )

    /** Signed-in account details and the tip of the day, which are not part of the user document. */
    private data class AccountDetails(
        val profilePictureUrl: String?,
        val userEmail: String?,
        val dailyTip: String
    )

    private val resultsSummary = combine(
        testResultRepository.count(),
        answerRepository.getRecentMistakes(),
        testResultRepository.getAverageScore(),
        testResultRepository.get().map { results -> results.any { it.category == ExamRules.OFFICIAL_EXAM_CATEGORY } }
    ) { totalTests, mistakes, avgScore, hasTakenExam ->
        ResultsSummary(
            totalTests = totalTests,
            mistakesCount = mistakes.size,
            avgScore = (avgScore ?: 0.0).toInt(),
            hasTakenExam = hasTakenExam
        )
    }

    private val pathSummary = combine(_pathNodes, _isPathLoading, _pathError) { nodes, isLoading, error ->
        PathSummary(nodes = nodes.orEmpty(), isLoading = isLoading, error = error)
    }

    private val accountDetails = combine(
        authRepository.currentUserPhotoUrlFlow,
        authRepository.currentUserEmailFlow,
        _dailyTip
    ) { profilePictureUrl, userEmail, dailyTip ->
        AccountDetails(profilePictureUrl = profilePictureUrl, userEmail = userEmail, dailyTip = dailyTip)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.get(),
        resultsSummary,
        pathSummary,
        accountDetails,
        pendingFirstStepRewards.next
    ) { user, results, path, account, firstStepReward ->
        if (user == null) {
            HomeUiState(
                dailyTip = account.dailyTip,
                profilePictureUrl = account.profilePictureUrl,
                userEmail = account.userEmail
            )
        } else {
            val now = System.currentTimeMillis()
            // Settled here too, so a lost streak never flashes as alive while the save lands.
            val streak = DailyStreak.of(user).settledAt(now)
            HomeUiState(
                userName = user.name ?: "Futuro Conductor",
                profilePictureUrl = account.profilePictureUrl,
                userEmail = account.userEmail,
                streak = streak.current,
                practicedToday = streak.hasPracticedOn(now),
                avgScore = results.avgScore,
                totalTests = results.totalTests,
                practiceDays = user.practiceDays,
                xpLevel = user.level,
                xp = user.xp,
                claimableRankRewards = RankProgression.rewards.count { reward ->
                    reward.requiredXp <= user.xp && reward.id !in user.claimedRankRewards
                },
                mistakesCount = results.mistakesCount,
                coinsCount = user.coins,
                streakFreezes = streak.freezes,
                highestStreak = streak.highest,
                dailyTip = account.dailyTip,
                lastPracticeTimestamp = user.lastPracticeTimestamp,
                pathNodes = path.nodes,
                isPathLoading = path.isLoading,
                pathError = path.error,
                // The bar stays up until everything is done *and* the closing simulacro was
                // taken, so an unfinished task keeps its coins available even after an exam.
                firstSteps = user.firstSteps.takeIf { it.isActive && !(it.isComplete && results.hasTakenExam) },
                firstStepTestNode = firstStepTestNodeOf(path.nodes),
                firstStepReward = firstStepReward
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
            analyticsTracker.logout()
            analyticsTracker.resetUser()
        }
    }

    /** Reports that the first-steps bar is on screen; only the first call per Home is tracked. */
    fun onFirstStepsShown() {
        if (hasTrackedFirstStepsShown) return
        hasTrackedFirstStepsShown = true
        analyticsTracker.firstStepsShown(tasksDone = uiState.value.firstSteps?.doneCount ?: 0)
    }

    /** Reports that the student tapped the bar's closing "haz tu primer simulacro" button. */
    fun onFirstStepsExamClicked() {
        analyticsTracker.firstStepsExamClicked()
    }

    /**
     * Hides the first-steps bar for good. Tasks stop paying from here on, so the screen asks
     * for confirmation before calling this while coins are still pending.
     */
    fun dismissFirstSteps() {
        val tasksDone = uiState.value.firstSteps?.doneCount ?: 0
        viewModelScope.launch {
            userRepository.dismissFirstSteps()
            analyticsTracker.firstStepsDismissed(tasksDone)
        }
    }

    /** Marks the reward on screen as celebrated so the next queued one (if any) can show. */
    fun dismissFirstStepReward() {
        pendingFirstStepRewards.consume()
    }

    fun generateNextPathNodesCount(count: Int = 5) {
        if (_isPathLoading.value) return
        viewModelScope.launch {
            _isPathLoading.value = true
            _pathError.value = null
            try {
                generateNextPathNodesUseCase(count)
            } catch (e: Exception) {
                _pathError.value = e.localizedMessage
            } finally {
                _isPathLoading.value = false
            }
        }
    }
}
