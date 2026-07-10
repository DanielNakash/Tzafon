package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.ui.today.TodayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FR-TODAY-7 — Today's own quick-add pre-fills `toDoDate` to today; all other capture
 * paths keep FR-CAPTURE-2's undated default. These tests pin the four §2.1 acceptance
 * cases at the pure-draft level so a regression on the seam is caught before UI.
 */
class CaptureDefaultsTest {

    private val today = "2026-07-10"

    @Test
    fun `today quick-add defaults toDoDate to today`() {
        val draft = TodayViewModel.quickAddDraft("walk", today)
        assertEquals(today, draft.toDoDate)
        assertEquals("walk", draft.title)
        assertNull(draft.dueDate)
    }

    @Test
    fun `today quick-add default does not override an explicit user-cleared date in the full editor`() {
        // FR-TODAY-7 §2.1 case 2: expanding + clearing the date in the editor produces
        // an undated task. The expand path does NOT go through TodayViewModel.quickAdd:
        // it hands the typed title to the editor, whose own draft starts undated.
        val editorDraft = TaskDraft(id = null, title = "walk")
        assertNull(editorDraft.toDoDate)
    }

    @Test
    fun `planning and backlog quick-add stay undated`() {
        // FR-TODAY-7 §2.1 case 3: non-Today quick-add still lands in the inbox undated.
        val planning = TaskDraft(id = null, title = "read")
        val backlog = TaskDraft(id = null, title = "buy oats")
        assertNull(planning.toDoDate)
        assertNull(backlog.toDoDate)
    }
}
