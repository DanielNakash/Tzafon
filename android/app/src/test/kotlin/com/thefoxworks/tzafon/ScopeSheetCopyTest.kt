package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.DraftDiff
import com.thefoxworks.tzafon.domain.editor.ScopeRow
import com.thefoxworks.tzafon.domain.editor.ScopeSheetCopy
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v2.13.0 — the scope-at-save and delete sheets' words: each row's badge,
 * its consequence line and whether it can be picked, and the "You changed"
 * list as the chosen scope reads it. The occurrence is Thu 17 Sep 2026 of a
 * Tue & Thu series.
 */
class ScopeSheetCopyTest {

    private val tueThu = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4))
    private val weekly = TaskDraft(
        id = "S1__2026-09-17",
        title = "Water plants",
        toDoDate = "2026-09-17",
        seriesId = "S1",
        occurrenceDate = "2026-09-17",
        recurrence = RecurrenceDraft(rule = tueThu),
    )

    private fun sheet(draft: TaskDraft, before: TaskDraft = weekly, earlier: Int = 26, chosen: EditScope = EditScope.ONE) =
        ScopeSheetCopy.save(draft, DraftDiff.of(before, draft), earlier = earlier, chosen = chosen)

    @Test
    fun `a plain edit offers all three rows, defaulting to just this one`() {
        val s = sheet(weekly.copy(title = "Water the plants"))
        assertEquals(
            listOf(
                ScopeRow(EditScope.ONE, badge = "17 SEP", line = "Only Thu 17 Sep changes; the series stays as it is."),
                ScopeRow(EditScope.FORWARD, badge = null, line = "Edits forward in place. Past occurrences stay as they happened."),
                ScopeRow(EditScope.ALL, badge = "26", line = "Rewrites every occurrence, past ones included, done ones too."),
            ),
            s.rows,
        )
        assertEquals(EditScope.ONE, s.default)
        assertEquals(listOf("Title changed"), s.youChanged)
    }

    @Test
    fun `the whole-series badge hides at 0 but the row stays`() {
        val s = sheet(weekly.copy(title = "Water the plants"), earlier = 0)
        assertEquals(listOf(EditScope.ONE, EditScope.FORWARD, EditScope.ALL), s.rows.map { it.scope })
        assertEquals(null, s.rows.last().badge)
    }

    @Test
    fun `a repeat-card change disables just this one and defaults forward`() {
        val s = sheet(weekly.copy(recurrence = RecurrenceDraft(rule = tueThu, endDate = "2026-12-31")))
        assertEquals(
            ScopeRow(EditScope.ONE, badge = "17 SEP", line = "Repeat changes reach the series.", enabled = false),
            s.rows[0],
        )
        assertEquals(listOf(true, true), s.rows.drop(1).map { it.enabled })
        assertEquals(EditScope.FORWARD, s.default)
    }

    @Test
    fun `turning repeat off offers only forward, ending the series the day before`() {
        val s = sheet(weekly.copy(recurrence = null))
        assertEquals(
            ScopeRow(EditScope.FORWARD, badge = null, line = "This one becomes a one-off task; the series ends Wed 16 Sep."),
            s.rows[1],
        )
        assertEquals(listOf(false, true, false), s.rows.map { it.enabled })
        assertEquals(EditScope.FORWARD, s.default)
        assertEquals(listOf("Repeat turned off"), s.youChanged)
    }

    // ── the delete sheet ─────────────────────────────────────

    @Test
    fun `the delete sheet offers the same three rows with its own lines`() {
        assertEquals(
            listOf(
                ScopeRow(EditScope.ONE, badge = "17 SEP", line = "Removes Thu 17 Sep; it won't come back."),
                ScopeRow(EditScope.FORWARD, badge = null, line = "Removes this one and every later one. Done ones stay."),
                ScopeRow(
                    EditScope.ALL, badge = "4",
                    line = "Removes the series and every occurrence not done. Done ones stay as one-off tasks.",
                ),
            ),
            ScopeSheetCopy.delete(occurrenceDate = "2026-09-17", earlierNotDone = 4),
        )
        assertEquals(null, ScopeSheetCopy.delete(occurrenceDate = "2026-09-17", earlierNotDone = 0).last().badge)
    }

    // ── moving the to-do date (Thu 17 → Fri 18) ──────────────

    private fun moved(rule: Rule): Pair<TaskDraft, TaskDraft> {
        val before = weekly.copy(recurrence = RecurrenceDraft(rule = rule))
        return before to before.copy(toDoDate = "2026-09-18")
    }

    @Test
    fun `on a weekly repeat the move stays on this one in every scope`() {
        val (before, after) = moved(tueThu)
        val one = sheet(after, before, chosen = EditScope.ONE)
        assertEquals(
            listOf(
                "Friday moves; the series keeps its Tue & Thu rhythm.",
                "Edits forward in place. Past occurrences stay as they happened. The to-do date moves this one only.",
                "Rewrites every occurrence, past ones included, done ones too. The to-do date moves this one only.",
            ),
            one.rows.map { it.line },
        )
        assertEquals(listOf("To-do date Thu 17 → Fri 18"), one.youChanged)
        assertEquals(listOf("To-do date Thu 17 → Fri 18 · this one only"), sheet(after, before, chosen = EditScope.FORWARD).youChanged)
        assertEquals(listOf("To-do date Thu 17 → Fri 18 · this one only"), sheet(after, before, chosen = EditScope.ALL).youChanged)
    }

    @Test
    fun `on a monthly repeat the move stays on this one too`() {
        val (before, after) = moved(Rule(pattern = Pattern.MONTHDAY, monthDate = 17))
        val s = sheet(after, before, chosen = EditScope.ALL)
        assertEquals("Friday moves; the series keeps its 17th-of-the-month rhythm.", s.rows[0].line)
        assertEquals(
            "Rewrites every occurrence, past ones included, done ones too. The to-do date moves this one only.",
            s.rows[2].line,
        )
        assertEquals(listOf("To-do date Thu 17 → Fri 18 · this one only"), s.youChanged)
    }

    @Test
    fun `on an every-N-days repeat the rhythm re-counts from the new date`() {
        val (before, after) = moved(Rule(pattern = Pattern.INTERVAL, interval = 3, unit = IntervalUnit.DAY))
        val s = sheet(after, before, chosen = EditScope.FORWARD)
        assertEquals(
            listOf(
                "Friday moves; the series keeps its every-3-days rhythm.",
                "Edits forward in place. Past occurrences stay as they happened. From Fri 18, every 3 days.",
                "Rewrites every occurrence, past ones included, done ones too. From Fri 18, every 3 days.",
            ),
            s.rows.map { it.line },
        )
        assertEquals(listOf("To-do date Thu 17 → Fri 18"), s.youChanged)
    }

    @Test
    fun `the rhythm names daily repeats and several weekdays`() {
        val (before, after) = moved(Rule(pattern = Pattern.INTERVAL, interval = 1, unit = IntervalUnit.DAY))
        assertEquals("Friday moves; the series keeps its daily rhythm.", sheet(after, before).rows[0].line)
        assertEquals(
            "Edits forward in place. Past occurrences stay as they happened. From Fri 18, every day.",
            sheet(after, before).rows[1].line,
        )
        val (b2, a2) = moved(tueThu.copy(weekdays = listOf(1, 3, 5)))
        assertEquals("Friday moves; the series keeps its Mon, Wed & Fri rhythm.", sheet(a2, b2).rows[0].line)
    }

    @Test
    fun `a move into another month names the month`() {
        val before = weekly.copy(toDoDate = "2026-09-29", occurrenceDate = "2026-09-29",
            recurrence = RecurrenceDraft(rule = Rule(pattern = Pattern.INTERVAL, interval = 3, unit = IntervalUnit.DAY)))
        val after = before.copy(toDoDate = "2026-10-02")
        assertEquals(
            "Edits forward in place. Past occurrences stay as they happened. From Fri 2 Oct, every 3 days.",
            sheet(after, before).rows[1].line,
        )
    }
}
