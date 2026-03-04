package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.local.room.entities.LessonNodeEntity
import com.jesuskrastev.bali.data.remote.firestore.entities.LessonNodeFirestore
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType

fun LessonNodeEntity.toDomain(): LessonNode {
    return LessonNode(
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

fun LessonNodeFirestore.toDomain(): LessonNode {
    return LessonNode(
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

fun LessonNode.toEntity(): LessonNodeEntity {
    return LessonNodeEntity(
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

fun LessonNode.toFirestore(): LessonNodeFirestore {
    return LessonNodeFirestore(
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
