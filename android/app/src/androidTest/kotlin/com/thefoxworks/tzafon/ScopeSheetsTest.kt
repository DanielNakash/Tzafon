package com.thefoxworks.tzafon

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule as JUnitRule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2.13.0 — scope asked at save, and the delete sheets: "This task repeats"
 * opens only for a repeating task's field changes, its radio rows read their
 * consequence, Keep editing keeps the draft; Delete asks a scope on a
 * repeating task and a plain confirmation on a one-off.
 */
@RunWith(AndroidJUnit4::class)
class ScopeSheetsTest {

    @get:JUnitRule
    val rule = createComposeRule()

    private val occurrence = TaskDraft(
        id = "S1__2026-09-17", title = "Water plants", toDoDate = "2026-09-17",
        seriesId = "S1", occurrenceDate = "2026-09-17",
        recurrence = RecurrenceDraft(rule = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4))),
    )
    private val oneOff = TaskDraft(id = "T1", title = "Write the report", toDoDate = "2026-10-12")

    private var saved: Pair<TaskDraft, EditScope>? = null
    private var deleted: Pair<String, EditScope>? = null
    private var counted: Boolean? = null

    private fun editor(initial: TaskDraft) {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = initial,
                    today = "2026-09-16",
                    onSave = { d, s -> saved = d to s },
                    onDelete = { id, s -> deleted = id to s },
                    onSetState = { _, _ -> },
                    onClose = { },
                    countEarlier = { _, notDoneOnly -> counted = notDoneOnly; if (notDoneOnly) 4 else 26 },
                )
            }
        }
    }

    private fun radio(label: String) = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton).and(hasText(label, substring = true)),
    )

    private fun retitle(from: String, to: String) =
        rule.onNode(hasSetTextAction().and(hasText(from))).performTextReplacement(to)

    @Test
    fun aRepeatingEdit_asksAtSave_withWhatChangedAndJustThisOneChosen() {
        editor(occurrence)
        retitle("Water plants", "Water the plants")
        rule.onNodeWithText("Save").performClick()

        rule.onNodeWithText("This task repeats").assertIsDisplayed()
        rule.onNodeWithText("YOU CHANGED").assertIsDisplayed()
        rule.onNodeWithText("Title changed").assertIsDisplayed()
        radio("Just this one").assertIsSelected()
        radio("This and every future one").assertIsNotSelected()
        // the whole-series badge counts every earlier occurrence, Done included
        radio("The whole series").assertIsNotSelected()
        rule.onNodeWithText("26").assertIsDisplayed()
        assertEquals(false, counted)
        // TalkBack reads each row's consequence with its label
        radio("Only Thu 17 Sep changes; the series stays as it is.").assertIsSelected()
        assertNull(saved)
    }

    @Test
    fun saveChange_savesUnderTheChosenScope() {
        editor(occurrence)
        retitle("Water plants", "Water the plants")
        rule.onNodeWithText("Save").performClick()
        radio("The whole series").performClick()
        radio("The whole series").assertIsSelected()
        rule.onNodeWithText("Save change").performClick()

        assertEquals(EditScope.ALL, saved!!.second)
        assertEquals("Water the plants", saved!!.first.title)
    }

    @Test
    fun keepEditing_keepsTheDraftAndSavesNothing() {
        editor(occurrence)
        retitle("Water plants", "Water the plants")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Keep editing").performClick()

        rule.onNodeWithText("This task repeats").assertDoesNotExist()
        rule.onNode(hasSetTextAction().and(hasText("Water the plants"))).assertIsDisplayed()
        assertNull(saved)
    }

    @Test
    fun turningRepeatOff_leavesOnlyThisAndEveryFutureOne() {
        editor(occurrence)
        rule.onNodeWithText("Timing").performClick()
        rule.onNodeWithText("Repeat this task").performClick()
        rule.onNodeWithText("Save").performClick()

        radio("Just this one").assertIsNotEnabled()
        radio("Repeat changes reach the series.").assertIsNotEnabled()
        radio("This and every future one").assertIsSelected()
        radio("The whole series").assertIsNotEnabled()
        // a disabled row can't be chosen
        radio("Just this one").performClick()
        radio("This and every future one").assertIsSelected()
        rule.onNodeWithText("Save change").performClick()
        assertEquals(EditScope.FORWARD, saved!!.second)
    }

    @Test
    fun aOneOffEdit_savesWithoutAsking() {
        editor(oneOff)
        retitle("Write the report", "Write the final report")
        rule.onNodeWithText("Save").performClick()

        rule.onNodeWithText("This task repeats").assertDoesNotExist()
        assertEquals("Write the final report", saved!!.first.title)
    }

    @Test
    fun deletingARepeatingTask_asksTheScope_andDeletesUnderTheChoice() {
        editor(occurrence)
        rule.onNodeWithText("Delete task").performClick()

        rule.onNodeWithText("Delete a repeating task").assertIsDisplayed()
        rule.onNodeWithText("YOU CHANGED").assertDoesNotExist()
        radio("Just this one").assertIsSelected()
        radio("Removes Thu 17 Sep; it won't come back.").assertIsSelected()
        // the whole-series badge counts earlier occurrences not Done
        rule.onNodeWithText("4").assertIsDisplayed()
        assertEquals(true, counted)

        radio("This and every future one").performClick()
        rule.onNodeWithText("Delete").performClick()
        assertEquals("S1__2026-09-17" to EditScope.FORWARD, deleted)
    }

    @Test
    fun keep_closesTheDeleteSheetAndDeletesNothing() {
        editor(occurrence)
        rule.onNodeWithText("Delete task").performClick()
        rule.onNodeWithText("Keep").performClick()

        rule.onNodeWithText("Delete a repeating task").assertDoesNotExist()
        assertNull(deleted)
    }

    @Test
    fun deletingAOneOff_asksToConfirm_withNoOptions() {
        editor(oneOff)
        rule.onNodeWithText("Delete task").performClick()

        rule.onNodeWithText("Delete this task?").assertIsDisplayed()
        radio("Just this one").assertDoesNotExist()
        assertNull(deleted)
        rule.onNodeWithText("Delete").performClick()
        assertEquals("T1" to EditScope.ONE, deleted)
    }
}
