package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.AlignmentLists
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.GoalStep
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v2.13.0 — the Alignment pane's lists: the one habit a task ticks and the
 * goals it moves, with their static subtitles (no streak, no progress
 * percent: DEC-2 / DM-NOT).
 */
class AlignmentListsTest {

    private val today = "2026-10-10"

    // ── Ticks a habit · one ──

    @Test
    fun `a habit's subtitle is its cadence in the Habits tab's words`() {
        assertEquals("3× / WEEK", AlignmentLists.cadence(Habit("h", "Run", target = 3.0)))
        assertEquals("1× / WEEK", AlignmentLists.cadence(Habit("h", "Call home", target = 1.0)))
        assertEquals(
            "10 PAGES / DAY",
            AlignmentLists.cadence(Habit("h", "Read", kind = HabitKind.QUANTITATIVE, target = 10.0, unit = "pages")),
        )
        assertEquals(
            "1,500 / DAY",
            AlignmentLists.cadence(Habit("h", "Steps", kind = HabitKind.QUANTITATIVE, target = 1500.0)),
        )
        assertEquals(
            "2.5 KM / DAY",
            AlignmentLists.cadence(Habit("h", "Walk", kind = HabitKind.QUANTITATIVE, target = 2.5, unit = "km")),
        )
    }

    @Test
    fun `seven times a week reads DAILY`() {
        assertEquals("DAILY", AlignmentLists.cadence(Habit("h", "Write in the morning", target = 7.0)))
    }

    @Test
    fun `every habit is listed in Habits-tab order with the linked one selected`() {
        val habits = listOf(
            Habit("b", "Read 10 pages", createdAt = 20),
            Habit("a", "Write in the morning", createdAt = 10),
            Habit("c", "Stretch", createdAt = 30),
        )
        val rows = AlignmentLists.habits(habits, linkedId = "c")
        assertEquals(listOf("a", "b", "c"), rows.map { it.id })
        assertEquals(listOf(false, false, true), rows.map { it.selected })
        assertEquals("Write in the morning", rows.first().name)
    }

    @Test
    fun `tapping a habit selects only it, and tapping the selected one deselects it`() {
        assertEquals("a", AlignmentLists.tapHabit(linkedId = null, tapped = "a"))
        assertEquals("b", AlignmentLists.tapHabit(linkedId = "a", tapped = "b"))
        assertNull(AlignmentLists.tapHabit(linkedId = "a", tapped = "a"))
    }

    // ── Moves a goal · any ──

    private val draft = Goal("g1", "Complete the first draft", type = GoalType.ACCUMULATIVE, targetQty = 80000.0, currentQty = 62000.0, unit = "words")
    private val marathon = Goal("g2", "Run a half-marathon", type = GoalType.ACCUMULATIVE, targetQty = 21.0, currentQty = 8.0, unit = "km")
    private val course = Goal(
        "g3", "Finish the course", type = GoalType.STEPPED,
        steps = listOf(GoalStep("Defined", true), GoalStep("a", true), GoalStep("b", true), GoalStep("c"), GoalStep("d")),
    )

    @Test
    fun `goal subtitles by type, never a made-up percent`() {
        assertEquals("62,000 / 80,000 WORDS", AlignmentLists.goalSubtitle(draft, today))
        assertEquals("8 / 21 KM", AlignmentLists.goalSubtitle(marathon, today))
        assertEquals("3 OF 5 STEPS", AlignmentLists.goalSubtitle(course, today))
        assertEquals("BY 12 DEC", AlignmentLists.goalSubtitle(Goal("g", "Ship it", deadline = "2026-12-12"), today))
        assertEquals("BY 3 FEB 2027", AlignmentLists.goalSubtitle(Goal("g", "Ship it", deadline = "2027-02-03"), today))
        assertNull(AlignmentLists.goalSubtitle(Goal("g", "Be kinder"), today))
    }

    @Test
    fun `odd goal shapes still read plainly`() {
        assertEquals(
            "2.5 / 10",
            AlignmentLists.goalSubtitle(Goal("g", "Save", type = GoalType.ACCUMULATIVE, targetQty = 10.0, currentQty = 2.5), today),
        )
        assertNull(AlignmentLists.goalSubtitle(Goal("g", "Plan", type = GoalType.STEPPED), today))
    }

    @Test
    fun `ongoing goals are listed, plus non-ongoing ones the task was opened linked to`() {
        val done = draft.copy(id = "done", state = GoalState.COMPLETED)
        val frozen = course.copy(id = "frozen", state = GoalState.FROZEN)
        val other = marathon.copy(id = "other", state = GoalState.COMPLETED)
        val rows = AlignmentLists.goals(
            goals = listOf(draft, done, frozen, other, marathon),
            linkedIds = listOf("g1", "frozen"),
            openedWith = listOf("g1", "done", "frozen"),
            today = today,
        )
        assertEquals(listOf("g1", "done", "frozen", "g2"), rows.map { it.id })
        assertEquals(listOf(true, false, true, false), rows.map { it.linked })
    }

    @Test
    fun `a non-ongoing goal's subtitle leads with its status`() {
        val rows = AlignmentLists.goals(
            goals = listOf(
                draft.copy(state = GoalState.COMPLETED),
                course.copy(state = GoalState.FROZEN),
                Goal("g", "Be kinder", state = GoalState.COMPLETED),
            ),
            linkedIds = listOf("g1", "g3", "g"),
            openedWith = listOf("g1", "g3", "g"),
            today = today,
        )
        assertEquals(listOf("COMPLETED · 62,000 / 80,000 WORDS", "FROZEN · 3 OF 5 STEPS", "COMPLETED"), rows.map { it.subtitle })
    }

    @Test
    fun `once a non-ongoing goal is unticked and saved it isn't offered again`() {
        val done = draft.copy(state = GoalState.COMPLETED)
        val reopened = AlignmentLists.goals(listOf(done, marathon), linkedIds = emptyList(), openedWith = emptyList(), today = today)
        assertEquals(listOf("g2"), reopened.map { it.id })
    }

    @Test
    fun `tapping a goal ticks or unticks it, keeping the others`() {
        assertEquals(listOf("g1", "g2"), AlignmentLists.tapGoal(listOf("g1"), "g2"))
        assertEquals(listOf("g2"), AlignmentLists.tapGoal(listOf("g1", "g2"), "g1"))
    }
}
