package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.editor.CardBody
import com.thefoxworks.tzafon.domain.editor.CardSpot
import com.thefoxworks.tzafon.domain.editor.CardTap
import com.thefoxworks.tzafon.domain.editor.DeadlineKind
import com.thefoxworks.tzafon.domain.editor.RepeatCard
import com.thefoxworks.tzafon.domain.editor.RepeatType
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.13.0 — the Timing pane's inline repeat card as pure logic: the type
 * segment, the read-only old rules, the summary, the Ends / Deadline footer,
 * the body swap, the fold and turning repeat on. Today is Thu 17 Sep 2026.
 */
class RepeatCardTest {

    private val today = "2026-09-17"
    private val draft = TaskDraft(id = null, title = "Water plants")

    // ── turning repeat on and off ─────────────────────────────

    @Test
    fun `turning repeat on for an undated Open draft dates it today, weekly on today's weekday`() {
        val on = RepeatCard.toggle(draft, today)
        assertEquals(today, on.toDoDate)
        assertEquals(TaskState.OPEN, on.state)
        assertEquals(Pattern.WEEKDAY, on.recurrence!!.rule.pattern)
        assertEquals(listOf(4), on.recurrence!!.rule.weekdays)
        assertEquals(DueMode.NONE, on.recurrence!!.dueMode)
    }

    @Test
    fun `turning repeat on for a Backlog draft makes it Open today`() {
        val on = RepeatCard.toggle(draft.copy(state = TaskState.BACKLOG), today)
        assertEquals(TaskState.OPEN, on.state)
        assertEquals(today, on.toDoDate)
    }

    @Test
    fun `turning repeat on for a dated draft keeps its date and anchors the rule on it`() {
        val on = RepeatCard.toggle(draft.copy(toDoDate = "2026-09-22"), today)
        assertEquals("2026-09-22", on.toDoDate)
        assertEquals(listOf(2), on.recurrence!!.rule.weekdays) // Tuesday
        assertEquals(22, on.recurrence!!.rule.monthDate)
    }

    @Test
    fun `a draft that already has a deadline starts with one fixed deadline`() {
        val on = RepeatCard.toggle(draft.copy(toDoDate = today, dueDate = "2026-12-31"), today)
        assertEquals(DueMode.SINGULAR, on.recurrence!!.dueMode)
        assertEquals("2026-12-31", on.dueDate)
    }

    @Test
    fun `turning repeat off clears only the rule`() {
        val on = RepeatCard.toggle(draft.copy(state = TaskState.BACKLOG), today)
        val off = RepeatCard.toggle(on, today)
        assertNull(off.recurrence)
        assertEquals(on.copy(recurrence = null), off)
    }

    // ── the type segment, both ways ───────────────────────────

