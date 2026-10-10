package com.jesuskrastev.bali.domain.exam

import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.domain.path.LessonQuestionBank

/**
 * A lesson the student has studied, with the written questions it teaches.
 *
 * @property nodeId id of the lesson's path node
 * @property title the lesson's title, as the path shows it
 * @property questions the bank's questions for that lesson
 */
data class ExamLesson(
    val nodeId: String,
    val title: String,
    val questions: List<LessonQuestionBank.StaticQuestion>
)

/**
 * Decides which part of the syllabus an exam may ask about: only what the student has studied.
 *
 * An exam that asks about a lesson the student never opened is unanswerable, however well it is
 * written. So a section exam covers the completed lessons of its own section, and a final
 * simulacro (a section with no lessons of its own) covers every lesson completed so far.
 */
object ExamScope {

    /**
     * Lists the lessons an exam covers.
     *
     * @param exam the exam node being taken
     * @param pathNodes the student's whole learning path, in any order
     * @param questionsOf the questions a lesson holds; replaceable in tests
     * @return the completed lessons in scope that have written questions, in path order; empty
     *   when the student has completed none of them
     */
    fun lessonsFor(
        exam: LessonNode,
        pathNodes: List<LessonNode>,
        questionsOf: (String) -> List<LessonQuestionBank.StaticQuestion> = LessonQuestionBank::allQuestionsForNode
    ): List<ExamLesson> {
        val lessons = pathNodes.filter { it.nodeType == NodeType.LESSON }
        val isSectionExam = lessons.any { it.sectionIndex == exam.sectionIndex }

        return lessons
            .filter { it.status == NodeStatus.COMPLETED }
            .filter { !isSectionExam || it.sectionIndex == exam.sectionIndex }
            .sortedBy { it.orderIndex }
            .map { ExamLesson(nodeId = it.id, title = it.title, questions = questionsOf(it.id)) }
            .filter { it.questions.isNotEmpty() }
    }
}
