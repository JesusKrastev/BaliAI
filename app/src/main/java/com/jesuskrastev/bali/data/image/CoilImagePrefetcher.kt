package com.jesuskrastev.bali.data.image

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.jesuskrastev.bali.domain.exam.ImagePrefetcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * [ImagePrefetcher] backed by the app's Coil image loader.
 *
 * It goes through the same loader the screens use, so a picture that loads here is in the cache
 * when the exam shows it.
 */
class CoilImagePrefetcher @Inject constructor(
    @ApplicationContext private val context: Context
) : ImagePrefetcher {

    /**
     * Loads [url] through Coil.
     *
     * @param url the picture's address
     * @return true when the picture loaded within [TIMEOUT_MILLIS]; false on any failure or timeout
     */
    override suspend fun isLoadable(url: String): Boolean = withTimeoutOrNull(TIMEOUT_MILLIS) {
        val request = ImageRequest.Builder(context).data(url).build()
        context.imageLoader.execute(request) is SuccessResult
    } ?: false

    private companion object {
        /** Longer than this and the student would be waiting on a picture the exam cannot count on. */
        const val TIMEOUT_MILLIS = 8_000L
    }
}
