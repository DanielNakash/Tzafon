package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.data.db.toDomain
import com.thefoxworks.tzafon.data.db.toEntity
import com.thefoxworks.tzafon.data.repo.RoomGoalRepository
import com.thefoxworks.tzafon.data.repo.RoomHabitRepository
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import com.thefoxworks.tzafon.domain.status.AmountPrompt
import com.thefoxworks.tzafon.domain.status.StatusChanges
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.13.0 — list status changes, their toast and Undo, through
 * [StatusChanges] over the Room repositories and in-memory DAOs.
 *
 * "Write 500 words" ticks the quantitative habit "Write in the morning"
 * (words), which moves the goal "First draft" (62,000 of 80,000 words).
 */
class StatusUndoTest {

    private val today = "2026-10-14"
    private val taskDao = FakeTaskDao()
    private val seriesDao = FakeSeriesDao()
    private val habitDao = FakeHabitDao()
    private val goalDao = FakeGoalDao()
    private var clock = 100L
    private val habits = RoomHabitRepository(habitDao)
    private val goals = RoomGoalRepository(goalDao, now = { clock })
    private val repo = RoomTaskRepository(taskDao, seriesDao, habits, goals, now = { clock })
    private var chimes = 0
    private val changes = StatusChanges(repo, habits, goals, today = { today }, onDone = { chimes++ })

    private val write = Task(id = "T1", title = "Write 500 words", toDoDate = today, habitId = "H1")
    private val physio = Task(id = "T2", title = "Book the physio appointment", toDoDate = "2026-10-16")

    private fun seed() = runBlocking {
        habits.upsert(
            Habit(id = "H1", name = "Write in the morning", kind = HabitKind.QUANTITATIVE, target = 500.0, unit = "words", goalId = "G1"),
        )
        goals.upsert(
            Goal(id = "G1", title = "First draft", type = GoalType.ACCUMULATIVE, targetQty = 80_000.0, unit = "words", currentQty = 62_000.0),
        )
        taskDao.upsert(write.toEntity())
        taskDao.upsert(physio.toEntity())
    }

    private suspend fun task(id: String): Task = taskDao.get(id)!!.toDomain()
    private suspend fun draftTotal(): Double = goals.getGoal("G1")!!.currentQty
    private fun toastId(): Long = changes.toast.value!!.id

    @Test
    fun markDone_withAmount_ticksMovesAndUndoTakesItBack() = runBlocking {
        seed()
        changes.change("T1", TaskState.DONE, habitAmount = 500.0)

        val toast = changes.toast.value!!
        assertEquals("Done · ticked Write in the morning · moved First draft to 62.5K words", toast.text)
        assertTrue(toast.undoable)
        assertEquals(62_500.0, draftTotal(), 0.0)
        assertEquals(500.0, habits.logsForTask("T1").single().amount)
        assertEquals(1, chimes)

        changes.undo(toast.id)

        assertEquals(TaskState.OPEN, task("T1").state)
        assertNull(task("T1").completedAt)
        assertEquals(62_000.0, draftTotal(), 0.0)
        assertTrue(habits.logsForTask("T1").isEmpty())
        assertTrue(goals.contributionsFor("T1").isEmpty())
        assertNull(changes.toast.value)
    }

    @Test
    fun markOpen_undoReappliesTheSameAmountWithoutAsking() = runBlocking {
        seed()
        repo.setState("T1", TaskState.DONE, today, habitAmount = 500.0)
        val done = task("T1")
        val row = goals.contributionsFor("T1").single()

        clock = 200L
        changes.change("T1", TaskState.OPEN)
        assertEquals("Marked open · Write 500 words", changes.toast.value!!.text)
        assertEquals(62_000.0, draftTotal(), 0.0)
        assertEquals(0, chimes)

        changes.undo(toastId())

        assertEquals(done, task("T1"))
        assertEquals(62_500.0, draftTotal(), 0.0)
        assertEquals(500.0, habits.logsForTask("T1").single().amount)
        assertEquals(row, goals.contributionsFor("T1").single())
    }

