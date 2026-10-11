package com.thefoxworks.tzafon

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.StatusSubject
import com.thefoxworks.tzafon.ui.status.TaskStatusSheet
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-DESIGN-3 — the status sheet's header title follows its own direction:
 * the title fills the header's column, so a Hebrew title aligns right (as on
 * Today's rows) and an English one stays left. A wrap-content title would sit
 * at the start whatever its direction.
 */
@RunWith(AndroidJUnit4::class)
class StatusSheetHeaderDirectionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(title: String) {
        rule.setContent {
            TzafonTheme {
                TaskStatusSheet(
                    subject = StatusSubject(title = title, state = TaskState.OPEN, toDoDate = null),
                    onApply = { _, _ -> },
                    onClose = {},
                )
            }
        }
    }

    /** The title's laid-out first line, as (lineLeft, lineRight, boxWidth). */
    private fun titleLine(title: String): Triple<Float, Float, Float> {
        val node = rule.onNodeWithText(title).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val layout = layouts.first()
        return Triple(layout.getLineLeft(0), layout.getLineRight(0), node.size.width.toFloat())
    }

    @Test
    fun hebrewTitle_alignsToTheRightOfTheHeader() {
        val title = "טקסט בעברית 123"
        show(title)
        val (left, right, width) = titleLine(title)
        assertEquals("the Hebrew line ends at the box's right edge", width, right, 2f)
        assertTrue("the box is wider than the text, so the line starts well in from the left (left=$left)", left > 40f)
    }

    @Test
    fun englishTitle_staysLeft() {
        val title = "Write 500 words"
        show(title)
        val (left, right, width) = titleLine(title)
        assertEquals("the English line starts at the box's left edge", 0f, left, 2f)
        assertTrue("the box fills the column, past the text's end (right=$right, width=$width)", width - right > 40f)
    }
}
