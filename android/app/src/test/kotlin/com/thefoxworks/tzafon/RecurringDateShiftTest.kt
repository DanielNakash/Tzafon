package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FR-REC-6 — a date change on a recurring occurrence (FORWARD / ALL) actually
 * shifts the series' phase. Exercises [RoomTaskRepository.saveDraft] via
 * in-memory DAO fakes so the anchor arithmetic is verified without an emulator.
 */
class RecurringDateShiftTest {

    private val intervalTwoDays = Rule(
        pattern = Pattern.INTERVAL,
        interval = 2,
        unit = IntervalUnit.DAY,
    )

    /** Seed a "Cleaning" series generated at start, start+2, start+4, … up to horizon. */
    private suspend fun seedSeries(taskDao: FakeTaskDao, seriesDao: FakeSeriesDao, today: String, start: String): Series {
        val series = Series(
            id = "S1",
            title = "Cleaning",
            rule = intervalTwoDays,
            ruleAnchor = start,
            startDate = start,
            generatedThrough = start,
        )
        seriesDao.upsert(series.toEntity())
        val repo = RoomTaskRepository(taskDao, seriesDao)
        repo.topUp(today, 60)
        return series
    }

    private fun seriesOccurrenceDates(taskDao: FakeTaskDao, seriesId: String): List<String> =
        taskDao.rows.value.values
            .filter { it.seriesId == seriesId }
            .mapNotNull { it.occurrenceDate }
            .sorted()

    @Test
    fun `FR-REC-6 FORWARD shifts the series phase by the user's new date`() = runBlocking {
        val today = "2026-12-02"
        val start = "2026-12-01"
        val taskDao = FakeTaskDao()
        val seriesDao = FakeSeriesDao()
        seedSeries(taskDao, seriesDao, today, start)
        val repo = RoomTaskRepository(taskDao, seriesDao)

        // pre-shift: expect 12-01, 12-03, 12-05, 12-07, …
        val before = seriesOccurrenceDates(taskDao, "S1")
        assertTrue("expected 12-03 in pre-shift set: $before", "2026-12-03" in before)

        val existing = taskDao.get("S1__2026-12-03")?.toDomain()
        assertNotNull("seed produced the 12-03 occurrence", existing)

        val draft = TaskDraft(
            id = existing!!.id,
            title = "Cleaning",
            toDoDate = "2026-12-04",
            recurrence = RecurrenceDraft(rule = intervalTwoDays),
        )
        repo.saveDraft(draft, EditScope.FORWARD, today)

        val after = seriesOccurrenceDates(taskDao, "S1")
        // 12-01 keeps; 12-03/05/07 gone; regenerated at 12-04/06/08 …
        assertTrue("kept pre-shift 12-01: $after", "2026-12-01" in after)
        assertTrue("stale 12-03 pruned: $after", "2026-12-03" !in after)
        assertTrue("stale 12-05 pruned: $after", "2026-12-05" !in after)
        assertTrue("new anchor 12-04 present: $after", "2026-12-04" in after)
        assertTrue("new 12-06 present: $after", "2026-12-06" in after)
        assertTrue("new 12-08 present: $after", "2026-12-08" in after)

        val updated = seriesDao.get("S1")?.toDomain()
        assertEquals("2026-12-04", updated?.ruleAnchor)
        assertEquals("startDate stays on FORWARD", "2026-12-01", updated?.startDate)
    }

    @Test
    fun `FR-REC-6 ALL rewrites startDate and ruleAnchor to the new date`() = runBlocking {
        val today = "2026-12-02"
        val start = "2026-12-01"
        val taskDao = FakeTaskDao()
        val seriesDao = FakeSeriesDao()
        seedSeries(taskDao, seriesDao, today, start)
        val repo = RoomTaskRepository(taskDao, seriesDao)

        val existing = taskDao.get("S1__2026-12-03")?.toDomain()!!
        val draft = TaskDraft(
            id = existing.id,
            title = "Cleaning",
            toDoDate = "2026-12-04",
            recurrence = RecurrenceDraft(rule = intervalTwoDays),
        )
        repo.saveDraft(draft, EditScope.ALL, today)

        val after = seriesOccurrenceDates(taskDao, "S1")
        assertTrue("pre-anchor 12-01 open row is cleared on ALL: $after", "2026-12-01" !in after)
        assertTrue("new anchor 12-04 present: $after", "2026-12-04" in after)
        assertTrue("new 12-06 present: $after", "2026-12-06" in after)
        assertTrue("new 12-08 present: $after", "2026-12-08" in after)

        val updated = seriesDao.get("S1")?.toDomain()
        assertEquals("2026-12-04", updated?.ruleAnchor)
        assertEquals("2026-12-04", updated?.startDate)
    }

    @Test
    fun `FR-REC-6 backward shift also takes effect (FORWARD)`() = runBlocking {
        val today = "2026-12-02"
        val start = "2026-12-01"
        val taskDao = FakeTaskDao()
        val seriesDao = FakeSeriesDao()
        seedSeries(taskDao, seriesDao, today, start)
        val repo = RoomTaskRepository(taskDao, seriesDao)

        val existing = taskDao.get("S1__2026-12-05")?.toDomain()!!
        val draft = TaskDraft(
            id = existing.id,
            title = "Cleaning",
            toDoDate = "2026-12-04",
            recurrence = RecurrenceDraft(rule = intervalTwoDays),
        )
        repo.saveDraft(draft, EditScope.FORWARD, today)

        val after = seriesOccurrenceDates(taskDao, "S1")
        assertTrue("kept 12-01: $after", "2026-12-01" in after)
        assertTrue("kept 12-03: $after", "2026-12-03" in after)
        assertTrue("stale 12-05 pruned: $after", "2026-12-05" !in after)
        assertTrue("stale 12-07 pruned: $after", "2026-12-07" !in after)
        assertTrue("new 12-04 present: $after", "2026-12-04" in after)
        assertTrue("new 12-06 present: $after", "2026-12-06" in after)
    }

    @Test
    fun `FR-REC-6 no-op date change preserves the current schedule`() = runBlocking {
        val today = "2026-12-02"
        val start = "2026-12-01"
        val taskDao = FakeTaskDao()
        val seriesDao = FakeSeriesDao()
        seedSeries(taskDao, seriesDao, today, start)
        val repo = RoomTaskRepository(taskDao, seriesDao)

        val existing = taskDao.get("S1__2026-12-05")?.toDomain()!!
        // no date change — title-only edit with FORWARD
        val draft = TaskDraft(
            id = existing.id,
            title = "Cleaning up",
            toDoDate = existing.occurrenceDate,
            recurrence = RecurrenceDraft(rule = intervalTwoDays),
        )
        repo.saveDraft(draft, EditScope.FORWARD, today)

        val after = seriesOccurrenceDates(taskDao, "S1")
        assertTrue("dates unchanged for no-op: 12-01/03/05/07/09 all present: $after",
            listOf("2026-12-01", "2026-12-03", "2026-12-05", "2026-12-07", "2026-12-09").all { it in after })
        assertEquals("v2.13.0: an edit that moves no date leaves the rule anchor alone", "2026-12-01", seriesDao.get("S1")?.toDomain()?.ruleAnchor)
    }
}
