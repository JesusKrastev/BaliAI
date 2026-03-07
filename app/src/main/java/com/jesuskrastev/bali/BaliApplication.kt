package com.jesuskrastev.bali

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import com.jesuskrastev.bali.data.remote.interceptors.UserAgentInterceptor
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient

@HiltAndroidApp
class BaliApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .build()

        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Skip heavy SDK initialization in Robolectric tests
        if (isRobolectric()) {
            return
        }

        // Enable verbose logging to debug issues (remove in production)
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Replace with your 36-character App ID from Dashboard > Settings > Keys & IDs
        OneSignal.initWithContext(this, BuildConfig.ONE_SIGNAL_APP_ID)

        MixpanelAPI.getInstance(this, BuildConfig.MIXPANEL_TOKEN, true)
    }

    private fun isRobolectric(): Boolean {
        return try {
            val isRoboClassPresent = Class.forName("org.robolectric.Robolectric") != null
            if (isRoboClassPresent) return true
            false
        } catch (e: Exception) {
            val fingerprint = android.os.Build.FINGERPRINT ?: ""
            fingerprint.contains("robolectric", ignoreCase = true) ||
            android.os.Build.DEVICE.contains("robolectric", ignoreCase = true)
        }
    }
}