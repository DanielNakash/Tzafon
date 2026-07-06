package com.thefoxworks.tzafon.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema evolution is additive and default-tolerant (NFR-DATA-1): later
 * milestones add tables/columns via destructive-free migrations.
 */
@Database(
    entities = [TaskEntity::class, SeriesEntity::class, HabitEntity::class, HabitLogEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class TzafonDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun seriesDao(): SeriesDao
    abstract fun habitDao(): HabitDao

    companion object {
        /** M4 — habits + per-date logs (DM-HABIT), purely additive. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `habits` (
                        `id` TEXT NOT NULL, `name` TEXT NOT NULL,
                        `kind` TEXT NOT NULL DEFAULT 'FREQUENCY',
                        `target` REAL NOT NULL DEFAULT 3.0,
                        `unit` TEXT, `targetDays` INTEGER,
                        `cueType` TEXT, `cueLabel` TEXT, `cueTime` TEXT,
                        `primaryThemeId` TEXT, `goalId` TEXT,
                        `startedAt` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`))""",
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `habit_logs` (
                        `habitId` TEXT NOT NULL, `date` TEXT NOT NULL,
                        `done` INTEGER NOT NULL DEFAULT 1,
                        `amount` REAL,
                        `source` TEXT NOT NULL DEFAULT 'DIRECT',
                        `sourceTaskId` TEXT,
                        PRIMARY KEY(`habitId`, `date`))""",
                )
            }
        }

        fun build(context: Context): TzafonDatabase =
            Room.databaseBuilder(context, TzafonDatabase::class.java, "tzafon.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
