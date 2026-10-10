package com.jesuskrastev.bali.domain.exam

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.domain.path.LessonQuestionBank.StaticQuestion
import org.junit.Test

class ExamScopeTest {

    private val question = StaticQuestion("¿Pregunta?", listOf("A", "B", "C"), 0, "Porque sí")

    private fun node(
        id: String,
        section: Int,
        order: Int,
        status: NodeStatus,
        type: NodeType = NodeType.LESSON
    ) = LessonNode(
        id = id, orderIndex = order, title = "Título $id", description = "",
        status = status, sectionIndex = section, nodeType = type
    )

    private val path = listOf(
        node("a1", section = 0, order = 0, status = NodeStatus.COMPLETED),
        node("a2", section = 0, order = 1, status = NodeStatus.COMPLETED),
        node("a3", section = 0, order = 2, status = NodeStatus.UNLOCKED),
        node("a-exam", section = 0, order = 3, status = NodeStatus.LOCKED, type = NodeType.EXAM),
        node("b1", section = 1, order = 4, status = NodeStatus.COMPLETED),
        node("b-exam", section = 1, order = 5, status = NodeStatus.UNLOCKED, type = NodeType.EXAM),
        node("sim", section = 9, order = 6, status = NodeStatus.UNLOCKED, type = NodeType.EXAM)
    )

    private val everyLessonHasQuestions: (String) -> List<StaticQuestion> = { listOf(question) }

    @Test
    fun `a section exam covers only the completed lessons of its own section`() {
        val lessons = ExamScope.lessonsFor(path.first { it.id == "a-exam" }, path, everyLessonHasQuestions)

        assertThat(lessons.map { it.nodeId }).containsExactly("a1", "a2").inOrder()
    }

    @Test
    fun `a lesson the student has not completed is never asked about`() {
        val lessons = ExamScope.lessonsFor(path.first { it.id == "a-exam" }, path, everyLessonHasQuestions)

        assertThat(lessons.map { it.nodeId }).doesNotContain("a3")
    }

    @Test
    fun `a final simulacro covers every lesson completed so far`() {
        val lessons = ExamScope.lessonsFor(path.first { it.id == "sim" }, path, everyLessonHasQuestions)

        assertThat(lessons.map { it.nodeId }).containsExactly("a1", "a2", "b1").inOrder()
    }

    @Test
    fun `lessons without written questions are left out`() {
        val lessons = ExamScope.lessonsFor(path.first { it.id == "a-exam" }, path) { id ->
            if (id == "a1") listOf(question) else emptyList()
        }

        assertThat(lessons.map { it.nodeId }).containsExactly("a1")
    }

    @Test
    fun `an exam with nothing completed in scope covers nothing`() {
        val nothingDone = path.map { if (it.nodeType == NodeType.LESSON) it.copy(status = NodeStatus.LOCKED) else it }

        assertThat(ExamScope.lessonsFor(nothingDone.first { it.id == "b-exam" }, nothingDone, everyLessonHasQuestions)).isEmpty()
    }

    @Test
    fun `lessons come back in path order whatever order the path is given in`() {
        val lessons = ExamScope.lessonsFor(path.first { it.id == "sim" }, path.reversed(), everyLessonHasQuestions)

        assertThat(lessons.map { it.nodeId }).containsExactly("a1", "a2", "b1").inOrder()
    }
}
