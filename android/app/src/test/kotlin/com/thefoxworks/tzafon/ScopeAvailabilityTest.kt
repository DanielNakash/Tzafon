package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.DraftDiff
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.13.0 — which edit scopes a repeating task's changes allow, which one the
 * sheet starts on, and whether the sheet is asked at all.
 */
class ScopeAvailabilityTest {

    private val weekly = RecurrenceDraft(rule = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2)))
    private val base = TaskDraft(
        id = "S1__2026-10-13", title = "Run", toDoDate = "2026-10-13",
        recurrence = weekly, seriesId = "S1", occurrenceDate = "2026-10-13",
    )

    private fun changes(draft: TaskDraft) = DraftDiff.of(base, draft)

    @Test
    fun `field and date edits allow every scope and start on just this one`() {
        listOf(
            base.copy(title = "Run 5k"),
            base.copy(description = "Easy pace"),
            base.copy(habitId = "H1"),
            base.copy(goalIds = listOf("G1")),
            base.copy(toDoDate = "2026-10-14"),
            base.copy(dueDate = "2026-10-15"),
        ).forEach { draft ->
            val c = changes(draft)
            assertTrue(EditScope.entries.all { c.allows(it) })
            assertEquals(EditScope.ONE, c.defaultScope)
            assertTrue(c.asksScope)
        }
    }

    @Test
    fun `a repeat card change rules out just this one and starts on forward`() {
        listOf(
            base.copy(recurrence = weekly.copy(rule = weekly.rule.copy(weekdays = listOf(3)))),
            base.copy(recurrence = weekly.copy(endDate = "2026-12-31")),
        ).forEach { draft ->
            val c = changes(draft)
            assertFalse(c.allows(EditScope.ONE))
            assertTrue(c.allows(EditScope.FORWARD))
            assertTrue(c.allows(EditScope.ALL))
            assertEquals(EditScope.FORWARD, c.defaultScope)
        }
    }

    @Test
    fun `turning repeat off allows forward only`() {
        val c = changes(base.copy(recurrence = null))
        assertEquals(listOf(EditScope.FORWARD), EditScope.entries.filter { c.allows(it) })
        assertEquals(EditScope.FORWARD, c.defaultScope)
    }

    @Test
    fun `no changes, or only a status, asks no scope`() {
        assertFalse(changes(base).asksScope)
        assertFalse(changes(base.copy(state = TaskState.DONE)).asksScope)
        assertTrue(changes(base.copy(state = TaskState.DONE, title = "Run 5k")).asksScope)
    }
}
