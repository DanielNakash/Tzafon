package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind

/** One row of "Ticks a habit · one": a radio, at most one selected. */
data class HabitRow(val id: String, val name: String, val cadence: String, val selected: Boolean)

/** One row of "Moves a goal · any": a checkbox; [subtitle] is null for a goal with nothing to show. */
data class GoalRow(val id: String, val title: String, val subtitle: String?, val linked: Boolean)

/**
 * v2.13.0 — the Alignment pane's habit and goal lists. Subtitles are static
 * facts about the habit or goal, never a streak or a progress percent
 * (DEC-2 / DM-NOT); `Goal.pct()`'s stand-in 40% for a generic goal never shows.
 */
object AlignmentLists {

    /** Every habit (habits have no archived state), in Habits-tab order. */
    fun habits(habits: List<Habit>, linkedId: String?): List<HabitRow> =
        habits.sortedBy { it.createdAt }.map { HabitRow(it.id, it.name, cadence(it), it.id == linkedId) }

    /** Tapping a habit links only it; tapping the linked one unlinks it (no "No habit" row). */
    fun tapHabit(linkedId: String?, tapped: String): String? = tapped.takeIf { it != linkedId }

    /** The Habits tab's target, in caps: `3× / WEEK`, `10 PAGES / DAY`, and `DAILY` for 7× a week. */
    fun cadence(h: Habit): String = when (h.kind) {
        HabitKind.FREQUENCY -> if (h.target.toInt() >= 7) "DAILY" else "${h.target.toInt()}× / WEEK"
        HabitKind.QUANTITATIVE -> listOfNotNull(qty(h.target), h.unit?.trim()?.takeIf { it.isNotEmpty() })
            .joinToString(" ").uppercase() + " / DAY"
    }

    /**
     * Ongoing goals, plus any completed or frozen goal the task is linked to
     * now ([linkedIds]) or was when the editor opened ([openedWith]), so an
     * unticked one stays to re-tick until Save, and is gone for good after.
     * Goals keep the order given.
     */
    fun goals(goals: List<Goal>, linkedIds: List<String>, openedWith: List<String>, today: String): List<GoalRow> =
        goals.filter { it.state == GoalState.ONGOING || it.id in linkedIds || it.id in openedWith }
            .map { g ->
                val facts = goalSubtitle(g, today)
                val subtitle = if (g.state == GoalState.ONGOING) facts
                else listOfNotNull(g.state.name, facts).joinToString(" · ")
                GoalRow(g.id, g.title, subtitle, g.id in linkedIds)
            }

    /** Ticking adds the goal after the others; unticking removes it. */
    fun tapGoal(linkedIds: List<String>, tapped: String): List<String> =
        if (tapped in linkedIds) linkedIds - tapped else linkedIds + tapped

    /**
     * What a goal row says under its title: `62,000 / 80,000 WORDS`,
     * `3 OF 5 STEPS`, or a generic goal's deadline, `BY 12 DEC` (with the
     * year when it isn't [today]'s); null when there's nothing to say.
     */
    fun goalSubtitle(g: Goal, today: String): String? = when (g.type) {
        GoalType.ACCUMULATIVE -> listOfNotNull("${qty(g.currentQty)} / ${qty(g.targetQty)}", g.unit?.trim()?.takeIf { it.isNotEmpty() })
            .joinToString(" ").uppercase()
        GoalType.STEPPED -> g.steps.takeIf { it.isNotEmpty() }?.let { "${it.count { s -> s.done }} OF ${it.size} STEPS" }
        GoalType.GENERIC -> g.deadline?.let { byDate(it, today) }
    }

    private fun byDate(iso: String, today: String): String {
        val d = Dates.parse(iso)
        val year = if (d.year != Dates.parse(today).year) " ${d.year}" else ""
        return "BY ${d.dayOfMonth} ${Dates.MO[d.monthValue - 1].uppercase()}$year"
    }

    private fun qty(v: Double): String =
        if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else v.toString()
}
