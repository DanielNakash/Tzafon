package com.thefoxworks.tzafon

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2.13.0 — the inline Alignment pane (FR-EDITOR-6): the cue edited in
 * place with its type, chips, × and time field; one habit as a radio list;
 * any goals as checkboxes; the inherited theme line; and Save stopping on a
 * half-typed time.
 */
@RunWith(AndroidJUnit4::class)
class AlignmentPaneTest {

    @get:Rule
    val rule = createComposeRule()

    private val today = "2026-10-10"
    private val task = TaskDraft(id = "T1", title = "Write the report", toDoDate = "2026-10-12")

    private val writing = Theme("TH1", "Finish the novel", "why", "2026-10-01", "2026-12-31")
    private val health = Theme("TH2", "Move every day", "why", "2026-10-01", "2026-12-31", accentSlot = 1)
    private val habits = listOf(
        Habit("H1", "Write in the morning", target = 7.0, createdAt = 1),
        Habit("H2", "Read", kind = HabitKind.QUANTITATIVE, target = 10.0, unit = "pages", createdAt = 2),
    )
    private val goals = listOf(
        Goal("G1", "Complete the first draft", type = GoalType.ACCUMULATIVE, targetQty = 80_000.0,
            currentQty = 62_000.0, unit = "words", primaryThemeId = writing.id),
        Goal("G2", "Run a half-marathon", type = GoalType.ACCUMULATIVE, targetQty = 21.0,
            currentQty = 8.0, unit = "km", primaryThemeId = health.id),
    )

    private var saved: TaskDraft? = null

    private fun editor(initial: TaskDraft = task, cueUses: List<Cue> = emptyList()) {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = initial,
                    today = today,
                    onSave = { d, _ -> saved = d },
                    onDelete = { _, _ -> },
                    onClose = {},
                    habits = habits,
                    goals = goals,
                    themes = listOf(writing, health),
                    cueUses = cueUses,
                )
            }
        }
        tab("Alignment").performClick()
    }

    private fun tab(name: String) = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)
            .and(hasText(name)),
    )

    private fun save() = rule.onNodeWithText("Save").performClick()
    private fun labelField() = rule.onNode(hasSetTextAction().and(hasContentDescription("Cue")))
    private fun timeField() = rule.onNode(hasSetTextAction().and(hasContentDescription("Cue time")))

    /** FR-EDITOR-6.1 (owner's ruling on #28): the cue first, as on slide 3, then the habit, the goals and the theme. */
    @Test
    fun sectionsRunCueHabitGoalsThenTheme() {
        editor()
        val cue = rule.onNodeWithText("CUE · WHEN X, I WILL DO Y").assertIsDisplayed().getUnclippedBoundsInRoot()
        val habit = rule.onNodeWithText("TICKS A HABIT · ONE").assertIsDisplayed().getUnclippedBoundsInRoot()
        val goal = rule.onNodeWithText("MOVES A GOAL · ANY").assertIsDisplayed().getUnclippedBoundsInRoot()
        val theme = rule.onNodeWithContentDescription("No theme yet — it comes from a goal").getUnclippedBoundsInRoot()
        assertTrue(cue.top < habit.top && habit.top < goal.top && goal.top < theme.top)
    }

    @Test
    fun pickACueTypeAndAChip_savesThatCue() {
        editor(cueUses = listOf(Cue(CueType.AT_PLACE, "At the studio")))
        rule.onNodeWithText("At a place").performClick()
        rule.onNodeWithText("At the studio").performClick()
        labelField().assert(hasText("At the studio"))
        timeField().assertDoesNotExist()
        save()
        assertEquals(Cue(CueType.AT_PLACE, "At the studio"), saved?.cue)
    }

    @Test
    fun theClearButton_removesTheCue() {
        editor(initial = task.copy(cue = Cue(CueType.AFTER_ROUTINE, "After the first coffee")))
        labelField().assert(hasText("After the first coffee"))
        rule.onNodeWithContentDescription("Clear cue").performClick()
        labelField().assert(!hasText("After the first coffee"))
        save()
        assertNull(saved!!.cue)
    }

    @Test
    fun anAfterARoutineCue_takesAnOptionalTime() {
        editor()
        labelField().performTextReplacement("After lunch")
        timeField().performTextReplacement("13:15")
        save()
        assertEquals(Cue(CueType.AFTER_ROUTINE, "After lunch", "13:15"), saved?.cue)
    }

    /** FR-EDITOR-6.5 — Save switches to Alignment, focuses the time field and marks it incomplete. */
    @Test
    fun saveWithAHalfTypedTime_jumpsToTheFieldAndSavesNothing() {
        editor()
        rule.onNodeWithText("At a time").performClick()
        timeField().performTextReplacement("7:3")
        tab("Task").performClick()
        save()
        tab("Alignment").assertIsSelected()
        timeField().assertIsFocused()
        rule.onNodeWithText("Use HH:mm, like 07:30").assertIsDisplayed()
        assertNull(saved)
    }

    /** FR-EDITOR-6.6 — one habit; tapping the selected one deselects it. */
    @Test
    fun aHabitSelectsAndDeselects() {
        editor()
        val read = rule.onNode(hasText("Read").and(hasText("10 PAGES / DAY")))
        rule.onNodeWithText("DAILY").assertIsDisplayed()
        read.performClick()
        read.assertIsSelected()
        tab("Alignment").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Changed"))
        read.performClick()
        read.assertIsNotSelected()
        save()
        assertNull(saved) // nothing changed: Save closes without writing
    }

    /** FR-EDITOR-6.7 / 6.8 — any goals; the theme line follows them. */
    @Test
    fun tickingTwoGoals_updatesTheThemeLine_andSaves() {
        editor()
        val draft = rule.onNode(hasText("Complete the first draft").and(hasText("62,000 / 80,000 WORDS")))
        val run = rule.onNode(hasText("Run a half-marathon").and(hasText("8 / 21 KM")))
        draft.assertIsOff().performClick()
        draft.assertIsOn()
        rule.onNodeWithContentDescription("Theme Finish the novel — inherited from the goal").assertExists()
        run.performClick()
        run.assertIsOn()
        rule.onNodeWithContentDescription("Themes Finish the novel · Move every day — inherited from the goals").assertExists()
        rule.onNode(hasText("Write in the morning")).performClick()
        save()
        assertEquals(listOf("G1", "G2"), saved?.goalIds)
        assertEquals("H1", saved?.habitId)
    }
}
