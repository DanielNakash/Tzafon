package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.InheritedTheme
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.Theme
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v2.13.0 — the Alignment pane's read-only theme line: a task has no theme of
 * its own, it inherits the primary themes of the goals it moves and the habit
 * it ticks. `Task.themeId` plays no part.
 */
class InheritedThemeTest {

    private fun theme(id: String, name: String) = Theme(id, name, why = "why", windowStart = "2026-10-01", windowEnd = "2026-12-31")
    private val novel = theme("t1", "Finish the novel")
    private val health = theme("t2", "Move more")
    private val themes = listOf(novel, health)

    private val draftGoal = Goal("g1", "Complete the first draft", primaryThemeId = "t1", themeIds = listOf("t1", "t2"))
    private val editGoal = Goal("g2", "Edit chapter one", primaryThemeId = "t1")
    private val runGoal = Goal("g3", "Run a half-marathon", primaryThemeId = "t2")
    private val bareGoal = Goal("g4", "Be kinder")
    private val goals = listOf(draftGoal, editGoal, runGoal, bareGoal)
    private val writing = Habit("h1", "Write in the morning", primaryThemeId = "t1")
    private val walking = Habit("h2", "Walk", primaryThemeId = "t2")
    private val habits = listOf(writing, walking)

    private fun line(goalIds: List<String> = emptyList(), habitId: String? = null, themeId: String? = null) =
        InheritedTheme.of(TaskDraft(id = "T", title = "T", goalIds = goalIds, habitId = habitId, themeId = themeId), goals, habits, themes)

    @Test
    fun `one goal's primary theme, inherited from the goal`() {
        val l = line(goalIds = listOf("g1"))
        assertEquals(listOf("Finish the novel"), l.names)
        assertEquals("Theme Finish the novel — inherited from the goal", l.sentence)
    }

    @Test
    fun `the habit's theme, inherited from the habit`() {
        assertEquals("Theme Move more — inherited from the habit", line(habitId = "h2").sentence)
    }

    @Test
    fun `several goals sharing a theme name it once`() {
        assertEquals("Theme Finish the novel — inherited from the goals", line(goalIds = listOf("g1", "g2")).sentence)
    }

    @Test
    fun `several themes are listed goals first, then the habit`() {
        assertEquals("Themes Finish the novel · Move more — inherited from the goals", line(goalIds = listOf("g1", "g3")).sentence)
        val mixed = line(goalIds = listOf("g3"), habitId = "h1")
        assertEquals(listOf("Move more", "Finish the novel"), mixed.names)
        assertEquals("Themes Move more · Finish the novel — inherited from the goal and the habit", mixed.sentence)
    }

    @Test
    fun `only links that bring a theme are named as its source`() {
        assertEquals("Theme Move more — inherited from the habit", line(goalIds = listOf("g4"), habitId = "h2").sentence)
        assertEquals("Theme Finish the novel — inherited from the goals and the habit", line(goalIds = listOf("g1", "g2"), habitId = "h1").sentence)
    }

    @Test
    fun `nothing linked, or nothing with a theme, says where a theme comes from`() {
        assertEquals(emptyList<String>(), line().names)
        assertEquals("No theme yet — it comes from a goal", line().sentence)
        assertEquals("No theme yet — it comes from a goal", line(goalIds = listOf("g4")).sentence)
    }

    @Test
    fun `the task's own theme id is ignored`() {
        assertEquals("No theme yet — it comes from a goal", line(themeId = "t2").sentence)
        assertEquals("Theme Finish the novel — inherited from the goal", line(goalIds = listOf("g1"), themeId = "t2").sentence)
    }

    @Test
    fun `links to a deleted goal, habit or theme are skipped`() {
        val orphan = Goal("g9", "Orphan", primaryThemeId = "gone")
        val l = InheritedTheme.of(
            TaskDraft(id = "T", title = "T", goalIds = listOf("missing", "g9"), habitId = "missing"),
            goals + orphan, habits, themes,
        )
        assertEquals("No theme yet — it comes from a goal", l.sentence)
    }
}
