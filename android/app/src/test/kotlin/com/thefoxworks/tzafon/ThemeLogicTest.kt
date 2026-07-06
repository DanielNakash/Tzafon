package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeState
import com.thefoxworks.tzafon.domain.themes.ThemeLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** M6 — the ≤3-active cap (at activation) and FR-DIR-6 window arrivals. */
class ThemeLogicTest {

    private val today = "2026-07-06"

    private fun theme(
        id: String,
        state: ThemeState = ThemeState.ACTIVE,
        start: String = "2026-06-01",
        end: String = "2026-08-31",
    ) = Theme(id = id, name = id, why = "why", windowStart = start, windowEnd = end, state = state)

    @Test
    fun `the cap bites at activation, not creation`() {
        val two = listOf(theme("a"), theme("b"), theme("u", ThemeState.UPCOMING))
        assertTrue(ThemeLogic.canActivate(two, "u")) // upcoming never counts
        val three = two + theme("c")
        assertFalse(ThemeLogic.canActivate(three, "u"))
        // re-activating an already-active theme is always fine
        assertTrue(ThemeLogic.canActivate(three, "a"))
    }

    @Test
    fun `window label reads as orientation - quarter and week index`() {
        val t = theme("a", start = "2026-06-01", end = "2026-08-31") // 91 days
        assertEquals("QUARTER · WEEK 6", ThemeLogic.windowLabel(t, today))
        val short = theme("b", start = "2026-07-06", end = "2026-08-03") // 28 days
        assertEquals("4 WEEKS · WEEK 1", ThemeLogic.windowLabel(short, today))
    }

    @Test
    fun `default window is one quarter out`() {
        assertEquals("2026-10-05", ThemeLogic.defaultWindowEnd("2026-07-06"))
    }

    // ── FR-DIR-6 — the arrival decision table ──

    @Test
    fun `arrival with a free slot activates quietly`() {
        val themes = listOf(theme("a"), theme("b"), theme("u", ThemeState.UPCOMING, start = "2026-07-01"))
        assertEquals(
            ThemeLogic.Arrival.Activate,
            ThemeLogic.onWindowArrival(themes, themes[2], today),
        )
    }

    @Test
    fun `arrival with three active swaps out an ended window`() {
        val themes = listOf(
            theme("a"),
            theme("b", end = "2026-07-01"), // ended
            theme("c"),
            theme("u", ThemeState.UPCOMING, start = "2026-07-05"),
        )
        assertEquals(
            ThemeLogic.Arrival.Swap("b"),
            ThemeLogic.onWindowArrival(themes, themes[3], today),
        )
    }

    @Test
    fun `arrival with three live actives prompts the user`() {
        val themes = listOf(
            theme("a"), theme("b"), theme("c"),
            theme("u", ThemeState.UPCOMING, start = "2026-07-05"),
        )
        assertEquals(
            ThemeLogic.Arrival.Prompt,
            ThemeLogic.onWindowArrival(themes, themes[3], today),
        )
    }

    @Test
    fun `arrived upcoming themes are detected by start date`() {
        val themes = listOf(
            theme("u1", ThemeState.UPCOMING, start = "2026-07-06"),
            theme("u2", ThemeState.UPCOMING, start = "2026-08-01"),
        )
        assertEquals(listOf("u1"), ThemeLogic.arrivedUpcoming(themes, today).map { it.id })
    }

    @Test
    fun `accent slots cycle without colliding among live themes`() {
        val themes = listOf(
            theme("a").copy(accentSlot = 0),
            theme("b", ThemeState.UPCOMING).copy(accentSlot = 1),
        )
        assertEquals(2, ThemeLogic.nextAccentSlot(themes))
    }
}
