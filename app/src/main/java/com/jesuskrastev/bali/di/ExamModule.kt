package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.image.CoilImagePrefetcher
import com.jesuskrastev.bali.domain.exam.ImagePrefetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExamModule {

    @Binds
    @Singleton
    abstract fun bindImagePrefetcher(impl: CoilImagePrefetcher): ImagePrefetcher
}
