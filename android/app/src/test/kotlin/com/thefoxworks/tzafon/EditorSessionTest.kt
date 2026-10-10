package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.DraftDiff
import com.thefoxworks.tzafon.domain.editor.EditorSession
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.editor.SaveProblem
import com.thefoxworks.tzafon.domain.editor.SaveStep
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import com.thefoxworks.tzafon.ui.editor.EditorSessionCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.13.0 — the three-pane editor frame: the opening snapshot, the draft and
 * the active tab, with the per-pane changed dots, the discard prompt and the
 * order Save reports problems in.
 */
class EditorSessionTest {

    private val task = TaskDraft(id = "T1", title = "Write the report")

    @Test
    fun `the editor opens on Task with no dots and closes without asking`() {
        val s = EditorSession.open(task)
        assertEquals(Pane.TASK, s.tab)
        assertEquals(emptySet<Pane>(), s.changedPanes)
        assertFalse(s.asksBeforeClosing)
    }

    @Test
    fun `each edit lights only its own pane's dot`() {
        val s = EditorSession.open(task)
        assertEquals(setOf(Pane.TASK), s.edit { it.copy(title = "Write the final report") }.changedPanes)
        assertEquals(setOf(Pane.TIMING), s.edit { it.copy(toDoDate = "2026-10-12") }.changedPanes)
        assertEquals(setOf(Pane.ALIGNMENT), s.edit { it.copy(habitId = "H1") }.changedPanes)
        assertEquals(
            setOf(Pane.TASK, Pane.ALIGNMENT),
            s.edit { it.copy(description = "Draft first") }.edit { it.copy(goalIds = listOf("G1")) }.changedPanes,
        )
    }

    @Test
    fun `undoing an edit clears the dot and closing no longer asks`() {
        val edited = EditorSession.open(task).edit { it.copy(title = "Something else") }
        assertTrue(edited.asksBeforeClosing)
        val undone = edited.edit { it.copy(title = "Write the report") }
        assertEquals(emptySet<Pane>(), undone.changedPanes)
        assertFalse(undone.asksBeforeClosing)
    }

    @Test
    fun `presets are part of the snapshot, so an untouched quick-add closes silently`() {
        val quickAdd = TaskDraft(id = null, title = "Call mum", toDoDate = "2026-10-10",
            cue = Cue(CueType.AT_TIME, "", "18:00"))
        val s = EditorSession.open(quickAdd)
        assertEquals(emptySet<Pane>(), s.changedPanes)
        assertFalse(s.asksBeforeClosing)
    }

    @Test
    fun `a single-task status change lights the Task dot`() {
        val s = EditorSession.open(task).edit { it.copy(state = TaskState.DONE) }
        assertEquals(setOf(Pane.TASK), s.changedPanes)
    }

    @Test
    fun `a status already stored by the interim status sheet is no change`() {
        val s = EditorSession.open(task).edit { it.copy(title = "New title") }.stored(TaskState.FROZEN)
        assertEquals(TaskState.FROZEN, s.draft.state)
        assertEquals(listOf("Title changed"), DraftDiff.of(s.snapshot, s.draft).changes.map { it.line })
    }

    @Test
    fun `switching tabs keeps the draft and its dots`() {
        val s = EditorSession.open(task).edit { it.copy(title = "New title") }.show(Pane.TIMING)
        assertEquals(Pane.TIMING, s.tab)
        assertEquals("New title", s.draft.title)
        assertEquals(setOf(Pane.TASK), s.changedPanes)
    }

    // ── Save: the first problem, in order ─────────────────────

    @Test
    fun `a saveable draft has no problem`() {
        assertNull(EditorSession.open(task).saveProblem())
    }

    @Test
    fun `a blank or whitespace title blocks Save on the Task pane`() {
        val p = EditorSession.open(task).edit { it.copy(title = "   ") }.saveProblem()
        assertEquals(SaveProblem.BLANK_TITLE, p)
        assertEquals(Pane.TASK, p!!.pane)
    }

