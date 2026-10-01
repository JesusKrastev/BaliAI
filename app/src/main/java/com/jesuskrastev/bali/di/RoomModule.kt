package com.jesuskrastev.bali.di

import android.content.Context
import androidx.room.Room
import com.jesuskrastev.bali.data.local.room.BaliDatabase
import com.jesuskrastev.bali.data.local.room.dao.AnswerDao
import com.jesuskrastev.bali.data.local.room.dao.ChatMessageDao
import com.jesuskrastev.bali.data.local.room.dao.TestResultDao
import com.jesuskrastev.bali.data.local.room.dao.UserDao
import com.jesuskrastev.bali.data.local.room.dao.LessonNodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BaliDatabase =
        Room
            .databaseBuilder(
                context,
                BaliDatabase::class.java,
                "bali_database"
            )
            .addMigrations(
                BaliDatabase.MIGRATION_1_2, 
                BaliDatabase.MIGRATION_2_3, 
                BaliDatabase.MIGRATION_3_4, 
                BaliDatabase.MIGRATION_4_5, 
                BaliDatabase.MIGRATION_5_6, 
                BaliDatabase.MIGRATION_6_7, 
                BaliDatabase.MIGRATION_7_8,
                BaliDatabase.MIGRATION_8_9,
                BaliDatabase.MIGRATION_9_10,
                BaliDatabase.MIGRATION_10_11,
                BaliDatabase.MIGRATION_11_12,
                BaliDatabase.MIGRATION_12_13,
                BaliDatabase.MIGRATION_13_14,
                BaliDatabase.MIGRATION_14_15
            )
            .build()

    @Provides
    fun provideUserDao(db: BaliDatabase): UserDao =
        db.userDao()

    @Provides
    fun provideTestResultDao(db: BaliDatabase): TestResultDao =
        db.testResultDao()

    @Provides
    fun provideAnswerDao(db: BaliDatabase): AnswerDao =
        db.answerDao()

    @Provides
    fun provideLessonNodeDao(db: BaliDatabase): LessonNodeDao =
        db.lessonNodeDao()

    @Provides
    fun provideChatMessageDao(db: BaliDatabase): ChatMessageDao =
        db.chatMessageDao()
}
