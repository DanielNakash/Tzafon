package com.thefoxworks.tzafon.domain.recurrence

import com.thefoxworks.tzafon.domain.dates.Dates
import java.time.LocalDate

/**
 * The recurrence engine — a behavior-faithful Kotlin re-implementation of the
 * v1.1.0 web engine (src/utils/recurrence.js, the reference oracle), with the
 * v2 fixes built in from the start:
 *  - FR-REC-1  nearest-occurrence floor (a far-future series is always
 *              represented by at least its earliest upcoming occurrence)
 *  - FR-REC-3  effective horizon (callers pass a horizon that may extend
 *              beyond the rolling 60-day window)
 *
 * All dates are ISO 'yyyy-MM-dd' strings; weekday indexes are Sunday-based
 * 0..6 (JS Date.getDay() convention); weeks are phase-aligned to the anchor's
 * Sunday-started week exactly like the oracle.
 */

enum class Pattern { INTERVAL, WEEKDAY, MONTHDAY }
enum class IntervalUnit { DAY, WEEK, MONTH }
enum class MonthMode { DATE, WEEKDAY }
enum class DueMode { NONE, SINGULAR, RECURRING }

data class Rule(
    val pattern: Pattern = Pattern.WEEKDAY,
    val interval: Int = 1,
    val unit: IntervalUnit = IntervalUnit.WEEK,
    val weekdays: List<Int> = emptyList(),
    val weekInterval: Int = 1,
    val monthMode: MonthMode = MonthMode.DATE,
    val monthDate: Int = 1,
    val monthWeekPos: Int = 1,      // 1..4, or -1 for "last"
    val monthWeekday: Int = 0,
    val monthInterval: Int = 1,
)

/** Everything planOccurrences needs to know about a series. */
data class SeriesSpec(
    val rule: Rule,
    val ruleAnchor: String?,
    val startDate: String?,
    val endDate: String? = null,
    val dueMode: DueMode = DueMode.NONE,
    val dueRule: Rule? = null,
    val dueSingular: String? = null,
    val exceptions: List<String> = emptyList(),
)

data class PlannedOccurrence(val occurrenceDate: String, val toDo: String, val due: String?)
data class Plan(val create: List<PlannedOccurrence>, val generatedThrough: String)

object Recurrence {

    /** Hard cap so a bad rule can never loop forever (matches the oracle). */
    private const val MAX_ITERS = 4000

    /** The rolling bulk-generation window, carried from v1.1.0 series.js. */
    const val HORIZON_DAYS = 60L

    fun defaultRule(today: LocalDate = LocalDate.now()): Rule = Rule(
        pattern = Pattern.WEEKDAY,
        interval = 1,
        unit = IntervalUnit.WEEK,
        weekdays = listOf(Dates.dayOfWeek(today)),
        weekInterval = 1,
        monthMode = MonthMode.DATE,
        monthDate = today.dayOfMonth,
        monthWeekPos = 1,
        monthWeekday = Dates.dayOfWeek(today),
        monthInterval = 1,
    )

    // ── date math helpers ─────────────────────────────────────

    /** Month k (0-based offset may exceed range) with the day clamped to its length. */
    private fun addMonthsClamped(base: LocalDate, monthsToAdd: Long, day: Int): LocalDate {
        val firstOfTarget = base.withDayOfMonth(1).plusMonths(monthsToAdd)
        return firstOfTarget.withDayOfMonth(minOf(day, firstOfTarget.lengthOfMonth()))
    }

    /** nth (1-based) weekday in a month, or the last when pos == -1; null if absent. */
    private fun nthWeekdayOfMonth(year: Int, month: Int, weekday: Int, pos: Int): LocalDate? {
        val first = LocalDate.of(year, month, 1)
        if (pos == -1) {
            val last = first.withDayOfMonth(first.lengthOfMonth())
            val back = (Dates.dayOfWeek(last) - weekday + 7) % 7
            return last.minusDays(back.toLong())
        }
        val fwd = (weekday - Dates.dayOfWeek(first) + 7) % 7
        val day = 1 + fwd + (pos - 1) * 7
        return if (day > first.lengthOfMonth()) null else first.withDayOfMonth(day)
    }

