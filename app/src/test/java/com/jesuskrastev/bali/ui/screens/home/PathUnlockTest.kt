package com.jesuskrastev.bali.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.domain.repository.PathRepository
import com.jesuskrastev.bali.domain.repository.PathSeenRepository
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

private fun node(order: Int, status: NodeStatus) = LessonNode(
    id = "node_$order",
    orderIndex = order,
    title = "Node $order",
    description = "",
    status = status,
    nodeType = NodeType.LESSON
)

/** A path of [size] nodes where the first [completed] are done, the next is open and the rest locked. */
private fun pathWith(completed: Int, size: Int = 6) = List(size) { index ->
    node(
        index,
        when {
            index < completed -> NodeStatus.COMPLETED
            index == completed -> NodeStatus.UNLOCKED
            else -> NodeStatus.LOCKED
        }
    )
}

class PathUnlockTest {

    @Test
    fun `the frontier is the furthest node that is not locked`() {
        assertThat(pathFrontierOrder(pathWith(completed = 0))).isEqualTo(0)
        assertThat(pathFrontierOrder(pathWith(completed = 3))).isEqualTo(3)
    }

    @Test
    fun `a path with nothing open or no nodes has no frontier`() {
        assertThat(pathFrontierOrder(emptyList())).isNull()
        assertThat(pathFrontierOrder(List(3) { node(it, NodeStatus.LOCKED) })).isNull()
    }

    @Test
    fun `the first time Home is seen nothing animates, however far the path is`() {
        assertThat(detectPathUnlock(frontier = 40, lastSeen = null)).isNull()
    }

    @Test
    fun `a node opened since the last visit animates from where the user was`() {
        assertThat(detectPathUnlock(frontier = 4, lastSeen = 3)).isEqualTo(PathUnlock(fromOrder = 3, toOrder = 4))
    }

    @Test
    fun `several nodes opened since the last visit animate as one stretch`() {
        assertThat(detectPathUnlock(frontier = 6, lastSeen = 3)).isEqualTo(PathUnlock(fromOrder = 3, toOrder = 6))
    }

    @Test
    fun `no progress means no animation`() {
        assertThat(detectPathUnlock(frontier = 3, lastSeen = 3)).isNull()
        assertThat(detectPathUnlock(frontier = 2, lastSeen = 3)).isNull()
    }
}

class PathUnlockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private class FakeSeenRepository(var stored: Pair<String, Int>? = null) : PathSeenRepository {
        override suspend fun lastSeenFrontier(userId: String): Int? = stored?.takeIf { it.first == userId }?.second
        override suspend fun markSeen(userId: String, frontierOrder: Int) {
            stored = userId to frontierOrder
        }
    }

    private class MutablePathRepository(initial: List<LessonNode>) : PathRepository {
        val nodes = MutableStateFlow(initial)
        override fun getPathNodes(userId: String): Flow<List<LessonNode>> = nodes
        override suspend fun saveGeneratedNodes(userId: String, nodes: List<LessonNode>) {}
        override suspend fun updateNodeStatus(userId: String, nodeId: String, status: String, scorePercentage: Int?) {}
        override suspend fun getLastUnlockedNodeOrder(userId: String): Int = 0
    }

    private fun viewModel(paths: MutablePathRepository, seen: FakeSeenRepository) =
        PathUnlockViewModel(FakeAuthRepository(currentUserId = "u1"), paths, seen)

    @Test
    fun `a first visit is silent and remembers the position`() = runTest {
        val seen = FakeSeenRepository()
        val vm = viewModel(MutablePathRepository(pathWith(completed = 5)), seen)

        assertThat(vm.uiState.value.isReady).isTrue()
        assertThat(vm.uiState.value.unlock).isNull()
        assertThat(seen.stored).isEqualTo("u1" to 5)
    }

    @Test
    fun `another account on the same phone starts silent`() = runTest {
        val seen = FakeSeenRepository(stored = "someone_else" to 1)
        val vm = viewModel(MutablePathRepository(pathWith(completed = 5)), seen)

        assertThat(vm.uiState.value.unlock).isNull()
        assertThat(seen.stored).isEqualTo("u1" to 5)
    }

    @Test
    fun `a node opened since the last visit is reported, and only remembered once it has played`() = runTest {
        val seen = FakeSeenRepository(stored = "u1" to 2)
        val vm = viewModel(MutablePathRepository(pathWith(completed = 3)), seen)

        assertThat(vm.uiState.value.unlock).isEqualTo(PathUnlock(fromOrder = 2, toOrder = 3))
        assertThat(seen.stored).isEqualTo("u1" to 2)

        vm.onUnlockPlayed()

        assertThat(vm.uiState.value.unlock).isNull()
        assertThat(seen.stored).isEqualTo("u1" to 3)
    }

    @Test
    fun `a Home rebuilt before the animation played still gets it`() = runTest {
        val seen = FakeSeenRepository(stored = "u1" to 2)
        val paths = MutablePathRepository(pathWith(completed = 3))
        val first = viewModel(paths, seen)
        val rebuilt = viewModel(paths, seen)

        assertThat(first.uiState.value.unlock).isNotNull()
        assertThat(rebuilt.uiState.value.unlock).isEqualTo(PathUnlock(fromOrder = 2, toOrder = 3))
    }

    @Test
    fun `completing a node while Home is alive is picked up`() = runTest {
        val seen = FakeSeenRepository(stored = "u1" to 2)
        val paths = MutablePathRepository(pathWith(completed = 2))
        val vm = viewModel(paths, seen)
        assertThat(vm.uiState.value.unlock).isNull()

        paths.nodes.value = pathWith(completed = 3)

        assertThat(vm.uiState.value.unlock).isEqualTo(PathUnlock(fromOrder = 2, toOrder = 3))
    }

    @Test
    fun `a path with no nodes yet is not ready`() = runTest {
        val vm = viewModel(MutablePathRepository(emptyList()), FakeSeenRepository())

        assertThat(vm.uiState.value.isReady).isFalse()
    }
}
