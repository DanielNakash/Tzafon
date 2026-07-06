package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M4 — DM-HABIT-2/3/4/5 rate math, week boundaries by week start (DM-REL-2),
 * the long arc, fresh starts and the task→log date rule.
 */
class HabitMathTest {

    // 2026-07-06 is a Monday
    private val today = "2026-07-06"

    private fun freq(id: String = "h1", target: Double = 3.0) =
        Habit(id = id, name = id, kind = HabitKind.FREQUENCY, target = target)

    private fun quant(id: String = "h2", target: Double = 500.0, days: Int = 5) =
        Habit(id = id, name = id, kind = HabitKind.QUANTITATIVE, target = target, unit = "words", targetDays = days)

    private fun log(habitId: String, date: String, amount: Double? = null) =
        HabitLog(habitId = habitId, date = date, amount = amount)

    // ── week boundaries (DM-REL-2) ────────────────────────────

    @Test
    fun `week start setting moves the period boundary`() {
        assertEquals("2026-07-05", HabitMath.weekStartFor(today, "SUNDAY"))   // Sun Jul 5
        assertEquals("2026-07-06", HabitMath.weekStartFor(today, "MONDAY"))   // today itself
        assertEquals("2026-07-04", HabitMath.weekStartFor(today, "SATURDAY")) // Sat Jul 4
    }

    @Test
    fun `week dates run seven days from the period start`() {
        val dates = HabitMath.weekDates(today, "SUNDAY")
        assertEquals("2026-07-05", dates.first())
        assertEquals("2026-07-11", dates.last())
        assertEquals(7, dates.size)
    }

    // ── in-period rate (forgiving, DM-HABIT-2) ────────────────

    @Test
    fun `frequency rate counts done days this period only`() {
        val logs = listOf(
            log("h1", "2026-07-05"),           // this week (Sun start)
            log("h1", "2026-07-06"),           // this week
            log("h1", "2026-07-03"),           // last week — not counted
        )
        val week = HabitMath.week(freq(), logs, today, "SUNDAY")
        assertEquals(2, week.doneDays)
    }

    @Test
    fun `quantitative week sums amounts and counts logged days`() {
        val logs = listOf(
            log("h2", "2026-07-05", 600.0),
            log("h2", "2026-07-06", 450.0),
        )
        val week = HabitMath.week(quant(), logs, today, "SUNDAY")
        assertEquals(1050.0, week.amountSum, 0.001)
        assertEquals(2, week.doneDays)
        assertEquals(600.0, week.amounts[0], 0.001) // Sunday slot
    }

    // ── task→log date rule (DM-HABIT-5) ───────────────────────

    @Test
    fun `task completion logs on its to-do date, else today`() {
        assertEquals("2026-07-04", HabitMath.logDateFor(Task(id = "t", title = "t", toDoDate = "2026-07-04"), today))
        assertEquals(today, HabitMath.logDateFor(Task(id = "t", title = "t"), today))
    }

    // ── long arc (never a streak) ─────────────────────────────

    @Test
    fun `arc counts weeks since the first log and misses never reset it`() {
        val logs = listOf(
            log("h1", "2026-05-04"),          // 9 weeks back (Mon)
            // a long gap — the arc keeps counting
            log("h1", "2026-07-06"),
        )
        assertEquals(10, HabitMath.arcWeeks(freq(), logs, today, "SUNDAY"))
        assertEquals(1, HabitMath.arcWeeks(freq(), emptyList(), today, "SUNDAY"))
    }

    @Test
    fun `arc label speaks weeks then months - honest timeline copy`() {
        assertEquals("First week", HabitMath.arcLabel(1))
        assertEquals("Running 6 weeks", HabitMath.arcLabel(6))
        assertEquals("Running 3 months", HabitMath.arcLabel(12))
    }

    // ── fresh start (DM-HABIT-4) ──────────────────────────────

    @Test
    fun `fresh start shows after a short week, clears on first activity`() {
        val shortLastWeek = listOf(
            log("h1", "2026-06-29"), // last week (Sun Jun 28 start): only 1 of 3
        )
        assertTrue(HabitMath.freshStart(freq(), shortLastWeek, today, "SUNDAY"))
        // logging anything this week clears the slate card
        val active = shortLastWeek + log("h1", "2026-07-05")
        assertFalse(HabitMath.freshStart(freq(), active, today, "SUNDAY"))
        // a brand-new habit with no history never shows it
        assertFalse(HabitMath.freshStart(freq(), emptyList(), today, "SUNDAY"))
    }

    // ── history grid ──────────────────────────────────────────

    @Test
    fun `history grid marks full and partial days across five weeks`() {
        val h = quant()
        val logs = listOf(
            log("h2", "2026-07-05", 600.0),  // current week, ≥ target → 2
            log("h2", "2026-07-06", 200.0),  // current week, partial → 1
            log("h2", "2026-06-28", 500.0),  // one week back → 2
        )
        val grid = HabitMath.historyGrid(h, logs, today, "SUNDAY")
        assertEquals(5, grid.size)
        assertEquals(7, grid[4].size)
        assertEquals(2, grid[4][0]) // this week Sunday
        assertEquals(1, grid[4][1]) // this week Monday partial
        assertEquals(2, grid[3][0]) // last week Sunday
        assertEquals(0, grid[0][0]) // five weeks back, nothing
    }
}
