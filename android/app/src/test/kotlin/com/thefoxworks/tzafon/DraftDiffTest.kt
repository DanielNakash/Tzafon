package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.ChangeField
import com.thefoxworks.tzafon.domain.editor.DraftDiff
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.13.0 — the "You changed" diff: the opening snapshot against the draft,
 * compared as Save would store them, one entry per field, tagged by pane.
 */
class DraftDiffTest {

    private val base = TaskDraft(id = "T1", title = "Write the report")

    private fun lines(after: TaskDraft, before: TaskDraft = base) =
        DraftDiff.of(before, after).changes.map { it.line }

    @Test
    fun `an untouched draft has no changes`() {
        assertTrue(DraftDiff.of(base, base.copy()).isEmpty)
    }

    @Test
    fun `a title edit reads Title changed on the Task pane`() {
        val diff = DraftDiff.of(base, base.copy(title = "Write the final report"))
        assertEquals(listOf("Title changed"), diff.changes.map { it.line })
        assertEquals(ChangeField.TITLE, diff.changes.single().field)
        assertEquals(setOf(Pane.TASK), diff.panes)
    }

    @Test
    fun `a title differing only in surrounding spaces is no change`() {
        assertEquals(emptyList<String>(), lines(base.copy(title = "  Write the report ")))
    }

    // ── notes: never shows the text ──────────────────────────

    @Test
    fun `notes read added, changed or removed`() {
        val withNotes = base.copy(description = "Two pages")
        assertEquals(listOf("Notes added"), lines(withNotes))
        assertEquals(listOf("Notes changed"), lines(withNotes.copy(description = "Three pages"), before = withNotes))
        assertEquals(listOf("Notes removed"), lines(base, before = withNotes))
    }

    @Test
    fun `undoing a notes edit clears the change`() {
        val typed = base.copy(description = "Two pages")
        assertTrue(DraftDiff.of(base, typed.copy(description = "")).isEmpty)
    }

    // ── status ───────────────────────────────────────────────

    @Test
    fun `a status change reads before and after on the Task pane`() {
        val diff = DraftDiff.of(base, base.copy(state = TaskState.DONE))
        assertEquals(listOf("Status Open → Done"), diff.changes.map { it.line })
        assertEquals(setOf(Pane.TASK), diff.panes)
        assertEquals(listOf("Status Done → Backlog"), lines(base.copy(state = TaskState.BACKLOG), before = base.copy(state = TaskState.DONE)))
    }

    @Test
    fun `a Done status carries the How much answer with the habit's unit`() {
        val done = base.copy(state = TaskState.DONE, habitAmount = 500.0)
        assertEquals(listOf("Status Open → Done · 500 words"), DraftDiff.of(base, done, amountUnit = "words").changes.map { it.line })
        assertEquals(listOf("Status Open → Done · 2.5"), lines(base.copy(state = TaskState.DONE, habitAmount = 2.5)))
    }

    // ── dates (Thu 17 Sep 2026 is a Thursday) ─────────────────

    private val dated = base.copy(toDoDate = "2026-09-17")

    @Test
    fun `a to-do date move reads both days on the Timing pane`() {
        val diff = DraftDiff.of(dated, dated.copy(toDoDate = "2026-09-18"))
        assertEquals(listOf("To-do date Thu 17 → Fri 18"), diff.changes.map { it.line })
        assertEquals(setOf(Pane.TIMING), diff.panes)
    }

    @Test
    fun `the month shows only when the two dates fall in different months`() {
        assertEquals(
            listOf("To-do date Wed 30 Sep → Fri 2 Oct"),
            lines(base.copy(toDoDate = "2026-10-02"), before = base.copy(toDoDate = "2026-09-30")),
        )
    }

    @Test
    fun `a lone date carries its month against Undated or None`() {
        assertEquals(listOf("To-do date Undated → Fri 18 Sep"), lines(base.copy(toDoDate = "2026-09-18")))
        assertEquals(listOf("To-do date Thu 17 Sep → Undated"), lines(base, before = dated))
        assertEquals(listOf("Deadline None → Fri 25 Sep"), lines(dated.copy(dueDate = "2026-09-25"), before = dated))
        assertEquals(listOf("Deadline Fri 25 Sep → None"), lines(dated, before = dated.copy(dueDate = "2026-09-25")))
    }

