package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.ui.theme.isRtl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR-DESIGN-3.6.1 — first-strong-character direction test. */
class BiDiTest {

    @Test
    fun `pure LTR returns false`() {
        assertFalse("abc".isRtl())
        assertFalse("Review draft".isRtl())
    }

    @Test
    fun `pure RTL returns true`() {
        assertTrue("אבג".isRtl())
        assertTrue("لعربية".isRtl())
    }

    @Test
    fun `first strong character wins on mixed input`() {
        assertFalse("abc אבג".isRtl())
        assertTrue("אבג abc".isRtl())
    }

    @Test
    fun `digits and neutrals resolve to LTR`() {
        assertFalse("123".isRtl())
        assertFalse("".isRtl())
        assertFalse("   ".isRtl())
        assertFalse("!!!".isRtl())
    }

    @Test
    fun `whitespace is skipped when searching for the first strong character`() {
        assertTrue("  אבג".isRtl())
        assertFalse("  abc".isRtl())
    }
}
