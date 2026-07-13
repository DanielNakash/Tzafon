package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.themes.GoalLinkLogic
import com.thefoxworks.tzafon.domain.themes.GoalLinkLogic.LinkSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** FR-DIR-8.6 — deterministic promotion rule + selection invariants. */
class GoalLinkLogicTest {

    @Test fun `fromGoal splits primary out of themeIds`() {
        val sel = GoalLinkLogic.fromGoal("A", listOf("A", "B", "C"))
        assertEquals("A", sel.primaryId)
        assertEquals(listOf("B", "C"), sel.extraServes)
    }

    @Test fun `fromGoal handles orphan with serves-also`() {
        val sel = GoalLinkLogic.fromGoal(null, listOf("B", "C"))
        assertNull(sel.primaryId)
        assertEquals(listOf("B", "C"), sel.extraServes)
    }

    @Test fun `themeIds is union primary+extra, deduped`() {
        val sel = LinkSelection("A", listOf("B", "C"))
        assertEquals(listOf("A", "B", "C"), sel.themeIds())
    }

    @Test fun `themeIds is empty when orphan and no extras`() {
        val sel = LinkSelection(null, emptyList())
        assertEquals(emptyList<String>(), sel.themeIds())
    }

    // ── promotion rule (FR-DIR-8.6) ────────────────────────────

    @Test fun `deselect primary promotes topmost extra`() {
        val start = LinkSelection("A", listOf("B", "C"))
        val next = GoalLinkLogic.tapPrimary(start, "A")
        assertEquals("B", next.primaryId)
        assertEquals(listOf("C"), next.extraServes)
    }

    @Test fun `deselect primary chain empties correctly`() {
        var s = LinkSelection("A", listOf("B", "C"))
        s = GoalLinkLogic.tapPrimary(s, "A")   // A off → promote B
        assertEquals("B", s.primaryId)
        assertEquals(listOf("C"), s.extraServes)
        s = GoalLinkLogic.tapPrimary(s, "B")   // B off → promote C
        assertEquals("C", s.primaryId)
        assertEquals(emptyList<String>(), s.extraServes)
        s = GoalLinkLogic.tapPrimary(s, "C")   // C off → no promotion left
        assertNull(s.primaryId)
        assertEquals(emptyList<String>(), s.extraServes)
    }

    @Test fun `tap different theme swaps primary and clears from extras`() {
        val start = LinkSelection("A", listOf("B", "C"))
        val next = GoalLinkLogic.tapPrimary(start, "B")
        assertEquals("B", next.primaryId)
        assertEquals(listOf("C"), next.extraServes)
    }

    @Test fun `tap different theme not in extras just switches primary`() {
        val start = LinkSelection("A", listOf("B"))
        val next = GoalLinkLogic.tapPrimary(start, "Z")
        assertEquals("Z", next.primaryId)
        assertEquals(listOf("B"), next.extraServes)
    }

    // ── explicit "No theme (orphan)" ───────────────────────────

    @Test fun `tapOrphan clears primary and preserves extras`() {
        val start = LinkSelection("A", listOf("B", "C"))
        val next = GoalLinkLogic.tapOrphan(start)
        assertNull(next.primaryId)
        assertEquals(listOf("B", "C"), next.extraServes)
    }

    // ── serves-also toggle ────────────────────────────────────

    @Test fun `toggleExtra adds if not present`() {
        val start = LinkSelection("A", listOf("B"))
        val next = GoalLinkLogic.toggleExtra(start, "C")
        assertEquals("A", next.primaryId)
        assertEquals(listOf("B", "C"), next.extraServes)
    }

    @Test fun `toggleExtra removes if present`() {
        val start = LinkSelection("A", listOf("B", "C"))
        val next = GoalLinkLogic.toggleExtra(start, "B")
        assertEquals(listOf("C"), next.extraServes)
    }

    @Test fun `toggleExtra ignores primary (cannot deselect via serves-also)`() {
        val start = LinkSelection("A", listOf("B"))
        val next = GoalLinkLogic.toggleExtra(start, "A")
        assertEquals(start, next)
    }
}
