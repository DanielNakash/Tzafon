package com.thefoxworks.tzafon.domain.attribution

import com.thefoxworks.tzafon.domain.model.ContributionVia
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.Task

/**
 * DM-ATTR — one task-completion contributes to any given goal at most once.
 * Pure planning: gather → dedupe → apply-once rules; the repository turns
 * the plan into ledger rows (NFR-DATA-2) so reversal is exact.
 */
object Attribution {

    /**
     * One planned effect on one goal. [amount] == null means directional
     * only — the wiring shows, identity accrues, no numeric change and no
     * ledger row (DM-ATTR-3).
     */
    data class Planned(
        val goalId: String,
        val amount: Double?,
        val via: ContributionVia,
    )

    /**
     * Plan the goal effects of completing [task].
     *
     * [enteredAmount] is the single "how much?" answer (it feeds the habit
     * log and stands as the explicit task→goal amount). Rules per goal:
     *  - reachable directly AND via the habit → prefer the explicit amount,
     *    never both summed (the diamond, DM-ATTR-1.3)
     *  - Accumulative + direct link → the entered amount
     *  - Accumulative reachable only via habit → auto-advance only when the
     *    habit is quantitative and units match (DM-ATTR-3), by the logged
     *    amount
     *  - Stepped / Generic / unit mismatch → directional only
     */
    fun planOnDone(
        task: Task,
        habit: Habit?,
        goalsById: Map<String, Goal>,
        enteredAmount: Double?,
    ): List<Planned> {
        val direct = task.goalIds.toSet()
        val viaHabit = setOfNotNull(habit?.goalId?.takeIf { habit.id == task.habitId })
        val reachable = (direct + viaHabit) // deduped by construction

        return reachable.mapNotNull { goalId ->
            val goal = goalsById[goalId] ?: return@mapNotNull null
            val isDirect = goalId in direct
            when {
                goal.type != GoalType.ACCUMULATIVE ->
                    Planned(goalId, null, if (isDirect) ContributionVia.DIRECT else ContributionVia.HABIT)

                isDirect ->
                    // explicit task→goal amount (preferred over the habit path)
                    Planned(goalId, enteredAmount?.takeIf { it > 0 }, ContributionVia.DIRECT)

                else -> {
                    // habit-only path: quantitative + matching units auto-advance
                    val unitsMatch = habit?.unit != null &&
                        goal.unit != null &&
                        habit.unit.trim().equals(goal.unit.trim(), ignoreCase = true)
                    Planned(
                        goalId,
                        if (unitsMatch) enteredAmount?.takeIf { it > 0 } else null,
                        ContributionVia.HABIT,
                    )
                }
            }
        }
    }

    /** The exact per-goal deltas to subtract when reversing [taskId]'s rows. */
    fun reversalDeltas(contributions: List<com.thefoxworks.tzafon.domain.model.Contribution>, taskId: String): Map<String, Double> =
        contributions.filter { it.taskId == taskId }
            .groupBy { it.goalId }
            .mapValues { (_, rows) -> rows.sumOf { it.amount } }

    /** Whether completing [task] needs the "how much?" prompt (DM-GOAL-3/DM-HABIT-5). */
    fun needsAmountPrompt(task: Task, habit: Habit?, goalsById: Map<String, Goal>): Boolean {
        val quantHabit = habit != null && habit.id == task.habitId &&
            habit.kind == com.thefoxworks.tzafon.domain.model.HabitKind.QUANTITATIVE
        val directAccumulative = task.goalIds.any { goalsById[it]?.type == GoalType.ACCUMULATIVE }
        return quantHabit || directAccumulative
    }

    /** The unit to show on the prompt — the habit's, else the first goal's. */
    fun promptUnit(task: Task, habit: Habit?, goalsById: Map<String, Goal>): String? =
        habit?.takeIf { it.id == task.habitId }?.unit
            ?: task.goalIds.firstNotNullOfOrNull { goalsById[it]?.takeIf { g -> g.type == GoalType.ACCUMULATIVE }?.unit }
}
