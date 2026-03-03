package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType

@Entity(tableName = "ai_lesson_nodes")
data class AILessonNodeEntity(
    @PrimaryKey val id: String,
    val orderIndex: Int,
    val title: String,
    val description: String,
    val status: NodeStatus,
    val scorePercentage: Int? = null,
    @ColumnInfo(defaultValue = "0") val sectionIndex: Int = 0,
    @ColumnInfo(defaultValue = "") val sectionTitle: String = "",
    @ColumnInfo(defaultValue = "0") val unitIndex: Int = 0,
    @ColumnInfo(defaultValue = "LESSON") val nodeType: NodeType = NodeType.LESSON,
    @ColumnInfo(defaultValue = "lesson_test") val iconResName: String = "lesson_test"
)
