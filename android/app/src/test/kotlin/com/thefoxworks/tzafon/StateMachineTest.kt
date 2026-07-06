package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.StateMachine.Guard
import com.thefoxworks.tzafon.domain.model.TaskState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** DM-TASK-1/2/3 + FR-REC-5 + FR-BACKLOG-2-guard transition matrix. */
class StateMachineTest {

    // ── basic flips ───────────────────────────────────────────

    @Test
    fun `open to done applies contribution and stamps completion`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.DONE, isRecurring = false)
        assertTrue(fx.setCompleted)
        assertTrue(fx.applyContribution)
        assertFalse(fx.reverseContribution)
    }

    @Test
    fun `done back to open reverses contribution exactly`() {
        val fx = StateMachine.transition(TaskState.DONE, TaskState.OPEN, isRecurring = false)
        assertTrue(fx.clearCompleted)
        assertTrue(fx.reverseContribution)
        assertFalse(fx.applyContribution)
    }

    @Test
    fun `done to closed reverses the contribution (skip after the fact)`() {
        val fx = StateMachine.transition(TaskState.DONE, TaskState.CLOSED, isRecurring = false)
        assertTrue(fx.clearCompleted)
        assertTrue(fx.reverseContribution)
    }

    @Test
    fun `open to closed is a calm skip - nothing to reverse, links untouched`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.CLOSED, isRecurring = false)
        assertFalse(fx.reverseContribution)
        assertFalse(fx.clearToDoDate) // DM-TASK-8: dates never change automatically
    }

    // ── freeze (FR-REC-5) ─────────────────────────────────────

    @Test
    fun `freezing a recurring task needs confirmation and terminates the series`() {
        assertEquals(
            Guard.ConfirmFreezeSeries,
            StateMachine.guardFor(TaskState.OPEN, TaskState.FROZEN, isRecurring = true),
        )
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.FROZEN, isRecurring = true)
        assertTrue(fx.freezeSeries)
    }

    @Test
    fun `freezing a standalone task needs no confirmation`() {
        assertNull(StateMachine.guardFor(TaskState.OPEN, TaskState.FROZEN, isRecurring = false))
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.FROZEN, isRecurring = false)
        assertFalse(fx.freezeSeries)
    }

    // ── thaw (DM-TASK-2) ──────────────────────────────────────

    @Test
    fun `thaw to open restores the series and preserves dates`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.OPEN, isRecurring = true)
        assertTrue(fx.restoreSeries)
        assertFalse(fx.clearToDoDate)
    }

    @Test
    fun `thaw to done applies contribution and restores the series`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.DONE, isRecurring = true)
        assertTrue(fx.applyContribution)
        assertTrue(fx.setCompleted)
        assertTrue(fx.restoreSeries)
    }

    @Test
    fun `thaw to closed just settles - series stays terminated`() {
        val fx = StateMachine.transition(TaskState.FROZEN, TaskState.CLOSED, isRecurring = true)
        assertFalse(fx.restoreSeries)
        assertFalse(fx.applyContribution)
    }

    // ── backlog (FR-BACKLOG guards; the full table lands M3) ──

    @Test
    fun `backlog clears the date - unscheduled by definition`() {
        val fx = StateMachine.transition(TaskState.OPEN, TaskState.BACKLOG, isRecurring = false)
        assertTrue(fx.clearToDoDate)
    }

    @Test
    fun `done to backlog resurrects - clears the mark and reverses`() {
        val fx = StateMachine.transition(TaskState.DONE, TaskState.BACKLOG, isRecurring = false)
        assertTrue(fx.clearCompleted)
        assertTrue(fx.reverseContribution)
        assertTrue(fx.clearToDoDate)
    }

    @Test
    fun `recurring to backlog is forbidden - the guard offers frozen`() {
        assertEquals(
            Guard.RecurringCannotBacklog,
            StateMachine.guardFor(TaskState.OPEN, TaskState.BACKLOG, isRecurring = true),
        )
        assertThrows(IllegalArgumentException::class.java) {
            StateMachine.transition(TaskState.OPEN, TaskState.BACKLOG, isRecurring = true)
        }
    }

    @Test
    fun `every non-backlog transition is reachable - no dead ends`() {
        val states = TaskState.entries
        for (from in states) {
            for (to in states) {
                if (to == TaskState.BACKLOG) continue
                // must not throw
                StateMachine.transition(from, to, isRecurring = true)
                StateMachine.transition(from, to, isRecurring = false)
            }
        }
    }

    // ── M3 — the full FR-BACKLOG-3 out-of-backlog table ───────

    @Test
    fun `backlog to open is the pull - nothing reversed, links reactivate forward`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.OPEN, isRecurring = false)
        assertEquals(TaskState.OPEN, fx.newState)
        assertFalse(fx.reverseContribution)
        assertFalse(fx.clearToDoDate)
    }

    @Test
    fun `backlog to done - did it without scheduling, counted once`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.DONE, isRecurring = false)
        assertEquals(TaskState.DONE, fx.newState)
        assertTrue(fx.setCompleted)
        assertTrue(fx.applyContribution)
    }

    @Test
    fun `backlog to closed - decided against it`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.CLOSED, isRecurring = false)
        assertEquals(TaskState.CLOSED, fx.newState)
        assertFalse(fx.reverseContribution) // nothing was ever contributed
    }

    @Test
    fun `backlog to frozen is a no-op - both are parked`() {
        val fx = StateMachine.transition(TaskState.BACKLOG, TaskState.FROZEN, isRecurring = false)
        assertEquals(TaskState.BACKLOG, fx.newState)
        assertFalse(fx.freezeSeries)
        assertFalse(fx.clearCompleted)
    }
}
