package com.jesuskrastev.bali.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.domain.model.DrivingTopic
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.ProgressStats
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.ReadinessResult
import com.jesuskrastev.bali.domain.model.TopicMastery
import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliBackgroundGradient

/**
 * "Mis estadísticas": answers "will I pass?" and shows how the user's study is going — the time
 * left to the exam, the exam verdict, the latest mock exams, accuracy by topic, the week's activity
 * and how consistent they have been. It is also where the exam date is set and changed.
 *
 * @param onBackClick closes the screen
 * @param viewModel owner of the figures
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBackClick: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExamDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showExamDatePicker) {
        ExamDatePickerDialog(
            initialDateMillis = uiState.plan.targetMillis,
            onConfirm = { pickerMillis ->
                viewModel.setExamDate(pickerMillis)
                showExamDatePicker = false
            },
            onDismiss = { showExamDatePicker = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis estadísticas", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        val stats = uiState.stats
        if (stats == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            StatsContent(
                stats = stats,
                plan = uiState.plan,
                onDateClick = { showExamDatePicker = true },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

/**
 * Lays the statistics blocks out in reading order: the time left to the exam, then the verdict,
 * then the evidence for it, then the habits behind it.
 *
 * @param stats the figures to render
 * @param plan the date the countdown counts down to
 * @param onDateClick opens the exam date picker
 * @param modifier layout modifier
 */
@Composable
internal fun StatsContent(
    stats: ProgressStats,
    plan: PlanSummary,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BaliBackgroundGradient())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExamCountdownCard(
            plan = plan,
            readiness = stats.readiness,
            studiedToday = stats.studyDays.lastOrNull() == true,
            onDateClick = onDateClick
        )
        ReadinessHero(stats.readiness)
        RecentMocksCard(stats.readiness)
        if (stats.readiness.history.isNotEmpty()) MockHistoryCard(stats.readiness)
        PracticeTotals(stats)
        TopicsCard(stats.topics)
        WeekCard(stats)
        ConsistencyCard(stats)
        Text(
            text = "Es una estimación a partir de tus simulacros y no garantiza el resultado del examen real.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Colors the verdict.
 *
 * @param level the semaphore
 * @return green, amber, red, or a neutral slate before there is a verdict
 */
@Composable
private fun levelColor(level: ReadinessLevel): Color = when (level) {
    ReadinessLevel.READY -> BaliAccentGreen
    ReadinessLevel.ALMOST -> StatsAmber
    ReadinessLevel.NOT_YET -> BaliAccentRed
    ReadinessLevel.NOT_ENOUGH_DATA -> MaterialTheme.colorScheme.primary
}

/**
 * The headline block: a ring with the chance of passing (or the progress toward having enough
 * data), what it means and the one thing to do next.
 *
 * @param readiness the verdict
 */
@Composable
private fun ReadinessHero(readiness: ReadinessResult) {
    val color = levelColor(readiness.level)
    val copy = readinessCopyOf(readiness)
    val probability = readiness.passProbability

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.20f), Color.Transparent)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatsPill(copy.status, color)
            if (probability != null) {
                ProgressRing(
                    progress = probability,
                    color = color,
                    centerText = percentText(probability),
                    centerCaption = "de aprobar",
                    description = "Probabilidad estimada de aprobar: ${percentText(probability)}"
                )
            } else {
                ProgressRing(
                    progress = readiness.mocksTaken.toFloat() / CalculateReadinessUseCase.MIN_MOCKS,
                    color = color,
                    centerText = "${readiness.mocksTaken}/${CalculateReadinessUseCase.MIN_MOCKS}",
                    centerCaption = "simulacros",
                    description = "${readiness.mocksTaken} de ${CalculateReadinessUseCase.MIN_MOCKS} simulacros hechos"
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = copy.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            NextStep(copy.nextStep, color)
        }
    }
}

/**
 * Tinted line telling the user the next thing to do.
 *
 * @param text the advice
 * @param color accent matching the verdict
 */
