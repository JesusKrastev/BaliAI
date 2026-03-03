package com.jesuskrastev.bali.data.local.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jesuskrastev.bali.domain.model.NodeStatus

@Entity(tableName = "ai_lesson_nodes")
data class AILessonNodeEntity(
    @PrimaryKey val id: String,
    val orderIndex: Int,
    val title: String,
    val description: String,
    val status: NodeStatus,
    val scorePercentage: Int? = null
)
