package com.jesuskrastev.bali

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.perf.FirebasePerformance
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

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "reminders"
    }

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

    /** Initializes telemetry and third-party SDKs unless the process is a Robolectric test. */
    override fun onCreate() {
        super.onCreate()

        // Skip heavy SDK initialization in Robolectric tests
        if (isRobolectric()) {
            return
        }

        if (BuildConfig.DEBUG) {
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        }
        initAppCheck()
        createNotificationChannel()
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

    /**
     * Attests that requests really come from this app before Firebase AI Logic will serve
     * them.
     *
     * Firebase AI Logic keeps the Gemini credentials out of the APK, but its endpoint is
     * reachable by anyone who knows the project — App Check is what closes it, so this is
     * half of the protection rather than an optional extra. Which provider does the
     * attesting depends on the build type, so it comes from the source sets.
     */
    private fun initAppCheck() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Bali AI",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de estudio y logros"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
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
