package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule

/** The editor's three panes (v2.13.0). */
enum class Pane { TASK, TIMING, ALIGNMENT }

/** Every editor field the diff reports, in the order the "You changed" list shows them. */
enum class ChangeField(val pane: Pane) {
    TITLE(Pane.TASK),
    NOTES(Pane.TASK),
    STATUS(Pane.TASK),
    TODO_DATE(Pane.TIMING),
    DEADLINE(Pane.TIMING),
    REPEAT(Pane.TIMING),
    ENDS(Pane.TIMING),
    DEADLINE_MODE(Pane.TIMING),
    DEADLINE_OFFSET(Pane.TIMING),
    CUE(Pane.ALIGNMENT),
    HABIT(Pane.ALIGNMENT),
    GOALS(Pane.ALIGNMENT),
}

/**
 * One changed field and its "You changed" line. [repeatCard] marks a change
 * to the series' own settings, which can't stay on just this occurrence.
 */
data class Change(val field: ChangeField, val line: String, val repeatCard: Boolean = false)

/** What a draft changed since the editor opened, in pane order. */
data class DraftChanges(val changes: List<Change>, val repeatTurnedOff: Boolean = false) {
    val isEmpty: Boolean get() = changes.isEmpty()

    /** The panes that carry a changed dot. */
    val panes: Set<Pane> get() = changes.mapTo(mutableSetOf()) { it.field.pane }

    /** Anything on the repeat card changed, so "Just this one" can't take it. */
    val touchesRepeatCard: Boolean get() = changes.any { it.repeatCard }
}

/**
 * v2.13.0 — the "You changed" diff. Compares the editor's opening snapshot
 * with the draft as Save would store them, so undoing an edit clears it.
 * Pure and cheap: the editor runs it on every edit for the per-pane dots,
 * the discard prompt and the scope-at-save sheet.
 */
object DraftDiff {

    /** [amountUnit] is the linked habit's unit, read with the "How much?" answer. */
    fun of(snapshot: TaskDraft, draft: TaskDraft, amountUnit: String? = null): DraftChanges {
        val out = mutableListOf<Change>()
        if (snapshot.title.trim() != draft.title.trim()) out += Change(ChangeField.TITLE, "Title changed")
        presence("Notes", snapshot.description.ifEmpty { null }, draft.description.ifEmpty { null })
            ?.let { out += Change(ChangeField.NOTES, it) }
        if (snapshot.state != draft.state) {
            val amount = draft.habitAmount?.takeIf { draft.state == TaskState.DONE }
                ?.let { " · " + listOfNotNull(amount(it), amountUnit).joinToString(" ") }
            out += Change(ChangeField.STATUS, "Status ${status(snapshot.state)} → ${status(draft.state)}${amount.orEmpty()}")
        }
        if (snapshot.toDoDate != draft.toDoDate) {
            out += Change(ChangeField.TODO_DATE, "To-do date ${dayPair(snapshot.toDoDate, draft.toDoDate, "Undated")}")
        }
        deadline(snapshot, draft)?.let { out += it }
        out += repeat(snapshot.recurrence, draft.recurrence)
        presence("Cue", snapshot.cue, draft.cue)?.let { out += Change(ChangeField.CUE, it) }
        presence("Habit", snapshot.habitId, draft.habitId)?.let { out += Change(ChangeField.HABIT, it) }
        goals(snapshot.goalIds.toSet(), draft.goalIds.toSet())?.let { out += Change(ChangeField.GOALS, it) }
        return DraftChanges(
            changes = out.sortedBy { it.field.ordinal },
            repeatTurnedOff = snapshot.recurrence != null && draft.recurrence == null,
        )
    }

    /** Turning repeat on or off is one entry; otherwise the rule and Ends each count. */
    private fun repeat(before: RecurrenceDraft?, after: RecurrenceDraft?): List<Change> = when {
        before == null && after == null -> emptyList()
        before == null -> listOf(Change(ChangeField.REPEAT, "Repeat turned on", repeatCard = true))
        after == null -> listOf(Change(ChangeField.REPEAT, "Repeat turned off", repeatCard = true))
        else -> listOfNotNull(
            Change(ChangeField.REPEAT, "Repeat changed", repeatCard = true)
                .takeIf { meaning(before.rule) != meaning(after.rule) },
            Change(ChangeField.ENDS, "Ends ${dayPair(before.endDate, after.endDate, "Never")}", repeatCard = true)
                .takeIf { before.endDate != after.endDate },
        )
    }

