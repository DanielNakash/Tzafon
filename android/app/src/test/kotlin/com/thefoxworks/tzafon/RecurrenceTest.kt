package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.domain.recurrence.Rule
import com.thefoxworks.tzafon.domain.recurrence.SeriesSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Oracle-parity suite: mirrors the v1.1.0 web engine tests
 * (PROJECT/src/utils/recurrence.test.js) case-for-case, then adds the v2
 * fixes — FR-REC-1 nearest-occurrence floor and FR-REC-2 conditional year.
 */
class RecurrenceTest {

    private fun interval(interval: Int, unit: IntervalUnit) =
        Rule(pattern = Pattern.INTERVAL, interval = interval, unit = unit)

    private fun weekday(weekdays: List<Int>, weekInterval: Int = 1) =
        Rule(pattern = Pattern.WEEKDAY, weekdays = weekdays, weekInterval = weekInterval)

    private fun monthDate(monthDate: Int, monthInterval: Int = 1) =
        Rule(pattern = Pattern.MONTHDAY, monthMode = MonthMode.DATE, monthDate = monthDate, monthInterval = monthInterval)

    private fun monthWk(monthWeekday: Int, monthWeekPos: Int, monthInterval: Int = 1) =
        Rule(pattern = Pattern.MONTHDAY, monthMode = MonthMode.WEEKDAY, monthWeekday = monthWeekday, monthWeekPos = monthWeekPos, monthInterval = monthInterval)

    private fun spec(
        rule: Rule,
        anchor: String = "2026-06-01",
        endDate: String? = null,
        dueMode: DueMode = DueMode.NONE,
        dueRule: Rule? = null,
        dueSingular: String? = null,
        exceptions: List<String> = emptyList(),
    ) = SeriesSpec(rule, anchor, anchor, endDate, dueMode, dueRule, dueSingular, exceptions)

    // ── enumerateDates — interval ─────────────────────────────

    @Test
    fun `every 3 days`() {
        assertEquals(
            listOf("2026-06-01", "2026-06-04", "2026-06-07", "2026-06-10"),
            Recurrence.enumerateDates(interval(3, IntervalUnit.DAY), "2026-06-01", "2026-06-01", "2026-06-12"),
        )
    }

    @Test
    fun `every 2 weeks`() {
        assertEquals(
            listOf("2026-06-01", "2026-06-15", "2026-06-29"),
            Recurrence.enumerateDates(interval(2, IntervalUnit.WEEK), "2026-06-01", "2026-06-01", "2026-07-01"),
        )
    }

    @Test
    fun `monthly clamps short months`() {
        assertEquals(
            listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30"),
            Recurrence.enumerateDates(interval(1, IntervalUnit.MONTH), "2026-01-31", "2026-01-31", "2026-04-30"),
        )
    }

    @Test
    fun `respects from lower bound but stays phase-aligned to anchor`() {
        assertEquals(
            listOf("2026-06-07", "2026-06-10"),
            Recurrence.enumerateDates(interval(3, IntervalUnit.DAY), "2026-06-01", "2026-06-05", "2026-06-12"),
        )
    }

    // ── enumerateDates — weekday ──────────────────────────────

    @Test
    fun `every Monday and Thursday weekly`() {
        // 2026-06-01 is a Monday
        assertEquals(
            listOf("2026-06-01", "2026-06-04", "2026-06-08", "2026-06-11"),
            Recurrence.enumerateDates(weekday(listOf(1, 4)), "2026-06-01", "2026-06-01", "2026-06-14"),
        )
    }

    @Test
    fun `every Tuesday every 2 weeks`() {
        assertEquals(
            listOf("2026-06-02", "2026-06-16", "2026-06-30"),
            Recurrence.enumerateDates(weekday(listOf(2), 2), "2026-06-01", "2026-06-01", "2026-06-30"),
        )
    }

