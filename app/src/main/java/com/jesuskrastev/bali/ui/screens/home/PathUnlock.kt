package com.jesuskrastev.bali.ui.screens.home

import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus

/**
 * A stretch of the learning path that opened since Home last showed it, to be played as an
 * animation: the connector into each new node fills in and the new node pops open.
 *
 * @property fromOrder `orderIndex` of the furthest unlocked node Home showed last time
 * @property toOrder `orderIndex` of the furthest unlocked node now; always greater than [fromOrder]
 */
data class PathUnlock(val fromOrder: Int, val toOrder: Int)

/**
 * The furthest point the user has reached on the path.
 *
 * @param nodes every node of the path
 * @return the highest `orderIndex` among nodes that are not locked, or null for an empty path
 */
fun pathFrontierOrder(nodes: List<LessonNode>): Int? =
    nodes.filter { it.status != NodeStatus.LOCKED }.maxOfOrNull { it.orderIndex }

/**
 * Decides whether Home has something to celebrate.
 *
 * @param frontier the furthest unlocked node now, see [pathFrontierOrder]
 * @param lastSeen the furthest one Home showed before, null when it never has (first install,
 *   or another account): nothing animates then, so a new install does not replay the whole path
 * @return the [PathUnlock] to play, or null when the path has not advanced
 */
fun detectPathUnlock(frontier: Int, lastSeen: Int?): PathUnlock? =
    if (lastSeen != null && frontier > lastSeen) PathUnlock(fromOrder = lastSeen, toOrder = frontier) else null
