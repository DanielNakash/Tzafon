package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.backlog.BacklogViewModel
import com.thefoxworks.tzafon.ui.today.TodayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FR-BACKLOG-5 — a task created FROM the Backlog view defaults to `state = BACKLOG`
 * and `toDoDate = null`, so it lands in the someday pool instead of Today's Open pool.
 * These tests pin the §5.3 acceptance cases at the pure-draft level. Turning
 * repeat on for a Backlog draft (FR-BACKLOG-5.3) lives in `RepeatCardTest` since
 * v2.13.0 (FR-EDITOR-5.10).
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
}
