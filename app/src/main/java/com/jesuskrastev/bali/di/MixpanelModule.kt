package com.jesuskrastev.bali.di

import android.content.Context
import com.jesuskrastev.bali.BuildConfig
import com.mixpanel.android.mpmetrics.MixpanelAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MixpanelModule {

    @Provides
    @Singleton
    fun provideMixpanelAPI(
        @ApplicationContext context: Context
    ): MixpanelAPI {
        if (android.os.Build.FINGERPRINT == "robolectric") {
            // Return a mock or handle test case. 
            // MixpanelAPI.getInstance might still be called if needed, 
            // but we want to avoid the real background processing.
            // For now, let's try to return a dummy instance if possible, 
            // but MixpanelAPI.getInstance is the only way to get one.
            // Let's just return the instance but with flushOnBackground = false
            return MixpanelAPI.getInstance(context, "test_token", false)
        }
        return MixpanelAPI.getInstance(context, BuildConfig.MIXPANEL_TOKEN, true)
    }
}