@Composable
private fun NextStep(text: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * The latest five mock exams as dots, with the goal to reach before the real exam.
 *
 * @param readiness the verdict holding the latest mock exams
 */
@Composable
private fun RecentMocksCard(readiness: ReadinessResult) {
    StatsCard(title = "Tus últimos simulacros", subtitle = recentSummaryOf(readiness)) {
        RecentMockDots(readiness.recent, CalculateReadinessUseCase.RECENT_WINDOW)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val goalReached = readiness.passedInRecent >= CalculateReadinessUseCase.RECENT_GOAL
            StatsBar(
                fraction = readiness.passedInRecent.toFloat() / CalculateReadinessUseCase.RECENT_GOAL,
                color = if (goalReached) BaliAccentGreen else MaterialTheme.colorScheme.primary
            )
            Text(
                text = recentGoalText(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Score of every mock exam over time, with the average, the best and the trend.
 *
 * @param readiness the verdict holding the exam history
 */
@Composable
private fun MockHistoryCard(readiness: ReadinessResult) {
    StatsCard(
        title = "Evolución de los simulacros",
        subtitle = "Aciertos sobre ${ExamRules.QUESTION_COUNT}, los últimos $HISTORY_BARS"
    ) {
        MockHistoryChart(readiness.history.takeLast(HISTORY_BARS))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MiniStat(
                value = readiness.averageScore?.let(::formatDecimal) ?: "—",
                caption = "Media",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                value = readiness.bestScore?.toString() ?: "—",
                caption = "Mejor",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                value = readiness.trend?.let(::trendText) ?: "—",
                caption = "Tendencia",
                modifier = Modifier.weight(1f),
                valueColor = when {
                    readiness.trend == null -> MaterialTheme.colorScheme.onSurface
                    readiness.trend > 0f -> BaliAccentGreen
                    readiness.trend < 0f -> BaliAccentRed
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/**
 * Four totals: questions answered, accuracy, mock exams and practice sessions.
 *
 * @param stats the figures
 */
@Composable
private fun PracticeTotals(stats: ProgressStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Rounded.QuestionAnswer,
                value = stats.totalQuestions.toString(),
                label = "Preguntas respondidas",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Rounded.CheckCircle,
                value = stats.accuracy?.let(::percentText) ?: "—",
                label = "De acierto",
                tint = stats.accuracy?.let { accuracyColor(it) } ?: MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Rounded.Timer,
                value = stats.readiness.mocksTaken.toString(),
                label = "Simulacros hechos",
                tint = StatsAmber,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Rounded.School,
                value = stats.practiceSessions.toString(),
                label = "Sesiones de práctica",
                tint = BaliAccentGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Accuracy per topic, weakest first, plus the topics not practiced yet. Topics with too few
 * questions are shown muted instead of rated.
 *
 * @param topics topics with practice data, weakest first
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicsCard(topics: List<TopicMastery>) {
    val weakest = topics.firstOrNull { it.isReliable && it.accuracy < TopicMastery.STRONG_ACCURACY }
    val untouched = DrivingTopic.entries.filter { topic -> topics.none { it.topic == topic } }

    StatsCard(
        title = "Dominio por tema",
        subtitle = if (topics.isEmpty()) {
            "Haz sesiones de práctica y verás aquí en qué temas fallas más."
        } else {
            "Aciertos en tus sesiones de práctica"
        }
    ) {
        if (weakest != null) {
            StatsPill("Para reforzar: ${weakest.topic.displayName}", accuracyColor(weakest.accuracy))
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            topics.forEach { TopicRow(it) }
        }
        if (untouched.isNotEmpty() && topics.isNotEmpty()) {
            Text(
                text = "Sin practicar todavía",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (untouched.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                untouched.forEach { StatsPill(it.displayName, MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

/**
 * One topic: name, accuracy and a bar. Without enough questions the bar is muted and the share
 * replaced by a note, so one lucky session never reads as mastery.
 *
 * @param mastery the topic's figures
 */
@Composable
private fun TopicRow(mastery: TopicMastery) {
    val color = if (mastery.isReliable) accuracyColor(mastery.accuracy) else MaterialTheme.colorScheme.outline
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mastery.topic.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (mastery.isReliable) percentText(mastery.accuracy) else "Pocas preguntas",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        StatsBar(fraction = mastery.accuracy, color = color, height = 8.dp)
        Text(
            text = "${plural(mastery.total, "pregunta")} · ${plural(mastery.sessions, "sesión", "sesiones")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Questions answered each day of the last week.
 *
 * @param stats the figures
 */
@Composable
private fun WeekCard(stats: ProgressStats) {
    val weekQuestions = stats.week.sumOf { it.questions }
    val activeDays = stats.week.count { it.questions > 0 }
    StatsCard(
        title = "Esta semana",
        subtitle = if (weekQuestions == 0) {
            "Aún no has respondido preguntas en los últimos 7 días."
        } else {
            "${plural(weekQuestions, "pregunta")} en ${plural(activeDays, "día")}."
        }
    ) {
        WeekChart(stats.week)
    }
}

/**
 * The last 28 days as a calendar, with streak, level and experience.
 *
 * @param stats the figures
 */
@Composable
private fun ConsistencyCard(stats: ProgressStats) {
    StatsCard(
        title = "Constancia",
        subtitle = "Has estudiado ${stats.studyDaysCount} de los últimos ${CalculateProgressStatsUseCase.CALENDAR_DAYS} días"
    ) {
        StudyCalendar(stats.studyDays)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MiniStat(
                value = stats.currentStreak.toString(),
                caption = "Racha actual",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                value = stats.highestStreak.toString(),
                caption = "Mejor racha",
                modifier = Modifier.weight(1f)
            )
            MiniStat(
                value = stats.level.toString(),
                caption = "Nivel",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${stats.xp} XP acumulados",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Mock exams the evolution chart draws. */
private const val HISTORY_BARS = 10
