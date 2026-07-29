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

    // ── FR-NAV-11 — tab tap always lands on tab base; lateral hops don't accumulate ──

    /**
     * FR-NAV-11 control case — the original v2.5.0 single-hop leak: opening
     * Backlog on top of Today, then switching tabs, then returning to Today
     * must land on Today (not on Backlog stashed under Today's saveState).
     */
    @Test
    fun `FR-NAV-9 single-hop control - reference view is popped before tab switch`() {
        val end = simulateNav(
            startStack = listOf("today"),
            actions = listOf(
                NavAction.GoRef("backlog"),
                NavAction.GoTab("habits"),
                NavAction.GoTab("today"),
            ),
        )
        assertEquals("landed on today, not backlog", "today", end.last())
        assertFalse("no reference view on the stack", end.any { it in REFERENCE_ROUTES })
    }

    /**
     * FR-NAV-11.2 / FR-NAV-11.8 — a lateral hop from All Tasks to Backlog
     * collapses onto the entry tab, not on top of All Tasks. So five
     * alternations leave at most one reference view above the entry tab.
     */
    @Test
    fun `lateral alltasks-backlog hops do not accumulate on the stack`() {
        val end = simulateNav(
            startStack = listOf("today", "habits"),
            actions = listOf(
                NavAction.GoRef("alltasks"),
                NavAction.GoRef("backlog"),
                NavAction.GoRef("alltasks"),
                NavAction.GoRef("backlog"),
                NavAction.GoRef("alltasks"),
            ),
        )
        assertEquals("exactly one reference view above the entry tab", 1, end.count { it in REFERENCE_ROUTES })
        assertEquals(listOf("today", "habits", "alltasks"), end)
    }

    /**
     * FR-NAV-11.4 — the escalation the user hit: repeated lateral hops must
     * NOT force `2×` tab presses to reach Habits. A single Habits tap after
     * any number of hops must land on Habits with no reference view on top.
     */
    @Test
    fun `single tab tap lands on Habits after 5 lateral hops`() {
        val end = simulateNav(
            startStack = listOf("today", "habits"),
            actions = buildList {
                repeat(5) {
                    add(NavAction.GoRef("alltasks"))
                    add(NavAction.GoRef("backlog"))
                }
                add(NavAction.GoTab("habits"))
            },
        )
        assertEquals("single tap on Habits landed on Habits", "habits", end.last())
        assertFalse("no reference view above Habits", end.any { it in REFERENCE_ROUTES })
    }

    /**
     * FR-NAV-11.8 — from a reference view, opening the other reference view
     * still works (the popUpTo target is the entry tab, walking past the
     * current top which is itself a reference view).
     */
    @Test
    fun `goRef from a reference view still replaces it with the new reference view`() {
        val end = simulateNav(
            startStack = listOf("today", "habits", "alltasks"),
            actions = listOf(NavAction.GoRef("backlog")),
        )
        assertEquals(listOf("today", "habits", "backlog"), end)
    }

    /**
     * FR-NAV-11.9 — after `[today, habits, backlog]`, a tab tap on Habits
     * lands on Habits (not on Backlog). The reference view is popped before
     * the switch.
     */
    @Test
    fun `tapping the entry tab from a reference view lands on the tab base`() {
        val end = simulateNav(
            startStack = listOf("today", "habits", "backlog"),
            actions = listOf(NavAction.GoTab("habits")),
        )
        assertEquals("habits", end.last())
        assertFalse(end.any { it in REFERENCE_ROUTES })
    }

    /**
     * FR-NAV-11.1 — even a pathological stack with several stacked reference
     * views (never produced by the real callers post-FR-NAV-11.2, but a
     * defence-in-depth safety net) unwinds cleanly on a tab tap.
     */
    @Test
    fun `goTab pops multiple stacked reference views defence-in-depth`() {
        val end = simulateNav(
            startStack = listOf("today", "habits", "alltasks", "backlog", "alltasks"),
            actions = listOf(NavAction.GoTab("habits")),
        )
        assertEquals("habits", end.last())
        assertFalse(end.any { it in REFERENCE_ROUTES })
    }

    /** FR-NAV-11.2 — the popUpTo target walks past reference views to the nearest tab. */
    @Test
    fun `refPopUpTarget walks past reference views to the nearest non-reference route`() {
        assertEquals("today", refPopUpTarget(listOf("today"), todayRoute = "today"))
        assertEquals("habits", refPopUpTarget(listOf("today", "habits"), todayRoute = "today"))
        assertEquals(
            "habits",
            refPopUpTarget(listOf("today", "habits", "alltasks"), todayRoute = "today"),
        )
        assertEquals(
            "habits",
            refPopUpTarget(
                listOf("today", "habits", "alltasks", "backlog"),
                todayRoute = "today",
            ),
        )
        // Only reference views (never produced in practice, but sane fallback).
        assertEquals(
            "today",
            refPopUpTarget(listOf("alltasks", "backlog"), todayRoute = "today"),
        )
    }
}
