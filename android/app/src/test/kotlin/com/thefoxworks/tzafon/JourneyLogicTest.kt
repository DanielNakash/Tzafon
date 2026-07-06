package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.domain.journey.JourneyLogic
import com.thefoxworks.tzafon.domain.model.ArchivedOutcome
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** FR-JOURNEY — mirror math and copy. */
class JourneyLogicTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun epoch(date: String): Long =
        LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun task(
        id: String,
        state: TaskState = TaskState.DONE,
        completedOn: String? = null,
        themeId: String? = null,
        habitId: String? = null,
        goalIds: List<String> = emptyList(),
    ) = Task(
        id = id, title = id, state = state,
        completedAt = completedOn?.let { epoch(it) },
        themeId = themeId, habitId = habitId, goalIds = goalIds,
    )

    // ── quarter aggregate ─────────────────────────────────────

    @Test
    fun `quarter start snaps to the calendar quarter`() {
        assertEquals("2026-07-01", JourneyLogic.quarterStart("2026-07-07"))
        assertEquals("2026-04-01", JourneyLogic.quarterStart("2026-06-30"))
        assertEquals("2026-01-01", JourneyLogic.quarterStart("2026-03-31"))
        assertEquals("2026-10-01", JourneyLogic.quarterStart("2026-12-25"))
    }

    @Test
    fun `aligned counts only linked, DONE, in-quarter tasks`() {
        val tasks = listOf(
            task("themed", completedOn = "2026-07-03", themeId = "th"),        // counts
            task("habitual", completedOn = "2026-07-05", habitId = "h"),       // counts
            task("goalish", completedOn = "2026-07-01", goalIds = listOf("g")), // counts
            task("unlinked", completedOn = "2026-07-04"),                       // no link
            task("lastQuarter", completedOn = "2026-06-20", themeId = "th"),    // out of window
            task("open", state = TaskState.OPEN, themeId = "th"),               // not done
        )
        assertEquals(3, JourneyLogic.quarterAligned(tasks, "2026-07-07", zone))
    }

    @Test
    fun `month-year stamp reads like the design`() {
        assertEquals("MAY 2026", JourneyLogic.monthYear(epoch("2026-05-14"), zone))
    }

    // ── arcs (never streaks) ──────────────────────────────────

    @Test
    fun `arc detail grows and never shames`() {
        assertEquals("the first week", JourneyLogic.arcDetail(1))
        assertEquals("12 weeks of showing up", JourneyLogic.arcDetail(12))
        assertEquals("4 months of showing up", JourneyLogic.arcDetail(16))
    }

    // ── directions over time ──────────────────────────────────

    private fun theme(
        id: String,
        name: String,
        archivedAt: Long? = null,
        outcome: ArchivedOutcome? = null,
        renewedTo: String? = null,
    ) = Theme(
        id = id, name = name, why = "because",
        windowStart = "2026-01-01", windowEnd = "2026-03-31",
        state = if (archivedAt != null) ThemeState.ARCHIVED else ThemeState.ACTIVE,
        archivedOutcome = outcome, renewedToThemeId = renewedTo, archivedAt = archivedAt,
    )

    @Test
    fun `transitions render renewed, evolved, and retired lines newest first`() {
        val themes = listOf(
            theme("old", "Year of Health", archivedAt = 200, outcome = ArchivedOutcome.RENEWED, renewedTo = "next"),
            theme("cook", "Learn to cook", archivedAt = 100, outcome = ArchivedOutcome.RETIRED),
            theme("write", "Just write", archivedAt = 300, outcome = ArchivedOutcome.EVOLVED, renewedTo = "next"),
            theme("next", "Move more, feel strong"), // live successor — not itself a row
        )
        val rows = JourneyLogic.transitions(themes)
        assertEquals(listOf("write", "old", "cook"), rows.map { it.theme.id })

        assertEquals("grew into", rows[0].mid)
        assertEquals("Move more, feel strong", rows[0].to)
        assertEquals("next", rows[0].successor?.id)

        assertEquals("renewed as", rows[1].mid)

        assertEquals("set down, with thanks", rows[2].mid)
        assertEquals("retired", rows[2].to)
        assertNull(rows[2].successor)
    }

    @Test
    fun `a renewal pointing at a deleted theme falls back to retired copy`() {
        val rows = JourneyLogic.transitions(
            listOf(theme("old", "Gone", archivedAt = 1, outcome = ArchivedOutcome.RENEWED, renewedTo = "missing")),
        )
        assertEquals("set down, with thanks", rows[0].mid)
        assertEquals("retired", rows[0].to)
    }

    // ── review timeline ───────────────────────────────────────

    @Test
    fun `snapshot counts parse the first line only`() {
        assertEquals(9 to 4, JourneyLogic.snapshotCounts("9|4\n H Run · 2 of 3"))
        assertNull(JourneyLogic.snapshotCounts(""))
        assertNull(JourneyLogic.snapshotCounts("not a snapshot"))
    }

    @Test
    fun `timeline kicker marks the monthly upgrade`() {
        assertEquals("WEEK OF JUL 6", JourneyLogic.timelineKicker("2026-07-06", monthly = false))
        assertEquals("THE MONTH, GENTLY · JUL 6", JourneyLogic.timelineKicker("2026-07-06", monthly = true))
    }
}
