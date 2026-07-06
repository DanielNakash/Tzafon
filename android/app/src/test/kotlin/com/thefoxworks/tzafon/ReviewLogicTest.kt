package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.ReviewKind
import com.thefoxworks.tzafon.domain.model.ReviewStatus
import com.thefoxworks.tzafon.domain.review.ReviewLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** M7 — DM-REVIEW-1/4 scheduling: slots, the monthly upgrade, resume rules. */
class ReviewLogicTest {

    // July 2026: the 1st is a Wednesday; Sundays fall on 5, 12, 19, 26.

    @Test
    fun `weekly slot is the chosen week start`() {
        assertEquals("2026-07-05", ReviewLogic.slotFor("2026-07-08", "SUNDAY"))
        assertEquals("2026-07-06", ReviewLogic.slotFor("2026-07-08", "MONDAY"))
    }

    @Test
    fun `monthly rides the first weekly slot on or after the 1st`() {
        // Sun Jul 5 is the first Sunday on/after Jul 1 → monthly upgrade
        assertEquals(ReviewKind.MONTHLY, ReviewLogic.kindFor("2026-07-05"))
        // the later Sundays stay weekly — never two long looks in a month
        assertEquals(ReviewKind.WEEKLY, ReviewLogic.kindFor("2026-07-12"))
        assertEquals(ReviewKind.WEEKLY, ReviewLogic.kindFor("2026-07-26"))
        // when the 1st IS the week start, it upgrades itself
        assertEquals(ReviewKind.MONTHLY, ReviewLogic.kindFor("2026-11-01")) // Sun Nov 1
    }

    @Test
    fun `one review per week by construction - the id is the slot`() {
        // the same slot yields the same id whatever day you open it
        assertEquals(
            ReviewLogic.slotFor("2026-07-05", "SUNDAY"),
            ReviewLogic.slotFor("2026-07-11", "SUNDAY"),
        )
    }

    // ── DM-REVIEW-4 — surfacing and resume ────────────────────

    @Test
    fun `untouched invite shows on the day and the day after, then rests`() {
        assertTrue(ReviewLogic.shouldSurface(null, "2026-07-05", "SUNDAY"))
        assertTrue(ReviewLogic.shouldSurface(null, "2026-07-06", "SUNDAY"))
        assertFalse(ReviewLogic.shouldSurface(null, "2026-07-07", "SUNDAY"))
    }

    @Test
    fun `partial reviews stay resumable all week`() {
        val partial = Review(
            id = "2026-07-05", periodStart = "2026-07-05", periodEnd = "2026-07-11",
            status = ReviewStatus.PARTIAL,
        )
        assertTrue(ReviewLogic.shouldSurface(partial, "2026-07-09", "SUNDAY"))
    }

    @Test
    fun `done or skipped never nags again`() {
        val done = Review(
            id = "2026-07-05", periodStart = "2026-07-05", periodEnd = "2026-07-11",
            status = ReviewStatus.DONE,
        )
        assertFalse(ReviewLogic.shouldSurface(done, "2026-07-05", "SUNDAY"))
        assertFalse(ReviewLogic.shouldSurface(done.copy(status = ReviewStatus.SKIPPED), "2026-07-06", "SUNDAY"))
    }

    @Test
    fun `a new week brings a fresh slot even after last week's review`() {
        val lastWeek = Review(
            id = "2026-06-28", periodStart = "2026-06-28", periodEnd = "2026-07-04",
            status = ReviewStatus.DONE,
        )
        assertTrue(ReviewLogic.shouldSurface(lastWeek, "2026-07-05", "SUNDAY"))
    }

    @Test
    fun `snapshot encodes the mirror numbers compactly`() {
        val snap = ReviewLogic.Snapshot(
            doneCount = 14, alignedCount = 9,
            habitRates = listOf(ReviewLogic.HabitRate("Run", 2, 3, 66, null)),
            goalDeltas = listOf(ReviewLogic.GoalDelta("First draft", "+3,200 words")),
        )
        val enc = snap.encode()
        assertTrue(enc.startsWith("14|9"))
        assertTrue(enc.contains("H Run · 2 of 3"))
        assertTrue(enc.contains("G First draft · +3,200 words"))
    }
}
