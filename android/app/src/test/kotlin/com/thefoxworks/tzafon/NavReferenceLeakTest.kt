package com.thefoxworks.tzafon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FR-NAV-9 — reference-view leak across bottom-nav tab clicks. The crux of the fix is the
 * decision in `goTab`: when the tap departs FROM a reference view (All Tasks / Backlog)
 * that was opened on top of a tab, the reference view must be popped off the stack BEFORE
 * the tab switch, so the save/restore tab machinery can never stash and later restore it
 * onto a tab. These tests pin that classification rule (`isReferenceView`) directly.
 */
class NavReferenceLeakTest {

    @Test
    fun `reference views are exactly all-tasks and backlog`() {
        assertEquals(setOf("alltasks", "backlog"), REFERENCE_ROUTES)
    }

    // FR-NAV-9.1 — a reference view on top must be recognised so goTab pops it first.
    @Test
    fun `reference views are classified as reference`() {
        assertTrue(isReferenceView("alltasks"))
        assertTrue(isReferenceView("backlog"))
    }

    // FR-NAV-9.2 — real tabs are never popped, so within-tab state is preserved.
    @Test
    fun `real tabs are not reference views`() {
        assertFalse(isReferenceView("today"))
        assertFalse(isReferenceView("planning"))
        assertFalse(isReferenceView("habits"))
        assertFalse(isReferenceView("directions"))
        assertFalse(isReferenceView("journey"))
    }

    // A null / unknown route is not a reference view — goTab leaves the stack untouched.
    @Test
    fun `unknown or null route is not a reference view`() {
        assertFalse(isReferenceView(null))
        assertFalse(isReferenceView("editor"))
    }
}
