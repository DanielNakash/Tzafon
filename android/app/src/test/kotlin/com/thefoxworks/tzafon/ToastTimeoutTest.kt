package com.thefoxworks.tzafon

import androidx.compose.ui.platform.AccessibilityManager
import com.thefoxworks.tzafon.ui.status.TOAST_MILLIS
import com.thefoxworks.tzafon.ui.status.toastMillis
import org.junit.Assert.assertEquals
import org.junit.Test

/** FR-STATUS-4.3 / NFR-A11Y-1.6 — the toast shows 6 s, longer when TalkBack asks for it. */
class ToastTimeoutTest {

    /** Stands in for TalkBack's recommended timeout: triples it, and records what was asked. */
    private class TalkBack : AccessibilityManager {
        var askedControls: Boolean? = null
        override fun calculateRecommendedTimeoutMillis(
            originalTimeoutMillis: Long,
            containsIcons: Boolean,
            containsText: Boolean,
            containsControls: Boolean,
        ): Long {
            askedControls = containsControls
            return originalTimeoutMillis * 3
        }
    }

    @Test
    fun `without an accessibility manager the toast shows six seconds`() {
        assertEquals(6_000L, TOAST_MILLIS)
        assertEquals(TOAST_MILLIS, toastMillis(null, undoable = true))
    }

    @Test
    fun `TalkBack lengthens it, and knows the Undo is a control`() {
        val talkBack = TalkBack()
        assertEquals(18_000L, toastMillis(talkBack, undoable = true))
        assertEquals(true, talkBack.askedControls)

        assertEquals(18_000L, toastMillis(talkBack, undoable = false))
        assertEquals(false, talkBack.askedControls)
    }
}
