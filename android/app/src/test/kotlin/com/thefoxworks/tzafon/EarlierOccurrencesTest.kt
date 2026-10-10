package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v2.13.0 — the whole-series badge counts: occurrences dated before this one
 * (the save sheet), or only those not Done (the delete sheet). A daily series
 * from Sun 20 Sep 2026, generated through 30 Sep.
 */
class EarlierOccurrencesTest {

    private val taskDao = FakeTaskDao()
    private val seriesDao = FakeSeriesDao()
    private val repo = RoomTaskRepository(taskDao, seriesDao, now = { 1L })

    private fun seed() = runBlocking {
        seriesDao.upsert(
            Series(
                id = "S1",
                title = "Water plants",
                rule = Rule(pattern = Pattern.INTERVAL, interval = 1, unit = IntervalUnit.DAY),
                ruleAnchor = "2026-09-20",
                startDate = "2026-09-20",
                generatedThrough = "2026-09-20",
            ).toEntity(),
        )
        repo.topUp("2026-09-20", 10)
    }

    private fun markDone(date: String) = runBlocking {
        val t = taskDao.get("S1__$date")!!.toDomain()
        taskDao.upsert(t.copy(state = TaskState.DONE).toEntity())
    }

    @Test
    fun `the save count is every occurrence dated before this one, Done included`() = runBlocking {
        seed()
        markDone("2026-09-21")
        assertEquals(5, repo.countEarlierOccurrences("S1__2026-09-25", notDoneOnly = false))
    }

    @Test
    fun `the delete count leaves out Done occurrences`() = runBlocking {
        seed()
        markDone("2026-09-21")
        markDone("2026-09-22")
        assertEquals(3, repo.countEarlierOccurrences("S1__2026-09-25", notDoneOnly = true))
    }

    @Test
    fun `the first occurrence and a one-off task count 0`() = runBlocking {
        seed()
        taskDao.upsert(Task(id = "T1", title = "One-off", toDoDate = "2026-09-25").toEntity())
        assertEquals(0, repo.countEarlierOccurrences("S1__2026-09-20", notDoneOnly = false))
        assertEquals(0, repo.countEarlierOccurrences("T1", notDoneOnly = false))
    }
}
