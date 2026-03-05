package com.jesuskrastev.bali.domain.model

enum class NodeStatus {
    LOCKED,
    UNLOCKED,
    COMPLETED
}

/**
 * Represents a single node on the user's learning path.
 *
 * A node can be a regular lesson, a section review, an exam, or a dynamic AI-generated node.
 * It tracks its completion status, score, and position within the learning path structure.
 *
 * @property id The unique identifier for this node.
 * @property orderIndex The deterministic order of this node in the learning path.
 * @property title The display title of the node.
 * @property description A brief description of the node's content.
 * @property status The current availability and completion status of the node.
 * @property scorePercentage The best score achieved on this node, if completed.
 * @property nodeType The categorization of the node (e.g., LESSON, REVIEW, EXAM, AI_DYNAMIC).
 */
data class LessonNode(
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
    val iconResName: String = "lesson_test",
    val completedCount: Int = 0
)
