package com.thefoxworks.tzafon

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import com.thefoxworks.tzafon.ui.today.ReorderableTaskList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** FR-TODAY-2 — the long-press drag reorders rows and persists the result. */
@RunWith(AndroidJUnit4::class)
class ReorderTest {

    @get:Rule
    val rule = createComposeRule()

    private fun task(id: String, order: Long) = Task(
        id = id, title = id, toDoDate = "2026-07-06", sortOrder = order, createdAt = order,
    )

    @Test
    fun longPressDrag_swapsNeighbours_andPersistsOrder() {
        var persisted: List<String>? = null
        val tasks = listOf(task("alpha", 1), task("bravo", 2), task("charlie", 3))

        rule.setContent {
            TzafonTheme {
                ReorderableTaskList(
                    tasks = tasks,
                    today = "2026-07-06",
                    onToggle = {},
                    onOpen = {},
                    onPersist = { persisted = it },
                )
            }
        }

        val rowHeightPx = rule.onNodeWithText("bravo").fetchSemanticsNode().size.height.toFloat()

        // long-press "bravo", drag up one full row, release
        rule.onNodeWithText("bravo").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, -rowHeightPx * 0.4f))
            moveBy(Offset(0f, -rowHeightPx * 0.4f))
            moveBy(Offset(0f, -rowHeightPx * 0.4f))
            up()
        }
        rule.waitForIdle()

        assertEquals(listOf("bravo", "alpha", "charlie"), persisted)
    }
}
