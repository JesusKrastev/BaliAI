package com.jesuskrastev.bali.data.remote

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class RemoteConfigProvider @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getWeeklyGoal(): Int {
        return remoteConfig.getLong("weekly_goal").toInt().let { if (it == 0) 5 else it }
    }
}
