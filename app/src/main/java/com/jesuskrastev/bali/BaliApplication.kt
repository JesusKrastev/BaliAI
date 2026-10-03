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
import com.jesuskrastev.bali.domain.model.NotificationCategory
import com.onesignal.OneSignal
import com.posthog.PostHogInterface
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class BaliApplication : Application(), ImageLoaderFactory {

    /** Lazy so PostHog is only created after the Robolectric guard in [onCreate]. */
    @Inject
    lateinit var posthog: Lazy<PostHogInterface>

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
        if (RobolectricDetector.isRobolectric()) {
            return
        }

        if (BuildConfig.DEBUG) {
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        }
        initAppCheck()
        createNotificationChannels()
        OneSignal.initWithContext(this, BuildConfig.ONE_SIGNAL_APP_ID)
        Purchases.logLevel = LogLevel.DEBUG
        val builder = PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY)
        Purchases.configure(
            builder
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
        linkRevenueCatToPostHog()
    }

    /**
     * Tells RevenueCat which PostHog person this install is (the `$posthogUserId` subscriber
     * attribute), so purchase events from its PostHog integration land on the same person as
     * the in-app events instead of on a separate `$RCAnonymousID` one.
     *
     * Only tags the subscriber: it cannot change purchases or entitlements. Resolving the
     * lazy client here also creates PostHog at process start, rather than on the first
     * screen that injects an analytics tracker.
     */
    private fun linkRevenueCatToPostHog() {
        Purchases.sharedInstance.setPostHogUserId(posthog.get().distinctId())
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

    /**
     * Creates one Android channel per [NotificationCategory], so the user can silence a kind of
     * push from the system settings and the OneSignal messages choose theirs by id. Creating a
     * channel that exists only refreshes its name and description: the importance the user may
     * have changed is kept.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            NotificationCategory.entries.forEach { category ->
                val importance = when (category) {
                    NotificationCategory.STUDY, NotificationCategory.STREAK -> NotificationManager.IMPORTANCE_HIGH
                    NotificationCategory.PROMOTIONS -> NotificationManager.IMPORTANCE_LOW
                }
                val channel = NotificationChannel(category.channelId, category.title, importance).apply {
                    description = category.description
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