    @Test
    fun backlog_undoBringsTheToDoDateBack() = runBlocking {
        seed()
        changes.change("T2", TaskState.BACKLOG)
        assertEquals("Moved to Backlog · Book the physio appointment", changes.toast.value!!.text)
        assertNull(task("T2").toDoDate)

        changes.undo(toastId())

        assertEquals(physio, task("T2"))
    }

    @Test
    fun undo_doesNothingOnceTheTaskHasChangedAgain() = runBlocking {
        seed()
        changes.change("T2", TaskState.CLOSED)
        repo.reschedule("T2", "2026-10-20") // e.g. sync, or another screen

        changes.undo(toastId())

        assertEquals(TaskState.CLOSED, task("T2").state)
        assertEquals("2026-10-20", task("T2").toDoDate)
        assertNull(changes.toast.value)
    }

    @Test
    fun aNewToastReplacesTheOldOneAndTakesItsUndo() = runBlocking {
        seed()
        changes.change("T1", TaskState.DONE, habitAmount = 500.0)
        val first = toastId()
        changes.change("T2", TaskState.CLOSED)
        assertEquals("Closed · Book the physio appointment", changes.toast.value!!.text)

        changes.undo(first)

        assertEquals(TaskState.DONE, task("T1").state)
        assertEquals("Closed · Book the physio appointment", changes.toast.value!!.text)
    }

    @Test
    fun wholeSeriesFreeze_showsAToastWithoutUndo() = runBlocking {
        seriesDao.upsert(
            Series(
                id = "S1", title = "Run", rule = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(3)),
                ruleAnchor = today, startDate = today, generatedThrough = today,
            ).toEntity(),
        )
        repo.topUp(today, 30)

        changes.change("S1__$today", TaskState.FROZEN)

        val toast = changes.toast.value!!
        assertEquals("Series frozen · Run", toast.text)
        assertFalse(toast.undoable)
        changes.undo(toast.id)
        assertTrue(seriesDao.get("S1")!!.toDomain().frozen)
        assertEquals(TaskState.FROZEN, task("S1__$today").state)
    }

    @Test
    fun chime_onlyOnOpenToDone_neverOnUndo() = runBlocking {
        seed()
        changes.change("T2", TaskState.DONE)
        assertEquals(1, chimes)

        changes.undo(toastId())
        assertEquals(1, chimes)

        changes.change("T2", TaskState.DONE)
        changes.change("T2", TaskState.OPEN)
        changes.undo(toastId()) // back to Done, silently
        assertEquals(TaskState.DONE, task("T2").state)
        assertEquals(2, chimes)

        changes.change("T2", TaskState.CLOSED)
        changes.change("T2", TaskState.DONE)
        assertEquals(2, chimes)
    }

    @Test
    fun subject_readsTheRepeatCardsShortSummary() = runBlocking {
        val series = Series(
            id = "S1", title = "Run", rule = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4)),
            ruleAnchor = today, startDate = today, generatedThrough = today,
        )
        seriesDao.upsert(series.toEntity())

        val subject = changes.subject(Task(id = "S1__$today", title = "Run", toDoDate = today, seriesId = "S1"))

        assertEquals("Every Tue & Thu", subject.repeat)
    }

    @Test
    fun amountPrompt_onlyForAmountTasks() = runBlocking {
        seed()
        assertEquals(AmountPrompt("How much? · Write in the morning", "words", 500.0), changes.amountPrompt(write))
        assertNull(changes.amountPrompt(physio))
    }

    @Test
    fun dismiss_dropsTheToastAndItsUndo() = runBlocking {
        seed()
        changes.change("T2", TaskState.FROZEN)
        val id = toastId()
        changes.dismiss(id)
        assertNull(changes.toast.value)

        changes.undo(id)
        assertEquals(TaskState.FROZEN, task("T2").state)
    }
}
