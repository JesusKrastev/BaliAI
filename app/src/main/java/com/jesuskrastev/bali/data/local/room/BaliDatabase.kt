package com.jesuskrastev.bali.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.jesuskrastev.bali.data.local.room.dao.AnswerDao
import com.jesuskrastev.bali.data.local.room.dao.TestResultDao
import com.jesuskrastev.bali.data.local.room.dao.UserDao
import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
import com.jesuskrastev.bali.data.local.room.entities.TestResultEntity
import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import com.jesuskrastev.bali.data.local.room.entities.LessonNodeEntity
import com.jesuskrastev.bali.data.local.room.dao.LessonNodeDao

@Database(
    entities = [UserEntity::class, TestResultEntity::class, AnswerEntity::class, LessonNodeEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BaliDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun testResultDao(): TestResultDao
    abstract fun answerDao(): AnswerDao
    abstract fun lessonNodeDao(): LessonNodeDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ai_lesson_nodes RENAME TO lesson_nodes")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ai_lesson_nodes ADD COLUMN sectionIndex INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ai_lesson_nodes ADD COLUMN sectionTitle TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE ai_lesson_nodes ADD COLUMN unitIndex INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ai_lesson_nodes ADD COLUMN nodeType TEXT NOT NULL DEFAULT 'LESSON'")
                database.execSQL("ALTER TABLE ai_lesson_nodes ADD COLUMN iconResName TEXT NOT NULL DEFAULT 'lesson_test'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_lesson_nodes` (
                        `id` TEXT NOT NULL, 
                        `orderIndex` INTEGER NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `scorePercentage` INTEGER, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN practiceDays TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN lastEnergyUpdateTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Migración de la tabla 'users'
                database.execSQL("""
                    CREATE TABLE users_new (
                        id TEXT NOT NULL PRIMARY KEY, 
                        name TEXT, 
                        licenseType TEXT, 
                        experience TEXT, 
                        reasons TEXT NOT NULL, 
                        examDateMillis INTEGER, 
                        dailyGoal TEXT, 
                        learningPreference TEXT, 
                        difficultTopics TEXT NOT NULL, 
                        concern TEXT, 
                        studyTime TEXT, 
                        lastPracticeTimestamp INTEGER NOT NULL, 
                        currentStreak INTEGER NOT NULL, 
                        xp INTEGER NOT NULL, 
                        level INTEGER NOT NULL, 
                        energy INTEGER NOT NULL, 
                        coins INTEGER NOT NULL, 
                        streakFreezes INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO users_new (id, name, licenseType, experience, reasons, examDateMillis, dailyGoal, learningPreference, difficultTopics, concern, studyTime, lastPracticeTimestamp, currentStreak, xp, level, energy, coins, streakFreezes)
                    SELECT CAST(id AS TEXT), name, licenseType, experience, reasons, examDateMillis, dailyGoal, learningPreference, difficultTopics, concern, studyTime, lastPracticeTimestamp, currentStreak, xp, level, energy, coins, streakFreezes FROM users
                """.trimIndent())
                database.execSQL("DROP TABLE users")
                database.execSQL("ALTER TABLE users_new RENAME TO users")

                // 2. Migración de la tabla 'test_results'
                database.execSQL("""
                    CREATE TABLE test_results_new (
                        id TEXT NOT NULL PRIMARY KEY, 
                        category TEXT NOT NULL, 
                        score INTEGER NOT NULL, 
                        total INTEGER NOT NULL, 
                        timestamp INTEGER NOT NULL, 
                        isPassed INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO test_results_new (id, category, score, total, timestamp, isPassed)
                    SELECT CAST(id AS TEXT), category, score, total, timestamp, isPassed FROM test_results
                """.trimIndent())
                database.execSQL("DROP TABLE test_results")
                database.execSQL("ALTER TABLE test_results_new RENAME TO test_results")

                // 3. Migración de la tabla 'answers'
                database.execSQL("""
                    CREATE TABLE answers_new (
                        id TEXT NOT NULL PRIMARY KEY, 
                        testId TEXT NOT NULL, 
                        questionText TEXT NOT NULL, 
                        selectedOption INTEGER NOT NULL, 
                        isCorrect INTEGER NOT NULL, 
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO answers_new (id, testId, questionText, selectedOption, isCorrect, timestamp)
                    SELECT CAST(id AS TEXT), CAST(testId AS TEXT), questionText, selectedOption, isCorrect, timestamp FROM answers
                """.trimIndent())
                database.execSQL("DROP TABLE answers")
                database.execSQL("ALTER TABLE answers_new RENAME TO answers")
            }
        }
    }
}