package com.jesuskrastev.bali.di

import android.content.Context
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.RobolectricDetector
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.posthog.android.replay.PostHogScreenshotColorMode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PostHogModule {

    /**
     * Provides the singleton PostHog client used to mirror Firebase events.
     *
     * Application lifecycle events (installed, opened, updated, backgrounded) are on, which is
     * what makes retention measurable. Screen views are not captured automatically: the app is
     * a single Activity, so the SDK could only ever see one screen; navigation reports them
     * explicitly through [AnalyticsTracker] instead.
     *
     * Session replay is on in release builds only, so development runs don't eat into the
     * monthly recording quota. It uses screenshot mode, the only mode that works with Jetpack
     * Compose. Text inputs and images are masked, Logcat capture is off, and the resolution and
     * colour depth are lowered to keep the cost on low-end devices small. Screens showing a
     * name, an email or chat text mask those elements further with the `replayMask` modifier.
     *
     * Every event, the SDK's own included, carries the `environment` property so debug traffic
     * can be filtered out. Under Robolectric the client is opted out and replay is not
     * started, so unit tests never touch the network.
     *
     * @param context application context, required to initialize the SDK
     * @return a configured [PostHogInterface] instance
     */
    @Provides
    @Singleton
    fun providePostHog(
        @ApplicationContext context: Context
    ): PostHogInterface {
        val isRobolectric = RobolectricDetector.isRobolectric()
        val config = PostHogAndroidConfig(
            apiKey = if (isRobolectric) "test_token" else BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST
        ).apply {
            captureApplicationLifecycleEvents = true
            captureScreenViews = false
            sessionReplay = !isRobolectric && !BuildConfig.DEBUG
            sessionReplayConfig.apply {
                screenshot = true
                screenshotScale = 0.5f
                screenshotColorMode = PostHogScreenshotColorMode.RGB_565
                screenshotCompressionQuality = 30
                throttleDelayMs = 1000
                maskAllTextInputs = true
                maskAllImages = true
                captureLogcat = false
            }
        }
        return PostHogAndroid.with(context, config).apply {
            register(AnalyticsTracker.KEY_ENVIRONMENT, AnalyticsTracker.currentEnvironment())
            if (isRobolectric) optOut()
        }
    }
}
