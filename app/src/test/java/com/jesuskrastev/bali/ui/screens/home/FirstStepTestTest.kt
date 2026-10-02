package com.jesuskrastev.bali.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.domain.path.DgtLearningPathTemplate
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import org.junit.Test

class FirstStepTestTest {

    /**
     * Builds a path node for these tests.
     *
     * @param id node id; the real bank only knows the ids of the DGT template.
     * @param order position in the path.
     * @param type kind of node.
     * @param status availability of the node.
     * @return the node.
     */
    private fun node(id: String, order: Int, type: NodeType, status: NodeStatus) =
        LessonNode(id = id, orderIndex = order, title = id, description = "", status = status, nodeType = type)

    @Test
    fun `a new account gets the first lesson of the path, which has written questions`() {
        val path = DgtLearningPathTemplate.buildInitialPath()

        val chosen = firstStepTestNodeOf(path)

        assertThat(chosen?.id).isEqualTo("node_0_0_lesson")
        assertThat(chosen?.nodeType).isEqualTo(NodeType.LESSON)
        assertThat(LessonQuestionBank.hasQuestionsForNode(chosen!!.id)).isTrue()
    }

    @Test
    fun `a review waiting next is skipped for the last lesson already done`() {
        val path = listOf(
            node("node_0_0_lesson", 0, NodeType.LESSON, NodeStatus.COMPLETED),
            node("node_0_1_lesson", 1, NodeType.LESSON, NodeStatus.COMPLETED),
            node("node_0_7_review", 2, NodeType.REVIEW, NodeStatus.UNLOCKED),
            node("node_0_8_exam", 3, NodeType.EXAM, NodeStatus.LOCKED)
        )

        assertThat(firstStepTestNodeOf(path)?.id).isEqualTo("node_0_1_lesson")
    }

    @Test
    fun `an exam waiting next is skipped, so the step never opens a mock exam`() {
        val path = listOf(
            node("node_0_6_lesson", 0, NodeType.LESSON, NodeStatus.COMPLETED),
            node("node_0_8_exam", 1, NodeType.EXAM, NodeStatus.UNLOCKED)
        )

        assertThat(firstStepTestNodeOf(path)?.id).isEqualTo("node_0_6_lesson")
    }

    @Test
    fun `an unlocked lesson wins over the ones already done, whatever the list order`() {
        val path = listOf(
            node("node_0_2_lesson", 2, NodeType.LESSON, NodeStatus.UNLOCKED),
            node("node_0_0_lesson", 0, NodeType.LESSON, NodeStatus.COMPLETED),
            node("node_0_1_lesson", 1, NodeType.LESSON, NodeStatus.COMPLETED)
        )

        assertThat(firstStepTestNodeOf(path)?.id).isEqualTo("node_0_2_lesson")
    }

    @Test
    fun `a lesson without written questions is skipped, since its test would come from Gemini`() {
        val path = listOf(
            node("written", 0, NodeType.LESSON, NodeStatus.COMPLETED),
            node("unwritten", 1, NodeType.LESSON, NodeStatus.UNLOCKED)
        )

        val chosen = firstStepTestNodeOf(path, hasWrittenQuestions = { it == "written" })

        assertThat(chosen?.id).isEqualTo("written")
    }

    @Test
    fun `locked lessons are never opened`() {
        val path = listOf(node("node_0_0_lesson", 0, NodeType.LESSON, NodeStatus.LOCKED))

        assertThat(firstStepTestNodeOf(path)).isNull()
    }

    @Test
    fun `no path yet means no test to open`() {
        assertThat(firstStepTestNodeOf(emptyList())).isNull()
    }
}
