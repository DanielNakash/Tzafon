package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.attribution.Attribution
import com.thefoxworks.tzafon.domain.model.Contribution
import com.thefoxworks.tzafon.domain.model.ContributionVia
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalStep
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M5 — the DM-ATTR case table: diamond, multi-goal, unit mismatch, reversal. */
class AttributionTest {

    private val wordsGoal = Goal(
        id = "gW", title = "Finish the draft", type = GoalType.ACCUMULATIVE,
        targetQty = 60000.0, unit = "words", currentQty = 12000.0,
    )
    private val kmGoal = Goal(
        id = "gK", title = "Run 500 km", type = GoalType.ACCUMULATIVE,
        targetQty = 500.0, unit = "km", currentQty = 40.0,
    )
    private val steppedGoal = Goal(
        id = "gS", title = "Publish the zine", type = GoalType.STEPPED,
        steps = listOf(GoalStep("Defined this goal ✓", true), GoalStep("Draft", false)),
    )
    private val goals = mapOf("gW" to wordsGoal, "gK" to kmGoal, "gS" to steppedGoal)

    private val writeHabit = Habit(
        id = "h1", name = "Daily writing", kind = HabitKind.QUANTITATIVE,
        target = 500.0, unit = "words", goalId = "gW",
    )
    private val runHabit = Habit(
        id = "h2", name = "Run", kind = HabitKind.FREQUENCY, target = 3.0, goalId = "gK",
    )

    private fun task(goalIds: List<String> = emptyList(), habitId: String? = null) =
        Task(id = "t1", title = "t", goalIds = goalIds, habitId = habitId)

    // ── the diamond: direct + habit reach the same goal → once ──

    @Test
    fun `diamond counts once and prefers the explicit amount`() {
        val plan = Attribution.planOnDone(
            task(goalIds = listOf("gW"), habitId = "h1"),
            writeHabit, goals, enteredAmount = 620.0,
        )
        assertEquals(1, plan.size) // deduped
        assertEquals("gW", plan[0].goalId)
        assertEquals(620.0, plan[0].amount!!, 0.001)
        assertEquals(ContributionVia.DIRECT, plan[0].via)
    }

    // ── multi-goal: each reachable goal advances once ──

    @Test
    fun `multiple direct goals each advance once`() {
        val plan = Attribution.planOnDone(
            task(goalIds = listOf("gW", "gK")), habit = null, goals, enteredAmount = 100.0,
        )
        assertEquals(2, plan.size)
        assertTrue(plan.all { it.amount == 100.0 })
    }

    // ── unit mismatch: directional only (DM-ATTR-3) ──

    @Test
    fun `habit-only path advances only when units match`() {
        // words habit → words goal: advances
        val match = Attribution.planOnDone(
            task(habitId = "h1"), writeHabit, goals, enteredAmount = 500.0,
        )
        assertEquals(500.0, match[0].amount!!, 0.001)
        assertEquals(ContributionVia.HABIT, match[0].via)

        // frequency habit (no unit) → km goal: directional, no numeric change
        val mismatch = Attribution.planOnDone(
            task(habitId = "h2"), runHabit, goals, enteredAmount = null,
        )
        assertEquals(1, mismatch.size)
        assertNull(mismatch[0].amount)
    }

    @Test
    fun `stepped and generic goals never move numerically`() {
        val plan = Attribution.planOnDone(
            task(goalIds = listOf("gS")), habit = null, goals, enteredAmount = 99.0,
        )
        assertEquals(1, plan.size)
        assertNull(plan[0].amount) // wiring shows, bar doesn't move
    }

    // ── exact reversal (DM-ATTR-1.4 / NFR-DATA-2) ──

    @Test
    fun `reversal subtracts exactly the task's ledger rows`() {
        val ledger = listOf(
            Contribution("c1", "t1", "gW", 620.0, ContributionVia.DIRECT),
            Contribution("c2", "t1", "gK", 5.0, ContributionVia.HABIT),
            Contribution("c3", "tOther", "gW", 100.0, ContributionVia.DIRECT),
        )
        val deltas = Attribution.reversalDeltas(ledger, "t1")
        assertEquals(620.0, deltas["gW"]!!, 0.001)
        assertEquals(5.0, deltas["gK"]!!, 0.001)
        assertEquals(2, deltas.size) // the other task's row is untouched
    }

    // ── the "how much?" prompt rule ──

    @Test
    fun `prompt fires for quant habits or direct accumulative goals only`() {
        assertTrue(Attribution.needsAmountPrompt(task(habitId = "h1"), writeHabit, goals))
        assertTrue(Attribution.needsAmountPrompt(task(goalIds = listOf("gW")), null, goals))
        assertFalse(Attribution.needsAmountPrompt(task(goalIds = listOf("gS")), null, goals))
        assertFalse(Attribution.needsAmountPrompt(task(habitId = "h2"), runHabit, goals))
        assertEquals("words", Attribution.promptUnit(task(goalIds = listOf("gW")), null, goals))
    }
}
