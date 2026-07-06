package com.thefoxworks.tzafon.domain.habits

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.Task

/**
 * DM-HABIT-2/3/4 — the forgiving in-period rate, the long identity arc and
 * the fresh-start rule, all pure and week-start-aware (DM-REL-2).
 * No streaks, no adherence % — deliberately absent (DEC-2/4).
 */
object HabitMath {

    /** Sunday-based day index (0..6) of a week-start setting. */
    private fun startIndex(weekStart: String): Int = when (weekStart) {
        "MONDAY" -> 1
        "SATURDAY" -> 6
        else -> 0 // SUNDAY, the default
    }

    /** The ISO date the current period began (DM-REL-2 drives boundaries). */
    fun weekStartFor(date: String, weekStart: String): String {
        val dow = Dates.dayOfWeek(date)
        val back = (dow - startIndex(weekStart) + 7) % 7
        return Dates.addDays(date, -back.toLong())
    }

    /** The 7 dates of the period containing [date], in order. */
    fun weekDates(date: String, weekStart: String): List<String> {
        val start = weekStartFor(date, weekStart)
        return (0L..6L).map { Dates.addDays(start, it) }
    }

    /** DM-HABIT-5 — where a task-completion logs: its To Do date, else today. */
    fun logDateFor(task: Task, today: String): String = task.toDoDate ?: today

    data class WeekSnapshot(
        /** FREQUENCY: days done this period · QUANTITATIVE: days with a log */
        val doneDays: Int,
        /** QUANTITATIVE: total amount logged this period */
        val amountSum: Double,
        /** per-day amounts for the quant bars, week-start order */
        val amounts: List<Double>,
        /** per-day done flags, week-start order */
        val doneFlags: List<Boolean>,
    )

    /** The current period's forgiving rate numbers ("3 of 4 this week"). */
    fun week(habit: Habit, logs: List<HabitLog>, today: String, weekStart: String): WeekSnapshot {
        val dates = weekDates(today, weekStart)
        val byDate = logs.filter { it.habitId == habit.id && it.done }.associateBy { it.date }
        val flags = dates.map { byDate.containsKey(it) }
        val amounts = dates.map { byDate[it]?.amount ?: 0.0 }
        return WeekSnapshot(
            doneDays = flags.count { it },
            amountSum = amounts.sum(),
            amounts = amounts,
            doneFlags = flags,
        )
    }

    /**
     * The long arc — "Running 9 weeks". Weeks elapsed (inclusive) since the
     * first log or the habit's start, whichever is earlier. Never a streak:
     * misses don't reset it; it only grows while the habit lives.
     */
    fun arcWeeks(habit: Habit, logs: List<HabitLog>, today: String, weekStart: String): Int {
        val firstLog = logs.filter { it.habitId == habit.id }.minOfOrNull { it.date }
        val anchor = firstLog ?: return 1
        val start = weekStartFor(anchor, weekStart)
        val thisStart = weekStartFor(today, weekStart)
        return (Dates.dayDiff(thisStart, start) / 7L).toInt() + 1
    }

    /** "Running 9 weeks" / "First week" — honest weeks-to-months copy. */
    fun arcLabel(weeks: Int): String = when {
        weeks <= 1 -> "First week"
        weeks < 9 -> "Running $weeks weeks"
        weeks < 13 -> "Running ${weeks / 4} months"
        else -> "Running ${weeks / 4} months strong"
    }

    /**
     * DM-HABIT-4 — a gap surfaces a fresh start, never a wall of red:
     * show the clean-slate card when the *previous* period fell short and
     * this one hasn't started yet.
     */
    fun freshStart(habit: Habit, logs: List<HabitLog>, today: String, weekStart: String): Boolean {
        val thisWeek = week(habit, logs, today, weekStart)
        if (thisWeek.doneDays > 0) return false
        val prevDates = weekDates(Dates.addDays(weekStartFor(today, weekStart), -7), weekStart)
        val byDate = logs.filter { it.habitId == habit.id && it.done }.map { it.date }.toSet()
        val prevDone = prevDates.count { it in byDate }
        val targetDays = when (habit.kind) {
            HabitKind.FREQUENCY -> habit.target.toInt()
            HabitKind.QUANTITATIVE -> habit.targetDays ?: 7
        }
        // fell short last week AND has any history at all (not a brand-new habit)
        return prevDone < targetDays && logs.any { it.habitId == habit.id }
    }

    /**
     * The 5-week history grid: columns = weeks (oldest → current), rows = the
     * 7 days in week-start order. 0 = nothing · 1 = partial (quant, under the
     * daily target) · 2 = done/full.
     */
    fun historyGrid(habit: Habit, logs: List<HabitLog>, today: String, weekStart: String): List<List<Int>> {
        val thisStart = weekStartFor(today, weekStart)
        val byDate = logs.filter { it.habitId == habit.id && it.done }.associateBy { it.date }
        return (4 downTo 0).map { weeksBack ->
            val start = Dates.addDays(thisStart, -7L * weeksBack)
            (0L..6L).map { d ->
                val log = byDate[Dates.addDays(start, d)]
                when {
                    log == null -> 0
                    habit.kind == HabitKind.QUANTITATIVE &&
                        (log.amount ?: 0.0) < habit.target -> 1
                    else -> 2
                }
            }
        }
    }
}
