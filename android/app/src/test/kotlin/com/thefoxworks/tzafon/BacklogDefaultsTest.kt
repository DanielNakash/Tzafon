package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.backlog.BacklogViewModel
import com.thefoxworks.tzafon.ui.editor.toggleRecurrence
import com.thefoxworks.tzafon.ui.today.TodayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FR-BACKLOG-5 — a task created FROM the Backlog view defaults to `state = BACKLOG`
 * and `toDoDate = null`, so it lands in the someday pool instead of Today's Open pool.
 * These tests pin the §5.3 acceptance cases at the pure-draft level.
 */
class BacklogDefaultsTest {

    private val today = "2026-07-13"

    // FR-BACKLOG-5.1 — quick-add from Backlog writes BACKLOG state, undated.
    @Test
    fun `backlog quick-add defaults to BACKLOG state and no toDoDate`() {
        val draft = BacklogViewModel.quickAddDraft("buy oats")
        assertEquals(TaskState.BACKLOG, draft.state)
        assertEquals("buy oats", draft.title)
        assertNull(draft.toDoDate)
    }

    // FR-BACKLOG-5.5 — other views' quick-add stays OPEN and does NOT land in Backlog.
    @Test
    fun `today quick-add stays OPEN and undated-for-backlog`() {
        val draft = TodayViewModel.quickAddDraft("walk", today)
        assertEquals(TaskState.OPEN, draft.state)
        assertEquals(today, draft.toDoDate)
    }

    // FR-BACKLOG-5.5 — the TaskDraft default (used by Planning / All Tasks / capture) is OPEN.
    @Test
    fun `default TaskDraft state is OPEN`() {
        assertEquals(TaskState.OPEN, TaskDraft(id = null, title = "read").state)
    }

    // FR-BACKLOG-5.3 — adding recurrence to a Backlog-defaulted draft auto-promotes it
    // to Open with today's To Do date (Backlog + recurrence are incompatible, FR-BACKLOG-4).
    @Test
    fun `adding recurrence to a Backlog draft promotes it to Open with todays date`() {
        val backlog = TaskDraft(id = null, title = "stretch", state = TaskState.BACKLOG, toDoDate = null)
        val recurring = toggleRecurrence(backlog, today)
        assertNotNull(recurring.recurrence)
        assertEquals(TaskState.OPEN, recurring.state)
        assertEquals(today, recurring.toDoDate)
    }

    // FR-BACKLOG-5.3 — a non-Backlog draft keeps its state when recurrence is added.
    @Test
    fun `adding recurrence to an Open draft leaves its state and date alone`() {
        val open = TaskDraft(id = null, title = "gym", state = TaskState.OPEN, toDoDate = "2026-07-20")
        val recurring = toggleRecurrence(open, today)
        assertNotNull(recurring.recurrence)
        assertEquals(TaskState.OPEN, recurring.state)
        assertEquals("2026-07-20", recurring.toDoDate)
    }

    // Toggling recurrence back off clears the rule and does not touch state.
    @Test
    fun `removing recurrence clears the rule and preserves state`() {
        val backlog = TaskDraft(id = null, title = "stretch", state = TaskState.BACKLOG)
        val on = toggleRecurrence(backlog, today)
        val off = toggleRecurrence(on, today)
        assertNull(off.recurrence)
        // state was already promoted to OPEN when recurrence was added; it stays put.
        assertEquals(TaskState.OPEN, off.state)
    }
}