    // ── enumerateDates(rule, anchor, from, to) ────────────────

    /**
     * All ISO dates the rule produces in [from, to], phase-aligned to `anchor`.
     * Never returns dates earlier than the anchor.
     */
    fun enumerateDates(rule: Rule, anchor: String?, from: String?, to: String?): List<String> {
        if (anchor == null || from == null || to == null || from > to) return emptyList()
        val a = Dates.parse(anchor)
        val lo = if (from < anchor) anchor else from
        val out = sortedSetOf<String>()
        fun push(d: LocalDate) {
            val s = Dates.iso(d)
            if (s in lo..to) out.add(s)
        }

        when (rule.pattern) {
            Pattern.INTERVAL -> {
                val stepDays = when (rule.unit) {
                    IntervalUnit.DAY -> rule.interval.toLong()
                    IntervalUnit.WEEK -> rule.interval * 7L
                    IntervalUnit.MONTH -> 0L
                }
                for (k in 0 until MAX_ITERS) {
                    val d = if (rule.unit == IntervalUnit.MONTH) {
                        addMonthsClamped(a, k.toLong() * rule.interval, a.dayOfMonth)
                    } else {
                        a.plusDays(k * stepDays)
                    }
                    if (Dates.iso(d) > to) break
                    push(d)
                }
            }

            Pattern.WEEKDAY -> {
                // anchor's week, Sunday-started — identical phase to the oracle
                val weekStart = a.minusDays(Dates.dayOfWeek(a).toLong())
                val days = rule.weekdays.sorted()
                val weekInterval = if (rule.weekInterval < 1) 1 else rule.weekInterval
                for (w in 0 until MAX_ITERS) {
                    val ws = weekStart.plusDays(w * 7L)
                    if (Dates.iso(ws) > to) break // week's Sunday already past `to`
                    if (w % weekInterval == 0) {
                        for (wd in days) push(ws.plusDays(wd.toLong()))
                    }
                }
            }

            Pattern.MONTHDAY -> {
                val monthInterval = if (rule.monthInterval < 1) 1 else rule.monthInterval
                for (k in 0 until MAX_ITERS) {
                    val monthStart = a.withDayOfMonth(1).plusMonths(k.toLong())
                    if (Dates.iso(monthStart) > to) break
                    if (k % monthInterval == 0) {
                        val d = if (rule.monthMode == MonthMode.DATE) {
                            addMonthsClamped(monthStart, 0, rule.monthDate)
                        } else {
                            nthWeekdayOfMonth(monthStart.year, monthStart.monthValue, rule.monthWeekday, rule.monthWeekPos)
                        }
                        d?.let { push(it) }
                    }
                }
            }
        }
        return out.toList()
    }

    private fun minDate(vararg ds: String?): String? =
        ds.filterNotNull().minOrNull()

    // ── computeDue(series, toDoList, anchor, end) ─────────────

    /** Due date per occurrence index, aligned to toDoList (oracle-identical). */
    fun computeDue(series: SeriesSpec, toDoList: List<String>, anchor: String?, end: String?): List<String?> {
        if (series.dueMode == DueMode.SINGULAR) return toDoList.map { series.dueSingular }
        if (series.dueMode == DueMode.RECURRING && series.dueRule != null) {
            val dues = enumerateDates(series.dueRule, anchor, anchor, end)
            return toDoList.mapIndexed { i, _ -> dues.getOrNull(i) }
        }
        return toDoList.map { null }
    }

    // ── planOccurrences(series, horizon, existing, today) ─────

