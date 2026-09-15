package com.jesuskrastev.bali.di

import android.content.Context
import com.jesuskrastev.bali.BuildConfig
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
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
     * Provides the singleton PostHog client used to mirror Firebase/Mixpanel events.
     *
     * Session replay, autocapture and screen-view tracking are disabled: this app sends
     * the same explicit events it already sends to Firebase and Mixpanel, not raw UI
     * interactions. Under Robolectric the client is opted out so unit tests never touch
     * the network.
     *
     * @param context application context, required to initialize the SDK
     * @return a configured [PostHogInterface] instance
     */
    @Provides
    @Singleton
    fun providePostHog(
        @ApplicationContext context: Context
    ): PostHogInterface {
        val isRobolectric = android.os.Build.FINGERPRINT == "robolectric"
        val config = PostHogAndroidConfig(
            apiKey = if (isRobolectric) "test_token" else BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST
        ).apply {
            captureApplicationLifecycleEvents = false
            captureScreenViews = false
            sessionReplay = false
        }
        return PostHogAndroid.with(context, config).apply {
            if (isRobolectric) optOut()
        }
    }
}
