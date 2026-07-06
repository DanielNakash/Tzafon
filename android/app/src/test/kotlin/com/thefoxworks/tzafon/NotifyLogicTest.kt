package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.LogSource
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.notify.NotifyLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** FR-NOTIF-1 — reminders fire on the cue, and only while still wanted. */
class NotifyLogicTest {

    private val d = "2026-07-07"

    private fun task(
        id: String,
        cue: Cue?,
        toDo: String? = d,
        due: String? = null,
        state: TaskState = TaskState.OPEN,
    ) = Task(id = id, title = "t-$id", cue = cue, toDoDate = toDo, dueDate = due, state = state)

    private fun habit(id: String, cue: Cue?) = Habit(id = id, name = "h-$id", cue = cue)

    private fun at(time: String?) = Cue(CueType.AT_TIME, time ?: "", time)
    private fun routine(label: String, time: String?) = Cue(CueType.AFTER_ROUTINE, label, time)

    // ── time parsing ──────────────────────────────────────────

    @Test
    fun `minutesOf parses HH-mm and rejects junk`() {
        assertEquals(510, NotifyLogic.minutesOf("8:30"))
        assertEquals(0, NotifyLogic.minutesOf("0:00"))
        assertEquals(1439, NotifyLogic.minutesOf("23:59"))
        assertNull(NotifyLogic.minutesOf(null))
        assertNull(NotifyLogic.minutesOf(""))
        assertNull(NotifyLogic.minutesOf("25:00"))
        assertNull(NotifyLogic.minutesOf("noon"))
    }

    // ── task rules ────────────────────────────────────────────

    @Test
    fun `a task reminds on its planned day at the cue time`() {
        val rs = NotifyLogic.remindersFor(d, listOf(task("a", at("8:30"))), emptyList(), emptyList())
        assertEquals(1, rs.size)
        assertEquals("task-a-$d", rs[0].key)
        assertEquals("8:30", rs[0].time)
    }

    @Test
    fun `due-today also reminds, other days and states do not`() {
        val rs = NotifyLogic.remindersFor(
            d,
            listOf(
                task("due", at("9:00"), toDo = null, due = d),           // counts
                task("tomorrow", at("9:00"), toDo = "2026-07-08"),        // wrong day
                task("done", at("9:00"), state = TaskState.DONE),         // not open
                task("noCue", null),                                      // no cue
                task("place", Cue(CueType.AT_PLACE, "At the studio")),    // no clock
                task("routineNoTime", routine("After lunch", null)),      // no time set
            ),
            emptyList(), emptyList(),
        )
        assertEquals(listOf("task-due-$d"), rs.map { it.key })
    }

    @Test
    fun `after-routine cues with a time ride along, line shows both halves`() {
        val rs = NotifyLogic.remindersFor(
            d, listOf(task("r", routine("After the first coffee", "8:30"))), emptyList(), emptyList(),
        )
        assertEquals("After the first coffee · 8:30", rs[0].line)
    }

    // ── habit rules ───────────────────────────────────────────

    @Test
    fun `a habit reminds daily unless already logged today`() {
        val h1 = habit("morning", routine("After the first coffee", "8:30"))
        val h2 = habit("run", at("18:00"))
        val logs = listOf(HabitLog("morning", d, done = true, source = LogSource.DIRECT))
        val rs = NotifyLogic.remindersFor(d, emptyList(), listOf(h1, h2), logs)
        assertEquals(listOf("habit-run-$d"), rs.map { it.key })
    }

    @Test
    fun `an undone log does not silence the habit`() {
        val h = habit("run", at("18:00"))
        val logs = listOf(HabitLog("run", d, done = false, source = LogSource.DIRECT))
        val rs = NotifyLogic.remindersFor(d, emptyList(), listOf(h), logs)
        assertEquals(1, rs.size)
    }

    // ── intraday scheduling ───────────────────────────────────

    @Test
    fun `afterMinutes drops times already past and sorts the rest`() {
        val rs = NotifyLogic.remindersFor(
            d,
            listOf(task("late", at("21:00")), task("early", at("7:00"))),
            listOf(habit("mid", at("12:15"))),
            emptyList(),
            afterMinutes = 8 * 60, // 08:00 — "early" already passed
        )
        assertEquals(listOf("habit-mid-$d", "task-late-$d"), rs.map { it.key })
    }
}
