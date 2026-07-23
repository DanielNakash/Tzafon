package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.action.ActionLogic
import com.thefoxworks.tzafon.domain.action.ActionLogic.RangePreset
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** M2 acceptance: FR-TODAY membership, overload, FR-PLAN groups, FR-REC-3 horizon. */
class ActionLogicTest {

    private val today = "2026-07-05"

    private fun task(
        id: String,
        toDo: String? = null,
        due: String? = null,
        state: TaskState = TaskState.OPEN,
        sortOrder: Long = 0,
        createdAt: Long = 0,
        completedAt: Long? = null,
        focusDate: String? = null,
        seriesId: String? = null,
    ) = Task(
        id = id, title = id, toDoDate = toDo, dueDate = due, state = state,
        sortOrder = sortOrder, createdAt = createdAt, completedAt = completedAt,
        focusDate = focusDate, seriesId = seriesId,
    )

    // ── FR-TODAY-1/3 membership ───────────────────────────────

    @Test
    fun `scheduled today is in - overdue stays out`() {
        assertTrue(ActionLogic.isTodayTask(task("a", toDo = today), today))
        assertFalse(ActionLogic.isTodayTask(task("b", toDo = "2026-07-01"), today)) // FR-TODAY-3
    }

    @Test
    fun `due today surfaces even when scheduled for another day`() {
        assertTrue(ActionLogic.isTodayTask(task("a", toDo = "2026-07-09", due = today), today))
    }

    @Test
    fun `settled states never appear in today`() {
        for (s in listOf(TaskState.CLOSED, TaskState.FROZEN, TaskState.BACKLOG, TaskState.DONE)) {
            assertFalse(ActionLogic.isTodayTask(task("x", toDo = today, state = s), today))
        }
    }

    @Test
    fun `done strip counts scheduled and same-day completions`() {
        val bounds = 1_000L..2_000L
        assertTrue(ActionLogic.isDoneToday(task("a", toDo = today, state = TaskState.DONE), today, bounds.first, bounds.last))
        // undated quick-add completed within today's clock
        assertTrue(ActionLogic.isDoneToday(task("b", state = TaskState.DONE, completedAt = 1_500L), today, bounds.first, bounds.last))
        assertFalse(ActionLogic.isDoneToday(task("c", state = TaskState.DONE, completedAt = 5_000L), today, bounds.first, bounds.last))
    }

    // ── FR-TODAY-2 ordering ───────────────────────────────────

    @Test
    fun `manual sort order wins - capture order breaks ties`() {
        val out = ActionLogic.todayOrder(
            listOf(
                task("late", sortOrder = 2, createdAt = 1),
                task("first", sortOrder = 1, createdAt = 9),
                task("tieOld", sortOrder = 0, createdAt = 3),
                task("tieNew", sortOrder = 0, createdAt = 7),
            )
        )
        assertEquals(listOf("tieOld", "tieNew", "first", "late"), out.map { it.id })
    }

    // ── FR-TODAY-4/5 banners ──────────────────────────────────

    @Test
    fun `slippage counts open overdue only`() {
        val tasks = listOf(
            task("a", toDo = "2026-07-01"),
            task("b", toDo = "2026-07-04"),
            task("c", toDo = "2026-07-04", state = TaskState.CLOSED),
            task("d", toDo = today),
            task("e"), // undated is not slippage
        )
        assertEquals(2, ActionLogic.slippedCount(tasks, today))
    }

    // ── FR-TODAY-8: slippage count = planning overdue size ────

    @Test
    fun `FR-TODAY-8 slippedCount hides recurring slips whose series has today occurrence`() {
        val tasks = listOf(
            task("S__slip", toDo = "2026-07-03", seriesId = "S"),
            task("S__today", toDo = today, seriesId = "S"),
            task("oneoff", toDo = "2026-07-02"),
        )
        // one-off overdue counts; the slipped S occurrence is hidden by the series rule
        assertEquals(1, ActionLogic.slippedCount(tasks, today))
    }

    @Test
    fun `FR-TODAY-8 slippedCount today-occurrence state does not matter`() {
        for (todayState in listOf(TaskState.OPEN, TaskState.DONE, TaskState.CLOSED)) {
            val tasks = listOf(
                task("S__slip", toDo = "2026-07-03", seriesId = "S"),
                task("S__today", toDo = today, state = todayState, seriesId = "S"),
            )
            assertEquals("hidden regardless of state=$todayState", 0, ActionLogic.slippedCount(tasks, today))
        }
    }

