package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.CueDraft
import com.thefoxworks.tzafon.domain.editor.EditorSession
import com.thefoxworks.tzafon.domain.editor.SaveProblem
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.TaskDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.13.0 — the Alignment pane's inline cue: the fields each type saves, when
 * a cue exists at all, and the incomplete time that stops Save.
 */
class CueDraftTest {

    @Test
    fun `at a time needs a time and keeps an optional label`() {
        assertEquals(Cue(CueType.AT_TIME, "", "08:30"), CueDraft(CueType.AT_TIME, "", "08:30").cue())
        assertEquals(
            Cue(CueType.AT_TIME, "Right after lunch", "13:00"),
            CueDraft(CueType.AT_TIME, " Right after lunch ", " 13:00 ").cue(),
        )
        assertNull(CueDraft(CueType.AT_TIME, "Right after lunch", "").cue())
    }

    @Test
    fun `after a routine needs a label and keeps an optional time`() {
        assertEquals(Cue(CueType.AFTER_ROUTINE, "After coffee"), CueDraft(CueType.AFTER_ROUTINE, "After coffee", "").cue())
        assertEquals(
            Cue(CueType.AFTER_ROUTINE, "After coffee", "07:15"),
            CueDraft(CueType.AFTER_ROUTINE, "After coffee", "07:15").cue(),
        )
        assertNull(CueDraft(CueType.AFTER_ROUTINE, "  ", "07:15").cue())
    }

    @Test
    fun `at a place takes a label only and drops a stray time`() {
        assertEquals(Cue(CueType.AT_PLACE, "At the studio"), CueDraft(CueType.AT_PLACE, "At the studio", "09:00").cue())
        assertNull(CueDraft(CueType.AT_PLACE, "", "09:00").cue())
    }

    @Test
    fun `typed text survives switching type`() {
        val typed = CueDraft(CueType.AFTER_ROUTINE, "After coffee", "07:15")
        val back = typed.select(CueType.AT_PLACE).select(CueType.AFTER_ROUTINE)
        assertEquals(typed, back)
        assertEquals(Cue(CueType.AT_PLACE, "After coffee"), typed.select(CueType.AT_PLACE).cue())
    }

    @Test
    fun `a stored cue opens as its draft and an absent one opens empty on after a routine`() {
        val stored = Cue(CueType.AT_TIME, "", "08:30")
        assertEquals(stored, CueDraft.of(stored).cue())
        assertEquals(CueDraft(CueType.AFTER_ROUTINE, "", ""), CueDraft.of(null))
        assertNull(CueDraft.of(null).cue())
    }

    @Test
    fun `clearing empties both fields and keeps the type`() {
        val cleared = CueDraft(CueType.AT_TIME, "Lunch", "13:00").clear()
        assertEquals(CueDraft(CueType.AT_TIME, "", ""), cleared)
        assertNull(cleared.cue())
    }

    @Test
    fun `the label-less time cue still displays as the time alone`() {
        assertEquals("08:30", CueDraft(CueType.AT_TIME, "", "08:30").cue()!!.display())
    }

    @Test
    fun `a time typed but not yet HH mm is incomplete where the time field shows`() {
        assertTrue(CueDraft(CueType.AT_TIME, "", "8:3").timeIncomplete)
        assertTrue(CueDraft(CueType.AFTER_ROUTINE, "After coffee", "24:00").timeIncomplete)
        assertNull(CueDraft(CueType.AT_TIME, "", "8:3").cue())
    }

    @Test
    fun `an empty time or a hidden one is never incomplete`() {
        assertFalse(CueDraft(CueType.AT_TIME, "", "").timeIncomplete)
        assertFalse(CueDraft(CueType.AFTER_ROUTINE, "After coffee", "  ").timeIncomplete)
        assertFalse(CueDraft(CueType.AT_PLACE, "At the studio", "8:3").timeIncomplete)
        assertFalse(CueDraft(CueType.AT_TIME, "", "08:30").timeIncomplete)
    }

    @Test
    fun `an incomplete cue time stops Save after a blank title`() {
        val session = EditorSession.open(TaskDraft(id = "T1", title = "Write the report"))
        val halfTyped = CueDraft(CueType.AT_TIME, "", "8:3")
        assertEquals(SaveProblem.INCOMPLETE_CUE_TIME, session.saveProblem(halfTyped))
        assertEquals(SaveProblem.BLANK_TITLE, session.edit { it.copy(title = " ") }.saveProblem(halfTyped))
        assertNull(session.saveProblem(CueDraft(CueType.AT_TIME, "", "08:30")))
        assertNull(session.saveProblem(CueDraft(CueType.AT_PLACE, "At the studio", "8:3")))
    }
}
