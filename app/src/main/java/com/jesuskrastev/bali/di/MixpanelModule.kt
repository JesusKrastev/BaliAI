package com.jesuskrastev.bali.di

import android.content.Context
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.RobolectricDetector
import com.jesuskrastev.bali.TestLabDetector
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mixpanel.android.util.MPLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MixpanelModule {

    /**
     * Provides the singleton Mixpanel client.
     *
     * Under Robolectric it points at a dummy token, and on Firebase Test Lab (which Google
     * Play's pre-launch report also uses) it is created already opted out with automatic events
     * off, so a robot never counts as a user. [com.jesuskrastev.bali.BaliApplication] skips its
     * own eager `getInstance` in that case, so this is the call that creates the client.
     *
     * @param context application context, required to initialize the SDK
     * @return the shared [MixpanelAPI] instance
     */
    @Provides
    @Singleton
    fun provideMixpanelAPI(
        @ApplicationContext context: Context
    ): MixpanelAPI {
        if (RobolectricDetector.isRobolectric()) {
            return MixpanelAPI.getInstance(context, "test_token", false)
        }
        if (TestLabDetector.isTestLab(context)) {
            return MixpanelAPI
                .getInstance(context, BuildConfig.MIXPANEL_TOKEN, true, false)
                .also { it.optOutTracking() }
        }
        if (BuildConfig.DEBUG) {
            MPLog.setLevel(MPLog.VERBOSE)
        }
        return MixpanelAPI.getInstance(context, BuildConfig.MIXPANEL_TOKEN, true)
    }
}
