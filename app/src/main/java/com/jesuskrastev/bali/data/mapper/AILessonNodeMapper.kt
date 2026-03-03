package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.AILessonNodeEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.AILessonNodeFirestore
import com.jesuskrastev.bali.domain.model.AILessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType

fun AILessonNodeEntity.toDomain(): AILessonNode {
    return AILessonNode(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = status,
        scorePercentage = scorePercentage,
        sectionIndex = sectionIndex,
        sectionTitle = sectionTitle,
        unitIndex = unitIndex,
        nodeType = nodeType,
        iconResName = iconResName
    )
}

fun AILessonNodeFirestore.toDomain(): AILessonNode {
    return AILessonNode(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = try { NodeStatus.valueOf(status) } catch (e: Exception) { NodeStatus.LOCKED },
        scorePercentage = scorePercentage,
        sectionIndex = sectionIndex,
        sectionTitle = sectionTitle,
        unitIndex = unitIndex,
        nodeType = try { NodeType.valueOf(nodeType) } catch (e: Exception) { NodeType.LESSON },
        iconResName = iconResName
    )
}

fun AILessonNode.toEntity(): AILessonNodeEntity {
    return AILessonNodeEntity(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = status,
        scorePercentage = scorePercentage,
        sectionIndex = sectionIndex,
        sectionTitle = sectionTitle,
        unitIndex = unitIndex,
        nodeType = nodeType,
        iconResName = iconResName
    )
}

fun AILessonNode.toFirestore(): AILessonNodeFirestore {
    return AILessonNodeFirestore(
        id = id,
        orderIndex = orderIndex,
        title = title,
        description = description,
        status = status.name,
        scorePercentage = scorePercentage,
        sectionIndex = sectionIndex,
        sectionTitle = sectionTitle,
        unitIndex = unitIndex,
        nodeType = nodeType.name,
        iconResName = iconResName
    )
}
