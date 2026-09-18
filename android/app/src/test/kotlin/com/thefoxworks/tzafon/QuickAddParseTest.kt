package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.action.QuickAddParse
import com.thefoxworks.tzafon.domain.model.CueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * FR-CAPTURE-3.1 — the trailing-HH:MM parse, pinned at the pure-function level.
 * The case list is §3.2 acceptance 13: a valid token, no token, the invalid
 * shapes the DECISION deliberately rejects, a token-only title, a token that is
 * not at the end, a Hebrew title, and the multiple-space case.
 */
class QuickAddParseTest {

    private fun parse(s: String) = QuickAddParse.parse(s)

    @Test
    fun `a trailing token is lifted off the title`() {
        val p = parse("Call the bank 08:30")
        assertEquals("Call the bank", p.title)
        assertEquals("08:30", p.time)
    }

    @Test
    fun `no token leaves the title alone`() {
        val p = parse("Call the bank")
        assertEquals("Call the bank", p.title)
        assertNull(p.time)
    }

    @Test
    fun `the grammar is exactly HH_MM - looser shapes do not parse`() {
        // FR-CAPTURE-3.2 + DECISION: zero-padded, colon required, 00:00..23:59.
        for (raw in listOf(
            "Call the bank 8:30",    // not zero-padded
            "Call the bank 24:00",   // hour out of range
            "Call the bank 08:60",   // minute out of range
            "Call the bank 0830",    // no colon
            "Call the bank 8.30",    // a sum of money, not a time
            "Call the bank 8pm",     // 12-hour, out of scope
        )) {
            val p = parse(raw)
            assertEquals("the literal title must survive: $raw", raw, p.title)
            assertNull("must not parse: $raw", p.time)
        }
    }

    @Test
    fun `a time-only title does not parse`() {
        // FR-CAPTURE-3.4 — stripping would leave nothing to call the task.
        val p = parse("08:30")
        assertEquals("08:30", p.title)
        assertNull(p.time)
    }

    @Test
    fun `a token that is not at the end does not parse`() {
        val p = parse("Watch the 9:45 train")
        assertEquals("Watch the 9:45 train", p.title)
        assertNull(p.time)

        // even a *valid* token mid-title stays put
        val q = parse("Watch the 09:45 train")
        assertEquals("Watch the 09:45 train", q.title)
        assertNull(q.time)
    }

    @Test
    fun `trailing text after the token stops the parse`() {
        // acceptance 8 — the token is no longer trailing once you keep typing
        val p = parse("Call the bank 08:30 please")
        assertEquals("Call the bank 08:30 please", p.title)
        assertNull(p.time)
    }

    @Test
    fun `multiple spaces before the token still parse, and the title is trimmed`() {
        val p = parse("Call  08:30")
        assertEquals("Call", p.title)
        assertEquals("08:30", p.time)
    }

    @Test
    fun `surrounding whitespace is trimmed before matching`() {
        val p = parse("   Call the bank 08:30   ")
        assertEquals("Call the bank", p.title)
        assertEquals("08:30", p.time)
    }

    @Test
    fun `a Hebrew title parses on the logical string`() {
        // FR-CAPTURE-3.11 — matching runs on the code-point sequence the user
        // typed; the time renders at the visual left but is logically trailing.
        val p = parse("לצלצל לבנק 08:30")
        assertEquals("לצלצל לבנק", p.title)
        assertEquals("08:30", p.time)
    }

    @Test
    fun `the range boundaries parse`() {
        assertEquals("00:00", parse("Midnight thing 00:00").time)
        assertEquals("23:59", parse("Last thing 23:59").time)
    }

    @Test
    fun `the last token wins when a title ends in two times`() {
        val p = parse("Call 08:30 09:45")
        assertEquals("Call 08:30", p.title)
        assertEquals("09:45", p.time)
    }

    @Test
    fun `cueFor builds the label-less AT_TIME shape, and nothing for no time`() {
        // FR-CAPTURE-3.10 — label stays empty so Cue.display() (FR-CUE-3) renders
        // the row chip as the bare time.
        val cue = QuickAddParse.cueFor("08:30")!!
        assertEquals(CueType.AT_TIME, cue.type)
        assertEquals("", cue.label)
        assertEquals("08:30", cue.time)
        assertEquals("08:30", cue.display())
        assertNull(QuickAddParse.cueFor(null))
    }
}
