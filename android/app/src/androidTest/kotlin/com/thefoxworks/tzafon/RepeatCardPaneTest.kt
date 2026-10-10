package com.thefoxworks.tzafon

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.SeriesSpec
import com.thefoxworks.tzafon.domain.recurrence.Rule as RepeatRule
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2.13.0 — the repeat card in the Timing pane: turning repeat on, the three
 * types and their bodies, Ends through the calendar's End mode, the Deadline
 * body and its offset, the fold with its callout, Switch to offset on an old
 * deadline rule, and the series Save stores. Today is Saturday 2026-10-10.
 */
@RunWith(AndroidJUnit4::class)
class RepeatCardPaneTest {

    @get:Rule
    val rule = createComposeRule()

    private val today = "2026-10-10"
    private val tueThu = RepeatRule(pattern = Pattern.WEEKDAY, weekdays = listOf(2, 4))
    /** an existing occurrence of a Tue & Thu series, on Thu 15 Oct (the month's 3rd Thursday) */
    private val occurrence = TaskDraft(
        id = "S1__2026-10-15", title = "Water plants", toDoDate = "2026-10-15",
        seriesId = "S1", occurrenceDate = "2026-10-15", recurrence = RecurrenceDraft(rule = tueThu),
    )
    private var saved: Pair<TaskDraft, EditScope>? = null

    private fun editor(initial: TaskDraft, series: SeriesSpec? = null) {
        rule.setContent {
            TzafonTheme {
                TaskEditorScreen(
                    initial = initial, today = today, openOn = Pane.TIMING, seriesSpec = series,
                    onSave = { d, s -> saved = d to s }, onDelete = { _, _ -> }, onClose = {},
                )
            }
        }
    }

    private fun role(r: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, r)
    private fun toggle() = rule.onNode(role(Role.Switch)).performScrollTo()
    private fun segment(label: String) = rule.onNode(role(Role.Tab).and(hasText(label))).performScrollTo()
    private fun weekday(name: String) = rule.onNode(role(Role.Checkbox).and(hasContentDescription(name))).performScrollTo()
    private fun button(label: String) = rule.onNode(hasContentDescription(label)).performScrollTo()
    private fun text(t: String, substring: Boolean = false) = rule.onNode(hasText(t, substring = substring)).performScrollTo()
    private fun tile(label: String) = rule.onNode(role(Role.RadioButton).and(hasText(label)))
    private fun chip(label: String) = rule.onNode(role(Role.Button).and(hasText(label))).performScrollTo()
    private fun day(name: String) = rule.onNode(hasContentDescription(name, substring = true)).performScrollTo()

    /** Save on an existing repeating task asks a scope; the repeat card's changes take This and every future one. */
    private fun saveSeries(): TaskDraft {
        rule.onNodeWithText("Save").performClick()
        rule.onNode(role(Role.RadioButton).and(hasText("Just this one", substring = true))).assertIsNotEnabled()
        rule.onNodeWithText("Save change").performClick()
        assertEquals(EditScope.FORWARD, saved!!.second)
        return saved!!.first
    }

    @Test
    fun repeatOn_datesAnUndatedTaskToday_andWeeklyTueThuSaves() {
        editor(TaskDraft(id = null, title = "Stretch"))
        toggle().assertIsOff()
        toggle().performClick()
        toggle().assertIsOn()
        tile("TO-DO").assert(hasText("Today · Sat 10"))
        text("Every Sat").assertIsDisplayed()

        weekday("Tuesday").performClick()
        weekday("Thursday").performClick()
        weekday("Saturday").performClick()
        text("Every Tue & Thu").assertIsDisplayed()

        rule.onNodeWithText("Save").performClick()
        val stored = saved!!.first
        assertEquals(today, stored.toDoDate)
        assertEquals(Pattern.WEEKDAY, stored.recurrence!!.rule.pattern)
        assertEquals(listOf(2, 4), stored.recurrence!!.rule.weekdays)
    }

