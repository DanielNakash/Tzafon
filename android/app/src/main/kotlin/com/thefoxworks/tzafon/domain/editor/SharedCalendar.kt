package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import java.time.YearMonth

/** Which date a tap on the shared calendar sets. END is entered only from the repeat card's Ends body. */
enum class Armed { TODO, DEADLINE, END }

/**
 * v2.13.0 — the shared calendar's rules over an editor [draft], with no UI:
 * the rail of quick picks for the [armed] field, what a tap on a day does,
 * the marks for any day, the deadline-before-to-do warning and the captions.
 * Every change comes back as a new draft.
 */
class SharedCalendar(val draft: TaskDraft, val armed: Armed, val today: String) {

    /** One quick pick. [day] null is Undated / None / Never. */
    data class Chip(val label: String, val day: String?, val highlighted: Boolean, val enabled: Boolean = true)

    private val repeating get() = draft.recurrence != null
    private val backlog get() = draft.state == TaskState.BACKLOG
    private val dueMode get() = draft.recurrence?.dueMode

    /** An old custom deadline rule can't be set here, only switched to an offset. */
    private val deadlineLocked get() = dueMode == DueMode.RECURRING

    /** A per-occurrence deadline: so many days after each to-do date. */
    private val offset: Int? get() = draft.recurrence?.takeIf { it.dueMode == DueMode.OFFSET }?.dueOffsetDays

    /** The deadline drawn and tapped against; a per-occurrence one follows the to-do date. */
    private val deadline: String?
        get() {
            val o = offset
            val toDo = draft.toDoDate
            return if (o != null && toDo != null) Dates.addDays(toDo, o.toLong()) else draft.dueDate
        }

    val caption: String
        get() = when (armed) {
            Armed.TODO -> if (repeating) "A repeating task always has a to-do date" else "A TAP SETS TO-DO"
            Armed.DEADLINE -> "A TAP SETS THE DEADLINE"
            Armed.END -> "A TAP SETS THE END"
        }

    /** The line under the legend: what a tap does for the armed field, here. */
    val hint: String
        get() = when (armed) {
            Armed.TODO -> {
                val toDo = draft.toDoDate
                when {
                    backlog -> "Picking a day brings it out of Backlog."
                    repeating -> "A tap moves this occurrence's to-do date."
                    toDo == null -> "An undated task waits in Planning. Tap a day to plan it."
                    else -> "Tapping ${short(toDo)} again clears the to-do date. The task becomes undated" +
                        if (toDo == today) " and leaves Today." else "."
                }
            }
            Armed.DEADLINE -> when {
                deadlineLocked -> "An old custom rule sets these deadlines, so the calendar can't change them."
                offset != null -> "Each occurrence is due ${days(offset!!)} after its to-do date. A tap sets the gap."
                else -> "Deadlines are optional. \u201cNone\u201d removes the ring and the shading; the to-do date is untouched."
            }
            Armed.END -> "The series stops after the day you tap. \u201cNever\u201d keeps it going."
        }

    val toDoTile: String
        get() = when {
            backlog -> "Backlog"
            else -> draft.toDoDate?.let { if (it == today) "Today · ${short(it)}" else short(it) } ?: "Undated"
        }

    val deadlineTile: String
        get() {
            val day = deadline?.let(::short) ?: return "None"
            return offset?.let { "$day · +$it days each" } ?: day
        }

    val rail: List<Chip>
        get() = when (armed) {
            Armed.TODO -> toDoRail()
            Armed.DEADLINE -> deadlineRail()
            Armed.END -> endRail()
        }

    private fun toDoRail(): List<Chip> {
        val after = Dates.addDays(today, 2)
        return listOf(
            "Today" to today,
            "Tomorrow" to Dates.addDays(today, 1),
            Dates.WD[Dates.dayOfWeek(after)] to after,
            "Undated" to null,
        ).map { (label, day) ->
            Chip(
                label, day,
                highlighted = !backlog && day == draft.toDoDate,
                enabled = day != null || !repeating,
            )
        }
    }

    /** `<current> · +1 week · +2 weeks · None`, counted from the to-do date, else today. */
    private fun deadlineRail(): List<Chip> {
        val from = draft.toDoDate ?: today
        val current = deadline
        return listOfNotNull(
            current?.let { Chip(short(it), it, highlighted = true) },
            Chip("+1 week", Dates.addDays(from, 7), highlighted = false),
            Chip("+2 weeks", Dates.addDays(from, 14), highlighted = false),
            Chip("None", null, highlighted = current == null),
        ).map { if (deadlineLocked) it.copy(enabled = false) else it }
    }

    /** `+1 month · +3 months · End of year · Never`, counted from the to-do date. */
    private fun endRail(): List<Chip> {
        val from = Dates.parse(draft.toDoDate ?: today)
        val end = draft.recurrence?.endDate
        return listOf(
            "+1 month" to Dates.iso(from.plusMonths(1)),
            "+3 months" to Dates.iso(from.plusMonths(3)),
            "End of year" to Dates.iso(from.withDayOfYear(from.lengthOfYear())),
            "Never" to null,
        ).map { (label, day) -> Chip(label, day, highlighted = day == end) }
    }

    /** A tap on [day]: sets the armed field, or clears it when [day] is already its date. Dimmed days do nothing. */
    fun tap(day: String): TaskDraft = when {
        !mark(day).enabled -> draft
        armed == Armed.TODO -> setToDo(if (day == draft.toDoDate) null else day)
        armed == Armed.DEADLINE -> setDeadline(if (day == deadline) null else day)
        else -> setEnd(if (day == draft.recurrence?.endDate) null else day)
    }

