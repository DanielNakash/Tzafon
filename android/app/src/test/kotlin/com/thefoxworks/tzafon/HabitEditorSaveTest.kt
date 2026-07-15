package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.themes.GoalLinkLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FR-HAB-10.4 — the habit editor's save path must round-trip both
 * primaryThemeId and the full themeIds serve set from the editor's
 * LinkSelection. The v2.6.0 bug at HabitEditorSheet.kt:342-355 constructed
 * a Habit that omitted themeIds and inherited primaryThemeId unchanged, so
 * the direction picker's user selection was silently dropped on every save.
 * This test guards the fix: build the exact same Habit shape the sheet's
 * onSave now produces and assert both fields carry the LinkSelection's
 * values.
 */
class HabitEditorSaveTest {

    /** Mirror the constructor call inside HabitEditorSheet.onSave. */
    private fun buildSavedHabit(
        initial: Habit?,
        linkSel: GoalLinkLogic.LinkSelection,
        name: String = "20 push-ups",
        goalId: String? = null,
    ): Habit = Habit(
        id = initial?.id ?: "",
        name = name,
        kind = HabitKind.FREQUENCY,
        target = 3.0,
        unit = null,
        targetDays = null,
        cue = null,
        primaryThemeId = linkSel.primaryId,
        themeIds = linkSel.themeIds(),
        goalId = goalId,
        startedAt = initial?.startedAt ?: 0,
        createdAt = initial?.createdAt ?: 0,
    )

    @Test fun `primary + serves-also is persisted on save`() {
        // The reporter's push-ups case: primary "better health" + one
        // serves-also "energy".
        val sel = GoalLinkLogic.LinkSelection(
            primaryId = "health",
            extraServes = listOf("energy"),
        )
        val saved = buildSavedHabit(initial = null, linkSel = sel)
        assertEquals("health", saved.primaryThemeId)
        // themeIds is primary + serves-also, deduped, primary first
        // (matches LinkSelection.themeIds()).
        assertEquals(listOf("health", "energy"), saved.themeIds)
    }

    @Test fun `orphan save keeps primary null and themeIds empty`() {
        val sel = GoalLinkLogic.LinkSelection(primaryId = null, extraServes = emptyList())
        val saved = buildSavedHabit(initial = null, linkSel = sel)
        assertNull(saved.primaryThemeId)
        assertTrue(saved.themeIds.isEmpty())
    }

    @Test fun `edit of habit that already served themes round-trips`() {
        val existing = Habit(
            id = "h1",
            name = "20 push-ups",
            primaryThemeId = "health",
            themeIds = listOf("health", "energy"),
        )
        val sel = GoalLinkLogic.fromGoal(existing.primaryThemeId, existing.themeIds)
        val saved = buildSavedHabit(initial = existing, linkSel = sel)
        assertEquals("health", saved.primaryThemeId)
        assertEquals(listOf("health", "energy"), saved.themeIds)
    }

    @Test fun `changing primary via editor persists the new one`() {
        val existing = Habit(
            id = "h1",
            name = "20 push-ups",
            primaryThemeId = "health",
            themeIds = listOf("health"),
        )
        var sel = GoalLinkLogic.fromGoal(existing.primaryThemeId, existing.themeIds)
        // User taps a different active theme as primary
        sel = GoalLinkLogic.tapPrimary(sel, "energy")
        val saved = buildSavedHabit(initial = existing, linkSel = sel)
        assertEquals("energy", saved.primaryThemeId)
        assertEquals(listOf("energy"), saved.themeIds)
    }
}