    /**
     * The occurrences that should exist but don't yet. Behavior matches the
     * oracle, plus FR-REC-1: when the normal in-window set is empty and no
     * open occurrence on/after `today` is already materialized, additionally
     * materialize the single earliest occurrence on/after `today` so every
     * active series is always represented by at least its nearest occurrence.
     * Idempotent — deterministic slots, exceptions and existing respected.
     */
    fun planOccurrences(
        series: SeriesSpec,
        horizon: String,
        existingDates: Set<String> = emptySet(),
        today: String? = null,
    ): Plan {
        val anchor = series.ruleAnchor ?: series.startDate ?: return Plan(emptyList(), horizon)

        // end bound: earliest of horizon, series end date, and singular deadline
        val singularEnd = if (series.dueMode == DueMode.SINGULAR) series.dueSingular else null
        val end = minDate(horizon, series.endDate, singularEnd) ?: return Plan(emptyList(), horizon)

        val fullToDo = enumerateDates(series.rule, anchor, anchor, end)
        val dues = computeDue(series, fullToDo, anchor, end)
        val exceptions = series.exceptions.toSet()

        val create = mutableListOf<PlannedOccurrence>()
        fullToDo.forEachIndexed { i, occ ->
            if (occ in exceptions || occ in existingDates) return@forEachIndexed
            create.add(PlannedOccurrence(occ, occ, dues[i]))
        }

        // FR-REC-1 — the nearest-occurrence floor
        if (today != null && create.isEmpty()) {
            val hasUpcomingMaterialized = existingDates.any { it >= today }
            if (!hasUpcomingMaterialized) {
                nearestOccurrence(series, today)?.let { nearest ->
                    if (nearest.occurrenceDate !in existingDates) create.add(nearest)
                }
            }
        }

        return Plan(create, end)
    }

    /**
     * The single earliest occurrence on/after `today` (FR-REC-1), or null when
     * the series is exhausted (past its end / all remaining slots excepted).
     */
    fun nearestOccurrence(series: SeriesSpec, today: String): PlannedOccurrence? {
        val anchor = series.ruleAnchor ?: series.startDate ?: return null
        val singularEnd = if (series.dueMode == DueMode.SINGULAR) series.dueSingular else null
        // MAX_ITERS bounds iteration; ~11 years covers any sane far-future rule
        val farBound = minDate(series.endDate, singularEnd, Dates.addDays(today, 4000)) ?: return null
        val from = if (today < anchor) anchor else today
        val exceptions = series.exceptions.toSet()
        val candidates = enumerateDates(series.rule, anchor, from, farBound)
        val first = candidates.firstOrNull { it !in exceptions } ?: return null
        // dues pair by index over the full enumeration from the anchor (incl.
        // excepted slots), exactly like the oracle's computeDue alignment
        val due = when (series.dueMode) {
            DueMode.SINGULAR -> series.dueSingular
            DueMode.RECURRING -> series.dueRule?.let { dueRule ->
                val idx = enumerateDates(series.rule, anchor, anchor, first).size - 1
                enumerateDates(dueRule, anchor, anchor, farBound).getOrNull(idx)
            }
            DueMode.NONE -> null
        }
        return PlannedOccurrence(first, first, due)
    }

    // ── human-readable summary ────────────────────────────────

    fun summary(rule: Rule?): String {
        if (rule == null) return ""
        return when (rule.pattern) {
            Pattern.INTERVAL -> {
                val unit = rule.unit.name.lowercase()
                if (rule.interval == 1) "Every $unit" else "Every ${rule.interval} ${unit}s"
            }

            Pattern.WEEKDAY -> {
                val days = rule.weekdays.sorted().map { Dates.WD_FULL[it] }
                var s = when {
                    days.size == 7 -> "Every day"
                    days.isEmpty() -> "Weekly"
                    days.size > 2 -> "Every " + days.joinToString(", ") { it.take(3) }
                    else -> "Every " + days.joinToString(" & ")
                }
                if (rule.weekInterval > 1) s += ", every ${rule.weekInterval} weeks"
                s
            }

            Pattern.MONTHDAY -> {
                val ord = listOf("", "first", "second", "third", "fourth", "fifth")
                var base = if (rule.monthMode == MonthMode.DATE) {
                    "The " + Dates.ordinal(rule.monthDate)
                } else {
                    "The " + (if (rule.monthWeekPos == -1) "last" else ord[rule.monthWeekPos]) +
                        " " + Dates.WD_FULL[rule.monthWeekday]
                }
                base += if (rule.monthInterval == 1) " of every month" else " of every ${rule.monthInterval} months"
                base
            }
        }
    }
}
