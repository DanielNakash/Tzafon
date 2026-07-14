package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.HabitDao
import com.thefoxworks.tzafon.data.db.HabitEntity
import com.thefoxworks.tzafon.data.db.HabitLogEntity
import com.thefoxworks.tzafon.data.repo.RoomHabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FR-HAB-9.4 — un-log is a single write that clears the same fields the log
 * wrote. `logDirect(id, date, done = false, amount = null)` removes the log row
 * for that date; the goal contribution derived from that row (DM-ATTR-2) drops
 * with it because it is derived from the same row (no separate ledger to drift).
 *
 * Exercises the production [RoomHabitRepository] over an in-memory [HabitDao]
 * fake, so the both-directions toggle the UI now reaches on every surface is
 * verified without an emulator.
 */
class HabitUnlogTest {

    /** In-memory HabitDao — only the log methods are exercised here. */
    private class FakeHabitDao : HabitDao {
        val logs = MutableStateFlow<List<HabitLogEntity>>(emptyList())

        override fun observeAll(): Flow<List<HabitEntity>> = MutableStateFlow(emptyList())
        override suspend fun get(id: String): HabitEntity? = null
        override suspend fun upsert(habit: HabitEntity) {}
        override suspend fun delete(id: String) {}
        override fun observeLogs(): Flow<List<HabitLogEntity>> = logs
        override suspend fun upsertLog(log: HabitLogEntity) {
            logs.value = logs.value.filterNot { it.habitId == log.habitId && it.date == log.date } + log
        }
        override suspend fun deleteLog(habitId: String, date: String) {
            logs.value = logs.value.filterNot { it.habitId == habitId && it.date == date }
        }
        override suspend fun deleteLogsFor(habitId: String) {
            logs.value = logs.value.filterNot { it.habitId == habitId }
        }
        override suspend fun deleteLogsForTask(taskId: String) {
            logs.value = logs.value.filterNot { it.sourceTaskId == taskId }
        }
    }

    @Test
    fun `quantitative log then un-log clears the row atomically`() = runBlocking {
        val dao = FakeHabitDao()
        val repo = RoomHabitRepository(dao)

        repo.logDirect("h1", "2026-07-12", done = true, amount = 40.0)
        val afterLog = repo.observeLogs().first()
        assertEquals(1, afterLog.size)
        assertEquals(40.0, afterLog.single().amount!!, 0.0)
        assertTrue(afterLog.single().done)

        // FR-HAB-9.4 — a single un-log write removes the row entirely; amount and
        // done both go with it (no partial un-log, no orphaned contribution row).
        repo.logDirect("h1", "2026-07-12", done = false, amount = null)
        val afterUnlog = repo.observeLogs().first()
        assertTrue(afterUnlog.isEmpty())
    }

    @Test
    fun `frequency log then un-log leaves other dates untouched`() = runBlocking {
        val dao = FakeHabitDao()
        val repo = RoomHabitRepository(dao)

        repo.logDirect("h1", "2026-07-11", done = true, amount = null)
        repo.logDirect("h1", "2026-07-12", done = true, amount = null)
        assertEquals(2, repo.observeLogs().first().size)

        repo.logDirect("h1", "2026-07-12", done = false, amount = null)
        val remaining = repo.observeLogs().first()
        assertEquals(listOf("2026-07-11"), remaining.map { it.date })
    }

    @Test
    fun `un-logging a date that was never logged is a no-op`() = runBlocking {
        val dao = FakeHabitDao()
        val repo = RoomHabitRepository(dao)

        repo.logDirect("h1", "2026-07-12", done = false, amount = null)
        assertTrue(repo.observeLogs().first().isEmpty())
        assertNull(repo.observeLogs().first().firstOrNull())
    }
}
