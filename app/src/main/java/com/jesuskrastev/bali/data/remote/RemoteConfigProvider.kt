package com.jesuskrastev.bali.data.remote

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class RemoteConfigProvider @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    /**
     * Fetches the latest Remote Config values and activates them for this session.
     * A failure (e.g. offline) is reported to Crashlytics and otherwise ignored, so
     * callers keep working off whatever values were last activated.
     */
    suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}
