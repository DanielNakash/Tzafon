package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.dates.MonthGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/**
 * v2.13.0 — one month grid for every calendar, following the week-start
 * setting (FR-SET-1, DM-REL-2): header order and the leading blanks.
 */
class MonthGridTest {

    @Test
    fun `the header starts on the week-start day`() {
        val m = YearMonth.of(2026, 10)
        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), MonthGrid.of(m, "SUNDAY").header)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), MonthGrid.of(m, "MONDAY").header)
        assertEquals(listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"), MonthGrid.of(m, "SATURDAY").header)
    }

    @Test
    fun `an unknown setting falls back to Sunday`() {
        assertEquals("Sun", MonthGrid.of(YearMonth.of(2026, 10), "").header.first())
    }

    // Months of 2026 whose 1st falls on each weekday (Sun..Sat).
    private val firstOn = mapOf(
        "Sun" to YearMonth.of(2026, 2),
        "Mon" to YearMonth.of(2026, 6),
        "Tue" to YearMonth.of(2026, 9),
        "Wed" to YearMonth.of(2026, 4),
        "Thu" to YearMonth.of(2026, 1),
        "Fri" to YearMonth.of(2026, 5),
        "Sat" to YearMonth.of(2026, 8),
    )

    private fun blanks(m: YearMonth, ws: String) = MonthGrid.of(m, ws).weeks.first().takeWhile { it == null }.size

    @Test
    fun `leading blanks for a month starting on each weekday, Sunday start`() {
        val want = mapOf("Sun" to 0, "Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6)
        want.forEach { (wd, n) -> assertEquals("1st on $wd", n, blanks(firstOn.getValue(wd), "SUNDAY")) }
    }

    @Test
    fun `leading blanks for a month starting on each weekday, Monday start`() {
        val want = mapOf("Sun" to 6, "Mon" to 0, "Tue" to 1, "Wed" to 2, "Thu" to 3, "Fri" to 4, "Sat" to 5)
        want.forEach { (wd, n) -> assertEquals("1st on $wd", n, blanks(firstOn.getValue(wd), "MONDAY")) }
    }

    @Test
    fun `leading blanks for a month starting on each weekday, Saturday start`() {
        val want = mapOf("Sun" to 1, "Mon" to 2, "Tue" to 3, "Wed" to 4, "Thu" to 5, "Fri" to 6, "Sat" to 0)
        want.forEach { (wd, n) -> assertEquals("1st on $wd", n, blanks(firstOn.getValue(wd), "SATURDAY")) }
    }

    @Test
    fun `weeks are full rows holding every day of the month once, in order`() {
        val g = MonthGrid.of(YearMonth.of(2026, 2), "MONDAY")
        assertTrue(g.weeks.all { it.size == 7 })
        val days = g.weeks.flatten().filterNotNull()
        assertEquals((1..28).map { "2026-02-%02d".format(it) }, days)
    }
}
