package com.thefoxworks.tzafon.domain.journey

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.ArchivedOutcome
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.model.Theme
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * FR-JOURNEY — the identity mirror's numbers and copy. Everything here is
 * a reflection of accrued history; nothing awards, ranks, or counts streaks
 * (FR-JOURNEY-3 / DM-NOT).
 */
object JourneyLogic {

    /** Calendar-quarter start for the honest aggregate ("THIS QUARTER"). */
    fun quarterStart(today: String): String {
        val d = Dates.parse(today)
        val startMonth = ((d.monthValue - 1) / 3) * 3 + 1
        return Dates.iso(d.withDayOfMonth(1).withMonth(startMonth))
    }

    /**
     * FR-JOURNEY-2 aggregate — completed tasks this quarter that served a
     * chosen direction (same alignment rule as the Review snapshot: linked
     * to a theme, habit, or goal).
     */
    fun quarterAligned(tasks: List<Task>, today: String, zone: ZoneId = ZoneId.systemDefault()): Int {
        val start = quarterStart(today)
        return tasks.count { t ->
            t.state == TaskState.DONE &&
                (t.themeId != null || t.habitId != null || t.goalIds.isNotEmpty()) &&
                t.completedAt?.let {
                    Dates.iso(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) in start..today
                } == true
        }
    }

    /** "MAY 2026" — the milestone date stamp. */
    fun monthYear(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val d = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return "${d.month.name.take(3)} ${d.year}".uppercase(Locale.US)
    }

    /**
     * The long-arc detail — "12 weeks of showing up", never a streak.
     * Weeks only grow (HabitMath.arcWeeks); a miss never resets the copy.
     */
    fun arcDetail(weeks: Int): String = when {
        weeks <= 1 -> "the first week"
        weeks < 13 -> "$weeks weeks of showing up"
        else -> "${weeks / 4} months of showing up"
    }

    /** One line of the "Directions over time" timeline. */
    data class Transition(
        val theme: Theme,
        /** italic connective — "renewed as" / "grew into" / "set down, with thanks" */
        val mid: String,
        /** the right-hand bold — successor name or "retired" */
        val to: String,
        /** successor theme when the direction lives on (colors the line) */
        val successor: Theme?,
    )

    /** FR-JOURNEY-2 theme journey — archived directions, newest first. */
    fun transitions(themes: List<Theme>): List<Transition> {
        val byId = themes.associateBy { it.id }
        return themes
            .filter { it.archivedAt != null }
            .sortedByDescending { it.archivedAt }
            .map { t ->
                val successor = t.renewedToThemeId?.let { byId[it] }
                when {
                    successor != null && t.archivedOutcome == ArchivedOutcome.EVOLVED ->
                        Transition(t, "grew into", successor.name, successor)
                    successor != null ->
                        Transition(t, "renewed as", successor.name, successor)
                    else ->
                        Transition(t, "set down, with thanks", "retired", null)
                }
            }
    }

    /** Parse the snapshot's first line ("done|aligned") for the quiet fallback. */
    fun snapshotCounts(snapshot: String): Pair<Int, Int>? {
        val head = snapshot.lineSequence().firstOrNull() ?: return null
        val parts = head.split("|")
        if (parts.size != 2) return null
        val done = parts[0].trim().toIntOrNull() ?: return null
        val aligned = parts[1].trim().toIntOrNull() ?: return null
        return done to aligned
    }

    /** "WEEK OF JUL 6" / "THE MONTH, GENTLY · JUL 6" review-timeline kicker. */
    fun timelineKicker(periodStart: String, monthly: Boolean): String {
        val d = Dates.parse(periodStart)
        val stamp = "${d.month.name.take(3)} ${d.dayOfMonth}".uppercase(Locale.US)
        return if (monthly) "THE MONTH, GENTLY · $stamp" else "WEEK OF $stamp"
    }
}
