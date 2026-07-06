package com.thefoxworks.tzafon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("SELECT * FROM tasks WHERE seriesId = :seriesId")
    suspend fun occurrencesOf(seriesId: String): List<TaskEntity>

    @Query("UPDATE tasks SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Long)

    @Transaction
    suspend fun setSortOrders(orders: Map<String, Long>) {
        for ((id, order) in orders) setSortOrder(id, order)
    }
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series")
    fun observeAll(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series")
    suspend fun getAll(): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun get(id: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(series: SeriesEntity)

    @Query("DELETE FROM series WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM habit_logs")
    fun observeLogs(): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: String, date: String)

    /** DM-HABIT-7 — delete is destructive: the whole history goes with it. */
    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsFor(habitId: String)

    /** Exact reversal of a task's ledger rows (DM-ATTR-2). */
    @Query("DELETE FROM habit_logs WHERE sourceTaskId = :taskId AND source = 'TASK'")
    suspend fun deleteLogsForTask(taskId: String)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: String)

    // ── the contributions ledger (DM-ATTR-1 / NFR-DATA-2) ──

    @Query("SELECT * FROM contributions")
    fun observeContributions(): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE taskId = :taskId")
    suspend fun contributionsFor(taskId: String): List<ContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(row: ContributionEntity)

    @Query("DELETE FROM contributions WHERE taskId = :taskId")
    suspend fun deleteContributionsFor(taskId: String)

    @Query("DELETE FROM contributions WHERE goalId = :goalId")
    suspend fun deleteContributionsForGoal(goalId: String)
}

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes")
    fun observeAll(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun get(id: String): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(theme: ThemeEntity)

    @Query("DELETE FROM themes WHERE id = :id")
    suspend fun delete(id: String)
}
