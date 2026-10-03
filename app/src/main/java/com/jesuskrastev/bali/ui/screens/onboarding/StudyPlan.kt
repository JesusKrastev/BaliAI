package com.jesuskrastev.bali.ui.screens.onboarding

import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.domain.path.DgtLearningPathTemplate
import com.jesuskrastev.bali.ui.screens.stats.calendarDaysBetween
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * One stretch of the study plan: a week, a few weeks grouped, or a few days when the exam is close.
 *
 * @property title what the stretch is called on screen, such as "Semana 2" or "Días 1–4"
 * @property startMillis local midnight of its first day
 * @property endMillis local midnight of its last day
 * @property sections the learning-path sections studied in it, in path order; empty in the final stretch
 * @property reinforce topics the user failed in the mini-test that fall in this stretch
 * @property isFinalStretch true for the last stretch, kept for mock exams and review
 */
data class PlanBlock(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val sections: List<String>,
    val reinforce: List<String>,
    val isFinalStretch: Boolean
)

/**
 * The plan the onboarding reveals: the learning path laid out over the days left, with the topics
 * the mini-test caught flagged where they come up.
 *
 * It follows the order of the path in the app (sections unlock one after another), so the plan
 * never promises a route the app does not take.
 *
 * @property blocks the stretches, in order; the last one is the final stretch
 * @property days calendar days from today to the target day
 */
data class StudyPlan(val blocks: List<PlanBlock>, val days: Int)

/** Builds the [StudyPlan] from the user's answers. Pure, so it can be tested without a device. */
object StudyPlanBuilder {

    /** Questions a day the plan asks for: the pact and the daily streak are built on it. */
    const val DAILY_QUESTIONS = 10

    /** Mock exams a week the plan asks for. */
    const val WEEKLY_MOCK_EXAMS = 2

    /** Fewer days than this and the plan is told in days instead of weeks. */
    private const val WEEKS_FROM_DAYS = 14

    /** Stretches shown when the plan is told in days. */
    private const val DAY_BLOCKS = 3

    /** Most stretches shown: a far exam groups several weeks into one card instead of a long list. */
    private const val MAX_BLOCKS = 6

    /** The shortest plan: an exam tomorrow still gets a few days to work with. */
    private const val MIN_DAYS = 3

    private const val DAYS_PER_WEEK = 7

    /**
     * Section titles of the learning path, in order, without the closing block of mock exams.
     * Read from the path itself so the plan always names the sections the user will see.
     */
    val pathSections: List<Pair<Int, String>> by lazy {
        DgtLearningPathTemplate.buildInitialPath()
            .filter { it.nodeType == NodeType.LESSON }
            .distinctBy { it.sectionIndex }
            .sortedBy { it.sectionIndex }
            .map { it.sectionIndex to it.sectionTitle }
    }

    /**
     * Lays the learning path out between today and [targetMillis].
     *
     * @param now current time in millis, the first day of the plan
     * @param targetMillis the exam date, or the date the plan aims for when there is none
     * @param failed questions the user got wrong in the mini-test, to flag their topics
     * @param sections the path sections to spread, as section index and title
     * @return the plan
     */
    fun build(
        now: Long,
        targetMillis: Long,
        failed: List<QuizQuestion> = emptyList(),
        sections: List<Pair<Int, String>> = pathSections
    ): StudyPlan {
        val days = max(MIN_DAYS, calendarDaysBetween(now, targetMillis))
        val ranges = if (days < WEEKS_FROM_DAYS) dayRanges(days) else weekRanges(days)
        val studyBlocks = ranges.size - 1

        val blocks = ranges.mapIndexed { index, range ->
            val isFinal = index == ranges.lastIndex
            val blockSections = if (isFinal) emptyList() else sectionsFor(index, studyBlocks, sections)
            val sectionIndexes = blockSections.map { it.first }.toSet()
            PlanBlock(
                title = range.title,
                startMillis = dayAfter(now, range.firstDay),
                endMillis = dayAfter(now, range.lastDay),
                sections = blockSections.map { it.second },
                reinforce = if (isFinal) {
                    failed.map { it.topic }.distinct()
                } else {
                    failed.filter { it.sectionIndex in sectionIndexes }.map { it.topic }.distinct()
                },
                isFinalStretch = isFinal
            )
        }
        return StudyPlan(blocks = blocks, days = days)
    }

    /**
     * A run of days of the plan, counted from 0 (today).
     *
     * @property firstDay first day of the run
     * @property lastDay last day of the run, inclusive
     * @property title what the run is called on screen
     */
    private data class DayRange(val firstDay: Int, val lastDay: Int, val title: String)

    /**
     * Splits a short plan into [DAY_BLOCKS] runs of days, the longer ones first.
     *
     * @param days length of the plan
     * @return the runs, titled "Día n" or "Días a–b"
     */
    private fun dayRanges(days: Int): List<DayRange> {
        val base = days / DAY_BLOCKS
        val extra = days % DAY_BLOCKS
        var first = 0
        return List(DAY_BLOCKS) { index ->
            val length = base + if (index < extra) 1 else 0
            val last = first + length - 1
            DayRange(first, last, rangeTitle("Día", "Días", first + 1, last + 1)).also { first = last + 1 }
        }
    }

    /**
     * Splits a long plan into weeks, grouping several weeks per run when there are more than
     * [MAX_BLOCKS]. The last run may be shorter than a week: it ends on the target day.
     *
     * @param days length of the plan
     * @return the runs, titled "Semana n" or "Semanas a–b"
     */
    private fun weekRanges(days: Int): List<DayRange> {
        val weeks = ceil(days / DAYS_PER_WEEK.toDouble()).toInt()
        val count = min(weeks, MAX_BLOCKS)
        val base = weeks / count
        val extra = weeks % count
        var firstWeek = 0
        return List(count) { index ->
            val length = base + if (index < extra) 1 else 0
            val lastWeek = firstWeek + length - 1
            DayRange(
                firstDay = firstWeek * DAYS_PER_WEEK,
                lastDay = min((lastWeek + 1) * DAYS_PER_WEEK, days) - 1,
                title = rangeTitle("Semana", "Semanas", firstWeek + 1, lastWeek + 1)
            ).also { firstWeek = lastWeek + 1 }
        }
    }

    /**
     * Names a run: "Semana 3" for one unit, "Semanas 3–4" for several.
     *
     * @param singular the unit's name for one
     * @param plural the unit's name for several
     * @param from first unit, counted from 1
     * @param to last unit, counted from 1
     * @return the title
     */
    private fun rangeTitle(singular: String, plural: String, from: Int, to: Int): String =
        if (from == to) "$singular $from" else "$plural $from–$to"

    /**
     * The path sections that fall in one study stretch, spreading them evenly in path order.
     *
     * @param block position of the stretch among the study stretches
     * @param studyBlocks how many study stretches there are
     * @param sections every section, in path order
     * @return this stretch's share
     */
    private fun sectionsFor(
        block: Int,
        studyBlocks: Int,
        sections: List<Pair<Int, String>>
    ): List<Pair<Int, String>> {
        val from = block * sections.size / studyBlocks
        val to = (block + 1) * sections.size / studyBlocks
        return sections.subList(from, to)
    }

    /**
     * Local midnight [offset] calendar days after [now]'s day, safe across daylight-saving changes.
     *
     * @param now the reference instant
     * @param offset days to add
     * @return the day, at local midnight
     */
    private fun dayAfter(now: Long, offset: Int): Long = Calendar.getInstance()
        .apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, offset)
        }
        .timeInMillis
}
