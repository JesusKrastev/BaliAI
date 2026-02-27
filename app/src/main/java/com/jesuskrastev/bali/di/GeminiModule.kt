package com.jesuskrastev.bali.di

import com.google.ai.client.generativeai.GenerativeModel
import com.jesuskrastev.bali.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    fun provideGemini(): GenerativeModel =
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
}
