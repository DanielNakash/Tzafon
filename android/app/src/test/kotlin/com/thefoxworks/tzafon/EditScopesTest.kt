package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * v2.13.0 — edit and delete scopes on a repeating task, through
 * [RoomTaskRepository.saveDraft] and [RoomTaskRepository.deleteTask] over
 * in-memory DAOs.
 *
 * Unless a test says otherwise, the series is "Run" every Tuesday from
 * 2026-10-06, seen on Wednesday 2026-10-14.
 */
class EditScopesTest {

    private val today = "2026-10-14"
    private val taskDao = FakeTaskDao()
    private val seriesDao = FakeSeriesDao()
    private val ticks = TickLedger()
    private var clock = 1L
    private val repo = RoomTaskRepository(taskDao, seriesDao, habits = ticks, now = { clock })

    /** Records what Done ticks: (habit, task, amount). */
    private class TickLedger : HabitRepository {
        val logged = mutableListOf<Triple<String, String, Double?>>()
        override fun observeHabits(): Flow<List<Habit>> = emptyFlow()
        override fun observeLogs(): Flow<List<HabitLog>> = emptyFlow()
        override suspend fun getHabit(id: String): Habit? = null
        override suspend fun upsert(habit: Habit) = Unit
        override suspend fun delete(id: String) = Unit
        override suspend fun logDirect(habitId: String, date: String, done: Boolean, amount: Double?) = Unit
        override suspend fun logForTask(habitId: String, date: String, taskId: String, amount: Double?) {
            logged += Triple(habitId, taskId, amount)
        }
        override suspend fun reverseForTask(taskId: String) = Unit
    }

