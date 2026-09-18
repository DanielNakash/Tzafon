package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.backlog.BacklogViewModel
import com.thefoxworks.tzafon.ui.planning.PlanningViewModel
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
        val planning = PlanningViewModel.quickAddDraft("read", today)
        val backlog = BacklogViewModel.quickAddDraft("buy oats")
        assertNull(planning.toDoDate)
        assertNull(backlog.toDoDate)
    }

    // ── FR-CAPTURE-3 / FR-PLAN-7 — the parsed cue per surface ──

    @Test
    fun `today quick-add keeps its date and gains the parsed cue`() {
        // FR-CAPTURE-3.9 — the date rule is untouched; the cue rides along.
        val draft = TodayViewModel.quickAddDraft("Call the bank", today, "08:30")
        assertEquals(today, draft.toDoDate)
        assertEquals("Call the bank", draft.title)
        assertEquals(CueType.AT_TIME, draft.cue?.type)
        assertEquals("08:30", draft.cue?.time)
        assertEquals("", draft.cue?.label)
        assertEquals(TaskState.OPEN, draft.state)
    }

    @Test
    fun `planning quick-add with a parsed time is dated today`() {
        // FR-PLAN-7.1 — the one exception to FR-CAPTURE-2, conditional on the parse.
        val draft = PlanningViewModel.quickAddDraft("Call the bank", today, "08:30")
        assertEquals(today, draft.toDoDate)
        assertEquals("08:30", draft.cue?.time)
        assertEquals(TaskState.OPEN, draft.state) // FR-PLAN-7.7
    }

    @Test
    fun `planning quick-add without a time is still undated in the inbox`() {
        // FR-PLAN-7.1 — the v2.1.0 decision stands for a bare capture.
        val draft = PlanningViewModel.quickAddDraft("Call the bank", today)
        assertNull(draft.toDoDate)
        assertNull(draft.cue)
    }

    @Test
    fun `backlog quick-add stores the cue but keeps its state contract`() {
        // FR-CAPTURE-3.8 / FR-BACKLOG-5.1 — BACKLOG and undated, cue preserved.
        val draft = BacklogViewModel.quickAddDraft("Order new boots", "19:00")
        assertEquals(TaskState.BACKLOG, draft.state)
        assertNull(draft.toDoDate)
        assertEquals("Order new boots", draft.title)
        assertEquals("19:00", draft.cue?.time)
    }

    @Test
    fun `no parsed time leaves every surface byte-identical to v2_11_0`() {
        // FR-CAPTURE-3.3 — the overwhelmingly common case must not move.
        assertEquals(
            TodayViewModel.quickAddDraft("walk", today),
            TodayViewModel.quickAddDraft("walk", today, null),
        )
        assertNull(TodayViewModel.quickAddDraft("walk", today).cue)
        assertNull(BacklogViewModel.quickAddDraft("buy oats").cue)
        assertEquals(TaskState.BACKLOG, BacklogViewModel.quickAddDraft("buy oats").state)
    }
}
