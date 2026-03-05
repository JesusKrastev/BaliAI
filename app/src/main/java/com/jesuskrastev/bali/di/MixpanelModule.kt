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
        return MixpanelAPI.getInstance(context, BuildConfig.MIXPANEL_TOKEN, true)
    }
}
