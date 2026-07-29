package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FR-CUE-3.1 — one canonical composer shared by every surface that renders a
 * saved cue (task rows, editor slot, habit editor row, notification body).
 */
class CueDisplayTest {

    @Test
    fun `label and time show both, separated by a middle dot`() {
        assertEquals(
            "morning routine · 08:30",
            Cue(CueType.AT_TIME, "morning routine", "08:30").display(),
        )
    }

    @Test
    fun `label-less AT_TIME cue shows just the time (FR-CUE-2 shape)`() {
        assertEquals("08:30", Cue(CueType.AT_TIME, "", "08:30").display())
    }

    @Test
    fun `AFTER_ROUTINE with no time shows just the label`() {
        assertEquals(
            "after the first coffee",
            Cue(CueType.AFTER_ROUTINE, "after the first coffee", null).display(),
        )
    }

    @Test
    fun `AT_PLACE cue shows just the label`() {
        assertEquals("at the studio", Cue(CueType.AT_PLACE, "at the studio").display())
    }

    @Test
    fun `Hebrew label combines with the ASCII time verbatim`() {
        assertEquals(
            "אחרי הקפה · 08:30",
            Cue(CueType.AT_TIME, "אחרי הקפה", "08:30").display(),
        )
    }

    @Test
    fun `label equal to the time collapses to a single copy`() {
        assertEquals("08:30", Cue(CueType.AT_TIME, "08:30", "08:30").display())
    }
}
