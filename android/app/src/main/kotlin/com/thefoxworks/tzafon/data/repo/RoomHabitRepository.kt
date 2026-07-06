package com.thefoxworks.tzafon.data.repo

import com.thefoxworks.tzafon.data.db.HabitDao
import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.LogSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed HabitRepository (DM-HABIT; the DM-ATTR-2 habit ledger). */
class RoomHabitRepository(
    private val dao: HabitDao,
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeLogs(): Flow<List<HabitLog>> =
        dao.observeLogs().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getHabit(id: String): Habit? = dao.get(id)?.toDomain()

    override suspend fun upsert(habit: Habit) = dao.upsert(habit.toEntity())

    override suspend fun delete(id: String) {
        dao.deleteLogsFor(id) // DM-HABIT-7 — history goes with it
        dao.delete(id)
    }

    override suspend fun logDirect(habitId: String, date: String, done: Boolean, amount: Double?) {
        if (done) {
            dao.upsertLog(
                HabitLog(
                    habitId = habitId, date = date, done = true,
                    amount = amount, source = LogSource.DIRECT,
                ).toEntity(),
            )
        } else {
            dao.deleteLog(habitId, date)
        }
    }

    override suspend fun logForTask(habitId: String, date: String, taskId: String, amount: Double?) {
        dao.upsertLog(
            HabitLog(
                habitId = habitId, date = date, done = true,
                amount = amount, source = LogSource.TASK, sourceTaskId = taskId,
            ).toEntity(),
        )
    }

    override suspend fun reverseForTask(taskId: String) = dao.deleteLogsForTask(taskId)
}
