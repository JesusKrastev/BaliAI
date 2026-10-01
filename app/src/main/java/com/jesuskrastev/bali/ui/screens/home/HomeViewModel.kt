package com.jesuskrastev.bali.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.TestResult.Companion.OFFICIAL_EXAM_CATEGORY
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.PathRepository
import com.jesuskrastev.bali.domain.usecase.GenerateInitialPathUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateNextPathNodesUseCase
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.ui.util.StreakUiHelper
import com.jesuskrastev.bali.data.remote.RemoteConfigProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val dateTimeHelper: DateTimeHelper,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val pendingFirstStepRewards: PendingFirstStepRewards,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isPathLoading = MutableStateFlow(false)
    private val _pathError = MutableStateFlow<String?>(null)

    private val _dailyTip = MutableStateFlow("")

    /** Guards [AnalyticsTracker.firstStepsShown] so it fires once per Home, not per recomposition. */
    private var hasTrackedFirstStepsShown = false

    private val _pathNodes: StateFlow<List<LessonNode>?> = authRepository.currentUserFlow
        .flatMapLatest { userId -> 
             pathRepository.getPathNodes(userId ?: "") 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadDailyTip()
        observeAndAutoGeneratePath()
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

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.get(),
        testResultRepository.count(),
        answerRepository.getRecentMistakes(),
        testResultRepository.getAverageScore(),
        _dailyTip,
        _pathNodes,
        _isPathLoading,
        _pathError,
        authRepository.currentUserPhotoUrlFlow,
        authRepository.currentUserEmailFlow,
        testResultRepository.get().map { results -> results.any { it.category == OFFICIAL_EXAM_CATEGORY } },
        pendingFirstStepRewards.next
    ) { flows ->
        val user = flows[0] as User?
        val totalTests = flows[1] as Int
        val mistakes = flows[2] as List<*>
        val avgScore = flows[3] as Double? ?: 0.0
        val dailyTip = flows[4] as String
        val pathNodes = flows[5] as List<*>?
        val isPathLoading = flows[6] as Boolean
        val pathError = flows[7] as String?
        val profilePictureUrl = flows[8] as String?
        val userEmail = flows[9] as String?
        val hasTakenExam = flows[10] as Boolean
        val firstStepReward = flows[11] as FirstStepReward?

        @Suppress("UNCHECKED_CAST")
        val typedMistakes = mistakes as List<Answer>

        @Suppress("UNCHECKED_CAST")
        val typedPathNodes = (pathNodes ?: emptyList<LessonNode>()) as List<LessonNode>

        if (user == null) {
            HomeUiState(
                dailyTip = dailyTip,
                profilePictureUrl = profilePictureUrl,
                userEmail = userEmail
            )
        } else {
            val weeklyStreak = StreakUiHelper.generateWeeklyStreak(user.practiceDays)
            // Counted from practiceDays like the streak screens: user.weekSessions is only
            // refreshed on the next practice, so it can still hold last week's number.
            val weekSessions = weeklyStreak.count { it.status == StreakStatus.COMPLETED }
            val weeklyGoal = remoteConfigProvider.getWeeklyGoal()
            HomeUiState(
                userName = user.name ?: "Futuro Conductor",
                profilePictureUrl = profilePictureUrl,
                userEmail = userEmail,
                plan = planSummaryOf(user.examDateMillis, user.planTargetMillis, System.currentTimeMillis()),
                streak = user.currentStreak,
                weekSessions = weekSessions,
                weeklyGoal = weeklyGoal,
                weekProgressPercent = (weekSessions * 100 / weeklyGoal.coerceAtLeast(1)).coerceIn(0, 100),
                avgScore = avgScore.toInt(),
                totalTests = totalTests,
                practiceDays = user.practiceDays,
                xpLevel = user.level,
                mistakesCount = typedMistakes.size,
                coinsCount = user.coins,
                streakFreezes = user.streakFreezes,
                highestStreak = user.highestStreak,
                dailyTip = dailyTip,
                weeklyStreak = weeklyStreak,
                lastPracticeTimestamp = user.lastPracticeTimestamp,
                pathNodes = typedPathNodes,
                isPathLoading = isPathLoading,
                pathError = pathError,
                // The card stays up until everything is done *and* the closing simulacro was
                // taken, so an unfinished task keeps its coins available even after an exam.
                firstSteps = user.firstSteps.takeIf { it.isActive && !(it.isComplete && hasTakenExam) },
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

    /**
     * Saves the exam date picked on the plan card; from then on the card counts down to it.
     *
     * @param pickerMillis the date picker's selection, midnight UTC of the chosen day
     */
    fun setExamDate(pickerMillis: Long) {
        val examDay = localDayFromPickerMillis(pickerMillis)
        val hadPlanDate = uiState.value.plan.targetMillis != null
        viewModelScope.launch {
            userRepository.updateExamDate(examDay)
            analyticsTracker.examDateSet(
                daysUntil = calendarDaysBetween(System.currentTimeMillis(), examDay),
                hadPlanDate = hadPlanDate
            )
        }
    }

    /** Reports that the first-steps card is on screen; only the first call per Home is tracked. */
    fun onFirstStepsShown() {
        if (hasTrackedFirstStepsShown) return
        hasTrackedFirstStepsShown = true
        analyticsTracker.firstStepsShown(tasksDone = uiState.value.firstSteps?.doneCount ?: 0)
    }

    /** Reports that the student tapped the card's closing "haz tu primer simulacro" button. */
    fun onFirstStepsExamClicked() {
        analyticsTracker.firstStepsExamClicked()
    }

    /**
     * Hides the first-steps card for good. Tasks stop paying from here on, so the screen asks
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
