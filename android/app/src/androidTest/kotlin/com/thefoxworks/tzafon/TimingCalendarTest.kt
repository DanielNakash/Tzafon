package com.thefoxworks.tzafon

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule as RepeatRule
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2.13.0 — the shared calendar in the Timing pane: the armed tile, the
 * rails, tap-again-to-clear, the deadline window, the conflict fix, the
 * repeating guard, Reschedule opening with To do armed, and the dates Save
 * stores. Today is Saturday 2026-10-10 throughout.
 */
@RunWith(AndroidJUnit4::class)
class TimingCalendarTest {

    @get:Rule
    val rule = createComposeRule()

    private val today = "2026-10-10"
    private val task = TaskDraft(id = "T1", title = "Write the report", toDoDate = "2026-10-12")
    private var saved: TaskDraft? = null

    private fun editor(initial: TaskDraft = task, openOn: Pane = Pane.TIMING) {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = initial, today = today, openOn = openOn,
                    onSave = { d, _ -> saved = d }, onDelete = { _, _ -> }, onClose = {},
                )
            }
        }
    }

    private fun role(r: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, r)
    private fun tile(label: String) = rule.onNode(role(Role.RadioButton).and(hasText(label)))
    private fun chip(label: String) = rule.onNode(role(Role.Button).and(hasText(label))).performScrollTo()
    private fun day(name: String) = rule.onNode(hasContentDescription(name, substring = true)).performScrollTo()
    private fun tab(name: String) = rule.onNode(role(Role.Tab).and(hasText(name)))

    @Test
    fun toDoIsArmed_andAChipSetsIt() {
        editor()
        tile("TO-DO").assertIsSelected()
        tile("DEADLINE").assertIsNotSelected()
        rule.onNodeWithText("A TAP SETS TO-DO").assertIsDisplayed()
        chip("Tomorrow").performClick()
        tile("TO-DO").assert(hasText("Sun 11"))
        tab("Timing").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Changed"))
    }

    @Test
    fun aDayTapSetsTheToDoDate_andTappingItAgainClearsIt() {
        editor()
        day("Thursday 15 October").performClick()
        tile("TO-DO").assert(hasText("Thu 15"))
        day("Thursday 15 October, to-do date").performClick()
        tile("TO-DO").assert(hasText("Undated"))
    }

    @Test
    fun deadline_countsFromTheToDoDate_andNoneClearsIt() {
        editor()
        tile("DEADLINE").performClick()
        tile("DEADLINE").assertIsSelected()
        rule.onNodeWithText("A TAP SETS THE DEADLINE").assertIsDisplayed()
        chip("+1 week").performClick()
        tile("DEADLINE").assert(hasText("Mon 19"))
        day("Wednesday 14 October, in the window").assertIsDisplayed()
        chip("None").performClick()
        tile("DEADLINE").assert(hasText("None"))
    }

    @Test
    fun theArmedFieldAndMonth_surviveTabSwitchesAndRecreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent {
            TzafonTheme {
                TaskEditorScreen(initial = task, today = today, openOn = Pane.TIMING, onSave = { _, _ -> }, onDelete = { _, _ -> }, onClose = {})
            }
        }
        tile("DEADLINE").performClick()
        rule.onNode(hasContentDescription("Next month")).performClick()
        tab("Task").performClick()
        tab("Timing").performClick()
        tile("DEADLINE").assertIsSelected()
        rule.onNodeWithText("November 2026").assertIsDisplayed()

        restore.emulateSavedInstanceStateRestore()
        tile("DEADLINE").assertIsSelected()
        rule.onNodeWithText("November 2026").assertIsDisplayed()
    }

    @Test
    fun aDeadlineBeforeTheToDoDate_warns_andTheFixMovesTheToDoDate() {
        editor(task.copy(toDoDate = "2026-10-20", dueDate = "2026-10-16"))
        rule.onNodeWithText("You plan to work on this after it's due.").assertIsDisplayed()
        rule.onNodeWithText("Move the to-do date to Fri 16").performClick()
        rule.onNodeWithText("You plan to work on this after it's due.").assertDoesNotExist()
        tile("TO-DO").assert(hasText("Fri 16"))
    }

    @Test
    fun aRepeatingTask_cantGoUndated() {
        val rec = RecurrenceDraft(rule = RepeatRule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4)))
        editor(task.copy(toDoDate = "2026-10-13", recurrence = rec, seriesId = "S1", occurrenceDate = "2026-10-13"))
        chip("Undated").assertIsNotEnabled()
        chip("Tomorrow").assertIsEnabled()
        rule.onNodeWithText("A REPEATING TASK ALWAYS HAS A TO-DO DATE").assertIsDisplayed()
        day("Tuesday 13 October, to-do date").performClick()
        tile("TO-DO").assert(hasText("Tue 13"))
    }

    @Test
    fun rescheduleOpensTimingWithToDoArmed() {
        editor(openOn = Pane.TIMING)
        tab("Timing").assertIsSelected()
        tile("TO-DO").assertIsSelected()
        rule.onNodeWithText("October 2026").assertIsDisplayed()
    }

    @Test
    fun save_storesTheDatesThePaneShows() {
        editor(openOn = Pane.TASK)
        tab("Timing").performClick()
        day("Wednesday 14 October").performClick()
        tile("DEADLINE").performClick()
        chip("+2 weeks").performClick()
        rule.onNodeWithText("Save").performClick()
        assertEquals("2026-10-14", saved!!.toDoDate)
        assertEquals("2026-10-28", saved!!.dueDate)
        assertNull(saved!!.recurrence)
    }
}
