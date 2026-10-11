package com.thefoxworks.tzafon

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2.13.0 — the three-pane editor frame: the tab row, the per-pane changed
 * dot, the discard confirm, Save jumping to the first problem, and the
 * draft, tab and snapshot surviving recreation. Replaces the FR-EDITOR-1
 * single-column order with the fields' places in the three panes.
 */
@RunWith(AndroidJUnit4::class)
class TaskEditorFrameTest {

    @get:Rule
    val rule = createComposeRule()

    private val task = TaskDraft(id = "T1", title = "Write the report", toDoDate = "2026-10-12")
    private val today = "2026-10-10"

    private var saves = 0
    private var closes = 0

    private fun editor(initial: TaskDraft? = task) {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = initial,
                    today = today,
                    onSave = { _, _ -> saves++ },
                    onDelete = { _, _ -> },
                    onClose = { closes++ },
                )
            }
        }
    }

    private fun tab(name: String) = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)
            .and(hasText(name)),
    )

    private val changed = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Changed")
    private val unchanged = SemanticsMatcher("no changed dot") {
        it.config.getOrElse(SemanticsProperties.StateDescription) { "" } != "Changed"
    }

    private fun titleField() = rule.onNode(hasSetTextAction().and(hasText(task.title)))

    @Test
    fun opensOnTask_withTitleNotesStatusAndDelete_andNoDots() {
        editor()
        tab("Task").assertIsSelected().assert(unchanged)
        tab("Timing").assertIsNotSelected().assert(unchanged)
        tab("Alignment").assertIsNotSelected().assert(unchanged)
        rule.onNodeWithText("TITLE").assertIsDisplayed()
        rule.onNodeWithText("NOTES · OPTIONAL").assertIsDisplayed()
        rule.onNodeWithText("STATUS").assertIsDisplayed()
        rule.onNodeWithText("Delete task").assertIsDisplayed()
        rule.onNodeWithText("EDIT TASK").assertIsDisplayed()
    }

    @Test
    fun newTask_isTitledNewTask_withNoStatusAndNoDelete() {
        editor(initial = null)
        rule.onNodeWithText("NEW TASK").assertIsDisplayed()
        tab("Task").assertIsSelected()
        rule.onNodeWithText("STATUS").assertDoesNotExist()
        rule.onNodeWithText("Delete task").assertDoesNotExist()
    }

    /**
     * FR-EDITOR-1, superseded: each field now lives in its pane. Inside
     * Alignment the cue now comes first, as on slide 3 (FR-EDITOR-6.1).
     */
    @Test
    fun tabsShowTheirOwnFields_andAlignmentPutsTheCueFirst() {
        editor()
        tab("Timing").performClick()
        tab("Timing").assertIsSelected()
        tab("Task").assertIsNotSelected()
        rule.onNodeWithText("DATES · ONE CALENDAR, TWO FIELDS").assertIsDisplayed()
        rule.onNodeWithText("REPEAT").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("TITLE").assertDoesNotExist()

        tab("Alignment").performClick()
        val cue = rule.onNodeWithText("CUE · WHEN X, I WILL DO Y").assertIsDisplayed().getUnclippedBoundsInRoot()
        val habit = rule.onNodeWithText("TICKS A HABIT · ONE").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("the cue sits above the habit", cue.top < habit.top)
        rule.onNodeWithText("DATES · ONE CALENDAR, TWO FIELDS").assertDoesNotExist()
    }

    @Test
    fun anEditLightsItsPanesDot_andUndoingClearsIt() {
        editor()
        titleField().performTextReplacement("Write the final report")
        tab("Task").assert(changed)
        tab("Timing").assert(unchanged)
        tab("Alignment").assert(unchanged)
        rule.onNode(hasSetTextAction().and(hasText("Write the final report"))).performTextReplacement(task.title)
        tab("Task").assert(unchanged)
    }

    @Test
    fun cancelWithNoChanges_closesAtOnce() {
        editor()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Discard your changes?").assertDoesNotExist()
        assertEquals(1, closes)
    }

    @Test
    fun cancelWithChanges_asksFirst_keepEditingKeepsTheDraft_discardCloses() {
        editor()
        titleField().performTextReplacement("Something else")
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Discard your changes?").assertIsDisplayed()
        assertEquals(0, closes)

        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithText("Discard your changes?").assertDoesNotExist()
        rule.onNode(hasSetTextAction().and(hasText("Something else"))).assertIsDisplayed()
        assertEquals(0, closes)

        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Discard").performClick()
        assertEquals(1, closes)
        assertEquals(0, saves)
    }

    @Test
    fun saveWithABlankTitle_opensTaskAndFocusesTheTitle_andSavesNothing() {
        editor(initial = null)
        tab("Timing").performClick()
        rule.onNodeWithText("Save").performClick()
        tab("Task").assertIsSelected()
        rule.onAllNodes(hasSetTextAction())[0].assertIsFocused()
        assertEquals(0, saves)
    }

    @Test
    fun saveWithAnEdit_saves() {
        editor()
        titleField().performTextReplacement("Write the final report")
        rule.onNodeWithText("Save").performClick()
        assertEquals(1, saves)
    }

    /** FR-EDITOR-1.11 — Delete has its own confirm, so a changed draft never asks "Discard your changes?". */
    @Test
    fun deleteAfterAnEdit_neverAsksToDiscard() {
        editor()
        titleField().performTextReplacement("Something else")
        rule.onNodeWithText("Delete task").performScrollTo().performClick()
        rule.onNodeWithText("Delete this task?").assertIsDisplayed()
        rule.onNodeWithText("Discard your changes?").assertDoesNotExist()
    }

    /** FR-STATUS-1.9 — a date picked after Reschedule lands only at Save; Cancel throws it away. */
    @Test
    fun aRescheduledDate_appliesOnlyAtSave_andCancelDiscardsIt() {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = task, today = today,
                    onSave = { _, _ -> saves++ }, onDelete = { _, _ -> },
                    onClose = { closes++ }, openOn = Pane.TIMING,
                )
            }
        }
        rule.onNode(hasContentDescription("Wednesday 14 October", substring = true)).performClick()
        assertEquals(0, saves)
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Discard").performClick()
        assertEquals(1, closes)
        assertEquals(0, saves)
    }

    @Test
    fun aRescheduledDate_savesTheNewToDoDate() {
        var saved: TaskDraft? = null
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = task, today = today,
                    onSave = { d, _ -> saved = d }, onDelete = { _, _ -> },
                    onClose = {}, openOn = Pane.TIMING,
                )
            }
        }
        rule.onNode(hasContentDescription("Wednesday 14 October", substring = true)).performClick()
        rule.onNodeWithText("Save").performClick()
        assertEquals("2026-10-14", saved?.toDoDate)
    }

    /** v2.13.0 — Save with nothing changed just closes the editor and writes nothing. */
    @Test
    fun saveWithNoChanges_closesWithoutWriting() {
        editor()
        rule.onNodeWithText("Save").performClick()
        assertEquals(0, saves)
        assertEquals(1, closes)
    }

    @Test
    fun theDraftTabAndSnapshotSurviveRecreation() {
        val restore = StateRestorationTester(rule)
        restore.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = task, today = today,
                    onSave = { _, _ -> saves++ }, onDelete = { _, _ -> },
                    onClose = { closes++ },
                )
            }
        }
        titleField().performTextReplacement("Write the final report")
        tab("Timing").performClick()

        restore.emulateSavedInstanceStateRestore()

        tab("Timing").assertIsSelected()
        tab("Task").assert(changed)
        tab("Task").performClick()
        rule.onNode(hasSetTextAction().and(hasText("Write the final report"))).assertIsDisplayed()
        // the snapshot survived too: undoing the edit still clears the dot
        rule.onNode(hasSetTextAction().and(hasText("Write the final report"))).performTextReplacement(task.title)
        tab("Task").assert(unchanged)
    }
}
