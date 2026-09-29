package com.jesuskrastev.bali

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.perf.FirebasePerformance
import com.jesuskrastev.bali.data.remote.interceptors.UserAgentInterceptor
import com.mixpanel.android.mpmetrics.MixpanelAPI
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

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "reminders"
    }

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

    /**
     * Initializes telemetry and third-party SDKs unless the process is a Robolectric test.
     *
     * On Firebase Test Lab (which also runs Google Play's pre-launch report) everything still
     * starts so the app behaves normally, but the SDKs that would count the robot as a user
     * are silenced, see [muteEngagementSdks].
     */
    override fun onCreate() {
        super.onCreate()

        // Skip heavy SDK initialization in Robolectric tests
        if (RobolectricDetector.isRobolectric()) {
            return
        }

        val isTestLab = TestLabDetector.isTestLab(this)

        if (BuildConfig.DEBUG) {
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        }
        initAppCheck()
        createNotificationChannel()
        if (isTestLab) muteEngagementSdks()
        OneSignal.initWithContext(this, BuildConfig.ONE_SIGNAL_APP_ID)
        // On Test Lab MixpanelModule builds the client already opted out, on first injection.
        if (!isTestLab) MixpanelAPI.getInstance(this, BuildConfig.MIXPANEL_TOKEN, true)
        Purchases.logLevel = LogLevel.DEBUG
        val builder = PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY)
        Purchases.configure(
            builder
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
        // A Test Lab robot has no PostHog person worth linking to (its events are dropped).
        if (!isTestLab) linkRevenueCatToPostHog()
    }

    /**
     * Keeps a Firebase Test Lab robot out of the tools that count users: Firebase Analytics
     * stops collecting, and OneSignal is made to require consent that is never granted, so it
     * neither registers the device nor receives pushes. PostHog and Mixpanel are opted out
     * where they are created, in [com.jesuskrastev.bali.di.PostHogModule] and
     * [com.jesuskrastev.bali.di.MixpanelModule].
     *
     * Must run before OneSignal is initialized, since consent is read at that point.
     */
    private fun muteEngagementSdks() {
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false)
        OneSignal.consentRequired = true
    }

    /**
     * Tells RevenueCat which PostHog person this install is (the `$posthogUserId` subscriber
     * attribute), so purchase events from its PostHog integration land on the same person as
     * the in-app events instead of on a separate `$RCAnonymousID` one.
     *
     * This covers the anonymous, pre-login person. Once the user signs in, PostHog is
     * identified with the Firebase uid and
     * [com.jesuskrastev.bali.domain.repository.SubscriptionRepository.identify] moves the
     * RevenueCat customer to that same uid and refreshes the attribute.
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

    /** Creates the high-importance "reminders" channel that study reminders and achievements use (API 26+). */
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
}
