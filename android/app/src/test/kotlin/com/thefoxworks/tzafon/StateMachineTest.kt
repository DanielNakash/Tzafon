package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.StateMachine.Ask
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.TaskState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DM-TASK-1/2/3 + FR-REC-5 + FR-BACKLOG-2-guard transition matrix, and the
 * v2.13.0 series-scoped rules (freeze, thaw, close and reopen a series).
 */
class StateMachineTest {

    private val live = SeriesStatus(frozen = false, ended = false)
    private val frozen = SeriesStatus(frozen = true, ended = false)
    private val ended = SeriesStatus(frozen = false, ended = true)

    // ── basic flips ───────────────────────────────────────────

    @Test
    fun `open to done applies contribution and stamps completion`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.DONE, series = null)
        assertTrue(fx.setCompleted)
        assertTrue(fx.applyContribution)
        assertFalse(fx.reverseContribution)
    }

    @Test
    fun `done back to open reverses contribution exactly`() {
        val fx = StateMachine.transition(TaskState.DONE, TaskState.OPEN, series = null)
        assertTrue(fx.clearCompleted)
        assertTrue(fx.reverseContribution)
        assertFalse(fx.applyContribution)
    }

    @Test
    fun `done to closed reverses the contribution (skip after the fact)`() {
        val fx = StateMachine.transition(TaskState.DONE, TaskState.CLOSED, series = null)
        assertTrue(fx.clearCompleted)
        assertTrue(fx.reverseContribution)
    }

    @Test
    fun `open to closed is a calm skip - nothing to reverse, links untouched`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.CLOSED, series = null)
        assertFalse(fx.reverseContribution)
        assertFalse(fx.clearToDoDate) // DM-TASK-8: dates never change automatically
    }

    // ── freeze (FR-REC-5, v2.13.0: always the whole series) ───

    @Test
    fun `freezing a repeating task never asks for a scope and freezes the series`() {
        assertNull(StateMachine.askFor(TaskState.OPEN, TaskState.FROZEN, live))
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.FROZEN, live, StatusScope.THIS_ONE)
        assertEquals(TaskState.FROZEN, fx.newState)
        assertTrue(fx.freezeSeries)
    }

    @Test
    fun `freezing an occurrence that is already frozen still freezes the series`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.FROZEN, live)
        assertTrue(fx.freezeSeries)
    }

    @Test
    fun `freezing a one-off task asks nothing and touches no series`() {
        assertNull(StateMachine.askFor(TaskState.OPEN, TaskState.FROZEN, series = null))
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.FROZEN, series = null)
        assertFalse(fx.freezeSeries)
    }

    // ── thaw (DM-TASK-2, v2.13.0: asks while the series is frozen) ──

    @Test
    fun `thawing an occurrence of a frozen series asks this one or whole series`() {
        for (target in listOf(TaskState.OPEN, TaskState.DONE, TaskState.CLOSED)) {
            assertEquals(Ask.Scope, StateMachine.askFor(TaskState.FROZEN, target, frozen))
        }
    }

    @Test
    fun `thaw this one to open changes only the occurrence - series stays frozen`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.OPEN, frozen, StatusScope.THIS_ONE)
        assertEquals(TaskState.OPEN, fx.newState)
        assertFalse(fx.thawSeries)
        assertFalse(fx.clearToDoDate)
    }

    @Test
    fun `thaw whole series to open thaws the series and keeps the dates`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.OPEN, frozen, StatusScope.WHOLE_SERIES)
        assertTrue(fx.thawSeries)
        assertFalse(fx.clearToDoDate)
    }

    @Test
    fun `thaw whole series to done counts the occurrence and thaws the series`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.DONE, frozen, StatusScope.WHOLE_SERIES)
        assertTrue(fx.applyContribution)
        assertTrue(fx.setCompleted)
        assertTrue(fx.thawSeries)
    }

    @Test
    fun `thaw whole series to closed closes the series instead of thawing it`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.CLOSED, frozen, StatusScope.WHOLE_SERIES)
        assertFalse(fx.thawSeries)
        assertTrue(fx.closeSeries)
        assertFalse(fx.applyContribution)
    }

    @Test
    fun `a leftover frozen occurrence of a live series thaws alone and never asks`() {
        assertNull(StateMachine.askFor(TaskState.FROZEN, TaskState.OPEN, live))
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.OPEN, live, StatusScope.WHOLE_SERIES)
        assertFalse(fx.thawSeries)
    }

    // ── close / reopen a series (v2.13.0) ─────────────────────

    @Test
    fun `closing a repeating task asks this one or whole series`() {
        assertEquals(Ask.Scope, StateMachine.askFor(TaskState.OPEN, TaskState.CLOSED, live))
        assertEquals(Ask.Scope, StateMachine.askFor(TaskState.DONE, TaskState.CLOSED, live))
    }

    @Test
    fun `close this one settles only the occurrence`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.CLOSED, live, StatusScope.THIS_ONE)
        assertEquals(TaskState.CLOSED, fx.newState)
        assertFalse(fx.closeSeries)
    }

    @Test
    fun `close whole series closes the series`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.CLOSED, live, StatusScope.WHOLE_SERIES)
        assertTrue(fx.closeSeries)
    }

    @Test
    fun `close whole series on an occurrence already closed still closes the series`() {
        val fx = StateMachine.transition(TaskState.CLOSED, TaskState.CLOSED, live, StatusScope.WHOLE_SERIES)
        assertEquals(TaskState.CLOSED, fx.newState)
        assertTrue(fx.closeSeries)
    }

    @Test
    fun `a closed occurrence of an ended series asks before moving to open or done`() {
        assertEquals(Ask.Scope, StateMachine.askFor(TaskState.CLOSED, TaskState.OPEN, ended))
        assertEquals(Ask.Scope, StateMachine.askFor(TaskState.CLOSED, TaskState.DONE, ended))
    }

    @Test
    fun `reopen whole series from a closed occurrence reopens the series`() {
        val open = StateMachine.transition(TaskState.CLOSED, TaskState.OPEN, ended, StatusScope.WHOLE_SERIES)
        assertTrue(open.reopenSeries)
        val done = StateMachine.transition(TaskState.CLOSED, TaskState.DONE, ended, StatusScope.WHOLE_SERIES)
        assertTrue(done.reopenSeries)
        assertTrue(done.applyContribution)
    }

    @Test
    fun `reopen this one changes only the occurrence`() {
        val fx = StateMachine.transition(TaskState.CLOSED, TaskState.OPEN, ended, StatusScope.THIS_ONE)
        assertEquals(TaskState.OPEN, fx.newState)
        assertFalse(fx.reopenSeries)
    }

    @Test
    fun `a closed occurrence of a live series moves alone and never asks`() {
        assertNull(StateMachine.askFor(TaskState.CLOSED, TaskState.OPEN, live))
        val fx = StateMachine.transition(TaskState.CLOSED, TaskState.OPEN, live, StatusScope.WHOLE_SERIES)
        assertFalse(fx.reopenSeries)
    }

    // ── open / done outside thawing: the occurrence only ─────

    @Test
    fun `open and done on a repeating task never ask and ignore a whole-series scope`() {
        assertNull(StateMachine.askFor(TaskState.OPEN, TaskState.DONE, live))
        assertNull(StateMachine.askFor(TaskState.DONE, TaskState.OPEN, live))
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.DONE, live, StatusScope.WHOLE_SERIES)
        assertFalse(fx.freezeSeries || fx.thawSeries || fx.closeSeries || fx.reopenSeries)
    }

    @Test
    fun `a one-off task never asks for a scope`() {
        for (from in TaskState.entries) for (to in TaskState.entries) {
            assertNull("$from -> $to", StateMachine.askFor(from, to, series = null))
        }
    }

    // ── backlog (FR-BACKLOG guards; the full table lands M3) ──

    @Test
    fun `backlog clears the date - unscheduled by definition`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.BACKLOG, series = null)
        assertTrue(fx.clearToDoDate)
    }

    @Test
    fun `done to backlog resurrects - clears the mark and reverses`() {
        val fx = StateMachine.transition(TaskState.DONE, TaskState.BACKLOG, series = null)
        assertTrue(fx.clearCompleted)
        assertTrue(fx.reverseContribution)
        assertTrue(fx.clearToDoDate)
    }

    @Test
    fun `recurring to backlog is forbidden - the guard offers frozen`() {
        assertEquals(
            Ask.RecurringCannotBacklog,
            StateMachine.askFor(TaskState.OPEN, TaskState.BACKLOG, series = live),
        )
        assertThrows(IllegalArgumentException::class.java) {
            StateMachine.transition(TaskState.OPEN, TaskState.BACKLOG, series = live)
        }
    }

    @Test
    fun `every non-backlog transition is reachable - no dead ends`() {
        val states = TaskState.entries
        for (from in states) {
            for (to in states) {
                if (to == TaskState.BACKLOG) continue
                // must not throw
                StateMachine.transition(from, to, series = live)
                StateMachine.transition(from, to, series = null)
            }
        }
    }

    // ── M3 — the full FR-BACKLOG-3 out-of-backlog table ───────

    @Test
    fun `backlog to open is the pull - nothing reversed, links reactivate forward`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.OPEN, series = null)
        assertEquals(TaskState.OPEN, fx.newState)
        assertFalse(fx.reverseContribution)
        assertFalse(fx.clearToDoDate)
    }

    @Test
    fun `backlog to done - did it without scheduling, counted once`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.DONE, series = null)
        assertEquals(TaskState.DONE, fx.newState)
        assertTrue(fx.setCompleted)
        assertTrue(fx.applyContribution)
    }

    @Test
    fun `backlog to closed - decided against it`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.CLOSED, series = null)
        assertEquals(TaskState.CLOSED, fx.newState)
        assertFalse(fx.reverseContribution) // nothing was ever contributed
    }

    @Test
    fun `backlog to frozen is a no-op - both are parked`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.FROZEN, series = null)
        assertEquals(TaskState.BACKLOG, fx.newState)
        assertFalse(fx.freezeSeries)
        assertFalse(fx.clearCompleted)
    }
}
