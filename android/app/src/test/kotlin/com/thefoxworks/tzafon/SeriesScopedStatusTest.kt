package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.13.0 — series-scoped status changes (FR-REC-5 / DM-TASK-2 refined):
 * freezing, thawing, closing and reopening a whole series, exercised through
 * [RoomTaskRepository.setState] over in-memory DAOs.
 *
 * Every scenario is a daily series ("Water plants") starting 2026-09-20.
 */
class SeriesScopedStatusTest {

    private val taskDao = FakeTaskDao()
    private val seriesDao = FakeSeriesDao()
    private val repo = RoomTaskRepository(taskDao, seriesDao, now = { 1L })

    private val daily = Rule(pattern = Pattern.INTERVAL, interval = 1, unit = IntervalUnit.DAY)

    /** Seed the series and materialize its occurrences as the app would on [asOf]. */
    private suspend fun seed(asOf: String, frozen: Boolean = false) {
        seriesDao.upsert(
            Series(
                id = "S1",
                title = "Water plants",
                rule = daily,
                ruleAnchor = "2026-09-20",
                startDate = "2026-09-20",
                generatedThrough = "2026-09-20",
                frozen = frozen,
            ).toEntity(),
        )
        repo.topUp(asOf, 30)
    }

    private fun id(date: String) = "S1__$date"
    private suspend fun occ(date: String): Task? = taskDao.get(id(date))?.toDomain()
    private suspend fun series(): Series = seriesDao.get("S1")!!.toDomain()
    private suspend fun edit(date: String, change: (Task) -> Task) {
        taskDao.upsert(change(occ(date)!!).toEntity())
    }

    private fun dates(): List<String> =
        taskDao.rows.value.values.mapNotNull { it.occurrenceDate }.sorted()

    // ── freeze ────────────────────────────────────────────────

    @Test
    fun `freezing deletes every later open occurrence, edited or not, and keeps the rest`() = runBlocking {
        seed(asOf = "2026-09-26")
        edit("2026-09-28") { it.copy(title = "Water the ferns", overridden = true) }
        edit("2026-09-29") { it.copy(state = TaskState.DONE) }
        edit("2026-09-30") { it.copy(state = TaskState.CLOSED) }

        repo.setState(id("2026-09-26"), TaskState.FROZEN, today = "2026-09-26")

        assertTrue(series().frozen)
        assertEquals(TaskState.FROZEN, occ("2026-09-26")?.state)
        assertEquals("earlier ones untouched", TaskState.OPEN, occ("2026-09-24")?.state)
        assertNull("later open deleted", occ("2026-09-27"))
        assertNull("later edited open deleted too", occ("2026-09-28"))
        assertEquals("later done kept", TaskState.DONE, occ("2026-09-29")?.state)
        assertEquals("later closed kept", TaskState.CLOSED, occ("2026-09-30")?.state)
        assertNull(occ("2026-10-01"))

        repo.topUp("2026-09-26", 30)
        assertNull("a frozen series generates nothing", occ("2026-10-01"))
    }

    @Test
    fun `freezing a leftover frozen occurrence of a live series still freezes the series`() = runBlocking {
        seed(asOf = "2026-09-26")
        edit("2026-09-26") { it.copy(state = TaskState.FROZEN) }

        repo.setState(id("2026-09-26"), TaskState.FROZEN, today = "2026-09-26")

        assertTrue(series().frozen)
        assertNull(occ("2026-09-27"))
    }

    // ── thaw ──────────────────────────────────────────────────

    /** Frozen on 09-22 from its 09-22 occurrence; the app is now opened on 09-26. */
    private suspend fun frozenSinceTheTwentySecond() {
        seed(asOf = "2026-09-22")
        repo.setState(id("2026-09-22"), TaskState.FROZEN, today = "2026-09-22")
    }

    @Test
    fun `thaw this one changes only the occurrence and the series stays frozen`() = runBlocking {
        frozenSinceTheTwentySecond()

        repo.setState(id("2026-09-22"), TaskState.OPEN, today = "2026-09-26", scope = StatusScope.THIS_ONE)

        assertEquals(TaskState.OPEN, occ("2026-09-22")?.state)
        assertTrue(series().frozen)
        assertNull(occ("2026-09-26"))
    }

    @Test
    fun `thaw whole series fills it in from today and the frozen days never come back`() = runBlocking {
        frozenSinceTheTwentySecond()

        repo.setState(id("2026-09-22"), TaskState.OPEN, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)
        repo.topUp("2026-09-26", 30)

        assertFalse(series().frozen)
        assertEquals("keeps its date", TaskState.OPEN, occ("2026-09-22")?.state)
        for (gone in listOf("2026-09-23", "2026-09-24", "2026-09-25")) assertNull(gone, occ(gone))
        assertEquals(TaskState.OPEN, occ("2026-09-26")?.state)
        assertEquals(TaskState.OPEN, occ("2026-09-27")?.state)
    }

    @Test
    fun `thaw whole series to done counts the occurrence and thaws the series`() = runBlocking {
        frozenSinceTheTwentySecond()

        repo.setState(id("2026-09-22"), TaskState.DONE, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)

        assertEquals(TaskState.DONE, occ("2026-09-22")?.state)
        assertFalse(series().frozen)
        assertEquals(TaskState.OPEN, occ("2026-09-26")?.state)
    }

