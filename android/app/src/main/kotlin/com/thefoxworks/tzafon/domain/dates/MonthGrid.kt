package com.thefoxworks.tzafon.domain.dates

import java.time.YearMonth

/**
 * v2.13.0 — the one month grid every calendar draws, following the
 * week-start setting (FR-SET-1, DM-REL-2). Before this, every picker was
 * Sunday-first whatever the setting said.
 */
object MonthGrid {

    /** [header]: short weekday names from the week start; [weeks]: 7-day rows, null for blanks. */
    data class Grid(val header: List<String>, val weeks: List<List<String?>>)

    /** Sunday-based index (0..6) of a week-start setting: SUNDAY | MONDAY | SATURDAY. */
    fun startIndex(weekStart: String): Int = when (weekStart) {
        "MONDAY" -> 1
        "SATURDAY" -> 6
        else -> 0 // SUNDAY, the default
    }

    fun of(month: YearMonth, weekStart: String): Grid {
        val start = startIndex(weekStart)
        val header = (0 until 7).map { Dates.WD[(start + it) % 7] }
        val lead = (Dates.dayOfWeek(month.atDay(1)) - start + 7) % 7
        val cells = List<String?>(lead) { null } + (1..month.lengthOfMonth()).map { Dates.iso(month.atDay(it)) }
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        return Grid(header, padded.chunked(7))
    }
}