    private val tuesdays = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2))

    private suspend fun seed(rule: Rule = tuesdays, start: String = "2026-10-06") {
        seriesDao.upsert(
            Series(id = "S1", title = "Run", rule = rule, ruleAnchor = start, startDate = start, generatedThrough = start)
                .toEntity(),
        )
        repo.topUp(today, 60)
    }

    private fun occ(date: String) = "S1__$date"

    private suspend fun task(id: String): Task? = taskDao.get(id)?.toDomain()

    private suspend fun markDone(id: String) = taskDao.upsert(task(id)!!.copy(state = TaskState.DONE).toEntity())

    /** The draft the editor opens on, as it reads the stored task. */
    private suspend fun opened(id: String): TaskDraft {
        val t = task(id)!!
        return TaskDraft.of(t, t.seriesId?.let { seriesDao.get(it)?.toDomain() })
    }

    private fun seriesDates(): List<String> =
        taskDao.rows.value.values.filter { it.seriesId == "S1" }.mapNotNull { it.occurrenceDate }.sorted()

    // ── turning repeat off ───────────────────────────────────

    @Test
    fun `turning repeat off makes this one a one-off and ends the series the day before`() = runBlocking {
        seed()
        markDone(occ("2026-11-03"))

        repo.saveDraft(opened(occ("2026-10-20")).copy(title = "Run 5k", recurrence = null), EditScope.FORWARD, today)

        val oneOff = task(occ("2026-10-20"))!!
        assertNull(oneOff.seriesId)
        assertNull(oneOff.occurrenceDate)
        assertEquals("Run 5k", oneOff.title)
        assertEquals("2026-10-20", oneOff.toDoDate)
        assertEquals("2026-10-19", seriesDao.get("S1")?.toDomain()?.endDate)
        // earlier ones untouched, later ones go unless Done
        assertEquals(listOf("2026-10-06", "2026-10-13", "2026-11-03"), seriesDates())
        assertEquals("Run", task(occ("2026-10-13"))!!.title)

        repo.topUp(today, 60)
        assertEquals(listOf("2026-10-06", "2026-10-13", "2026-11-03"), seriesDates())
    }

    // ── scopes the changes don't allow ───────────────────────

    @Test
    fun `a repeat card change under just this one is refused and nothing is written`() = runBlocking {
        seed()
        val before = taskDao.rows.value to seriesDao.rows.value
        val wednesdays = opened(occ("2026-10-20")).let { it.copy(recurrence = it.recurrence!!.copy(rule = tuesdays.copy(weekdays = listOf(3)))) }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.saveDraft(wednesdays, EditScope.ONE, today) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.saveDraft(opened(occ("2026-10-20")).copy(recurrence = null), EditScope.ALL, today) }
        }
        assertEquals(before, taskDao.rows.value to seriesDao.rows.value)
    }

    // ── moving a to-do date (FR-REC-6 refined) ───────────────

    @Test
    fun `on a weekly rule a forward move shifts only this one, while its other edits reach on`() = runBlocking {
        seed(rule = tuesdays.copy(weekInterval = 2))
        assertEquals(listOf("2026-10-06", "2026-10-20", "2026-11-03"), seriesDates().take(3))

        repo.saveDraft(opened(occ("2026-10-20")).copy(title = "Run 5k", toDoDate = "2026-10-22"), EditScope.FORWARD, today)

        val moved = task(occ("2026-10-20"))!!
        assertEquals("2026-10-22", moved.toDoDate)
        assertEquals("Run 5k", moved.title)
        assertEquals(true, moved.overridden)
        val series = seriesDao.get("S1")?.toDomain()!!
        assertEquals("2026-10-06", series.ruleAnchor)
        assertEquals(tuesdays.copy(weekInterval = 2), series.rule)
        assertEquals("2026-11-03", task(occ("2026-11-03"))!!.toDoDate)
        assertEquals("Run 5k", task(occ("2026-11-03"))!!.title)
        assertEquals("Run", task(occ("2026-10-06"))!!.title)
    }

    @Test
    fun `on a monthly rule a whole-series move shifts only this one`() = runBlocking {
        val every2Months = Rule(pattern = Pattern.MONTHDAY, monthDate = 15, monthInterval = 2)
        seed(rule = every2Months, start = "2026-09-15")
        assertEquals(listOf("2026-09-15", "2026-11-15"), seriesDates().take(2))

        repo.saveDraft(opened(occ("2026-11-15")).copy(title = "Run 10k", toDoDate = "2026-11-18"), EditScope.ALL, today)

        assertEquals("2026-11-18", task(occ("2026-11-15"))!!.toDoDate)
        assertEquals("Run 10k", task(occ("2026-09-15"))!!.title)
        assertEquals("2026-09-15", seriesDao.get("S1")?.toDomain()?.ruleAnchor)
        assertEquals(listOf("2026-09-15", "2026-11-15"), seriesDates().take(2))
        seriesDates().drop(2).forEach { assertEquals("15", it.takeLast(2)) }
    }

    @Test
    fun `on an Every N days rule a forward move re-counts the rhythm from the new date`() = runBlocking {
        seed(rule = Rule(pattern = Pattern.INTERVAL, interval = 3, unit = com.thefoxworks.tzafon.domain.recurrence.IntervalUnit.DAY))
        assertEquals(listOf("2026-10-06", "2026-10-09", "2026-10-12", "2026-10-15"), seriesDates().take(4))

        repo.saveDraft(opened(occ("2026-10-15")).copy(toDoDate = "2026-10-16"), EditScope.FORWARD, today)

        assertEquals(listOf("2026-10-06", "2026-10-09", "2026-10-12", "2026-10-16", "2026-10-19"), seriesDates().take(5))
        assertEquals("2026-10-16", seriesDao.get("S1")?.toDomain()?.ruleAnchor)
    }

    // ── deleting ─────────────────────────────────────────────

    @Test
    fun `deleting forward ends the series the day before and keeps Done ones`() = runBlocking {
        seed()
        markDone(occ("2026-10-27"))

        repo.deleteTask(occ("2026-10-20"), EditScope.FORWARD)

        assertEquals("2026-10-19", seriesDao.get("S1")?.toDomain()?.endDate)
        assertEquals(listOf("2026-10-06", "2026-10-13", "2026-10-27"), seriesDates())
        repo.topUp(today, 60)
        assertEquals(listOf("2026-10-06", "2026-10-13", "2026-10-27"), seriesDates())
    }

    @Test
    fun `deleting the whole series keeps Done ones as one-off tasks`() = runBlocking {
        seed()
        markDone(occ("2026-10-06"))
        markDone(occ("2026-10-27"))

        repo.deleteTask(occ("2026-10-20"), EditScope.ALL)

        assertNull(seriesDao.get("S1"))
        assertEquals(setOf(occ("2026-10-06"), occ("2026-10-27")), taskDao.rows.value.keys)
        taskDao.rows.value.values.map { it.toDomain() }.forEach { kept ->
            assertNull(kept.seriesId)
            assertNull(kept.occurrenceDate)
            assertEquals(TaskState.DONE, kept.state)
        }
        assertEquals("2026-10-27", task(occ("2026-10-27"))!!.toDoDate)
    }

    @Test
    fun `deleting just this one still leaves the rest of the series`() = runBlocking {
        seed()
        repo.deleteTask(occ("2026-10-20"), EditScope.ONE)
        repo.topUp(today, 60)
        assertEquals(listOf("2026-10-06", "2026-10-13", "2026-10-27"), seriesDates().take(3))
        assertNull(seriesDao.get("S1")?.toDomain()?.endDate)
    }

    // ── status at Save ───────────────────────────────────────

    @Test
    fun `a status waiting in the draft applies after the field edits, with its amount`() = runBlocking {
        taskDao.upsert(Task(id = "T1", title = "Read", toDoDate = today, habitId = "H1").toEntity())

        repo.saveDraft(
            opened("T1").copy(title = "Read 20 pages", habitId = "H2", state = TaskState.DONE, habitAmount = 20.0),
            EditScope.ONE, today,
        )

        val t = task("T1")!!
        assertEquals("Read 20 pages", t.title)
        assertEquals(TaskState.DONE, t.state)
        assertEquals(listOf(Triple("H2", "T1", 20.0)), ticks.logged)
    }

    @Test
    fun `a status at Save reaches only this occurrence, whatever the scope`() = runBlocking {
        seed()
        repo.saveDraft(opened(occ("2026-10-20")).copy(title = "Run 5k", state = TaskState.DONE), EditScope.FORWARD, today)

        assertEquals(TaskState.DONE, task(occ("2026-10-20"))!!.state)
        assertEquals(TaskState.OPEN, task(occ("2026-10-27"))!!.state)
        assertEquals("Run 5k", task(occ("2026-10-27"))!!.title)
    }

    @Test
    fun `a status alone changes only the status, leaving the occurrence unedited`() = runBlocking {
        seed()
        repo.saveDraft(opened(occ("2026-10-20")).copy(state = TaskState.DONE), EditScope.ONE, today)

        val t = task(occ("2026-10-20"))!!
        assertEquals(TaskState.DONE, t.state)
        assertEquals(false, t.overridden)
    }

    @Test
    fun `a status follows an occurrence whose move re-counted the rhythm`() = runBlocking {
        seed(rule = Rule(pattern = Pattern.INTERVAL, interval = 3, unit = com.thefoxworks.tzafon.domain.recurrence.IntervalUnit.DAY))
        repo.saveDraft(opened(occ("2026-10-15")).copy(toDoDate = "2026-10-16", state = TaskState.DONE), EditScope.FORWARD, today)

        assertEquals(TaskState.DONE, task(occ("2026-10-16"))!!.state)
    }

    @Test
    fun `saving with no changes writes nothing`() = runBlocking {
        seed()
        val before = taskDao.rows.value to seriesDao.rows.value
        clock = 2L

        repo.saveDraft(opened(occ("2026-10-20")), EditScope.FORWARD, today)

        assertEquals(before, taskDao.rows.value to seriesDao.rows.value)
    }
}
