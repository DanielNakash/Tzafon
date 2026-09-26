package com.thefoxworks.tzafon

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