    @Test
    fun `dates in different years both carry their year`() {
        assertEquals(
            listOf("Deadline Thu 17 Sep 2026 → Fri 17 Sep 2027"),
            lines(dated.copy(dueDate = "2027-09-17"), before = dated.copy(dueDate = "2026-09-17")),
        )
    }

    // ── the repeat card ──────────────────────────────────────

    private val tueThu = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4))
    private val weekly = dated.copy(seriesId = "S1", occurrenceDate = "2026-09-17", recurrence = RecurrenceDraft(rule = tueThu))

    private fun withRec(change: (RecurrenceDraft) -> RecurrenceDraft) =
        weekly.copy(recurrence = change(weekly.recurrence!!))

    @Test
    fun `a rule change reads Repeat changed and touches the repeat card`() {
        val diff = DraftDiff.of(weekly, withRec { it.copy(rule = tueThu.copy(weekdays = listOf(2, 4, 6))) })
        assertEquals(listOf("Repeat changed"), diff.changes.map { it.line })
        assertTrue(diff.changes.single().repeatCard)
        assertTrue(diff.touchesRepeatCard)
        assertEquals(setOf(Pane.TIMING), diff.panes)
    }

    @Test
    fun `a rule that means the same is no change`() {
        // weekdays picked in another order, and a month field the weekly rule never reads
        val same = withRec { it.copy(rule = tueThu.copy(weekdays = listOf(4, 2), monthDate = 9)) }
        assertTrue(DraftDiff.of(weekly, same).isEmpty)
    }

    @Test
    fun `the end date reads like any date, on the repeat card`() {
        val diff = DraftDiff.of(weekly, withRec { it.copy(endDate = "2026-09-30") })
        assertEquals(listOf("Ends Never → Wed 30 Sep"), diff.changes.map { it.line })
        assertTrue(diff.touchesRepeatCard)
    }

    @Test
    fun `turning repeat off is one entry, whatever the card held`() {
        val full = withRec { it.copy(endDate = "2026-12-31", dueMode = DueMode.OFFSET, dueOffsetDays = 2) }
        val diff = DraftDiff.of(full, full.copy(recurrence = null))
        assertEquals(listOf("Repeat turned off"), diff.changes.map { it.line })
        assertTrue(diff.repeatTurnedOff)
        assertTrue(diff.touchesRepeatCard)
    }

    @Test
    fun `turning repeat on is one entry`() {
        val diff = DraftDiff.of(dated, dated.copy(recurrence = RecurrenceDraft(rule = tueThu, endDate = "2026-12-31")))
        assertEquals(listOf("Repeat turned on"), diff.changes.map { it.line })
        assertFalse(diff.repeatTurnedOff)
    }

    @Test
    fun `a deadline mode switch reads the two modes`() {
        val fixed = withRec { it.copy(dueMode = DueMode.SINGULAR) }.copy(dueDate = "2026-09-25")
        val perOccurrence = withRec { it.copy(dueMode = DueMode.OFFSET, dueOffsetDays = 2) }.copy(dueDate = "2026-09-19")
        val diff = DraftDiff.of(fixed, perOccurrence)
        assertEquals(listOf("Deadline One fixed → Per occurrence"), diff.changes.map { it.line })
        assertTrue(diff.touchesRepeatCard)
        val oldRule = withRec { it.copy(dueMode = DueMode.RECURRING, dueRule = tueThu) }.copy(dueDate = "2026-09-19")
        assertEquals(listOf("Deadline Custom rule → Per occurrence"), lines(perOccurrence, before = oldRule))
    }

    @Test
    fun `a per-occurrence offset reads days each, and its derived deadline is not listed`() {
        val two = withRec { it.copy(dueMode = DueMode.OFFSET, dueOffsetDays = 2) }.copy(dueDate = "2026-09-19")
        val three = withRec { it.copy(dueMode = DueMode.OFFSET, dueOffsetDays = 3) }.copy(dueDate = "2026-09-20")
        val diff = DraftDiff.of(two, three)
        assertEquals(listOf("Deadline +2 → +3 days each"), diff.changes.map { it.line })
        assertTrue(diff.touchesRepeatCard)
    }

    @Test
    fun `setting or clearing a repeating deadline reads the value against None`() {
        val fixed = withRec { it.copy(dueMode = DueMode.SINGULAR) }.copy(dueDate = "2026-09-25")
        assertEquals(listOf("Deadline None → Fri 25 Sep"), lines(fixed, before = weekly))
        val perOccurrence = withRec { it.copy(dueMode = DueMode.OFFSET, dueOffsetDays = 3) }.copy(dueDate = "2026-09-20")
        assertEquals(listOf("Deadline +3 days each → None"), lines(weekly, before = perOccurrence))
        assertTrue(DraftDiff.of(weekly, fixed).touchesRepeatCard)
    }

    @Test
    fun `moving a fixed deadline is a repeat-card change`() {
        val fixed = withRec { it.copy(dueMode = DueMode.SINGULAR) }.copy(dueDate = "2026-09-25")
        val diff = DraftDiff.of(fixed, fixed.copy(dueDate = "2026-09-28"))
        assertEquals(listOf("Deadline Fri 25 → Mon 28"), diff.changes.map { it.line })
        assertTrue(diff.touchesRepeatCard)
    }

    @Test
    fun `a to-do date move on a repeating task stays off the repeat card`() {
        val diff = DraftDiff.of(weekly, weekly.copy(toDoDate = "2026-09-18"))
        assertEquals(listOf("To-do date Thu 17 → Fri 18"), diff.changes.map { it.line })
        assertFalse(diff.touchesRepeatCard)
    }

    @Test
    fun `entries follow pane order, Task then Timing then Alignment`() {
        val all = withRec { it.copy(endDate = "2026-09-30") }
            .copy(title = "New", toDoDate = "2026-09-18", cue = coffee)
        assertEquals(
            listOf("Title changed", "To-do date Thu 17 → Fri 18", "Ends Never → Wed 30 Sep", "Cue added"),
            lines(all, before = weekly),
        )
    }

    // ── alignment ────────────────────────────────────────────

    private val coffee = Cue(CueType.AFTER_ROUTINE, "After the first coffee")

    @Test
    fun `cue and habit read added, changed or removed on the Alignment pane`() {
        val cued = base.copy(cue = coffee, habitId = "H1")
        val diff = DraftDiff.of(base, cued)
        assertEquals(listOf("Cue added", "Habit added"), diff.changes.map { it.line })
        assertEquals(setOf(Pane.ALIGNMENT), diff.panes)
        assertEquals(
            listOf("Cue changed", "Habit changed"),
            lines(cued.copy(cue = coffee.copy(time = "08:30"), habitId = "H2"), before = cued),
        )
        assertEquals(listOf("Cue removed", "Habit removed"), lines(base, before = cued))
    }

    @Test
    fun `goals count what was added or removed`() {
        val one = base.copy(goalIds = listOf("G1"))
        assertEquals(listOf("Goal added"), lines(one))
        assertEquals(listOf("2 goals added"), lines(base.copy(goalIds = listOf("G1", "G2"))))
        assertEquals(listOf("Goal removed"), lines(base, before = one))
        assertEquals(listOf("2 goals removed"), lines(base, before = base.copy(goalIds = listOf("G1", "G2"))))
        assertEquals(listOf("Goals changed"), lines(base.copy(goalIds = listOf("G2")), before = one))
    }

    @Test
    fun `reordering goals is no change`() {
        val two = base.copy(goalIds = listOf("G1", "G2"))
        assertTrue(DraftDiff.of(two, two.copy(goalIds = listOf("G2", "G1"))).isEmpty)
    }

    @Test
    fun `theme, commitment and identity fields never count`() {
        val moved = base.copy(themeId = "TH1", commitment = "I will", seriesId = "S9", occurrenceDate = "2026-09-17")
        assertTrue(DraftDiff.of(base, moved).isEmpty)
    }
}