    @Test
    fun `never emits a selected weekday earlier than the anchor`() {
        // anchor Wed 2026-06-03, Monday selected -> first Monday is 06-08
        assertEquals(
            listOf("2026-06-08", "2026-06-15"),
            Recurrence.enumerateDates(weekday(listOf(1)), "2026-06-03", "2026-06-03", "2026-06-15"),
        )
    }

    // ── enumerateDates — monthday ─────────────────────────────

    @Test
    fun `the 25th of every 2 months`() {
        assertEquals(
            listOf("2026-01-25", "2026-03-25", "2026-05-25"),
            Recurrence.enumerateDates(monthDate(25, 2), "2026-01-25", "2026-01-25", "2026-06-30"),
        )
    }

    @Test
    fun `the first Sunday of every month`() {
        assertEquals(
            listOf("2026-06-07", "2026-07-05", "2026-08-02"),
            Recurrence.enumerateDates(monthWk(0, 1), "2026-06-01", "2026-06-01", "2026-08-31"),
        )
    }

    @Test
    fun `the last Friday of every month`() {
        assertEquals(
            listOf("2026-06-26", "2026-07-31", "2026-08-28"),
            Recurrence.enumerateDates(monthWk(5, -1), "2026-06-01", "2026-06-01", "2026-08-31"),
        )
    }

    // ── computeDue ────────────────────────────────────────────

    @Test
    fun `dueMode none - all null`() {
        val s = spec(weekday(listOf(1)))
        assertEquals(
            listOf(null, null),
            Recurrence.computeDue(s, listOf("2026-06-01", "2026-06-08"), "2026-06-01", "2026-06-30"),
        )
    }

    @Test
    fun `singular - deadline on every occurrence`() {
        val s = spec(weekday(listOf(1)), dueMode = DueMode.SINGULAR, dueSingular = "2026-07-01")
        assertEquals(
            listOf("2026-07-01", "2026-07-01"),
            Recurrence.computeDue(s, listOf("2026-06-01", "2026-06-08"), "2026-06-01", "2026-07-01"),
        )
    }

    @Test
    fun `recurring - independent rule paired by index`() {
        val s = spec(weekday(listOf(1)), dueMode = DueMode.RECURRING, dueRule = weekday(listOf(5)))
        val toDo = Recurrence.enumerateDates(s.rule, "2026-06-01", "2026-06-01", "2026-06-21")
        assertEquals(
            listOf("2026-06-05", "2026-06-12", "2026-06-19"),
            Recurrence.computeDue(s, toDo, "2026-06-01", "2026-06-21"),
        )
    }

    // ── planOccurrences ───────────────────────────────────────

    @Test
    fun `generates To Do occurrences up to the horizon`() {
        val plan = Recurrence.planOccurrences(spec(weekday(listOf(1))), "2026-06-30")
        assertEquals(
            listOf("2026-06-01", "2026-06-08", "2026-06-15", "2026-06-22", "2026-06-29"),
            plan.create.map { it.occurrenceDate },
        )
    }

    @Test
    fun `idempotent - skips already-materialized slots`() {
        val plan = Recurrence.planOccurrences(spec(weekday(listOf(1))), "2026-06-30", setOf("2026-06-01", "2026-06-08"))
        assertEquals(
            listOf("2026-06-15", "2026-06-22", "2026-06-29"),
            plan.create.map { it.occurrenceDate },
        )
    }

    @Test
    fun `honors exceptions - deleted slots never regenerate`() {
        val plan = Recurrence.planOccurrences(spec(weekday(listOf(1)), exceptions = listOf("2026-06-15")), "2026-06-30")
        assertEquals(
            listOf("2026-06-01", "2026-06-08", "2026-06-22", "2026-06-29"),
            plan.create.map { it.occurrenceDate },
        )
    }

    @Test
    fun `stops at the end date`() {
        val plan = Recurrence.planOccurrences(spec(weekday(listOf(1)), endDate = "2026-06-15"), "2026-06-30")
        assertEquals(
            listOf("2026-06-01", "2026-06-08", "2026-06-15"),
            plan.create.map { it.occurrenceDate },
        )
    }