    /** The fields a rule's pattern actually reads, so leftovers from another pattern don't count. */
    private fun meaning(r: Rule): List<Any> = when (r.pattern) {
        Pattern.INTERVAL -> listOf(r.pattern, r.interval, r.unit)
        Pattern.WEEKDAY -> listOf(r.pattern, r.weekdays.toSortedSet(), r.weekInterval)
        Pattern.MONTHDAY -> when (r.monthMode) {
            MonthMode.DATE -> listOf(r.pattern, r.monthMode, r.monthDate, r.monthInterval)
            MonthMode.WEEKDAY -> listOf(r.pattern, r.monthMode, r.monthWeekPos, r.monthWeekday, r.monthInterval)
        }
    }

    /**
     * A one-off's deadline, or a repeating task's: its fixed date, its offset
     * (whose dates follow the to-do date and aren't listed), or a switch of mode.
     */
    private fun deadline(snapshot: TaskDraft, draft: TaskDraft): Change? {
        val a = snapshot.recurrence
        val b = draft.recurrence
        val dateChange = snapshot.dueDate != draft.dueDate
        fun dates(repeatCard: Boolean) =
            Change(ChangeField.DEADLINE, "Deadline ${dayPair(snapshot.dueDate, draft.dueDate, "None")}", repeatCard)
        if (a == null || b == null) return if (dateChange) dates(repeatCard = false) else null
        if (a.dueMode == b.dueMode) return when (a.dueMode) {
            DueMode.SINGULAR -> if (dateChange) dates(repeatCard = true) else null
            DueMode.OFFSET -> if (offset(a) != offset(b)) {
                Change(ChangeField.DEADLINE_OFFSET, "Deadline +${offset(a)} → +${offset(b)} days each", repeatCard = true)
            } else null
            // an old rule's (or a missing) deadline is this occurrence's own date
            DueMode.NONE, DueMode.RECURRING -> if (dateChange) dates(repeatCard = false) else null
        }
        if (a.dueMode == DueMode.NONE || b.dueMode == DueMode.NONE) {
            return Change(ChangeField.DEADLINE, "Deadline ${setting(a, snapshot.dueDate)} → ${setting(b, draft.dueDate)}", repeatCard = true)
        }
        return Change(ChangeField.DEADLINE_MODE, "Deadline ${modeName(a.dueMode)} → ${modeName(b.dueMode)}", repeatCard = true)
    }

    private fun offset(r: RecurrenceDraft) = r.dueOffsetDays ?: 0

    /** A repeating deadline set against None: its date, its offset or its old rule. */
    private fun setting(r: RecurrenceDraft, dueDate: String?): String = when (r.dueMode) {
        DueMode.NONE -> "None"
        DueMode.SINGULAR -> dueDate?.let { day(it) } ?: "None"
        DueMode.OFFSET -> "+${offset(r)} days each"
        DueMode.RECURRING -> modeName(r.dueMode)
    }

    private fun modeName(m: DueMode) = when (m) {
        DueMode.NONE -> "None"
        DueMode.SINGULAR -> "One fixed"
        DueMode.OFFSET -> "Per occurrence"
        DueMode.RECURRING -> "Custom rule"
    }

    /**
     * "Thu 17 → Fri 18". The month shows when the months differ or one side is
     * [missing], and the year when the years differ.
     */
    internal fun dayPair(before: String?, after: String?, missing: String): String {
        val a = before?.let(Dates::parse)
        val b = after?.let(Dates::parse)
        val years = a != null && b != null && a.year != b.year
        val months = years || a?.monthValue != b?.monthValue || a == null || b == null
        fun label(iso: String?) = iso?.let { day(it, month = months, year = years) } ?: missing
        return "${label(before)} → ${label(after)}"
    }

    /** "Thu 17", "Thu 17 Sep" or "Thu 17 Sep 2026". */
    internal fun day(iso: String, month: Boolean = true, year: Boolean = false): String {
        val d = Dates.parse(iso)
        return buildString {
            append(Dates.WD[Dates.dayOfWeek(d)]).append(' ').append(d.dayOfMonth)
            if (month) append(' ').append(Dates.MO[d.monthValue - 1])
            if (year) append(' ').append(d.year)
        }
    }

    /** Goals compare as a set, so reordering them is no change. */
    private fun goals(before: Set<String>, after: Set<String>): String? {
        val added = (after - before).size
        val removed = (before - after).size
        return when {
            added > 0 && removed > 0 -> "Goals changed"
            added == 1 -> "Goal added"
            added > 1 -> "$added goals added"
            removed == 1 -> "Goal removed"
            removed > 1 -> "$removed goals removed"
            else -> null
        }
    }

    private fun status(s: TaskState) = s.name.lowercase().replaceFirstChar { it.uppercase() }

    private fun amount(v: Double): String =
        if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else v.toString()

    /** "<Name> added / changed / removed", or null when nothing changed. */
    private fun presence(name: String, before: Any?, after: Any?): String? = when {
        before == after -> null
        before == null -> "$name added"
        after == null -> "$name removed"
        else -> "$name changed"
    }
}
