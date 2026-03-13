package com.jesuskrastev.bali

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import com.jesuskrastev.bali.data.remote.interceptors.UserAgentInterceptor
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.onesignal.OneSignal
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
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

        OneSignal.initWithContext(this, BuildConfig.ONE_SIGNAL_APP_ID)
        MixpanelAPI.getInstance(this, BuildConfig.MIXPANEL_TOKEN, true)
        Purchases.logLevel = LogLevel.DEBUG
        val builder = PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY)
        Purchases.configure(
            builder
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
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