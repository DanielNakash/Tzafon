package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.ui.components.canSaveCue
import com.thefoxworks.tzafon.ui.components.isValidHhMm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR-CUE-1.4 — canonical HH:mm gate for the AT_TIME save button. */
class CueSheetValidationTest {

    @Test
    fun `accepts zero-padded 24-hour HH mm`() {
        assertTrue(isValidHhMm("00:00"))
        assertTrue(isValidHhMm("08:30"))
        assertTrue(isValidHhMm("23:59"))
        assertTrue(isValidHhMm("12:00"))
    }

    @Test
    fun `rejects unpadded hours and minutes`() {
        assertFalse(isValidHhMm("8:30"))
        assertFalse(isValidHhMm("08:5"))
    }

    @Test
    fun `rejects out of range values`() {
        assertFalse(isValidHhMm("24:00"))
        assertFalse(isValidHhMm("12:60"))
        assertFalse(isValidHhMm("99:99"))
    }

    @Test
    fun `rejects missing colon and empty`() {
        assertFalse(isValidHhMm("0830"))
        assertFalse(isValidHhMm(""))
        assertFalse(isValidHhMm(":"))
        assertFalse(isValidHhMm("08"))
    }

    @Test
    fun `trims surrounding whitespace before matching`() {
        assertTrue(isValidHhMm(" 08:30 "))
        assertTrue(isValidHhMm("\t08:30"))
    }

    // FR-CUE-2 — per-trigger save gate (refines FR-CUE-1.5).
    @Test
    fun `AT_TIME saves with empty label on valid time`() {
        assertTrue(canSaveCue(CueType.AT_TIME, "", "08:30"))
    }

    @Test
    fun `AT_TIME does not save on invalid time regardless of label`() {
        assertFalse(canSaveCue(CueType.AT_TIME, "", "8:3"))
        assertFalse(canSaveCue(CueType.AT_TIME, "", ""))
        assertFalse(canSaveCue(CueType.AT_TIME, "morning routine", "24:00"))
    }

    @Test
    fun `AFTER_ROUTINE requires non-blank label, ignores time`() {
        assertFalse(canSaveCue(CueType.AFTER_ROUTINE, "", ""))
        assertFalse(canSaveCue(CueType.AFTER_ROUTINE, "   ", "08:30"))
        assertTrue(canSaveCue(CueType.AFTER_ROUTINE, "coffee", ""))
    }

    @Test
    fun `AT_PLACE requires non-blank label, ignores time`() {
        assertFalse(canSaveCue(CueType.AT_PLACE, "", ""))
        assertTrue(canSaveCue(CueType.AT_PLACE, "studio", ""))
    }
}
