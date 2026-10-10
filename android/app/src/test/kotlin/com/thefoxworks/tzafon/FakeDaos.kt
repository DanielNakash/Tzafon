package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.ContributionEntity
import com.thefoxworks.tzafon.data.db.GoalDao
import com.thefoxworks.tzafon.data.db.GoalEntity
import com.thefoxworks.tzafon.data.db.HabitDao
import com.thefoxworks.tzafon.data.db.HabitEntity
import com.thefoxworks.tzafon.data.db.HabitLogEntity
import com.thefoxworks.tzafon.data.db.SeriesDao
import com.thefoxworks.tzafon.data.db.SeriesEntity
import com.thefoxworks.tzafon.data.db.TaskDao
import com.thefoxworks.tzafon.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory DAO fakes, so [com.thefoxworks.tzafon.data.repo.RoomTaskRepository]
 * semantics are verified without an emulator.
 */
class FakeTaskDao : TaskDao {
    val rows = MutableStateFlow<Map<String, TaskEntity>>(emptyMap())
    override fun observeAll(): Flow<List<TaskEntity>> = MutableStateFlow(rows.value.values.toList())
    override suspend fun getAll(): List<TaskEntity> = rows.value.values.toList()
    override suspend fun get(id: String): TaskEntity? = rows.value[id]
    override suspend fun upsert(task: TaskEntity) { rows.value = rows.value + (task.id to task) }
    override suspend fun upsertAll(tasks: List<TaskEntity>) { rows.value = rows.value + tasks.associateBy { it.id } }
    override suspend fun update(task: TaskEntity) { rows.value = rows.value + (task.id to task) }
    override suspend fun delete(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll(ids: List<String>) { rows.value = rows.value - ids.toSet() }
    override suspend fun occurrencesOf(seriesId: String): List<TaskEntity> =
        rows.value.values.filter { it.seriesId == seriesId }
    override suspend fun setSortOrder(id: String, order: Long) {
        rows.value[id]?.let { rows.value = rows.value + (it.id to it.copy(sortOrder = order)) }
    }
}

class FakeSeriesDao : SeriesDao {
    val rows = MutableStateFlow<Map<String, SeriesEntity>>(emptyMap())
    override fun observeAll(): Flow<List<SeriesEntity>> = MutableStateFlow(rows.value.values.toList())
    override suspend fun getAll(): List<SeriesEntity> = rows.value.values.toList()
    override suspend fun get(id: String): SeriesEntity? = rows.value[id]
    override suspend fun upsert(series: SeriesEntity) { rows.value = rows.value + (series.id to series) }
    override suspend fun delete(id: String) { rows.value = rows.value - id }
}

class FakeHabitDao : HabitDao {
    val habits = MutableStateFlow<Map<String, HabitEntity>>(emptyMap())
    /** keyed by (habitId, date), the table's primary key */
    val logs = MutableStateFlow<Map<Pair<String, String>, HabitLogEntity>>(emptyMap())
    override fun observeAll(): Flow<List<HabitEntity>> = MutableStateFlow(habits.value.values.toList())
    override suspend fun get(id: String): HabitEntity? = habits.value[id]
    override suspend fun upsert(habit: HabitEntity) { habits.value = habits.value + (habit.id to habit) }
    override suspend fun delete(id: String) { habits.value = habits.value - id }
    override fun observeLogs(): Flow<List<HabitLogEntity>> = MutableStateFlow(logs.value.values.toList())
    override suspend fun upsertLog(log: HabitLogEntity) { logs.value = logs.value + ((log.habitId to log.date) to log) }
    override suspend fun deleteLog(habitId: String, date: String) { logs.value = logs.value - (habitId to date) }
    override suspend fun deleteLogsFor(habitId: String) { logs.value = logs.value.filterKeys { it.first != habitId } }
    override suspend fun deleteLogsForTask(taskId: String) {
        logs.value = logs.value.filterValues { !(it.sourceTaskId == taskId && it.source == "TASK") }
    }
    override suspend fun logsForTask(taskId: String): List<HabitLogEntity> =
        logs.value.values.filter { it.sourceTaskId == taskId && it.source == "TASK" }
}

class FakeGoalDao : GoalDao {
    val goals = MutableStateFlow<Map<String, GoalEntity>>(emptyMap())
    val contributions = MutableStateFlow<Map<String, ContributionEntity>>(emptyMap())
    override fun observeAll(): Flow<List<GoalEntity>> = MutableStateFlow(goals.value.values.toList())
    override suspend fun get(id: String): GoalEntity? = goals.value[id]
    override suspend fun upsert(goal: GoalEntity) { goals.value = goals.value + (goal.id to goal) }
    override suspend fun delete(id: String) { goals.value = goals.value - id }
    override fun observeContributions(): Flow<List<ContributionEntity>> = MutableStateFlow(contributions.value.values.toList())
    override suspend fun contributionsFor(taskId: String): List<ContributionEntity> =
        contributions.value.values.filter { it.taskId == taskId }
    override suspend fun insertContribution(row: ContributionEntity) { contributions.value = contributions.value + (row.id to row) }
    override suspend fun deleteContributionsFor(taskId: String) {
        contributions.value = contributions.value.filterValues { it.taskId != taskId }
    }
    override suspend fun deleteContributionsForGoal(goalId: String) {
        contributions.value = contributions.value.filterValues { it.goalId != goalId }
    }
    override suspend fun deleteContribution(id: String) { contributions.value = contributions.value - id }
}
