package com.jesuskrastev.bali.domain.exam

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Checks that a question's picture can really be shown before the question is put in an exam.
 *
 * A question such as "¿Qué indica esta señal?" cannot be answered without its picture, so an
 * exam must never reach the student with one whose picture failed to load.
 */
interface ImagePrefetcher {

    /**
     * Downloads [url] into the image cache, so the screen shows it instantly afterwards.
     *
     * @param url the picture's address
     * @return true when the picture loaded; false when it failed or took too long
     */
    suspend fun isLoadable(url: String): Boolean
}

/**
 * Loads many pictures at once.
 *
 * @param urls the pictures' addresses; repeated ones are loaded once
 * @return the addresses that could not be loaded
 */
suspend fun ImagePrefetcher.unloadableAmong(urls: Collection<String>): Set<String> = coroutineScope {
    urls.distinct()
        .map { url -> async { url to isLoadable(url) } }
        .awaitAll()
        .filterNot { (_, loaded) -> loaded }
        .mapTo(HashSet()) { (url, _) -> url }
}
