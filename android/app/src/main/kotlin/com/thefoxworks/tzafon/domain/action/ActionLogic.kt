package com.thefoxworks.tzafon.domain.action

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Recurrence

/**
 * Pure membership/grouping rules for the Action layer (FR-TODAY, FR-PLAN,
 * FR-REC-3). Kept free of Android so the M2 acceptance cases run as JUnit.
 */
object ActionLogic {

    /** FR-TODAY-5 — static first; adaptive baseline is a §11 tunable for later. */
    const val OVERLOAD_THRESHOLD = 8

    // ── Today (FR-TODAY) ──────────────────────────────────────

    /**
     * FR-TODAY-1: active tasks scheduled for today OR due today (the latter
     * flagged by the row's DUE TODAY pill). FR-TODAY-3: overdue stays out.
     */
    fun isTodayTask(t: Task, today: String): Boolean =
        t.state == TaskState.OPEN && (t.toDoDate == today || t.dueDate == today)

    /**
     * The done strip: tasks completed as part of today — scheduled/due today,
     * or (undated quick-adds) completed within today's clock bounds.
     */
    fun isDoneToday(t: Task, today: String, dayStartMs: Long, dayEndMs: Long): Boolean =
        t.state == TaskState.DONE && (
            t.toDoDate == today || t.dueDate == today ||
                (t.completedAt != null && t.completedAt in dayStartMs..dayEndMs)
            )

    /** FR-TODAY-2 ordering: manual sortOrder first, then capture order. */
    fun todayOrder(tasks: List<Task>): List<Task> =
        tasks.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))

    /** FR-TODAY-4 — count for the calm slippage banner. */
    fun slippedCount(tasks: List<Task>, today: String): Int =
        tasks.count { it.state == TaskState.OPEN && (it.toDoDate ?: today) < today }

    /** FR-TODAY-5 — supportive, never punitive; fires on open count only. */
    fun isOverloaded(openTodayCount: Int): Boolean = openTodayCount >= OVERLOAD_THRESHOLD

    /** DM-FOCUS-1 — the Today's-Focus marker (marking UI arrives in M7). */
    fun isFocusToday(t: Task, today: String): Boolean =
        t.state == TaskState.OPEN && t.focusDate == today

    // ── Planning (FR-PLAN) ────────────────────────────────────

    enum class RangePreset(val label: String) {
        DAYS_7("7 days"), DAYS_14("14 days"), DAYS_30("30 days"), MONTH("Month"), CUSTOM("Custom");

        companion object {
            fun parse(s: String?): RangePreset = entries.firstOrNull { it.name == s } ?: DAYS_7
        }
    }

    /** FR-PLAN-2 — the chosen range in days from today (inclusive end). */
    fun rangeDays(preset: RangePreset, today: String, customEnd: String?): Long = when (preset) {
        RangePreset.DAYS_7 -> 7
        RangePreset.DAYS_14 -> 14
        RangePreset.DAYS_30 -> 30
        RangePreset.MONTH -> {
            val d = Dates.parse(today)
            Dates.dayDiff(Dates.iso(d.withDayOfMonth(d.lengthOfMonth())), today)
        }
        RangePreset.CUSTOM ->
            customEnd?.let { maxOf(Dates.dayDiff(it, today), 1) } ?: 7
    }

    /**
     * FR-REC-3 / NFR-PERF-2 — the session's effective generation horizon:
     * never below the rolling 60-day window, extended when looking further.
     */
    fun effectiveHorizonDays(rangeDays: Long): Long = maxOf(Recurrence.HORIZON_DAYS, rangeDays)

    data class PlanningGroups(
        val overdue: List<Task>,
        /** date-keyed groups today..rangeEnd, in date order */
        val dated: List<Pair<Dates.Group, List<Task>>>,
        val inbox: List<Task>,
    )

    /** FR-PLAN-1 — overdue at top, today+future to the range end, then Inbox. */
    fun planningGroups(tasks: List<Task>, today: String, rangeDays: Long): PlanningGroups {
        val open = tasks.filter { it.state == TaskState.OPEN }
        val rangeEnd = Dates.addDays(today, rangeDays)

        val overdue = open.filter { (it.toDoDate ?: "") < today && it.toDoDate != null }
            .sortedBy { it.toDoDate }
        val inRange = open.filter { it.toDoDate != null && it.toDoDate >= today && it.toDoDate <= rangeEnd }
        val dated = inRange
            .groupBy { Dates.groupFor(it.toDoDate!!, today) }
            .toList()
            .sortedBy { it.first.order }
            .map { (g, items) -> g to todayOrder(items) }
        val inbox = open.filter { it.toDoDate == null }.sortedBy { it.createdAt }

        return PlanningGroups(overdue, dated, inbox)
    }

    /** "SLIPPED 3D AGO" — the decide-card meta line (FR-PLAN-3). */
    fun slippedDays(t: Task, today: String): Long =
        t.toDoDate?.let { Dates.dayDiff(today, it) } ?: 0

    // ── All Tasks (FR-ALL) ────────────────────────────────────

    /** FR-ALL-2 — simple title/description search; blank matches all. */
    fun matchesQuery(t: Task, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return t.title.contains(q, ignoreCase = true) ||
            (t.description?.contains(q, ignoreCase = true) ?: false)
    }

    /** The date a relative group key stands for (for horizon comparison). */
    private fun groupDate(key: String, today: String): String = when (key) {
        "overdue" -> Dates.addDays(today, -1)
        "today" -> today
        "tomorrow" -> Dates.addDays(today, 1)
        else -> key // dated groups are keyed by their ISO date
    }

    /**
     * FR-ALL-4 — where the horizon divider sits in the dated group list:
     * the index of the first group past the effective horizon (== size when
     * everything scheduled falls inside it).
     */
    fun horizonInsertIndex(groupKeys: List<String>, today: String, horizonDate: String): Int {
        val i = groupKeys.indexOfFirst { groupDate(it, today) > horizonDate }
        return if (i == -1) groupKeys.size else i
    }
}
