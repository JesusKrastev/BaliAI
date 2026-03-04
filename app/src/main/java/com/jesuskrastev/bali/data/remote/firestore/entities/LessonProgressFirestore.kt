package com.jesuskrastev.bali.data.remote.firestore.entities

/**
 * Tracks per-lesson completion progress, stored inside UserFirestore.lessonProgress.
 */
data class LessonProgressFirestore(
    val completedCount: Int = 0,
    val bestScore: Int = 0,
    val lastCompletedAt: Long = 0L
)
