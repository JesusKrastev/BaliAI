package com.jesuskrastev.bali.di

import android.content.Context
import com.jesuskrastev.bali.BuildConfig
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

    @Provides
    @Singleton
    fun provideMixpanelAPI(
        @ApplicationContext context: Context
    ): MixpanelAPI {
        if (android.os.Build.FINGERPRINT == "robolectric") {
            return MixpanelAPI.getInstance(context, "test_token", false)
        }
        if (BuildConfig.DEBUG) {
            MPLog.setLevel(MPLog.VERBOSE)
        }
        return MixpanelAPI.getInstance(context, BuildConfig.MIXPANEL_TOKEN, true)
    }
}