    private val tueThu = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(4, 2))
    private fun repeating(rule: Rule, toDo: String = today, rec: RecurrenceDraft = RecurrenceDraft(rule = rule)) =
        draft.copy(toDoDate = toDo, recurrence = rec.copy(rule = rule))

    @Test
    fun `a weekday rule reads as Weekly with its days and week step`() {
        val card = RepeatCard.of(repeating(tueThu.copy(weekInterval = 2)))!!
        assertEquals(RepeatType.WEEKLY, card.type)
        assertEquals(setOf(2, 4), card.weekdays)
        assertEquals(RepeatCard.Every(2, "weeks"), card.every)
        assertEquals("Every Tue & Thu, every 2 weeks", card.summary)
        assertFalse(card.readOnly)
    }

    @Test
    fun `a monthday rule reads as Monthly, offering the to-do date's two choices`() {
        val onThe17th = Rule(pattern = Pattern.MONTHDAY, monthMode = MonthMode.DATE, monthDate = 17)
        val card = RepeatCard.of(repeating(onThe17th))!!
        assertEquals(RepeatType.MONTHLY, card.type)
        assertEquals(RepeatCard.Monthly("On the 17th", "On the 3rd Thursday", MonthMode.DATE), card.monthly)
        assertEquals(RepeatCard.Every(1, "month"), card.every)
        assertEquals("The 17th of every month", card.summary)
    }

    @Test
    fun `a Monthly choice shows the rule's own day, not the to-do date's`() {
        val lastFriday = Rule(pattern = Pattern.MONTHDAY, monthMode = MonthMode.WEEKDAY, monthWeekPos = -1, monthWeekday = 5, monthInterval = 2)
        val card = RepeatCard.of(repeating(lastFriday))!!
        assertEquals(RepeatCard.Monthly("On the 17th", "On the last Friday", MonthMode.WEEKDAY), card.monthly)
        assertEquals("The last Friday of every 2 months", card.summary)
    }

    @Test
    fun `a day interval reads as Every N days`() {
        val card = RepeatCard.of(repeating(Rule(pattern = Pattern.INTERVAL, unit = IntervalUnit.DAY, interval = 3)))!!
        assertEquals(RepeatType.EVERY_N, card.type)
        assertEquals(RepeatCard.Every(3, "days"), card.every)
        assertEquals("Every 3 days", card.summary)
        assertFalse(card.readOnly)
        assertEquals("Every day", RepeatCard.of(repeating(Rule(pattern = Pattern.INTERVAL, unit = IntervalUnit.DAY, interval = 1)))!!.summary)
    }

    @Test
    fun `a draft without repeat has no card`() {
        assertNull(RepeatCard.of(draft))
    }

    @Test
    fun `picking Monthly anchors on the to-do date and carries the step`() {
        val d = RepeatCard.type(repeating(tueThu.copy(weekInterval = 2)), RepeatType.MONTHLY)
        val rule = d.recurrence!!.rule
        assertEquals(Pattern.MONTHDAY, rule.pattern)
        assertEquals(MonthMode.DATE, rule.monthMode)
        assertEquals(17, rule.monthDate)
        assertEquals(2, rule.monthInterval)
        assertEquals(RepeatType.MONTHLY, RepeatCard.of(d)!!.type)
    }

    @Test
    fun `picking Every N days gives a day interval with the step`() {
        val d = RepeatCard.type(repeating(tueThu.copy(weekInterval = 3)), RepeatType.EVERY_N)
        assertEquals(Rule(pattern = Pattern.INTERVAL, unit = IntervalUnit.DAY, interval = 3), d.recurrence!!.rule.let {
            Rule(pattern = it.pattern, unit = it.unit, interval = it.interval)
        })
        assertEquals("Every 3 days", RepeatCard.of(d)!!.summary)
    }

    @Test
    fun `picking Weekly anchors on the to-do date's weekday`() {
        val every3 = Rule(pattern = Pattern.INTERVAL, unit = IntervalUnit.DAY, interval = 3)
        val d = RepeatCard.type(repeating(every3, toDo = "2026-09-22"), RepeatType.WEEKLY)
        assertEquals(Pattern.WEEKDAY, d.recurrence!!.rule.pattern)
        assertEquals(listOf(2), d.recurrence!!.rule.weekdays)
        assertEquals(3, d.recurrence!!.rule.weekInterval)
    }

    @Test
    fun `picking the type already shown changes nothing`() {
        val d = repeating(tueThu)
        assertSame(d, RepeatCard.type(d, RepeatType.WEEKLY))
    }

    @Test
    fun `the step writes the shown type's own interval, at least 1`() {
        assertEquals(4, RepeatCard.step(repeating(tueThu), 4).recurrence!!.rule.weekInterval)
        val monthly = RepeatCard.type(repeating(tueThu), RepeatType.MONTHLY)
        assertEquals(6, RepeatCard.step(monthly, 6).recurrence!!.rule.monthInterval)
        val everyN = RepeatCard.type(repeating(tueThu), RepeatType.EVERY_N)
        assertEquals(1, RepeatCard.step(everyN, 0).recurrence!!.rule.interval)
    }

    @Test
    fun `weekday chips toggle, but the last day can't be taken away`() {
        val d = RepeatCard.weekday(repeating(tueThu), 5)
        assertEquals(setOf(2, 4, 5), RepeatCard.of(d)!!.weekdays)
        val one = RepeatCard.weekday(repeating(tueThu), 2)
        assertEquals(setOf(4), RepeatCard.of(one)!!.weekdays)
        assertSame(one, RepeatCard.weekday(one, 4))
    }

    @Test
    fun `the Monthly choice writes the to-do date's day, its weekday position, or its last weekday`() {
        val onThe17th = RepeatCard.type(repeating(tueThu), RepeatType.MONTHLY)
        val third = RepeatCard.monthly(onThe17th, MonthMode.WEEKDAY).recurrence!!.rule
        assertEquals(Triple(MonthMode.WEEKDAY, 3, 4), Triple(third.monthMode, third.monthWeekPos, third.monthWeekday))
        // Tue 29 Sep is the fifth Tuesday, so it reads as the last one
        val late = RepeatCard.type(repeating(tueThu, toDo = "2026-09-29"), RepeatType.MONTHLY)
        val last = RepeatCard.monthly(late, MonthMode.WEEKDAY).recurrence!!.rule
        assertEquals(-1, last.monthWeekPos)
        assertEquals("On the last Tuesday", RepeatCard.of(late)!!.monthly!!.onWeekday)
        val back = RepeatCard.monthly(RepeatCard.monthly(onThe17th, MonthMode.WEEKDAY), MonthMode.DATE).recurrence!!.rule
        assertEquals(Pair(MonthMode.DATE, 17), Pair(back.monthMode, back.monthDate))
    }

    // ── read-only old rules ───────────────────────────────────

    @Test
    fun `an old every-N-weeks rule shows under Every N with its unit, read-only`() {
        val fortnightly = Rule(pattern = Pattern.INTERVAL, unit = IntervalUnit.WEEK, interval = 2)
        val d = repeating(fortnightly)
        val card = RepeatCard.of(d)!!
        assertEquals(RepeatType.EVERY_N, card.type)
        assertTrue(card.readOnly)
        assertEquals(RepeatCard.Every(2, "weeks"), card.every)
        assertEquals("Every 2 weeks", card.summary)
        assertSame(d, RepeatCard.step(d, 5))
        assertSame(d, RepeatCard.type(d, RepeatType.EVERY_N))
        val weekly = RepeatCard.type(d, RepeatType.WEEKLY)
        assertEquals(Pattern.WEEKDAY, weekly.recurrence!!.rule.pattern)
        assertEquals(2, weekly.recurrence!!.rule.weekInterval)
        assertFalse(RepeatCard.of(weekly)!!.readOnly)
    }

    @Test
    fun `an old monthly interval rule is read-only too`() {
        val card = RepeatCard.of(repeating(Rule(pattern = Pattern.INTERVAL, unit = IntervalUnit.MONTH, interval = 1)))!!
        assertTrue(card.readOnly)
        assertEquals(RepeatCard.Every(1, "month"), card.every)
    }

    // ── the footer: Ends and Deadline ─────────────────────────

    private fun withDeadline(mode: DueMode, due: String? = null, offset: Int? = null, end: String? = null, dueRule: Rule? = null) =
        repeating(tueThu, rec = RecurrenceDraft(rule = tueThu, endDate = end, dueMode = mode, dueOffsetDays = offset, dueRule = dueRule))
            .copy(dueDate = due)

    @Test
    fun `with no deadline the footer reads Ends never and Deadline none, both tappable`() {
        val card = RepeatCard.of(withDeadline(DueMode.NONE))!!
        assertEquals(RepeatCard.Half("Ends", "never", tappable = true), card.ends)
        assertEquals(RepeatCard.Half("Deadline", "none", tappable = true), card.deadline)
        assertEquals(DeadlineKind.NONE, card.deadlineKind)
    }

    @Test
    fun `an end date reads On its day`() {
        assertEquals(RepeatCard.Half("Ends", "on Thu 31 Dec", tappable = true), RepeatCard.of(withDeadline(DueMode.NONE, end = "2026-12-31"))!!.ends)
    }

    @Test
    fun `a fixed deadline locks Ends at the deadline`() {
        val d = withDeadline(DueMode.SINGULAR, due = "2026-10-30")
        val card = RepeatCard.of(d)!!
        assertEquals(DeadlineKind.FIXED, card.deadlineKind)
        assertEquals(RepeatCard.Half("Ends", "at the deadline", tappable = false), card.ends)
        assertEquals(RepeatCard.Half("Deadline", "one fixed", tappable = true), card.deadline)
        assertSame(d, RepeatCard.ends(d, "2026-12-31"))
    }

    @Test
    fun `a fixed mode without a date is no deadline`() {
        assertEquals(DeadlineKind.NONE, RepeatCard.of(withDeadline(DueMode.SINGULAR, due = null))!!.deadlineKind)
    }

    @Test
    fun `Ends sets and clears the end date`() {
        val d = RepeatCard.ends(withDeadline(DueMode.NONE), "2026-12-31")
        assertEquals("2026-12-31", d.recurrence!!.endDate)
        assertNull(RepeatCard.ends(d, null).recurrence!!.endDate)
    }

    @Test
    fun `a per-occurrence deadline reads as its offset`() {
        val card = RepeatCard.of(withDeadline(DueMode.OFFSET, due = "2026-09-19", offset = 2))!!
        assertEquals(DeadlineKind.PER_OCCURRENCE, card.deadlineKind)
        assertEquals(RepeatCard.Half("Deadline", "+2 days each", tappable = true), card.deadline)
        assertEquals(2, card.offset)
        assertEquals("+1 day each", RepeatCard.of(withDeadline(DueMode.OFFSET, due = "2026-09-18", offset = 1))!!.deadline.value)
        assertEquals(RepeatCard.Half("Ends", "never", tappable = true), card.ends)
    }

    @Test
    fun `switching one fixed to per occurrence keeps the gap, held to 0-30`() {
        val d = RepeatCard.deadlineKind(withDeadline(DueMode.SINGULAR, due = "2026-09-20"), DeadlineKind.PER_OCCURRENCE)
        assertEquals(DueMode.OFFSET, d.recurrence!!.dueMode)
        assertEquals(3, d.recurrence!!.dueOffsetDays)
        assertEquals("2026-09-20", d.dueDate)
        val far = RepeatCard.deadlineKind(withDeadline(DueMode.SINGULAR, due = "2026-12-31"), DeadlineKind.PER_OCCURRENCE)
        assertEquals(30, far.recurrence!!.dueOffsetDays)
        assertEquals("2026-10-17", far.dueDate)
        val early = RepeatCard.deadlineKind(withDeadline(DueMode.SINGULAR, due = "2026-09-10"), DeadlineKind.PER_OCCURRENCE)
        assertEquals(0, early.recurrence!!.dueOffsetDays)
        assertEquals(today, early.dueDate)
    }

    @Test
    fun `switching per occurrence to one fixed keeps this occurrence's deadline and ends there`() {
        val d = RepeatCard.deadlineKind(withDeadline(DueMode.OFFSET, due = "2026-09-19", offset = 2, end = "2026-12-31"), DeadlineKind.FIXED)
        assertEquals(DueMode.SINGULAR, d.recurrence!!.dueMode)
        assertEquals("2026-09-19", d.dueDate)
        assertNull(d.recurrence!!.dueOffsetDays)
        assertNull(d.recurrence!!.endDate)
    }

    @Test
    fun `the offset stepper runs 0 to 30 and moves this occurrence's deadline`() {
        val d = withDeadline(DueMode.OFFSET, due = "2026-09-19", offset = 2)
        assertEquals(0..30, RepeatCard.OFFSET_RANGE)
        assertEquals("2026-09-22", RepeatCard.offset(d, 5).dueDate)
        assertEquals(30, RepeatCard.offset(d, 99).recurrence!!.dueOffsetDays)
        assertEquals(0, RepeatCard.offset(d, -1).recurrence!!.dueOffsetDays)
        val fixed = withDeadline(DueMode.SINGULAR, due = "2026-10-30")
        assertSame(fixed, RepeatCard.offset(fixed, 5))
    }

    // an old Monday series whose deadline rule falls on Wednesdays
    private val mondays = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(1))
    private val wednesdays = Rule(pattern = Pattern.WEEKDAY, weekdays = listOf(3))
    private val oldSeries = Series(
        id = "S1", title = "Report", rule = mondays, ruleAnchor = "2026-09-07", startDate = "2026-09-07",
        dueMode = DueMode.RECURRING, dueRule = wednesdays,
    )

    @Test
    fun `an old deadline rule is a read-only custom rule, switched only to an offset`() {
        val d = repeating(mondays, toDo = "2026-09-21", rec = RecurrenceDraft(rule = mondays, dueMode = DueMode.RECURRING, dueRule = wednesdays))
            .copy(dueDate = "2026-09-23")
        val card = RepeatCard.of(d)!!
        assertEquals(DeadlineKind.CUSTOM_RULE, card.deadlineKind)
        assertEquals(RepeatCard.Half("Deadline", "custom rule", tappable = true), card.deadline)
        assertSame(d, RepeatCard.deadlineKind(d, DeadlineKind.PER_OCCURRENCE))
        assertSame(d, RepeatCard.offset(d, 4))

        val switched = RepeatCard.switchToOffset(d, oldSeries.toSpec(), today)
        assertEquals(DueMode.OFFSET, switched.recurrence!!.dueMode)
        assertEquals(2, switched.recurrence!!.dueOffsetDays) // Mon 21 → Wed 23
        assertNull(switched.recurrence!!.dueRule)
        assertEquals("2026-09-23", switched.dueDate)
        assertEquals("+2 days each", RepeatCard.of(switched)!!.deadline.value)
    }

    @Test
    fun `Switch to offset gives 0 when the next occurrence has no deadline`() {
        val d = repeating(mondays, toDo = "2026-09-21", rec = RecurrenceDraft(rule = mondays, dueMode = DueMode.RECURRING, dueRule = wednesdays))
        val ended = oldSeries.copy(dueRule = wednesdays.copy(weekdays = listOf(3)), endDate = "2026-09-01")
        assertEquals(0, RepeatCard.switchToOffset(d, ended.toSpec(), today).recurrence!!.dueOffsetDays)
    }

    // ── the body swap ─────────────────────────────────────────

    @Test
    fun `tapping Ends or Deadline swaps the body, and Repeat goes back to the rule`() {
        val d = withDeadline(DueMode.OFFSET, due = "2026-09-19", offset = 2)
        assertEquals(CardTap.Show(CardBody.ENDS), RepeatCard.tap(d, CardSpot.ENDS))
        assertEquals(CardTap.Show(CardBody.DEADLINE), RepeatCard.tap(d, CardSpot.DEADLINE))
        assertEquals(CardTap.Show(CardBody.RULE), RepeatCard.tap(d, CardSpot.BACK))
        assertEquals(CardBody.DEADLINE, RepeatCard.of(d, body = CardBody.DEADLINE)!!.body)
        assertEquals(CardBody.RULE, RepeatCard.of(d)!!.body)
    }

    @Test
    fun `Deadline none arms the calendar's Deadline tile instead, since a date comes first`() {
        val d = withDeadline(DueMode.NONE)
        assertEquals(CardTap.ArmDeadline, RepeatCard.tap(d, CardSpot.DEADLINE))
        assertEquals(CardBody.RULE, RepeatCard.of(d, body = CardBody.DEADLINE)!!.body)
    }

    @Test
    fun `a locked Ends does nothing and its body can't show`() {
        val d = withDeadline(DueMode.SINGULAR, due = "2026-10-30")
        assertEquals(CardTap.Nothing, RepeatCard.tap(d, CardSpot.ENDS))
        assertEquals(CardBody.RULE, RepeatCard.of(d, body = CardBody.ENDS)!!.body)
    }

    // ── the fold ──────────────────────────────────────────────

    @Test
    fun `the card unfolds while To do is armed`() {
        val card = RepeatCard.of(withDeadline(DueMode.SINGULAR, due = "2026-10-30"))!!
        assertNull(card.folded)
        assertNull(card.callout)
    }

    @Test
    fun `armed on a fixed deadline the card folds, with the callout`() {
        val card = RepeatCard.of(withDeadline(DueMode.SINGULAR, due = "2026-10-30"), deadlineArmed = true)!!
        assertEquals("Every Tue & Thu · ONE FIXED DEADLINE", card.folded)
        assertEquals(RepeatCard.FIXED_CALLOUT, card.callout)
    }

    @Test
    fun `armed on a per-occurrence deadline the card folds without the callout`() {
        val card = RepeatCard.of(withDeadline(DueMode.OFFSET, due = "2026-09-19", offset = 2), deadlineArmed = true)!!
        assertEquals("Every Tue & Thu · DEADLINE +2 DAYS EACH", card.folded)
        assertNull(card.callout)
    }

    @Test
    fun `armed with no deadline yet the card folds, and the folded row can't open the body`() {
        val d = withDeadline(DueMode.NONE)
        assertEquals("Every Tue & Thu · NO DEADLINE", RepeatCard.of(d, deadlineArmed = true)!!.folded)
        assertEquals(CardTap.Nothing, RepeatCard.tap(d, CardSpot.FOLDED))
    }

    @Test
    fun `tapping the folded row opens the Deadline body, which unfolds the card`() {
        val d = withDeadline(DueMode.SINGULAR, due = "2026-10-30")
        assertEquals(CardTap.Show(CardBody.DEADLINE), RepeatCard.tap(d, CardSpot.FOLDED))
        val open = RepeatCard.of(d, body = CardBody.DEADLINE, deadlineArmed = true)!!
        assertNull(open.folded)
        assertNull(open.callout)
    }

    @Test
    fun `an old deadline rule folds as a custom rule`() {
        val d = repeating(mondays, toDo = "2026-09-21", rec = RecurrenceDraft(rule = mondays, dueMode = DueMode.RECURRING, dueRule = wednesdays))
        assertEquals("Every Mon · CUSTOM DEADLINE RULE", RepeatCard.of(d, deadlineArmed = true)!!.folded)
    }
}