    @Test
    fun `FR-TODAY-8 slippedCount equals planning overdue size on a mixed fixture`() {
        val tasks = listOf(
            // one-off overdue — counts
            task("oneoffA", toDo = "2026-07-01"),
            task("oneoffB", toDo = "2026-07-04"),
            // series with today occurrence Open → slip hidden
            task("A__slip", toDo = "2026-07-03", seriesId = "A"),
            task("A__today", toDo = today, seriesId = "A"),
            // series with today occurrence Done → slip hidden
            task("B__slip", toDo = "2026-07-02", seriesId = "B"),
            task("B__today", toDo = today, state = TaskState.DONE, seriesId = "B"),
            // series with no today occurrence → slip visible
            task("C__slip", toDo = "2026-07-02", seriesId = "C"),
            task("C__tomorrow", toDo = "2026-07-06", seriesId = "C"),
            // noise: today, inbox, done
            task("todayTask", toDo = today),
            task("inbox"),
            task("doneNoise", toDo = "2026-07-04", state = TaskState.DONE),
        )
        val count = ActionLogic.slippedCount(tasks, today)
        val planning = ActionLogic.planningGroups(tasks, today, rangeDays = 7)
        assertEquals(planning.overdue.size, count)
        assertEquals(3, count) // oneoffA, oneoffB, C__slip
    }

    @Test
    fun `overload fires at the static threshold`() {
        assertFalse(ActionLogic.isOverloaded(7))
        assertTrue(ActionLogic.isOverloaded(8)) // ~8, tunable §11
    }

    @Test
    fun `focus marker is today-scoped and open-only`() {
        assertTrue(ActionLogic.isFocusToday(task("a", toDo = today, focusDate = today), today))
        assertFalse(ActionLogic.isFocusToday(task("b", toDo = today, focusDate = "2026-07-04"), today))
        assertFalse(ActionLogic.isFocusToday(task("c", focusDate = today, state = TaskState.DONE), today))
    }

    // ── FR-PLAN groups ────────────────────────────────────────

    @Test
    fun `planning groups - overdue top then dated then inbox`() {
        val g = ActionLogic.planningGroups(
            listOf(
                task("over2", toDo = "2026-07-03"),
                task("over1", toDo = "2026-07-01"),
                task("today", toDo = today),
                task("tomorrow", toDo = "2026-07-06"),
                task("in5", toDo = "2026-07-10"),
                task("beyond", toDo = "2026-07-20"), // outside the 7-day range
                task("inbox1"),
                task("doneToday", toDo = today, state = TaskState.DONE),
                task("someday", state = TaskState.BACKLOG),
            ),
            today,
            rangeDays = 7,
        )
        assertEquals(listOf("over1", "over2"), g.overdue.map { it.id }) // oldest first
        assertEquals(listOf("Today", "Tomorrow", "Fri, Jul 10"), g.dated.map { it.first.label })
        assertEquals(listOf("inbox1"), g.inbox.map { it.id })
    }

    // ── FR-PLAN-5 slipped recurring hidden when next occurrence is today ──

    @Test
    fun `FR-PLAN-5 slipped recurring hidden when today occurrence exists - open done closed`() {
        // A single series S with a slipped Sunday occurrence + a today occurrence.
        // The today occurrence's state must not matter: Open, Done, Skipped(≈CLOSED).
        for (todayState in listOf(TaskState.OPEN, TaskState.DONE, TaskState.CLOSED)) {
            val g = ActionLogic.planningGroups(
                listOf(
                    task("S__slip", toDo = "2026-07-03", seriesId = "S"),
                    task("S__today", toDo = today, state = todayState, seriesId = "S"),
                ),
                today,
                rangeDays = 7,
            )
            assertEquals(
                "today occurrence state=$todayState should hide the slip",
                emptyList<String>(),
                g.overdue.map { it.id },
            )
        }
    }

    @Test
    fun `FR-PLAN-5 slipped recurring visible when no today occurrence`() {
        val g = ActionLogic.planningGroups(
            listOf(
                task("S__slip", toDo = "2026-07-03", seriesId = "S"),
                task("S__tomorrow", toDo = "2026-07-06", seriesId = "S"),
            ),
            today,
            rangeDays = 7,
        )
        assertEquals(listOf("S__slip"), g.overdue.map { it.id })
    }

