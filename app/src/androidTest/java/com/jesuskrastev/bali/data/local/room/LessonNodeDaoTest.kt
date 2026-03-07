package com.jesuskrastev.bali.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jesuskrastev.bali.data.local.room.dao.LessonNodeDao
import com.jesuskrastev.bali.data.local.room.entities.LessonNodeEntity
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

@HiltAndroidTest
class LessonNodeDaoTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private lateinit var database: BaliDatabase
    private lateinit var lessonNodeDao: LessonNodeDao

    @Before
    fun init() {
        hiltRule.inject()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BaliDatabase::class.java
        ).allowMainThreadQueries().build()
        lessonNodeDao = database.lessonNodeDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun insertNodesAndRetrieveThem() = runTest {
        val nodes = listOf(
            LessonNodeEntity(
                id = "node_1",
                orderIndex = 0,
                title = "Lesson 1",
                description = "First lesson",
                status = NodeStatus.UNLOCKED,
                scorePercentage = 0,
                sectionIndex = 0,
                unitIndex = 0,
                nodeType = NodeType.LESSON
            ),
            LessonNodeEntity(
                id = "node_2",
                orderIndex = 1,
                title = "Lesson 2",
                description = "Second lesson",
                status = NodeStatus.LOCKED,
                scorePercentage = 0,
                sectionIndex = 0,
                unitIndex = 0,
                nodeType = NodeType.LESSON
            )
        )

        lessonNodeDao.insertNodes(nodes)
        val retrieved = lessonNodeDao.getAllNodes().first()

        assertThat(retrieved).hasSize(2)
        assertThat(retrieved[0].title).isEqualTo("Lesson 1")
    }

    @Test
    fun completeNodeUpdatesStatusAndScore() = runTest {
        val node = LessonNodeEntity(
            id = "node_1",
            orderIndex = 0,
            title = "Lesson",
            description = "Desc",
            status = NodeStatus.UNLOCKED,
            scorePercentage = 0
        )

        lessonNodeDao.insertNodes(listOf(node))
        lessonNodeDao.completeNode("node_1", NodeStatus.COMPLETED.name, 90)

        val updated = lessonNodeDao.getNodeById("node_1")
        assertThat(updated?.status).isEqualTo(NodeStatus.COMPLETED)
        assertThat(updated?.scorePercentage).isEqualTo(90)
    }

    @Test
    fun updateNodeStatusChangesStatus() = runTest {
        val node = LessonNodeEntity(
            id = "node_1",
            orderIndex = 0,
            title = "L",
            description = "D",
            status = NodeStatus.UNLOCKED,
            scorePercentage = 0
        )

        lessonNodeDao.insertNodes(listOf(node))
        lessonNodeDao.updateNodeStatus("node_1", NodeStatus.UNLOCKED.name)

        val updated = lessonNodeDao.getNodeById("node_1")
        assertThat(updated?.status).isEqualTo(NodeStatus.UNLOCKED)
    }

    @Test
    fun getNodeByIdReturnsCorrectNode() = runTest {
        val node = LessonNodeEntity(
            id = "node_1",
            orderIndex = 0,
            title = "Lesson",
            description = "Desc",
            status = NodeStatus.UNLOCKED,
            scorePercentage = 0
        )

        lessonNodeDao.insertNodes(listOf(node))
        val retrieved = lessonNodeDao.getNodeById("node_1")

        assertThat(retrieved?.id).isEqualTo("node_1")
        assertThat(retrieved?.title).isEqualTo("Lesson")
    }

    @Test
    fun getMaxOrderIndexReturnsHighestIndex() = runTest {
        lessonNodeDao.insertNodes(listOf(
            LessonNodeEntity("1", 0, "L1", "D", NodeStatus.UNLOCKED, 0),
            LessonNodeEntity("2", 1, "L2", "D", NodeStatus.UNLOCKED, 0),
            LessonNodeEntity("3", 5, "L3", "D", NodeStatus.UNLOCKED, 0)
        ))

        val max = lessonNodeDao.getMaxOrderIndex()
        assertThat(max).isEqualTo(5)
    }

    @Test
    fun clearAllNodesRemovesAllNodes() = runTest {
        lessonNodeDao.insertNodes(listOf(
            LessonNodeEntity("1", 0, "L1", "D", NodeStatus.UNLOCKED, 0),
            LessonNodeEntity("2", 1, "L2", "D", NodeStatus.UNLOCKED, 0)
        ))

        lessonNodeDao.clearAllNodes()
        val all = lessonNodeDao.getAllNodes().first()

        assertThat(all).isEmpty()
    }
}
