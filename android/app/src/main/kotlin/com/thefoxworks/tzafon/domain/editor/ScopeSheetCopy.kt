package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule

/** One scope on the save or delete sheet: its badge, what it does, and whether it can be picked. */
data class ScopeRow(val scope: EditScope, val badge: String?, val line: String, val enabled: Boolean = true)

/** The scope-at-save sheet's words, as the chosen scope reads them. */
data class SaveSheet(val youChanged: List<String>, val rows: List<ScopeRow>, val default: EditScope)

/**
 * v2.13.0 — the words on the "This task repeats" sheet and the delete sheet.
 * Pure: it takes the diff, the draft and the count of earlier occurrences, and
 * writes each scope row's badge and consequence line.
 */
object ScopeSheetCopy {

    /** The deck's scope labels, the same wherever a scope is asked. */
    fun label(scope: EditScope): String = when (scope) {
        EditScope.ONE -> "Just this one"
        EditScope.FORWARD -> "This and every future one"
        EditScope.ALL -> "The whole series"
    }

    /**
     * [earlier] counts this series' occurrences dated before this one, Done
     * included; it badges the whole-series row, which always shows.
     */
    fun save(draft: TaskDraft, changes: DraftChanges, earlier: Int, chosen: EditScope): SaveSheet {
        val occurrence = draft.occurrenceDate ?: draft.toDoDate!!
        val off = changes.repeatTurnedOff
        val rule = draft.recurrence?.rule
        val movedTo = draft.toDoDate?.takeIf { changes.changes.any { it.field == ChangeField.TODO_DATE } }
        val one = when {
            !changes.allows(EditScope.ONE) ->
                ScopeRow(EditScope.ONE, badge(occurrence), "Repeat changes reach the series.", enabled = false)
            movedTo != null && rule != null -> ScopeRow(
                EditScope.ONE, badge(occurrence),
                "${Dates.WD_FULL[Dates.dayOfWeek(movedTo)]} moves; the series keeps its ${rhythm(rule)} rhythm.",
            )
            else -> ScopeRow(EditScope.ONE, badge(occurrence), "Only ${DraftDiff.day(occurrence)} changes; the series stays as it is.")
        }
        // the second sentence a to-do date move adds under the forward and whole-series rows
        val move = if (movedTo != null && rule != null) {
            if (recountsOnMove(rule)) {
                " From ${DraftDiff.day(movedTo, month = month(movedTo) != month(occurrence))}, ${every(rule)}."
            } else {
                " The to-do date moves this one only."
            }
        } else ""
        val forward = if (off) {
            "This one becomes a one-off task; the series ends ${DraftDiff.day(Dates.addDays(occurrence, -1))}."
        } else {
            "Edits forward in place. Past occurrences stay as they happened.$move"
        }
        val thisOneOnly = chosen != EditScope.ONE && rule != null && !recountsOnMove(rule)
        return SaveSheet(
            youChanged = changes.changes.map {
                if (it.field == ChangeField.TODO_DATE && thisOneOnly) "${it.line} · this one only" else it.line
            },
            rows = listOf(
                one,
                ScopeRow(EditScope.FORWARD, null, forward),
                ScopeRow(
                    EditScope.ALL, earlier.takeIf { it > 0 }?.toString(),
                    "Rewrites every occurrence, past ones included, done ones too.$move", enabled = changes.allows(EditScope.ALL),
                ),
            ),
            default = changes.defaultScope,
        )
    }

    /**
     * The delete sheet's rows. [earlierNotDone] counts occurrences dated before
     * this one that aren't Done: what the whole-series row removes beyond the
     * forward row, since Done ones stay.
     */
    fun delete(occurrenceDate: String, earlierNotDone: Int): List<ScopeRow> = listOf(
        ScopeRow(EditScope.ONE, badge(occurrenceDate), "Removes ${DraftDiff.day(occurrenceDate)}; it won't come back."),
        ScopeRow(EditScope.FORWARD, null, "Removes this one and every later one. Done ones stay."),
        ScopeRow(
            EditScope.ALL, earlierNotDone.takeIf { it > 0 }?.toString(),
            "Removes the series and every occurrence not done. Done ones stay as one-off tasks.",
        ),
    )

    /**
     * Whether moving an occurrence's to-do date under the forward or
     * whole-series scope re-counts the rhythm from the new date (Every N
     * days), rather than moving just this occurrence (weekly and monthly).
     */
    fun recountsOnMove(rule: Rule): Boolean = rule.pattern == Pattern.INTERVAL && rule.unit == IntervalUnit.DAY

    /** "Tue & Thu", "every-3-days", "17th-of-the-month": what "keeps its … rhythm" names. */
    private fun rhythm(rule: Rule): String = when (rule.pattern) {
        Pattern.INTERVAL -> {
            val unit = rule.unit.name.lowercase()
            if (rule.interval == 1) (if (rule.unit == IntervalUnit.DAY) "daily" else "${unit}ly") else "every-${rule.interval}-${unit}s"
        }
        Pattern.WEEKDAY -> {
            val days = rule.weekdays.distinct().sorted().map { Dates.WD[it] }
            when {
                days.size == 7 -> "daily"
                days.size <= 1 -> days.firstOrNull() ?: "weekly"
                else -> days.dropLast(1).joinToString(", ") + " & " + days.last()
            }
        }
        Pattern.MONTHDAY -> when (rule.monthMode) {
            MonthMode.DATE -> "${Dates.ordinal(rule.monthDate)}-of-the-month"
            MonthMode.WEEKDAY -> {
                val pos = if (rule.monthWeekPos == -1) "last" else listOf("first", "second", "third", "fourth", "fifth")[rule.monthWeekPos - 1]
                "$pos-${Dates.WD_FULL[rule.monthWeekday]}"
            }
        }
    }

    /** "every 3 days" or "every day" — an Every N days rule after a re-count. */
    private fun every(rule: Rule) = if (rule.interval == 1) "every day" else "every ${rule.interval} days"

    private fun month(iso: String) = iso.substring(0, 7)

    /** "17 SEP" — the just-this-one badge. */
    private fun badge(iso: String): String {
        val d = Dates.parse(iso)
        return "${d.dayOfMonth} ${Dates.MO[d.monthValue - 1].uppercase()}"
    }
}