    @Test
    fun monthly_onThe3rdThursday() {
        editor(occurrence)
        segment("Monthly").performClick()
        segment("Monthly").assertIsSelected()
        rule.onNode(role(Role.RadioButton).and(hasText("On the 15th"))).assertIsSelected()
        rule.onNode(role(Role.RadioButton).and(hasText("On the 3rd Thursday"))).performScrollTo().performClick()
        rule.onNode(role(Role.RadioButton).and(hasText("On the 3rd Thursday"))).assertIsSelected()

        val r = saveSeries().recurrence!!.rule
        assertEquals(Pattern.MONTHDAY, r.pattern)
        assertEquals(MonthMode.WEEKDAY, r.monthMode)
        assertEquals(3, r.monthWeekPos)
        assertEquals(4, r.monthWeekday)
    }

    @Test
    fun everyThreeDays() {
        editor(occurrence)
        segment("Every N days").performClick()
        text("Every day").assertIsDisplayed()
        button("More days").performClick()
        button("More days").performClick()
        text("Every 3 days").assertIsDisplayed()

        val r = saveSeries().recurrence!!.rule
        assertEquals(Pattern.INTERVAL, r.pattern)
        assertEquals(IntervalUnit.DAY, r.unit)
        assertEquals(3, r.interval)
    }

    @Test
    fun ends_onADate_throughTheCalendarsEndMode() {
        editor(occurrence)
        text("Ends never").performClick()
        text("‹ Repeat").assertIsDisplayed()
        segment("On a date").performClick()
        text("A TAP SETS THE END").assertIsDisplayed()
        day("Wednesday 14 October").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
        day("Saturday 31 October").performClick()
        text("Ends on Sat 31 Oct").assertIsDisplayed()

        // back to the rule hands the calendar back to To do
        text("‹ Repeat").performClick()
        text("A REPEATING TASK ALWAYS HAS A TO-DO DATE").assertIsDisplayed()
        assertEquals("2026-10-31", saveSeries().recurrence!!.endDate)
    }

    @Test
    fun deadline_firstFixedFolds_withTheCallout_thenPerOccurrencePlusTwo() {
        editor(occurrence)
        // no deadline yet: the half arms the calendar's Deadline tile, and the card folds
        text("Deadline none").performClick()
        tile("DEADLINE").assertIsSelected()
        text("NO DEADLINE").assertIsDisplayed()
        chip("+1 week").performClick()
        text("ONE FIXED DEADLINE").assertIsDisplayed()
        text(
            "One fixed deadline also ends the series — no occurrences are generated after Thu 22. " +
                "Switch the repeat to per occurrence to give every Tuesday and Thursday its own.",
        ).assertIsDisplayed()

        // the folded row opens the Deadline body
        text("ONE FIXED DEADLINE").performClick()
        segment("Per occurrence").performClick()
        text("Deadline +7 days each").assertIsDisplayed()
        repeat(5) { button("Earlier deadline").performClick() }
        text("Deadline +2 days each").assertIsDisplayed()

        val stored = saveSeries()
        assertEquals(DueMode.OFFSET, stored.recurrence!!.dueMode)
        assertEquals(2, stored.recurrence!!.dueOffsetDays)
        assertEquals("2026-10-17", stored.dueDate)
    }

    @Test
    fun switchToOffset_onAnOldDeadlineRule() {
        val mondays = RepeatRule(pattern = Pattern.WEEKDAY, weekdays = listOf(1))
        val wednesdays = RepeatRule(pattern = Pattern.WEEKDAY, weekdays = listOf(3))
        val old = TaskDraft(
            id = "S2__2026-10-12", title = "Weekly report", toDoDate = "2026-10-12", dueDate = "2026-10-14",
            seriesId = "S2", occurrenceDate = "2026-10-12",
            recurrence = RecurrenceDraft(rule = mondays, dueMode = DueMode.RECURRING, dueRule = wednesdays),
        )
        val spec = SeriesSpec(rule = mondays, ruleAnchor = "2026-10-12", startDate = "2026-10-12", dueMode = DueMode.RECURRING, dueRule = wednesdays)
        editor(old, spec)
        text("Deadline custom rule").performClick()
        text("Custom deadline rule").assertIsDisplayed()
        chip("Switch to offset").performClick()
        text("Deadline +2 days each").assertIsDisplayed()

        val stored = saveSeries()
        assertEquals(DueMode.OFFSET, stored.recurrence!!.dueMode)
        assertEquals(2, stored.recurrence!!.dueOffsetDays)
        assertNull(stored.recurrence!!.dueRule)
    }
}
