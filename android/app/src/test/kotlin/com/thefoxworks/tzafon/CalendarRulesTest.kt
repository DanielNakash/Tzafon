package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.Armed
import com.thefoxworks.tzafon.domain.editor.SharedCalendar
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/**
 * v2.13.0 — the shared calendar's rules: the armed field, the quick-pick
 * rails, what a day tap does, the marks, the conflict fix and the captions.
 * Today is Saturday 2026-10-10 throughout.
 */
class CalendarRulesTest {

    private val today = "2026-10-10"
    private val open = TaskDraft(id = "T1", title = "Write the report")
    private val tueThu = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4))
    private val repeating = open.copy(toDoDate = "2026-10-13", recurrence = RecurrenceDraft(rule = tueThu))

    private fun cal(draft: TaskDraft, armed: Armed = Armed.TODO) = SharedCalendar(draft, armed, today)
    private fun SharedCalendar.labels() = rail.map { it.label }
    private fun SharedCalendar.highlighted() = rail.filter { it.highlighted }.map { it.label }
    private fun SharedCalendar.chip(label: String) = rail.single { it.label == label }

    // ── To do ──

    @Test
    fun `the to-do rail is Today, Tomorrow, the weekday of today+2, Undated`() {
        assertEquals(listOf("Today", "Tomorrow", "Mon", "Undated"), cal(open).labels())
        assertEquals(listOf(today, "2026-10-11", "2026-10-12", null), cal(open).rail.map { it.day })
    }

    @Test
    fun `the chip matching the to-do date is highlighted, Undated when there is none`() {
        assertEquals(listOf("Undated"), cal(open).highlighted())
        assertEquals(listOf("Tomorrow"), cal(open.copy(toDoDate = "2026-10-11")).highlighted())
        assertEquals(emptyList<String>(), cal(open.copy(toDoDate = "2026-10-20")).highlighted())
    }

    @Test
    fun `a day tap sets the to-do date, and tapping it again clears it`() {
        val set = cal(open).tap("2026-10-20")
        assertEquals("2026-10-20", set.toDoDate)
        assertNull(cal(set).tap("2026-10-20").toDoDate)
        assertEquals(TaskState.OPEN, cal(set).tap("2026-10-20").state)
    }

    @Test
    fun `past days can be picked`() {
        assertEquals("2026-09-01", cal(open).tap("2026-09-01").toDoDate)
        assertTrue(cal(open).mark("2026-09-01").enabled)
    }

    @Test
    fun `when both dates fall on one day, a tap clears only the armed field`() {
        val same = open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-20")
        val todoCleared = cal(same, Armed.TODO).tap("2026-10-20")
        assertNull(todoCleared.toDoDate)
        assertEquals("2026-10-20", todoCleared.dueDate)
        val dueCleared = cal(same, Armed.DEADLINE).tap("2026-10-20")
        assertEquals("2026-10-20", dueCleared.toDoDate)
        assertNull(dueCleared.dueDate)
    }

    @Test
    fun `picking a chip sets its day, and a highlighted chip does nothing`() {
        assertEquals("2026-10-12", cal(open).pick(cal(open).chip("Mon")).toDoDate)
        val tomorrow = open.copy(toDoDate = "2026-10-11")
        assertEquals(tomorrow, cal(tomorrow).pick(cal(tomorrow).chip("Tomorrow")))
        assertNull(cal(tomorrow).pick(cal(tomorrow).chip("Undated")).toDoDate)
    }

    @Test
    fun `Undated keeps an Open task Open`() {
        val d = open.copy(toDoDate = "2026-10-11")
        assertEquals(TaskState.OPEN, cal(d).pick(cal(d).chip("Undated")).state)
    }

    @Test
    fun `a Backlog task reads Backlog, highlights no chip, and becomes Open with a picked date`() {
        val backlog = open.copy(state = TaskState.BACKLOG)
        assertEquals("Backlog", cal(backlog).toDoTile)
        assertEquals(emptyList<String>(), cal(backlog).highlighted())
        val tapped = cal(backlog).tap("2026-10-20")
        assertEquals("2026-10-20", tapped.toDoDate)
        assertEquals(TaskState.OPEN, tapped.state)
        assertEquals(TaskState.OPEN, cal(backlog).pick(cal(backlog).chip("Today")).state)
        assertEquals(backlog, cal(backlog).pick(cal(backlog).chip("Undated")))
    }

    @Test
    fun `a Backlog task can have a deadline without changing its status`() {
        val backlog = open.copy(state = TaskState.BACKLOG)
        val d = cal(backlog, Armed.DEADLINE).tap("2026-10-25")
        assertEquals("2026-10-25", d.dueDate)
        assertEquals(TaskState.BACKLOG, d.state)
        assertNull(d.toDoDate)
    }

    @Test
    fun `the to-do tile shows the day, or Undated`() {
        assertEquals("Undated", cal(open).toDoTile)
        assertEquals("Tue 13", cal(open.copy(toDoDate = "2026-10-13")).toDoTile)
    }

    @Test
    fun `a repeating task can't go undated`() {
        val c = cal(repeating)
        assertFalse(c.chip("Undated").enabled)
        assertEquals(repeating, c.pick(c.chip("Undated")))
        assertEquals(repeating, c.tap("2026-10-13"))
        assertEquals("A repeating task always has a to-do date", c.caption)
    }

    @Test
    fun `a repeating task's to-do tap moves this occurrence, even off the rule`() {
        assertEquals("2026-10-17", cal(repeating).tap("2026-10-17").toDoDate)
    }

    // ── Deadline ──

    private val dated = open.copy(toDoDate = "2026-10-13")

    @Test
    fun `with no deadline the rail is +1 week, +2 weeks, None, with None highlighted`() {
        val c = cal(dated, Armed.DEADLINE)
        assertEquals(listOf("+1 week", "+2 weeks", "None"), c.labels())
        assertEquals(listOf("None"), c.highlighted())
    }

    @Test
    fun `+1 and +2 weeks count from the to-do date`() {
        assertEquals(listOf("2026-10-20", "2026-10-27", null), cal(dated, Armed.DEADLINE).rail.map { it.day })
    }

    @Test
    fun `+1 and +2 weeks count from today when there is no to-do date`() {
        assertEquals(listOf("2026-10-17", "2026-10-24", null), cal(open, Armed.DEADLINE).rail.map { it.day })
    }

    @Test
    fun `with a deadline the rail leads with it, highlighted, and picking it does nothing`() {
        val d = dated.copy(dueDate = "2026-10-20")
        val c = cal(d, Armed.DEADLINE)
        assertEquals(listOf("Tue 20", "+1 week", "+2 weeks", "None"), c.labels())
        assertEquals(listOf("Tue 20"), c.highlighted())
        assertEquals(d, c.pick(c.rail.first()))
        assertNull(c.pick(c.chip("None")).dueDate)
        assertEquals("2026-10-27", c.pick(c.chip("+2 weeks")).dueDate)
    }

    @Test
    fun `a deadline tap sets it, and tapping it again clears it`() {
        val set = cal(dated, Armed.DEADLINE).tap("2026-10-25")
        assertEquals("2026-10-25", set.dueDate)
        assertNull(cal(set, Armed.DEADLINE).tap("2026-10-25").dueDate)
    }

    @Test
    fun `the deadline tile shows the day, or None`() {
        assertEquals("None", cal(dated, Armed.DEADLINE).deadlineTile)
        assertEquals("Sun 25", cal(dated.copy(dueDate = "2026-10-25")).deadlineTile)
    }

    @Test
    fun `the first deadline on a repeating task is a fixed one, and clearing it leaves none`() {
        val set = cal(repeating, Armed.DEADLINE).tap("2026-10-30")
        assertEquals("2026-10-30", set.dueDate)
        assertEquals(DueMode.SINGULAR, set.recurrence!!.dueMode)
        val cleared = cal(set, Armed.DEADLINE).tap("2026-10-30")
        assertNull(cleared.dueDate)
        assertEquals(DueMode.NONE, cleared.recurrence!!.dueMode)
    }

    @Test
    fun `an old custom deadline rule is read-only on the calendar`() {
        val custom = repeating.copy(
            dueDate = "2026-10-15",
            recurrence = repeating.recurrence!!.copy(dueMode = DueMode.RECURRING, dueRule = tueThu),
        )
        val c = cal(custom, Armed.DEADLINE)
        assertEquals(custom, c.tap("2026-10-20"))
        assertTrue(c.rail.none { it.enabled })
        assertFalse(c.mark("2026-10-20").enabled)
        assertTrue(cal(custom, Armed.TODO).mark("2026-10-20").enabled)
    }

    // ── Per-occurrence deadline (an offset after each to-do date) ──

    private val perOccurrence = repeating.copy(
        dueDate = "2026-10-15",
        recurrence = repeating.recurrence!!.copy(dueMode = DueMode.OFFSET, dueOffsetDays = 2),
    )

    @Test
    fun `the per-occurrence deadline tile shows this occurrence's day and the offset`() {
        assertEquals("Thu 15 · +2 days each", cal(perOccurrence, Armed.DEADLINE).deadlineTile)
        val noDay = perOccurrence.copy(dueDate = null)
        assertEquals("Thu 15 · +2 days each", cal(noDay, Armed.DEADLINE).deadlineTile)
        assertTrue(cal(noDay, Armed.DEADLINE).mark("2026-10-15").deadline)
    }

    @Test
    fun `a per-occurrence tap sets the offset to the gap from the to-do date`() {
        val d = cal(perOccurrence, Armed.DEADLINE).tap("2026-10-16")
        assertEquals(3, d.recurrence!!.dueOffsetDays)
        assertEquals(DueMode.OFFSET, d.recurrence!!.dueMode)
        assertEquals("2026-10-16", d.dueDate)
    }

    @Test
    fun `tapping the to-do day itself gives offset 0`() {
        val d = cal(perOccurrence, Armed.DEADLINE).tap("2026-10-13")
        assertEquals(0, d.recurrence!!.dueOffsetDays)
        assertEquals("2026-10-13", d.dueDate)
    }

    @Test
    fun `the offset is capped at 30 days`() {
        val d = cal(perOccurrence, Armed.DEADLINE).tap("2026-12-01")
        assertEquals(30, d.recurrence!!.dueOffsetDays)
        assertEquals("2026-11-12", d.dueDate)
    }

    @Test
    fun `per-occurrence days before the to-do date are dimmed and can't be tapped`() {
        val c = cal(perOccurrence, Armed.DEADLINE)
        assertFalse(c.mark("2026-10-12").enabled)
        assertTrue(c.mark("2026-10-13").enabled)
        assertEquals(perOccurrence, c.tap("2026-10-12"))
    }

    @Test
    fun `tapping the per-occurrence deadline again clears the deadline`() {
        val d = cal(perOccurrence, Armed.DEADLINE).tap("2026-10-15")
        assertNull(d.dueDate)
        assertEquals(DueMode.NONE, d.recurrence!!.dueMode)
        assertNull(d.recurrence!!.dueOffsetDays)
    }

    @Test
    fun `the per-occurrence rail sets the offset too`() {
        val c = cal(perOccurrence, Armed.DEADLINE)
        assertEquals(listOf("Thu 15", "+1 week", "+2 weeks", "None"), c.labels())
        assertEquals(7, c.pick(c.chip("+1 week")).recurrence!!.dueOffsetDays)
    }

    @Test
    fun `moving the to-do date carries a per-occurrence deadline along`() {
        val d = cal(perOccurrence, Armed.TODO).tap("2026-10-14")
        assertEquals("2026-10-14", d.toDoDate)
        assertEquals("2026-10-16", d.dueDate)
    }

    // ── End date ──

    @Test
    fun `the end rail is +1 month, +3 months, end of year, Never, from the to-do date`() {
        val c = cal(repeating, Armed.END)
        assertEquals(listOf("+1 month", "+3 months", "End of year", "Never"), c.labels())
        assertEquals(listOf("2026-11-13", "2027-01-13", "2026-12-31", null), c.rail.map { it.day })
        assertEquals(listOf("Never"), c.highlighted())
        assertEquals("A TAP SETS THE END", c.caption)
    }

    @Test
    fun `in December the end of year is the to-do date's own year`() {
        val dec = repeating.copy(toDoDate = "2026-12-05")
        assertEquals(listOf("2027-01-05", "2027-03-05", "2026-12-31", null), cal(dec, Armed.END).rail.map { it.day })
        val last = repeating.copy(toDoDate = "2026-12-31")
        assertEquals("2026-12-31", cal(last, Armed.END).chip("End of year").day)
    }

    @Test
    fun `an end tap sets the end date, and tapping it again clears it`() {
        val set = cal(repeating, Armed.END).tap("2026-12-20")
        assertEquals("2026-12-20", set.recurrence!!.endDate)
        assertEquals(listOf<String>(), cal(set, Armed.END).highlighted())
        assertNull(cal(set, Armed.END).tap("2026-12-20").recurrence!!.endDate)
        val c = cal(repeating, Armed.END)
        assertEquals("2026-11-13", c.pick(c.chip("+1 month")).recurrence!!.endDate)
        val month = c.pick(c.chip("+1 month"))
        assertEquals(listOf("+1 month"), cal(month, Armed.END).highlighted())
    }

    @Test
    fun `end days before the to-do date are dimmed and can't be tapped`() {
        val c = cal(repeating, Armed.END)
        assertFalse(c.mark("2026-10-12").enabled)
        assertTrue(c.mark("2026-10-13").enabled)
        assertEquals(repeating, c.tap("2026-10-12"))
    }

    // ── Marks ──

    private val window = open.copy(toDoDate = "2026-10-13", dueDate = "2026-10-16")

    @Test
    fun `to-do is filled, the deadline ringed, the days strictly between shaded`() {
        val c = cal(window)
        val todo = c.mark("2026-10-13")
        assertTrue(todo.toDo); assertFalse(todo.deadline); assertFalse(todo.window)
        val due = c.mark("2026-10-16")
        assertTrue(due.deadline); assertFalse(due.toDo); assertFalse(due.window)
        assertTrue(c.mark("2026-10-14").window)
        assertTrue(c.mark("2026-10-15").window)
        assertFalse(c.mark("2026-10-17").window)
        assertFalse(c.mark("2026-10-12").window)
    }

    @Test
    fun `both marks are drawn whichever field is armed`() {
        val c = cal(window, Armed.DEADLINE)
        assertTrue(c.mark("2026-10-13").toDo)
        assertTrue(c.mark("2026-10-16").deadline)
    }

    @Test
    fun `both dates on one day are filled and ringed, with no window`() {
        val m = cal(open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-20")).mark("2026-10-20")
        assertTrue(m.toDo); assertTrue(m.deadline); assertFalse(m.window)
    }

    @Test
    fun `today is dotted`() {
        assertTrue(cal(open).mark(today).today)
        assertFalse(cal(open).mark("2026-10-11").today)
    }

    @Test
    fun `a window that ended before today is still drawn, and past days are not dimmed`() {
        val past = open.copy(toDoDate = "2026-09-01", dueDate = "2026-09-04")
        val c = cal(past)
        assertTrue(c.mark("2026-09-01").toDo)
        assertTrue(c.mark("2026-09-02").window)
        assertTrue(c.mark("2026-09-04").deadline)
        assertTrue(c.mark("2026-09-02").enabled)
        assertTrue(cal(past, Armed.DEADLINE).mark("2026-08-01").enabled)
    }

    @Test
    fun `a deadline before the to-do date shades no window`() {
        val c = cal(open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-16"))
        assertFalse(c.mark("2026-10-18").window)
    }

    @Test
    fun `the end date gets its own mark`() {
        val ends = repeating.copy(recurrence = repeating.recurrence!!.copy(endDate = "2026-12-20"))
        assertTrue(cal(ends).mark("2026-12-20").end)
        assertFalse(cal(ends).mark("2026-12-21").end)
        assertFalse(cal(repeating).mark("2026-12-20").end)
    }

    // ── Deadline before the to-do date ──

    @Test
    fun `the warning shows only when the deadline is strictly earlier`() {
        assertNull(cal(window).conflict)
        assertNull(cal(open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-20")).conflict)
        assertNull(cal(open.copy(dueDate = "2026-10-16")).conflict)
        assertNull(cal(open.copy(toDoDate = "2026-10-20")).conflict)
        val c = cal(open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-16"))
        assertEquals("You plan to work on this after it's due.", c.conflict!!.warning)
        assertTrue(c.conflict!!.fixable)
    }

    @Test
    fun `the fix moves the to-do date to the deadline`() {
        val late = open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-16")
        val fixed = cal(late).fix()
        assertEquals("2026-10-16", fixed.toDoDate)
        assertEquals("2026-10-16", fixed.dueDate)
        assertNull(cal(fixed).conflict)
    }

    @Test
    fun `the fix is hidden when the deadline is already past`() {
        val late = open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-05")
        val c = cal(late)
        assertFalse(c.conflict!!.fixable)
        assertEquals(late, c.fix())
        assertTrue(cal(open.copy(toDoDate = "2026-10-20", dueDate = today)).conflict!!.fixable)
    }

    @Test
    fun `a fixed deadline on a repeating task can conflict, a per-occurrence one never does`() {
        val fixed = repeating.copy(
            toDoDate = "2026-10-20", dueDate = "2026-10-16",
            recurrence = repeating.recurrence!!.copy(dueMode = DueMode.SINGULAR),
        )
        assertTrue(cal(fixed).conflict != null)
        val each = perOccurrence.copy(toDoDate = "2026-10-20", dueDate = "2026-10-16")
        assertNull(cal(each).conflict)
    }

    // ── Captions and the opening month ──

    @Test
    fun `the caption follows the armed field`() {
        assertEquals("A TAP SETS TO-DO", cal(open, Armed.TODO).caption)
        assertEquals("A TAP SETS THE DEADLINE", cal(open, Armed.DEADLINE).caption)
        assertEquals("A TAP SETS THE END", cal(repeating, Armed.END).caption)
    }

    @Test
    fun `the calendar opens on the to-do date's month, else the deadline's, else this month`() {
        assertEquals(YearMonth.of(2026, 12), SharedCalendar.openingMonth(open.copy(toDoDate = "2026-12-03", dueDate = "2027-01-05"), today))
        assertEquals(YearMonth.of(2027, 1), SharedCalendar.openingMonth(open.copy(dueDate = "2027-01-05"), today))
        assertEquals(YearMonth.of(2026, 10), SharedCalendar.openingMonth(open, today))
        assertEquals(YearMonth.of(2026, 10), SharedCalendar.openingMonth(open.copy(state = TaskState.BACKLOG), today))
    }

    // ── What the Timing pane reads out (#27) ──

    @Test
    fun `the to-do tile leads with Today when the to-do date is today`() {
        assertEquals("Today · Sat 10", cal(open.copy(toDoDate = today)).toDoTile)
        assertEquals("Mon 12", cal(open.copy(toDoDate = "2026-10-12")).toDoTile)
    }

    @Test
    fun `the hint under the legend follows the armed field`() {
        assertEquals(
            "Tapping Mon 12 again clears the to-do date. The task becomes undated.",
            cal(open.copy(toDoDate = "2026-10-12")).hint,
        )
        assertEquals(
            "Tapping Sat 10 again clears the to-do date. The task becomes undated and leaves Today.",
            cal(open.copy(toDoDate = today)).hint,
        )
        assertEquals("An undated task waits in Planning. Tap a day to plan it.", cal(open).hint)
        assertEquals("Picking a day brings it out of Backlog.", cal(open.copy(state = TaskState.BACKLOG)).hint)
        assertEquals("A tap moves this occurrence's to-do date.", cal(repeating).hint)
        assertEquals(
            "Deadlines are optional. \u201cNone\u201d removes the ring and the shading; the to-do date is untouched.",
            cal(open, Armed.DEADLINE).hint,
        )
        val each = repeating.copy(recurrence = repeating.recurrence!!.copy(dueMode = DueMode.OFFSET, dueOffsetDays = 2))
        assertEquals("Each occurrence is due 2 days after its to-do date. A tap sets the gap.", cal(each, Armed.DEADLINE).hint)
        val custom = repeating.copy(recurrence = repeating.recurrence!!.copy(dueMode = DueMode.RECURRING, dueRule = tueThu))
        assertEquals("An old custom rule sets these deadlines, so the calendar can't change them.", cal(custom, Armed.DEADLINE).hint)
        assertEquals("The series stops after the day you tap. \u201cNever\u201d keeps it going.", cal(repeating, Armed.END).hint)
    }

    @Test
    fun `the fix names the day it moves the to-do date to`() {
        val late = open.copy(toDoDate = "2026-10-20", dueDate = "2026-10-16")
        assertEquals("Move the to-do date to Fri 16", cal(late).conflict!!.fix)
    }

    @Test
    fun `TalkBack reads each day with its marks`() {
        val window = open.copy(toDoDate = "2026-10-12", dueDate = "2026-10-16")
        assertEquals("Monday 12 October, to-do date", cal(window).describe("2026-10-12"))
        assertEquals("Friday 16 October, deadline", cal(window).describe("2026-10-16"))
        assertEquals("Wednesday 14 October, in the window", cal(window).describe("2026-10-14"))
        assertEquals("Saturday 10 October, today", cal(window).describe(today))
        assertEquals("Tuesday 20 October", cal(window).describe("2026-10-20"))
        assertEquals("Monday 12 October, to-do date, deadline", cal(open.copy(toDoDate = "2026-10-12", dueDate = "2026-10-12")).describe("2026-10-12"))
        val each = repeating.copy(recurrence = repeating.recurrence!!.copy(dueMode = DueMode.OFFSET, dueOffsetDays = 2))
        assertEquals("Monday 12 October, not available", cal(each, Armed.DEADLINE).describe("2026-10-12"))
        val ends = repeating.copy(recurrence = repeating.recurrence!!.copy(endDate = "2026-12-20"))
        assertEquals("Sunday 20 December, end date", cal(ends).describe("2026-12-20"))
    }
}
