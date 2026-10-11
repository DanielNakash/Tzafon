package com.thefoxworks.tzafon

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.ui.capture.QuickAddSheet
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-DESIGN-3 — while typing, a text input's line follows its own direction
 * across the whole input: a Hebrew line ends at the input's right edge, short
 * or longer than half a line, and an English one starts at the left. The
 * inner field must fill its decoration; a wrap-content field put RTL text
 * mid-line and pushed it right as it grew.
 */
@RunWith(AndroidJUnit4::class)
class TextInputDirectionTest {

    @get:Rule
    val rule = createComposeRule()

    private val shortHebrew = "שלום"
    private val longHebrew = "כתיבה של חמש מאות מילים לפרק"
    private val english = "Write the report"

    private fun editor() {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = TaskDraft(id = "T1", title = "x", description = "y", toDoDate = "2026-10-12"),
                    today = "2026-10-10",
                    onSave = { _, _ -> },
                    onDelete = { _, _ -> },
                    onClose = {},
                )
            }
        }
    }

    private fun quickAdd() {
        rule.setContent {
            TzafonTheme { QuickAddSheet(onSave = { _, _ -> }, onExpand = { _, _ -> }, onClose = {}) }
        }
    }

    /**
     * Types [text] into [field] and checks where its first line lands. [pad] is
     * the decoration's horizontal padding, so the field's own width should be
     * the input's width less twice that.
     */
    private fun assertLine(field: SemanticsNodeInteraction, text: String, pad: Dp, rtl: Boolean) {
        field.performTextReplacement(text)
        rule.waitForIdle()
        val node = field.fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val layout = layouts.first()
        val padPx = with(rule.density) { pad.toPx() }
        val content = node.size.width - 2 * padPx
        assertEquals("'$text': the field fills its input", content, layout.size.width.toFloat(), 2f)
        if (rtl) {
            assertEquals("'$text': the line ends at the right edge", content, layout.getLineRight(0), 2f)
        } else {
            assertEquals("'$text': the line starts at the left edge", 0f, layout.getLineLeft(0), 2f)
        }
    }

    @Test
    fun editorTitle_hebrewEndsRight_englishStartsLeft() {
        editor()
        val title = rule.onAllNodes(hasSetTextAction())[0]
        assertLine(title, shortHebrew, 14.dp, rtl = true)
        assertLine(title, longHebrew, 14.dp, rtl = true)
        assertLine(title, english, 14.dp, rtl = false)
    }

    @Test
    fun editorNotes_hebrewEndsRight_englishStartsLeft() {
        editor()
        val notes = rule.onAllNodes(hasSetTextAction())[1]
        assertLine(notes, shortHebrew, 14.dp, rtl = true)
        assertLine(notes, longHebrew, 14.dp, rtl = true)
        assertLine(notes, english, 14.dp, rtl = false)
    }

    @Test
    fun quickAdd_hebrewEndsRight_englishStartsLeft() {
        quickAdd()
        val input = rule.onAllNodes(hasSetTextAction())[0]
        assertLine(input, shortHebrew, 0.dp, rtl = true)
        assertLine(input, longHebrew, 0.dp, rtl = true)
        assertLine(input, english, 0.dp, rtl = false)
    }
}
