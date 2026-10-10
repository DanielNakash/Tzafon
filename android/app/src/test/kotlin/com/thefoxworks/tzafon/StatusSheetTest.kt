package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.ScopeOption
import com.thefoxworks.tzafon.domain.status.ScopePanel
import com.thefoxworks.tzafon.domain.status.StatusPick
import com.thefoxworks.tzafon.domain.status.StatusRow
import com.thefoxworks.tzafon.domain.status.StatusSheet
import com.thefoxworks.tzafon.domain.status.StatusSubject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v2.13.0 — the status sheet's words and rules: which rows are live, dimmed
 * or current, whether a pick applies at once or asks a scope, the scope
 * panel's options, the Done line and the header's meta line.
 */
class StatusSheetTest {

    private val live = SeriesStatus(frozen = false, ended = false)
    private val oneOff = StatusSubject(title = "Call the bank", state = TaskState.OPEN, toDoDate = "2026-09-17")
    private val repeating = StatusSubject(
        title = "Write 500 words",
        state = TaskState.OPEN,
        toDoDate = "2026-09-17",
        repeat = "Every Tuesday & Thursday",
        series = live,
    )

    @Test
    fun `a one-off task has five live rows, Done first, with NOW on its status`() {
        assertEquals(
            listOf(
                StatusRow(TaskState.DONE, "Done", "Finished. Files into Journey."),
                StatusRow(TaskState.OPEN, "Open", "Back on your list.", now = true),
                StatusRow(TaskState.CLOSED, "Closed", "No longer relevant. Kept in history, never counted as a miss."),
                StatusRow(TaskState.FROZEN, "Frozen", "On hold and hidden from the calendar until you thaw it."),
                StatusRow(TaskState.BACKLOG, "Backlog", "Off the calendar, into the backlog."),
            ),
            StatusSheet.rows(oneOff),
        )
    }

    @Test
    fun `a repeating task dims Backlog with its reason and says Frozen reaches the series`() {
        val rows = StatusSheet.rows(repeating).associateBy { it.status }
        assertEquals(
            StatusRow(TaskState.BACKLOG, "Backlog", "Off the calendar, into the backlog.", enabled = false, tag = "REPEATS — USE FREEZE"),
            rows.getValue(TaskState.BACKLOG),
        )
        assertEquals("Freezes the whole series until you thaw it.", rows.getValue(TaskState.FROZEN).description)
    }

    // ── header and footnote ──

    @Test
    fun `the meta line reads the to-do date, the repeat and the row's habit label`() {
        assertEquals("THU 17 SEP", StatusSheet.meta(oneOff))
        assertEquals("NO DATE", StatusSheet.meta(oneOff.copy(toDoDate = null, state = TaskState.BACKLOG)))
        assertEquals(
            "THU 17 SEP · EVERY TUESDAY & THURSDAY · TICKS WRITE IN THE MORNING",
            StatusSheet.meta(repeating.copy(habit = "Write in the morning", goals = listOf("First draft"))),
        )
    }

    @Test
    fun `the footnote explains the dimmed Backlog only when the task repeats`() {
        assertEquals(listOf("Nothing here is a failure."), StatusSheet.footnote(oneOff))
        assertEquals(
            listOf(
                "Backlog is unavailable while this repeats. Freeze it instead, or turn the repeat off in the editor.",
                "Nothing here is a failure.",
            ),
            StatusSheet.footnote(repeating),
        )
    }

    private fun done(habit: String?, vararg goals: String) =
        StatusSheet.rows(oneOff.copy(habit = habit, goals = goals.toList())).first().description

    @Test
    fun `Done names the habit first, then the goals`() {
        assertEquals("Counts toward Write in the morning.", done("Write in the morning"))
        assertEquals("Counts toward First draft.", done(null, "First draft"))
        assertEquals("Counts toward Write in the morning and First draft.", done("Write in the morning", "First draft"))
    }

    @Test
    fun `Done past two links names two and counts the rest`() {
        assertEquals("Counts toward Run, First draft and 1 more.", done("Run", "First draft", "Half marathon"))
        assertEquals("Counts toward A, B and 2 more.", done(null, "A", "B", "C", "D"))
    }

    // ── apply at once, ask a scope, or stay ──

    private val frozen = SeriesStatus(frozen = true, ended = false)
    private val ended = SeriesStatus(frozen = false, ended = true)
    private val onlyThis = "Only this occurrence changes."

    private fun panel(current: TaskState, target: TaskState, series: SeriesStatus?) =
        (StatusSheet.pick(current, target, series) as StatusPick.AskScope).panel

    @Test
    fun `a one-off task applies every other status at once and stays on its own`() {
        TaskState.entries.filter { it != TaskState.OPEN }.forEach {
            assertEquals(StatusPick.Apply(it), StatusSheet.pick(TaskState.OPEN, it, null))
        }
        assertEquals(StatusPick.Stay, StatusSheet.pick(TaskState.OPEN, TaskState.OPEN, null))
    }

