package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.AILessonNodeEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.AILessonNodeFirestore
import com.jesuskrastev.bali.domain.model.AILessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus

fun AILessonNodeEntity.toDomain(): AILessonNode {
    return AILessonNode(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = status,
        scorePercentage = scorePercentage
    )
}

fun AILessonNodeFirestore.toDomain(): AILessonNode {
    return AILessonNode(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = try { NodeStatus.valueOf(status) } catch (e: Exception) { NodeStatus.LOCKED },
        scorePercentage = scorePercentage
    )
}

fun AILessonNode.toEntity(): AILessonNodeEntity {
    return AILessonNodeEntity(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = status,
        scorePercentage = scorePercentage
    )
}

fun AILessonNode.toFirestore(): AILessonNodeFirestore {
    return AILessonNodeFirestore(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = status.name,
        scorePercentage = scorePercentage
    )
}
