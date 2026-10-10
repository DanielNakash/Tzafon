package com.thefoxworks.tzafon

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.data.db.TzafonDatabase
import com.thefoxworks.tzafon.data.repo.RoomTaskRepository
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule as RepeatRule
import com.thefoxworks.tzafon.domain.status.StatusChanges
import com.thefoxworks.tzafon.domain.status.StatusSubject
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.status.LocalStatusHost
import com.thefoxworks.tzafon.ui.status.StatusHost
import com.thefoxworks.tzafon.ui.status.StatusToastHost
import com.thefoxworks.tzafon.ui.status.TaskStatusControls
import com.thefoxworks.tzafon.ui.status.TaskStatusSheet
import com.thefoxworks.tzafon.ui.status.TaskStatusSheets
import com.thefoxworks.tzafon.ui.status.rememberTaskStatusControls
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2.13.0 — the status sheet: from a list, a pick that reaches a series opens
 * its scope panel and Back returns to the list; a one-off pick applies at once
 * with its toast; from the editor, a chip that reaches the series opens the
 * panel directly and Back leaves the draft alone; Reschedule opens the editor
 * on Timing with the to-do date armed.
 */
@RunWith(AndroidJUnit4::class)
class StatusSheetFlowTest {

    @get:Rule
    val rule = createComposeRule()

    private val today = "2026-10-10"
    private val live = SeriesStatus(frozen = false, ended = false)
    private val repeating = StatusSubject(
        title = "Write 500 words",
        state = TaskState.OPEN,
        toDoDate = "2026-10-15",
        repeat = "Every Tuesday & Thursday",
        series = live,
    )

    private var db: TzafonDatabase? = null

    @After
    fun closeDb() {
        db?.close()
    }

    private fun row(status: String) = rule.onNode(hasContentDescription("$status,", substring = true))

    @Test
    fun aPickThatReachesTheSeries_opensItsScopePanel_andBackReturnsToTheList() {
        val applied = mutableListOf<Pair<TaskState, StatusScope>>()
        rule.setContent {
            TzafonTheme {
                TaskStatusSheet(subject = repeating, onApply = { t, s -> applied += t to s }, onClose = {}, onReschedule = {}, onOpenEditor = {})
            }
        }
        rule.onNodeWithText("SET STATUS").assertIsDisplayed()
        row("Backlog").assertIsNotEnabled()
        row("Closed").performClick()

        rule.onNodeWithText("CLOSE WHAT?").assertIsDisplayed()
        rule.onNode(radio("Just this one")).assertIsSelected()
        rule.onNodeWithText("Close this one").assertIsDisplayed()
        rule.onNodeWithText("Reschedule…").assertDoesNotExist()
        rule.onNode(radio("The whole series")).performClick()
        rule.onNodeWithText("Close series").assertIsDisplayed()

        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("SET STATUS").assertIsDisplayed()
        rule.onNodeWithText("Reschedule…").assertIsDisplayed()
        assertTrue(applied.isEmpty())

        // the panel's button is what applies
        row("Closed").performClick()
        rule.onNodeWithText("Close this one").performClick()
        assertEquals(listOf(TaskState.CLOSED to StatusScope.THIS_ONE), applied)
    }

    @Test
    fun aOneOffPick_appliesAtOnce_withItsUndoToast() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TzafonDatabase::class.java).build()
        db = database
        val repo = RoomTaskRepository(database.taskDao(), database.seriesDao())
        val task: Task = runBlocking {
            repo.saveDraft(TaskDraft(id = null, title = "Call the bank", toDoDate = today), EditScope.ONE, today)
            repo.observeTasks().first().single()
        }
        val host = StatusHost(
            StatusChanges(tasks = repo, habits = null, goals = null, today = { today }),
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
        lateinit var controls: TaskStatusControls
        rule.setContent {
            CompositionLocalProvider(LocalStatusHost provides host) {
                TzafonTheme {
                    controls = rememberTaskStatusControls()
                    TaskStatusSheets(controls)
                    StatusToastHost(host, aboveNav = 0.dp)
                }
            }
        }
        rule.runOnIdle { controls.hold(task) }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("SET STATUS")).fetchSemanticsNodes().isNotEmpty() }
        row("Done").performClick()

        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Done · Call the bank")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("SET STATUS").assertDoesNotExist()
        rule.onNodeWithText("Undo").assertIsDisplayed()
        assertEquals(TaskState.DONE, runBlocking { repo.getTask(task.id)!!.state })
    }

    @Test
    fun anEditorChipThatReachesTheSeries_opensThePanelDirectly_andBackKeepsTheDraft() {
        val occurrence = TaskDraft(
            id = "S1__2026-10-15", title = "Write 500 words", toDoDate = "2026-10-15",
            seriesId = "S1", occurrenceDate = "2026-10-15",
            recurrence = RecurrenceDraft(rule = RepeatRule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4))),
        )
        val seriesActs = mutableListOf<TaskState>()
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = occurrence, today = today,
                    onSave = { _, _ -> }, onDelete = { _, _ -> }, onClose = {},
                    series = live,
                    onSeriesStatus = { _, target, _ -> seriesActs += target },
                )
            }
        }
        rule.onNode(radio("FROZEN")).performClick()
        rule.onNodeWithText("FREEZE THE SERIES").assertIsDisplayed()
        rule.onNodeWithText("Freeze series").assertIsDisplayed()
        rule.onNodeWithText("SET STATUS").assertDoesNotExist()
        rule.onNodeWithText("Open editor").assertDoesNotExist()

        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("FREEZE THE SERIES").assertDoesNotExist()
        rule.onNode(radio("OPEN")).assertIsSelected()
        assertTrue(seriesActs.isEmpty())

        // a whole-series choice with no draft changes acts at once
        rule.onNode(radio("FROZEN")).performClick()
        rule.onNodeWithText("Freeze series").performClick()
        assertEquals(listOf(TaskState.FROZEN), seriesActs)
    }

    @Test
    fun theSheetsReschedule_asksForTheEditor() {
        val opened = mutableListOf<String>()
        rule.setContent {
            TzafonTheme {
                TaskStatusSheet(subject = repeating, onApply = { _, _ -> }, onClose = {}, onReschedule = { opened += "reschedule" }, onOpenEditor = { opened += "editor" })
            }
        }
        rule.onNodeWithText("Reschedule…").performClick()
        assertEquals(listOf("reschedule"), opened)
    }

    @Test
    fun rescheduleOpensTheEditorOnTiming_withTheToDoDateArmed() {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = TaskDraft(id = "T1", title = "Call the bank", toDoDate = "2026-10-12"), today = today,
                    onSave = { _, _ -> }, onDelete = { _, _ -> }, onClose = {}, openOn = Pane.TIMING,
                )
            }
        }
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab).and(hasText("Timing")),
        ).assertIsSelected()
        // the shared calendar, with To do armed
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton).and(hasText("TO-DO")),
        ).assertIsSelected()
    }

    private fun radio(label: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton).and(hasText(label, substring = true))
}
