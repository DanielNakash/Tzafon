package com.thefoxworks.tzafon

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.components.HOLD_MILLIS
import com.thefoxworks.tzafon.ui.components.TaskCheckbox
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import com.thefoxworks.tzafon.ui.today.ReorderableTaskList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2.13.0 — the list-row checkbox: a tap under 350 ms, a hold at 350 ms,
 * moving off cancels, Also today's reorder never starts from it, and
 * TalkBack reaches the sheet without the gesture.
 */
@RunWith(AndroidJUnit4::class)
class CheckboxHoldTest {

    @get:Rule
    val rule = createComposeRule()

    private var taps = 0
    private var holds = 0

    private fun checkbox(state: TaskState = TaskState.OPEN) = rule.setContent {
        TzafonTheme {
            TaskCheckbox(state, onClick = { taps++ }, title = "Write 500 words", onHold = { holds++ })
        }
    }

    private val box get() = rule.onNodeWithContentDescription("Write 500 words")

    @Test
    fun aShortPress_isATap() {
        checkbox()
        box.performTouchInput {
            down(center)
            advanceEventTime(HOLD_MILLIS - 150)
            up()
        }
        rule.waitForIdle()
        assertEquals(1, taps)
        assertEquals(0, holds)
    }

    @Test
    fun holdingToTheThreshold_raisesTheSheet_andTheReleaseIsNoTap() {
        checkbox()
        box.performTouchInput {
            down(center)
            advanceEventTime(HOLD_MILLIS + 50)
            up()
        }
        rule.waitForIdle()
        assertEquals(0, taps)
        assertEquals(1, holds)
    }

    @Test
    fun movingPastTouchSlop_cancelsBoth() {
        checkbox()
        box.performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3))
            advanceEventTime(HOLD_MILLIS + 50)
            up()
        }
        rule.waitForIdle()
        assertEquals(0, taps)
        assertEquals(0, holds)
    }

    @Test
    fun alsoToday_holdingTheCheckbox_neverStartsAReorder() {
        var persisted: List<String>? = null
        var held: String? = null
        val tasks = listOf("alpha", "bravo", "charlie").mapIndexed { i, id ->
            Task(id = id, title = id, toDoDate = "2026-07-06", sortOrder = i.toLong(), createdAt = i.toLong())
        }
        rule.setContent {
            TzafonTheme {
                ReorderableTaskList(
                    tasks = tasks,
                    today = "2026-07-06",
                    onToggle = {},
                    onOpen = {},
                    onPersist = { persisted = it },
                    onHold = { held = it },
                )
            }
        }
        val rowHeightPx = rule.onNodeWithContentDescription("bravo").fetchSemanticsNode().size.height * 2f

        // the same long-press-drag ReorderTest uses, started on the checkbox
        rule.onNodeWithContentDescription("bravo").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, -rowHeightPx * 0.4f))
            moveBy(Offset(0f, -rowHeightPx * 0.4f))
            moveBy(Offset(0f, -rowHeightPx * 0.4f))
            up()
        }
        rule.waitForIdle()

        assertNull(persisted)
        assertEquals("bravo", held)
    }

    @Test
    fun talkBack_readsTitleAndStatus_andChangeStatusOpensTheSheet() {
        checkbox(TaskState.DONE)
        box.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Done"))
        box.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        box.assert(SemanticsMatcher("tap hint") { it.config[SemanticsActions.OnClick].label == "mark open" })

        val changeStatus = box.fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
        assertEquals("Change status", changeStatus.label)
        rule.runOnUiThread { changeStatus.action() }
        box.performSemanticsAction(SemanticsActions.OnLongClick)
        box.performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()

        assertEquals(2, holds)
        assertEquals(1, taps)
    }
}
