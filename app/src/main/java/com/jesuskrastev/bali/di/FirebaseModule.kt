package com.jesuskrastev.bali.di

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val settings = firestoreSettings {
            setLocalCacheSettings(
                persistentCacheSettings {
                    setSizeBytes(50L * 1024L * 1024L)  // 50 MB de caché
                }
            )
        }
        return Firebase.firestore.apply {
            firestoreSettings = settings
        }
    }
}