    @Test
    fun `Open and Done on an occurrence of a live series apply at once`() {
        assertEquals(StatusPick.Apply(TaskState.DONE), StatusSheet.pick(TaskState.OPEN, TaskState.DONE, live))
        assertEquals(StatusPick.Apply(TaskState.OPEN), StatusSheet.pick(TaskState.DONE, TaskState.OPEN, live))
        // a leftover Frozen occurrence of a live series thaws alone
        assertEquals(StatusPick.Apply(TaskState.OPEN), StatusSheet.pick(TaskState.FROZEN, TaskState.OPEN, live))
        // an occurrence thawed alone freezes alone again: its series is still frozen
        assertEquals(StatusPick.Apply(TaskState.FROZEN), StatusSheet.pick(TaskState.OPEN, TaskState.FROZEN, frozen))
    }

    @Test
    fun `Backlog on a repeating task stays put`() {
        assertEquals(StatusPick.Stay, StatusSheet.pick(TaskState.OPEN, TaskState.BACKLOG, live))
    }

    @Test
    fun `Closed on a repeating task asks, defaulting to just this one`() {
        assertEquals(
            ScopePanel(
                target = TaskState.CLOSED,
                heading = "CLOSE WHAT?",
                options = listOf(
                    ScopeOption(StatusScope.THIS_ONE, "Just this one", onlyThis, action = "Close this one"),
                    ScopeOption(
                        StatusScope.WHOLE_SERIES, "The whole series",
                        "Ends the series today. Past ones not done are closed; later ones stop appearing.",
                        action = "Close series",
                    ),
                ),
                default = StatusScope.THIS_ONE,
            ),
            panel(TaskState.OPEN, TaskState.CLOSED, live),
        )
    }

    @Test
    fun `Frozen on a repeating task has no choice, only Freeze series`() {
        assertEquals(
            ScopePanel(
                target = TaskState.FROZEN,
                heading = "FREEZE THE SERIES",
                options = listOf(
                    ScopeOption(
                        StatusScope.WHOLE_SERIES, "The whole series",
                        "No occurrences until you thaw it. This one stays, frozen.",
                        action = "Freeze series",
                    ),
                ),
                default = StatusScope.WHOLE_SERIES,
            ),
            panel(TaskState.OPEN, TaskState.FROZEN, live),
        )
    }

    @Test
    fun `thawing a frozen series asks, and the button follows the choice`() {
        val thawLine = "Occurrences come back from today. The days it spent frozen stay gone."
        val open = panel(TaskState.FROZEN, TaskState.OPEN, frozen)
        assertEquals("THAW WHAT?", open.heading)
        assertEquals(listOf("Mark open", "Thaw series"), open.options.map { it.action })
        assertEquals(listOf(onlyThis, thawLine), open.options.map { it.line })
        assertEquals(listOf("Mark done", "Thaw series"), panel(TaskState.FROZEN, TaskState.DONE, frozen).options.map { it.action })
        // thaw → Closed reads as Closed
        assertEquals(listOf("Close this one", "Close series"), panel(TaskState.FROZEN, TaskState.CLOSED, frozen).options.map { it.action })
    }

    @Test
    fun `reopening a closed series asks`() {
        val reopen = panel(TaskState.CLOSED, TaskState.OPEN, ended)
        assertEquals("REOPEN WHAT?", reopen.heading)
        assertEquals(listOf("Mark open", "Reopen series"), reopen.options.map { it.action })
        assertEquals("Occurrences come back from today. Past ones are never changed.", reopen.options[1].line)
        assertEquals(listOf("Mark done", "Reopen series"), panel(TaskState.CLOSED, TaskState.DONE, ended).options.map { it.action })
    }

    @Test
    fun `the current status's row stays, unless its series can still follow`() {
        // Frozen occurrence of a live series: Freeze series
        assertEquals("Freeze series", panel(TaskState.FROZEN, TaskState.FROZEN, live).options.single().action)
        assertEquals(StatusPick.Stay, StatusSheet.pick(TaskState.FROZEN, TaskState.FROZEN, frozen))
        // Closed occurrence of a series still running: Close series, with just this one greyed
        val close = panel(TaskState.CLOSED, TaskState.CLOSED, live)
        assertEquals(listOf(false, true), close.options.map { it.enabled })
        assertEquals(StatusScope.WHOLE_SERIES, close.default)
        assertEquals(StatusPick.Stay, StatusSheet.pick(TaskState.CLOSED, TaskState.CLOSED, ended))
        assertEquals(StatusPick.Stay, StatusSheet.pick(TaskState.DONE, TaskState.DONE, live))
    }
}