    /** A quick pick: a highlighted or disabled chip does nothing. */
    fun pick(chip: Chip): TaskDraft = when {
        chip.highlighted || !chip.enabled -> draft
        armed == Armed.TODO -> setToDo(chip.day)
        armed == Armed.DEADLINE -> setDeadline(chip.day)
        else -> setEnd(chip.day)
    }

    /**
     * How one day is drawn: [toDo] filled, [deadline] ringed, [window] shaded
     * (strictly between the two, only when the deadline comes later), [today]
     * dotted, [end] a bar. Every mark shows whichever field is armed; [enabled]
     * false dims the day and its tap does nothing.
     */
    data class DayMark(
        val toDo: Boolean,
        val deadline: Boolean,
        val window: Boolean,
        val today: Boolean,
        val end: Boolean,
        val enabled: Boolean,
    )

    fun mark(day: String): DayMark {
        val toDo = draft.toDoDate
        val due = deadline
        return DayMark(
            toDo = day == toDo,
            deadline = day == due,
            window = toDo != null && due != null && toDo < day && day < due,
            today = day == today,
            end = day == draft.recurrence?.endDate,
            enabled = when (armed) {
                Armed.TODO -> true
                Armed.DEADLINE -> !deadlineLocked && !(offset != null && beforeToDo(day))
                Armed.END -> !beforeToDo(day)
            },
        )
    }

    /** What TalkBack reads for [day]: "Thursday 17 September, to-do date", then any other marks. */
    fun describe(day: String): String {
        val d = Dates.parse(day)
        val m = mark(day)
        val name = "${Dates.WD_FULL[Dates.dayOfWeek(d)]} ${d.dayOfMonth} ${Dates.MO_FULL[d.monthValue - 1]}"
        val marks = listOfNotNull(
            "to-do date".takeIf { m.toDo },
            "deadline".takeIf { m.deadline },
            "in the window".takeIf { m.window },
            "end date".takeIf { m.end },
            "today".takeIf { m.today },
            "not available".takeIf { !m.enabled },
        )
        return (listOf(name) + marks).joinToString(", ")
    }

    /** "You plan to work on this after it's due."; [fixable] false hides the [fix] button. */
    data class Conflict(val warning: String, val fixable: Boolean, val fix: String)

    /**
     * A one-off or fixed deadline strictly earlier than the to-do date. Never
     * blocks Save. The fix is hidden once the deadline is past: the honest fix
     * there is to move the deadline.
     */
    val conflict: Conflict?
        get() {
            val toDo = draft.toDoDate ?: return null
            val due = draft.dueDate ?: return null
            if (dueMode == DueMode.OFFSET || deadlineLocked || due >= toDo) return null
            return Conflict("You plan to work on this after it's due.", fixable = due >= today, fix = "Move the to-do date to ${short(due)}")
        }

    /** The one-tap fix: the to-do date moves to the deadline. */
    fun fix(): TaskDraft = if (conflict?.fixable == true) setToDo(draft.dueDate) else draft

    private fun beforeToDo(day: String) = draft.toDoDate?.let { day < it } ?: false

    /**
     * A repeating task always has a to-do date; a date brings Backlog back to
     * Open (FR-BACKLOG-5); a per-occurrence deadline moves along.
     */
    private fun setToDo(day: String?): TaskDraft = when {
        day == null && (repeating || backlog) -> draft
        else -> draft.copy(
            toDoDate = day,
            state = if (day != null && backlog) TaskState.OPEN else draft.state,
            dueDate = offset?.let { o -> day?.let { Dates.addDays(it, o.toLong()) } } ?: draft.dueDate,
        )
    }

    /**
     * A one-off takes the date as is. On a repeating task the first deadline
     * is a fixed one, a per-occurrence one becomes the gap from the to-do date
     * (capped), and clearing either leaves none; an old custom rule stays put.
     */
    private fun setDeadline(day: String?): TaskDraft {
        val rec = draft.recurrence ?: return draft.copy(dueDate = day)
        val toDo = draft.toDoDate
        return when {
            deadlineLocked -> draft
            day == null -> draft.copy(dueDate = null, recurrence = rec.copy(dueMode = DueMode.NONE, dueOffsetDays = null))
            offset != null && toDo != null -> {
                val gap = Dates.dayDiff(day, toDo).toInt().coerceIn(0, Recurrence.MAX_DUE_OFFSET_DAYS)
                draft.copy(dueDate = Dates.addDays(toDo, gap.toLong()), recurrence = rec.copy(dueOffsetDays = gap))
            }
            // the first fixed deadline is the series' end, so an earlier end date goes (as RepeatCard's One fixed)
            dueMode == DueMode.SINGULAR && draft.dueDate != null -> draft.copy(dueDate = day)
            else -> draft.copy(dueDate = day, recurrence = rec.copy(dueMode = DueMode.SINGULAR, endDate = null))
        }
    }

    private fun setEnd(day: String?): TaskDraft {
        val rec = draft.recurrence ?: return draft
        return draft.copy(recurrence = rec.copy(endDate = day))
    }

    companion object {
        /** The to-do date's month, else the deadline's, else this month. Switching tiles never moves it. */
        fun openingMonth(draft: TaskDraft, today: String): YearMonth =
            YearMonth.from(Dates.parse(draft.toDoDate ?: draft.dueDate ?: today))
    }

    private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

    private fun short(iso: String) = "${Dates.WD[Dates.dayOfWeek(iso)]} ${Dates.parse(iso).dayOfMonth}"
}
