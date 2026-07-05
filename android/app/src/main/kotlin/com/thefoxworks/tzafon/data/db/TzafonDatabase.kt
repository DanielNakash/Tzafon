package com.thefoxworks.tzafon.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema evolution is additive and default-tolerant (NFR-DATA-1): later
 * milestones add tables/columns via destructive-free migrations.
 */
@Database(
    entities = [TaskEntity::class, SeriesEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TzafonDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun seriesDao(): SeriesDao

    companion object {
        fun build(context: Context): TzafonDatabase =
            Room.databaseBuilder(context, TzafonDatabase::class.java, "tzafon.db")
                .build()
    }
}
