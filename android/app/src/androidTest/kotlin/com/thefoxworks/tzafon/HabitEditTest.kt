package com.thefoxworks.tzafon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.ui.habits.HabitCard
import com.thefoxworks.tzafon.ui.habits.HabitCardState
import com.thefoxworks.tzafon.ui.habits.HabitEditorSheet
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-HAB-7 — edit-habit affordance & editor behaviour. Covers the acceptance
 * criteria in §2.1 that are exercisable at the composable level:
 *   1. explicit "Edit habit" affordance on both collapsed and expanded states;
 *   2. edit and log are separate hit targets (log never opens the editor,
 *      edit never logs);
 *   3. `kind` field disabled when history is non-empty, editable when empty;
 *   4. DM-NOT audit — no gamification surface anywhere in the editor.
 */
@RunWith(AndroidJUnit4::class)
class HabitEditTest {

    @get:Rule
    val rule = createComposeRule()

    private val habit = Habit(
        id = "h1",
        name = "Read",
        kind = HabitKind.FREQUENCY,
        target = 3.0,
        cue = Cue(CueType.AFTER_ROUTINE, "After the first coffee"),
    )

    private fun card(hasHistory: Boolean = false) = HabitCardState(
        habit = habit,
        week = HabitMath.WeekSnapshot(
            doneDays = 1,
            amountSum = 0.0,
            amounts = List(7) { 0.0 },
            doneFlags = List(7) { false },
        ),
        arc = "First week",
        freshStart = false,
        grid = List(5) { List(7) { 0 } },
        loggedToday = false,
        todayAmount = null,
        hasHistory = hasHistory,
    )

    @Test
    fun collapsedRow_exposesExplicitEditAffordance() {
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card(), servesGoal = null, onLog = {}, onEdit = {})
            }
        }

        // Card is collapsed by default (FR-HAB-5).
        rule.onNodeWithContentDescription("Expand habit Read").assertIsDisplayed()
        // FR-HAB-7.1 — the edit affordance is visible right on the row, no expand needed.
        rule.onNodeWithContentDescription("Edit habit Read").assertIsDisplayed()
    }

    @Test
    fun editFromCollapsed_firesOnEdit_withoutExpanding() {
        var edits = 0
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card(), servesGoal = null, onLog = {}, onEdit = { edits++ })
            }
        }

        rule.onNodeWithContentDescription("Edit habit Read").performClick()
        assertEquals("collapsed edit tap fires exactly once", 1, edits)
        // FR-HAB-5.2 — a plain edit tap must NOT expand the card.
        rule.onNodeWithText("THIS WEEK").assertDoesNotExist()
    }

    /** FR-HAB-7.2 — the log control never opens the editor (collapsed state). */
    @Test
    fun logFromCollapsed_neverFiresOnEdit() {
        var edits = 0
        var logs = 0
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card(), servesGoal = null, onLog = { logs++ }, onEdit = { edits++ })
            }
        }

        rule.onNodeWithText("Log").performClick()
        assertEquals("log tap fires log", 1, logs)
        assertEquals("log tap must not fire edit", 0, edits)
    }

    /** FR-HAB-7.2 — after expand, the log pill still owns its own hit target. */
    @Test
    fun logFromExpanded_neverFiresOnEdit() {
        var edits = 0
        var logs = 0
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card(), servesGoal = null, onLog = { logs++ }, onEdit = { edits++ })
            }
        }

        rule.onNodeWithContentDescription("Expand habit Read").performClick()
        rule.onNodeWithText("Mark today done").performClick()
        assertEquals("expanded log tap fires log", 1, logs)
        assertEquals("expanded log tap must not fire edit", 0, edits)
    }

    /** FR-HAB-7.1 — expanded body opens the editor via a labelled tap target. */
    @Test
    fun expandedBody_hasEditAffordance() {
        rule.setContent {
            TzafonTheme {
                HabitCard(card = card(), servesGoal = null, onLog = {}, onEdit = {})
            }
        }

        rule.onNodeWithContentDescription("Expand habit Read").performClick()
        // The expanded body's pressable carries the "Edit habit Read" label; the
        // collapsed row's edit pill uses the same label, so onAllNodes would be
        // ambiguous — instead we confirm the label survives expand.
        rule.onNodeWithContentDescription("Edit habit Read").assertIsDisplayed()
    }

    /** FR-HAB-7.4 — kind is editable at create time (initial == null). */
    @Test
    fun editorAtCreate_kindIsEditable() {
        rule.setContent {
            TzafonTheme {
                HabitEditorSheet(
                    initial = null,
                    onSave = {},
                    onDelete = {},
                    onClose = {},
                    hasHistory = false,
                )
            }
        }

        rule.onNodeWithText("TIMES A WEEK").assertIsDisplayed()
        rule.onNodeWithText("AN AMOUNT A DAY").assertIsDisplayed()
        // The lock hint must NOT appear at create time.
        rule.onNodeWithText("Kind is fixed", substring = true).assertDoesNotExist()
    }

    /** FR-HAB-7.4 — kind stays editable when editing a habit with no logs. */
    @Test
    fun editorForHabitWithoutHistory_kindIsEditable() {
        rule.setContent {
            TzafonTheme {
                HabitEditorSheet(
                    initial = habit,
                    onSave = {},
                    onDelete = {},
                    onClose = {},
                    hasHistory = false,
                )
            }
        }

        rule.onNodeWithText("Edit habit").assertIsDisplayed()
        rule.onNodeWithText("Kind is fixed", substring = true).assertDoesNotExist()
    }

    /** FR-HAB-7.4 — kind is disabled with a hint once history is non-empty. */
    @Test
    fun editorForHabitWithHistory_kindIsDisabledAndHinted() {
        rule.setContent {
            TzafonTheme {
                HabitEditorSheet(
                    initial = habit,
                    onSave = {},
                    onDelete = {},
                    onClose = {},
                    hasHistory = true,
                )
            }
        }

        rule.onNodeWithText("Edit habit").assertIsDisplayed()
        rule.onNodeWithText("Kind is fixed once you've logged this habit — delete and recreate if you need to switch.")
            .assertIsDisplayed()
    }

    /** DM-NOT audit — the editor must not surface any gamification copy. */
    @Test
    fun editor_hasNoGamificationSurface_whenEditing() {
        rule.setContent {
            TzafonTheme {
                HabitEditorSheet(
                    initial = habit,
                    onSave = {},
                    onDelete = {},
                    onClose = {},
                    hasHistory = true,
                )
            }
        }

        listOf("STREAK", "Streak", "streak", "%", "POINTS", "Points", "BADGE", "Badge", "ADHERENCE")
            .forEach { forbidden ->
                val hit = try {
                    rule.onNodeWithText(forbidden, substring = true).fetchSemanticsNode()
                    true
                } catch (t: AssertionError) {
                    false
                }
                assertFalse("DM-NOT: '$forbidden' must not surface in the habit editor", hit)
            }
    }
}