    @Test
    fun `FR-PLAN-5 one-off overdue tasks are never hidden`() {
        val g = ActionLogic.planningGroups(
            listOf(
                task("oneoff", toDo = "2026-07-03"), // seriesId == null
                task("unrelatedToday", toDo = today),
            ),
            today,
            rangeDays = 7,
        )
        assertEquals(listOf("oneoff"), g.overdue.map { it.id })
    }

    @Test
    fun `FR-PLAN-5 multiple slipped occurrences of one series all hidden`() {
        val g = ActionLogic.planningGroups(
            listOf(
                task("S__slip1", toDo = "2026-07-01", seriesId = "S"),
                task("S__slip2", toDo = "2026-07-03", seriesId = "S"),
                task("S__today", toDo = today, seriesId = "S"),
            ),
            today,
            rangeDays = 7,
        )
        assertEquals(emptyList<String>(), g.overdue.map { it.id })
    }

    @Test
    fun `FR-PLAN-5 independent series evaluated independently`() {
        // Series A has a today occurrence (its slip hides); series B does not (its slip stays).
        val g = ActionLogic.planningGroups(
            listOf(
                task("A__slip", toDo = "2026-07-03", seriesId = "A"),
                task("A__today", toDo = today, seriesId = "A"),
                task("B__slip", toDo = "2026-07-02", seriesId = "B"),
            ),
            today,
            rangeDays = 7,
        )
        assertEquals(listOf("B__slip"), g.overdue.map { it.id })
    }

    @Test
    fun `range presets - month runs to month end, custom to the picked date`() {
        assertEquals(7L, ActionLogic.rangeDays(RangePreset.DAYS_7, today, null))
        assertEquals(26L, ActionLogic.rangeDays(RangePreset.MONTH, today, null)) // Jul 5 → Jul 31
        assertEquals(70L, ActionLogic.rangeDays(RangePreset.CUSTOM, today, "2026-09-13"))
        assertEquals(7L, ActionLogic.rangeDays(RangePreset.CUSTOM, today, null)) // unset falls back
    }

    @Test
    fun `slipped days for the decide card`() {
        assertEquals(4L, ActionLogic.slippedDays(task("a", toDo = "2026-07-01"), today))
    }

    // ── FR-REC-3 effective horizon ────────────────────────────

    @Test
    fun `effective horizon never shrinks below 60 and extends with the range`() {
        assertEquals(60L, ActionLogic.effectiveHorizonDays(7))
        assertEquals(60L, ActionLogic.effectiveHorizonDays(30))
        assertEquals(90L, ActionLogic.effectiveHorizonDays(90)) // NFR-PERF-2
    }

    // ── FR-ALL-2 search ───────────────────────────────────────

    @Test
    fun `search matches title or description, case-insensitive, blank matches all`() {
        val t = Task(id = "a", title = "Water the plants", description = "The studio ferns")
        assertTrue(ActionLogic.matchesQuery(t, ""))
        assertTrue(ActionLogic.matchesQuery(t, "  "))
        assertTrue(ActionLogic.matchesQuery(t, "water"))
        assertTrue(ActionLogic.matchesQuery(t, "FERNS"))
        assertFalse(ActionLogic.matchesQuery(t, "kitchen"))
        assertFalse(ActionLogic.matchesQuery(Task(id = "b", title = "No description"), "ferns"))
    }

    // ── FR-ALL-4 horizon divider slot ─────────────────────────

    @Test
    fun `horizon divider sits after the last group inside the horizon`() {
        val horizon = "2026-09-03" // today + 60
        val keys = listOf("overdue", "today", "tomorrow", "2026-08-30", "2026-09-20")
        assertEquals(4, ActionLogic.horizonInsertIndex(keys, today, horizon))
        // everything inside → divider at the end of the dated groups
        assertEquals(3, ActionLogic.horizonInsertIndex(listOf("today", "tomorrow", "2026-07-20"), today, horizon))
        // far-future floor occurrence only (FR-REC-1) → divider before it
        assertEquals(0, ActionLogic.horizonInsertIndex(listOf("2027-01-05"), today, horizon))
    }
}
