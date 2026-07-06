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
    entities = [
        TaskEntity::class, SeriesEntity::class,
        HabitEntity::class, HabitLogEntity::class,
        GoalEntity::class, ContributionEntity::class,
        ThemeEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class TzafonDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun seriesDao(): SeriesDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun themeDao(): ThemeDao

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

        /** M5 — goals + the contributions ledger (DM-GOAL, DM-ATTR). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `goals` (
                        `id` TEXT NOT NULL, `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `type` TEXT NOT NULL DEFAULT 'GENERIC',
                        `steps` TEXT NOT NULL DEFAULT '',
                        `targetQty` REAL NOT NULL DEFAULT 0,
                        `unit` TEXT, `currentQty` REAL NOT NULL DEFAULT 0,
                        `state` TEXT NOT NULL DEFAULT 'ONGOING',
                        `deadline` TEXT, `commitment` TEXT, `primaryThemeId` TEXT,
                        `lastActivityAt` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`))""",
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `contributions` (
                        `id` TEXT NOT NULL, `taskId` TEXT NOT NULL,
                        `goalId` TEXT NOT NULL, `amount` REAL NOT NULL,
                        `via` TEXT NOT NULL DEFAULT 'DIRECT',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`))""",
                )
            }
        }

        /** M6 — themes + the shared-serve sets on goals/habits (DM-THEME). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `themes` (
                        `id` TEXT NOT NULL, `name` TEXT NOT NULL, `why` TEXT NOT NULL,
                        `windowStart` TEXT NOT NULL, `windowEnd` TEXT NOT NULL,
                        `state` TEXT NOT NULL DEFAULT 'UPCOMING',
                        `archivedOutcome` TEXT, `renewedToThemeId` TEXT,
                        `accentSlot` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `archivedAt` INTEGER,
                        PRIMARY KEY(`id`))""",
                )
                db.execSQL("ALTER TABLE `goals` ADD COLUMN `themeIds` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `themeIds` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun build(context: Context): TzafonDatabase =
            Room.databaseBuilder(context, TzafonDatabase::class.java, "tzafon.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