    @Test
    fun `singular due bounds the series and stamps every occurrence`() {
        val s = spec(weekday(listOf(1)), dueMode = DueMode.SINGULAR, dueSingular = "2026-06-15")
        val plan = Recurrence.planOccurrences(s, "2026-06-30")
        assertEquals(
            listOf("2026-06-01", "2026-06-08", "2026-06-15"),
            plan.create.map { it.occurrenceDate },
        )
        assertTrue(plan.create.all { it.due == "2026-06-15" })
    }

    // ── FR-REC-1 — nearest-occurrence floor ───────────────────

    @Test
    fun `far-future series still materializes its nearest occurrence`() {
        // yearly-ish rule: the 14th of every 12 months, anchored far ahead
        val s = spec(monthDate(14, 12), anchor = "2026-09-14")
        // horizon = today+60d ends before the anchor's first slot
        val plan = Recurrence.planOccurrences(s, "2026-08-01", emptySet(), today = "2026-06-05")
        assertEquals(listOf("2026-09-14"), plan.create.map { it.occurrenceDate })
    }

    @Test
    fun `floor is idempotent - existing upcoming occurrence suppresses it`() {
        val s = spec(monthDate(14, 12), anchor = "2026-09-14")
        val plan = Recurrence.planOccurrences(s, "2026-08-01", setOf("2026-09-14"), today = "2026-06-05")
        assertTrue(plan.create.isEmpty())
    }

    @Test
    fun `floor respects exceptions and picks the next slot`() {
        val s = spec(monthDate(14, 12), anchor = "2026-09-14", exceptions = listOf("2026-09-14"))
        val plan = Recurrence.planOccurrences(s, "2026-08-01", emptySet(), today = "2026-06-05")
        assertEquals(listOf("2027-09-14"), plan.create.map { it.occurrenceDate })
    }

    @Test
    fun `floor returns nothing when the series has ended`() {
        val s = spec(weekday(listOf(1)), anchor = "2026-01-05", endDate = "2026-03-01")
        assertNull(Recurrence.nearestOccurrence(s, "2026-06-05"))
    }

    @Test
    fun `floor pairs the recurring due by series index`() {
        // Mon To Do / Fri due, weekly; today far past several slots that are all materialized…
        val s = spec(weekday(listOf(1)), dueMode = DueMode.RECURRING, dueRule = weekday(listOf(5)))
        val nearest = Recurrence.nearestOccurrence(s, "2026-06-09")
        // next Monday on/after Jun 9 is Jun 15 (index 2); its Friday is Jun 19
        assertEquals("2026-06-15", nearest?.occurrenceDate)
        assertEquals("2026-06-19", nearest?.due)
    }

    // ── FR-REC-2 — conditional year in labels ─────────────────

    @Test
    fun `same-year label has no year`() {
        assertEquals("Mon, Sep 14", Dates.fmtDate("2026-09-14", today = "2026-06-05"))
    }

    @Test
    fun `different-year label carries the year`() {
        assertEquals("Tue, Jan 5, 2027", Dates.fmtDate("2027-01-05", today = "2026-06-05"))
    }

    // ── recurSummary ──────────────────────────────────────────

    @Test
    fun `summary interval`() {
        assertEquals("Every 3 days", Recurrence.summary(interval(3, IntervalUnit.DAY)))
    }

    @Test
    fun `summary weekday with interval`() {
        assertEquals("Every Monday & Thursday, every 2 weeks", Recurrence.summary(weekday(listOf(1, 4), 2)))
    }

    @Test
    fun `summary three weekdays use comma join`() {
        assertEquals("Every Mon, Wed, Fri", Recurrence.summary(weekday(listOf(1, 3, 5))))
    }

    @Test
    fun `summary monthday weekday`() {
        assertEquals("The last Sunday of every month", Recurrence.summary(monthWk(0, -1)))
    }
}
