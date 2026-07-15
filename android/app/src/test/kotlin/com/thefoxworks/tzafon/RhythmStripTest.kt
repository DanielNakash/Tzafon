package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.ui.habits.RhythmCellState
import com.thefoxworks.tzafon.ui.habits.rhythmCellState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FR-HAB-11 — the per-day render rule for the rolling rhythm strip. The
 * composable delegates every cell decision to `rhythmCellState`, so this
 * covers the "filled iff done, empty otherwise" and "proportional to
 * amount / target*1.15" contracts without spinning up Compose.
 */
class RhythmStripTest {

    private fun log(date: String, done: Boolean = true, amount: Double? = null) =
        HabitLog(habitId = "h1", date = date, done = done, amount = amount)

    @Test fun `frequency filled iff a done log exists for the date`() {
        val logs = listOf(log("2026-07-15"), log("2026-07-12"))
        val cellForToday = rhythmCellState(
            date = "2026-07-15", today = "2026-07-15", startedAtIso = null,
            logs = logs, kind = HabitKind.FREQUENCY, target = 3.0,
        )
        val cellYesterday = rhythmCellState(
            date = "2026-07-14", today = "2026-07-15", startedAtIso = null,
            logs = logs, kind = HabitKind.FREQUENCY, target = 3.0,
        )
        val cellThreeAgo = rhythmCellState(
            date = "2026-07-12", today = "2026-07-15", startedAtIso = null,
            logs = logs, kind = HabitKind.FREQUENCY, target = 3.0,
        )
        assertEquals(RhythmCellState.Freq(done = true), cellForToday)
        assertEquals(RhythmCellState.Freq(done = false), cellYesterday)
        assertEquals(RhythmCellState.Freq(done = true), cellThreeAgo)
    }

    @Test fun `frequency ignores rows where done is false`() {
        val logs = listOf(log("2026-07-14", done = false))
        val cell = rhythmCellState(
            date = "2026-07-14", today = "2026-07-15", startedAtIso = null,
            logs = logs, kind = HabitKind.FREQUENCY, target = 3.0,
        )
        assertEquals(RhythmCellState.Freq(done = false), cell)
    }

    @Test fun `quantitative fill fraction is amount over target times 1_15`() {
        val logs = listOf(log("2026-07-15", amount = 3.0))
        val cell = rhythmCellState(
            date = "2026-07-15", today = "2026-07-15", startedAtIso = null,
            logs = logs, kind = HabitKind.QUANTITATIVE, target = 3.0,
        )
        // 3.0 / (3.0 * 1.15) ≈ 0.869
        val expected = (3.0 / (3.0 * 1.15)).toFloat()
        assertEquals(expected, (cell as RhythmCellState.Quant).fillFraction, 0.001f)
    }

    @Test fun `quantitative zero-amount day yields zero fill`() {
        val cell = rhythmCellState(
            date = "2026-07-14", today = "2026-07-15", startedAtIso = null,
            logs = emptyList(), kind = HabitKind.QUANTITATIVE, target = 3.0,
        )
        assertEquals(0f, (cell as RhythmCellState.Quant).fillFraction, 0f)
    }

    @Test fun `quantitative fill is clamped to 1_0 for over-target days`() {
        val logs = listOf(log("2026-07-15", amount = 999.0))
        val cell = rhythmCellState(
            date = "2026-07-15", today = "2026-07-15", startedAtIso = null,
            logs = logs, kind = HabitKind.QUANTITATIVE, target = 3.0,
        )
        assertEquals(1f, (cell as RhythmCellState.Quant).fillFraction, 0f)
    }

    @Test fun `pre-startedAt days render as the muted placeholder`() {
        val cell = rhythmCellState(
            date = "2026-07-10", today = "2026-07-15", startedAtIso = "2026-07-12",
            logs = emptyList(), kind = HabitKind.FREQUENCY, target = 3.0,
        )
        assertEquals(RhythmCellState.PreStart, cell)
    }

    @Test fun `on or after startedAt renders normally`() {
        val cell = rhythmCellState(
            date = "2026-07-12", today = "2026-07-15", startedAtIso = "2026-07-12",
            logs = emptyList(), kind = HabitKind.FREQUENCY, target = 3.0,
        )
        assertTrue(cell is RhythmCellState.Freq)
    }
}