    @Test
    fun `a leftover frozen occurrence of a live series thaws alone`() = runBlocking {
        seed(asOf = "2026-09-26")
        edit("2026-09-24") { it.copy(state = TaskState.FROZEN) }
        val before = dates()

        repo.setState(id("2026-09-24"), TaskState.OPEN, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)

        assertEquals(TaskState.OPEN, occ("2026-09-24")?.state)
        assertEquals(before, dates())
        assertEquals(series().exceptions, emptyList<String>())
    }

    // ── close ─────────────────────────────────────────────────

    @Test
    fun `close whole series ends it today, closes what is not done and drops what is later`() = runBlocking {
        seed(asOf = "2026-09-26")
        edit("2026-09-21") { it.copy(state = TaskState.FROZEN) }
        edit("2026-09-22") { it.copy(title = "Water the ferns", overridden = true) }
        edit("2026-09-23") { it.copy(state = TaskState.DONE) }
        edit("2026-09-28") { it.copy(state = TaskState.DONE) }
        edit("2026-09-29") { it.copy(title = "Water the ferns", overridden = true) }

        repo.setState(id("2026-09-24"), TaskState.CLOSED, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)
        repo.topUp("2026-09-26", 30)

        assertEquals("2026-09-26", series().endDate)
        for (d in listOf("2026-09-20", "2026-09-21", "2026-09-22", "2026-09-24", "2026-09-25", "2026-09-26")) {
            assertEquals(d, TaskState.CLOSED, occ(d)?.state)
        }
        assertEquals("done is never touched", TaskState.DONE, occ("2026-09-23")?.state)
        assertEquals("done is never touched", TaskState.DONE, occ("2026-09-28")?.state)
        assertNull(occ("2026-09-27"))
        assertNull("later edited ones go too", occ("2026-09-29"))
        assertNull("nothing past the end date", occ("2026-10-01"))
    }

    @Test
    fun `close whole series from a later occurrence still ends it today`() = runBlocking {
        seed(asOf = "2026-09-26")

        repo.setState(id("2026-09-30"), TaskState.CLOSED, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)

        assertEquals("2026-09-26", series().endDate)
        assertEquals(TaskState.CLOSED, occ("2026-09-26")?.state)
        assertNull(occ("2026-09-30"))
    }

    @Test
    fun `close whole series on an occurrence already closed still closes the series`() = runBlocking {
        seed(asOf = "2026-09-26")
        edit("2026-09-26") { it.copy(state = TaskState.CLOSED) }

        repo.setState(id("2026-09-26"), TaskState.CLOSED, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)

        assertEquals("2026-09-26", series().endDate)
        assertNull(occ("2026-09-27"))
    }

    @Test
    fun `closing a frozen series clears the frozen mark`() = runBlocking {
        frozenSinceTheTwentySecond()

        repo.setState(id("2026-09-22"), TaskState.CLOSED, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)

        assertFalse(series().frozen)
        assertEquals("2026-09-26", series().endDate)
        assertEquals(TaskState.CLOSED, occ("2026-09-22")?.state)
    }

    @Test
    fun `close this one settles only the occurrence`() = runBlocking {
        seed(asOf = "2026-09-26")

        repo.setState(id("2026-09-26"), TaskState.CLOSED, today = "2026-09-26", scope = StatusScope.THIS_ONE)

        assertEquals(TaskState.CLOSED, occ("2026-09-26")?.state)
        assertNull(series().endDate)
        assertEquals(TaskState.OPEN, occ("2026-09-27")?.state)
    }

    // ── reopen ────────────────────────────────────────────────

    /** Closed as a whole series on 09-22; the app is now opened on 09-26. */
    private suspend fun closedOnTheTwentySecond() {
        seed(asOf = "2026-09-22")
        repo.setState(id("2026-09-22"), TaskState.CLOSED, today = "2026-09-22", scope = StatusScope.WHOLE_SERIES)
    }

    @Test
    fun `reopen whole series clears the end date and resumes from today, past untouched`() = runBlocking {
        closedOnTheTwentySecond()

        repo.setState(id("2026-09-21"), TaskState.OPEN, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)
        repo.topUp("2026-09-26", 30)

        assertNull("ends never", series().endDate)
        assertEquals(TaskState.OPEN, occ("2026-09-21")?.state)
        assertEquals("closed by the close, stays closed", TaskState.CLOSED, occ("2026-09-20")?.state)
        assertEquals("closed by the close, stays closed", TaskState.CLOSED, occ("2026-09-22")?.state)
        for (gone in listOf("2026-09-23", "2026-09-24", "2026-09-25")) assertNull(gone, occ(gone))
        assertEquals(TaskState.OPEN, occ("2026-09-26")?.state)
        assertEquals(TaskState.OPEN, occ("2026-09-27")?.state)
    }

    @Test
    fun `reopen whole series to done counts the occurrence and reopens the series`() = runBlocking {
        closedOnTheTwentySecond()

        repo.setState(id("2026-09-22"), TaskState.DONE, today = "2026-09-26", scope = StatusScope.WHOLE_SERIES)

        assertNull(series().endDate)
        assertEquals(TaskState.DONE, occ("2026-09-22")?.state)
        assertEquals(TaskState.OPEN, occ("2026-09-26")?.state)
    }

    @Test
    fun `reopen this one changes only the occurrence and the series stays closed`() = runBlocking {
        closedOnTheTwentySecond()

        repo.setState(id("2026-09-22"), TaskState.OPEN, today = "2026-09-26", scope = StatusScope.THIS_ONE)

        assertEquals(TaskState.OPEN, occ("2026-09-22")?.state)
        assertEquals("2026-09-22", series().endDate)
        assertNull(occ("2026-09-26"))
    }
}
