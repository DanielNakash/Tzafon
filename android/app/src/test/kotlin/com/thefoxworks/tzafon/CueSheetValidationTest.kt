package com.thefoxworks.tzafon

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
}
