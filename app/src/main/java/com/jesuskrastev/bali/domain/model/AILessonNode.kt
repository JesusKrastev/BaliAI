package com.jesuskrastev.bali.domain.model

enum class NodeStatus {
    LOCKED,
    UNLOCKED,
    COMPLETED
}

data class AILessonNode(
    val id: String,
    val orderIndex: Int,
    val title: String,
    val description: String,
    val status: NodeStatus,
    val scorePercentage: Int? = null,
    val sectionIndex: Int = 0,
    val sectionTitle: String = "",
    val unitIndex: Int = 0,
    val nodeType: NodeType = NodeType.LESSON,
    val iconResName: String = "lesson_test"
)
