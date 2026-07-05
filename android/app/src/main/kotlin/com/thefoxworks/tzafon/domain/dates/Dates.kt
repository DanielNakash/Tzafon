package com.thefoxworks.tzafon.domain.dates

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ISO 'yyyy-MM-dd' date helpers. Ported from the v1.1.0 web reference
 * (src/utils/dates.js) so string ordering, grouping and labels behave
 * identically. FR-REC-2: absolute labels carry the year only when it
 * differs from the current year.
 */
object Dates {
    val WD = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val WD_FULL = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val MO = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val MO_FULL = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(s: String): LocalDate = LocalDate.parse(s, ISO)
    fun iso(d: LocalDate): String = d.format(ISO)
    fun todayIso(): String = iso(LocalDate.now())
    fun addDays(s: String, n: Long): String = iso(parse(s).plusDays(n))
    fun dayDiff(a: String, b: String): Long = parse(a).toEpochDay() - parse(b).toEpochDay()

    /** Sunday-based day-of-week index 0..6, matching JS Date.getDay(). */
    fun dayOfWeek(d: LocalDate): Int = d.dayOfWeek.value % 7
    fun dayOfWeek(s: String): Int = dayOfWeek(parse(s))

    fun ordinal(n: Int): String {
        val v = n % 100
        val suffix = when {
            v in 11..13 -> "th"
            v % 10 == 1 -> "st"
            v % 10 == 2 -> "nd"
            v % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    /** "Sun, Sep 14" — or "Tue, Jan 5, 2027" when the year differs (FR-REC-2). */
    fun fmtDate(s: String, today: String = todayIso()): String {
        val d = parse(s)
        val base = "${WD[dayOfWeek(d)]}, ${MO[d.monthValue - 1]} ${d.dayOfMonth}"
        return if (d.year != parse(today).year) "$base, ${d.year}" else base
    }

    /** "Sunday, September 14" — with the same conditional year. */
    fun fmtLong(s: String, today: String = todayIso()): String {
        val d = parse(s)
        val base = "${WD_FULL[dayOfWeek(d)]}, ${MO_FULL[d.monthValue - 1]} ${d.dayOfMonth}"
        return if (d.year != parse(today).year) "$base, ${d.year}" else base
    }

    data class Group(val key: String, val label: String, val order: Long)

    /** Relative group for a To Do date: Overdue / Today / Tomorrow / dated. */
    fun groupFor(s: String, today: String): Group {
        val diff = dayDiff(s, today)
        return when {
            diff < 0 -> Group("overdue", "Overdue", 0)
            diff == 0L -> Group("today", "Today", 1)
            diff == 1L -> Group("tomorrow", "Tomorrow", 2)
            else -> Group(s, fmtDate(s, today), 3 + diff)
        }
    }
}