    @Test
    fun `an incomplete cue time blocks Save on the Alignment pane`() {
        val p = EditorSession.open(task).saveProblem(cueTimeIncomplete = true)
        assertEquals(SaveProblem.INCOMPLETE_CUE_TIME, p)
        assertEquals(Pane.ALIGNMENT, p!!.pane)
    }

    @Test
    fun `a blank title is reported before an incomplete cue time`() {
        val p = EditorSession.open(task).edit { it.copy(title = "") }.saveProblem(cueTimeIncomplete = true)
        assertEquals(SaveProblem.BLANK_TITLE, p)
    }

    // ── Save: what it does once nothing stops it ──────────────

    private val occurrence = TaskDraft(
        id = "S1__2026-09-17", title = "Water plants", toDoDate = "2026-09-17",
        seriesId = "S1", occurrenceDate = "2026-09-17",
        recurrence = RecurrenceDraft(rule = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4))),
    )

    @Test
    fun `a field edit to a repeating task asks where it lands`() {
        val s = EditorSession.open(occurrence).edit { it.copy(title = "Water the plants") }
        assertEquals(SaveStep.AskScope, s.saveStep)
    }

    @Test
    fun `an untouched existing task closes and writes nothing`() {
        assertEquals(SaveStep.Close, EditorSession.open(occurrence).saveStep)
        assertEquals(SaveStep.Close, EditorSession.open(task).saveStep)
    }

    @Test
    fun `an untouched new task still saves, presets and all`() {
        val quickAdd = TaskDraft(id = null, title = "Call mum", toDoDate = "2026-10-10")
        assertEquals(SaveStep.Write(EditScope.ONE), EditorSession.open(quickAdd).saveStep)
    }

    @Test
    fun `a status-only save on a repeating task lands on this one without asking`() {
        val s = EditorSession.open(occurrence).edit { it.copy(state = TaskState.DONE) }
        assertEquals(SaveStep.Write(EditScope.ONE), s.saveStep)
    }

    @Test
    fun `a one-off turned repeating starts a new series without asking`() {
        val s = EditorSession.open(task).edit { it.copy(recurrence = occurrence.recurrence) }
        assertFalse(s.repeats)
        assertTrue(s.saveStep is SaveStep.Write)
    }

    @Test
    fun `turning repeat off on an occurrence still asks, since the series ends`() {
        val s = EditorSession.open(occurrence).edit { it.copy(recurrence = null) }
        assertTrue(s.repeats)
        assertEquals(SaveStep.AskScope, s.saveStep)
    }

    // ── surviving rotation and process death ──────────────────

    @Test
    fun `a session round-trips through its saved form exactly`() {
        val series = TaskDraft(
            id = "S1__2026-10-12", title = "Run", description = "Easy pace", toDoDate = "2026-10-12",
            dueDate = "2026-10-14", state = TaskState.OPEN, seriesId = "S1", occurrenceDate = "2026-10-12",
            recurrence = RecurrenceDraft(
                rule = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(1, 3, 5)),
                endDate = "2026-12-31", dueMode = DueMode.OFFSET, dueOffsetDays = 2,
            ),
            cue = Cue(CueType.AFTER_ROUTINE, "After coffee", "07:30"),
            habitId = "H1", goalIds = listOf("G1", "G2"), habitAmount = 2.5,
        )
        val s = EditorSession.open(series)
            .edit { it.copy(title = "Run far", recurrence = null, goalIds = emptyList()) }
            .show(Pane.ALIGNMENT)
        assertEquals(s, EditorSessionCodec.decode(EditorSessionCodec.encode(s)))
    }

    @Test
    fun `a new task with nulls round-trips too`() {
        val s = EditorSession.open(TaskDraft(id = null, title = "")).show(Pane.TIMING)
        assertEquals(s, EditorSessionCodec.decode(EditorSessionCodec.encode(s)))
    }
}
