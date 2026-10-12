package com.jesuskrastev.bali.util

import com.jesuskrastev.bali.domain.exam.ImagePrefetcher
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [ImagePrefetcher] that loads every picture except the ones it is told to fail.
 *
 * @param broken addresses whose load fails
 * @param failEverything true to fail every address, as when the phone is offline
 */
class FakeImagePrefetcher(
    private val broken: Set<String> = emptySet(),
    private val failEverything: Boolean = false
) : ImagePrefetcher {

    /** Every address asked for, in order. */
    val requested = CopyOnWriteArrayList<String>()

    override suspend fun isLoadable(url: String): Boolean {
        requested += url
        return !failEverything && url !in broken
    }
}
