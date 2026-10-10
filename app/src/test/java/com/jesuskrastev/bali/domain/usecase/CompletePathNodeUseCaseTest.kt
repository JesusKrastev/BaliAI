package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.util.RecordingPathRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompletePathNodeUseCaseTest {

    private fun node(id: String, order: Int, status: NodeStatus, type: NodeType = NodeType.LESSON) =
        LessonNode(id = id, orderIndex = order, title = id, description = "", status = status, nodeType = type)

    private fun repository() = RecordingPathRepository(
        listOf(
            node("exam", 0, NodeStatus.UNLOCKED, NodeType.EXAM),
            node("next", 1, NodeStatus.LOCKED)
        )
    )

    @Test
    fun `a passing score completes the node and unlocks the next one`() = runTest {
        val path = repository()

        CompletePathNodeUseCase(path)("user", "exam", 80)

        assertThat(path.nodes.first { it.id == "exam" }.status).isEqualTo(NodeStatus.COMPLETED)
        assertThat(path.nodes.first { it.id == "exam" }.scorePercentage).isEqualTo(80)
        assertThat(path.nodes.first { it.id == "next" }.status).isEqualTo(NodeStatus.UNLOCKED)
    }

    @Test
    fun `a failing score is recorded but leaves the next node locked`() = runTest {
        val path = repository()

        CompletePathNodeUseCase(path)("user", "exam", 69)

        assertThat(path.nodes.first { it.id == "exam" }.status).isEqualTo(NodeStatus.COMPLETED)
        assertThat(path.nodes.first { it.id == "exam" }.scorePercentage).isEqualTo(69)
        assertThat(path.nodes.first { it.id == "next" }.status).isEqualTo(NodeStatus.LOCKED)
    }

    @Test
    fun `the passing mark itself unlocks the next node`() = runTest {
        val path = repository()

        CompletePathNodeUseCase(path)("user", "exam", CompletePathNodeUseCase.PASSING_ACCURACY)

        assertThat(path.nodes.first { it.id == "next" }.status).isEqualTo(NodeStatus.UNLOCKED)
    }

    @Test
    fun `a node that is already open is not reopened`() = runTest {
        val path = RecordingPathRepository(
            listOf(node("exam", 0, NodeStatus.COMPLETED, NodeType.EXAM), node("next", 1, NodeStatus.COMPLETED))
        )

        CompletePathNodeUseCase(path)("user", "exam", 100)

        assertThat(path.updates.map { it.nodeId }).containsExactly("exam")
    }

    @Test
    fun `the last node of the path has nothing to unlock`() = runTest {
        val path = RecordingPathRepository(listOf(node("exam", 0, NodeStatus.UNLOCKED, NodeType.EXAM)))

        CompletePathNodeUseCase(path)("user", "exam", 100)

        assertThat(path.updates).hasSize(1)
    }
}
