package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.domain.recurrence.Rule
import com.thefoxworks.tzafon.domain.recurrence.SeriesSpec
import java.time.LocalDate

/** The repeat card's type segment: `Weekly · Monthly · Every N days`. */
enum class RepeatType { WEEKLY, MONTHLY, EVERY_N }

/**
 * A repeating task's deadline as the card's Deadline half reads it: none,
 * one fixed date for the whole series, an offset after each occurrence's
 * to-do date, or an older series' deadline rule (read-only until switched
 * to an offset).
 */
enum class DeadlineKind { NONE, FIXED, PER_OCCURRENCE, CUSTOM_RULE }

/** What the card's body shows: the rule, or the Ends or Deadline settings swapped in its place. */
enum class CardBody { RULE, ENDS, DEADLINE }

/** Where the card was tapped: a footer half, the folded row, or the "‹ Repeat" link back. */
enum class CardSpot { ENDS, DEADLINE, FOLDED, BACK }

/** What a tap on the card leads to. */
sealed interface CardTap {
    data class Show(val body: CardBody) : CardTap
    /** "Deadline none": arm the calendar's Deadline tile, so a date is set before a mode is chosen. */
    data object ArmDeadline : CardTap
    data object Nothing : CardTap
}

/**
 * v2.13.0 — the Timing pane's inline repeat card (deck slides 3 and 5), as
 * pure logic over the editor's draft. [of] reads a draft into what the card
 * shows; the other functions are the card's controls, each taking the draft
 * and giving back the edited one.
 *
 * The segment maps one-to-one onto rule patterns: Weekly is `WEEKDAY`,
 * Monthly is `MONTHDAY` and Every N days is `INTERVAL` by the day. An older
 * `INTERVAL` rule by the week or month shows under Every N with its own
 * unit, read-only, until another type is picked; it's never rewritten.
 */
object RepeatCard {

    /** The "every [N] weeks" stepper under the type's body. */
    data class Every(val n: Int, val unit: String)

    /** Monthly's two choices, "On the 17th" and "On the 3rd Thursday", and which one the rule uses. */
    data class Monthly(val onDate: String, val onWeekday: String, val mode: MonthMode)

    /** One half of the footer, "Ends never" or "Deadline one fixed": its words and whether a tap opens it. */
    data class Half(val label: String, val value: String, val tappable: Boolean)

    /** The per-occurrence offset stepper's range, in days after each to-do date. */
    val OFFSET_RANGE = 0..Recurrence.MAX_DUE_OFFSET_DAYS

    /** What the card shows for a repeating draft. */
    data class View(
        /** the header's "Every Tue & Thu" */
        val summary: String,
        val type: RepeatType,
        /** an old `INTERVAL` rule by the week or month: shown, not editable */
        val readOnly: Boolean,
        val every: Every,
        /** Weekly's chips, Sunday-based 0..6 */
        val weekdays: Set<Int>,
        /** null unless Monthly */
        val monthly: Monthly?,
        val ends: Half,
        val deadline: Half,
        val deadlineKind: DeadlineKind,
        /** the per-occurrence offset; null for any other kind */
        val offset: Int?,
        /** the body shown: the one asked for, or the rule when that one can't open */
        val body: CardBody,
        /** the one-row card while the Deadline tile is armed, e.g. "Every Tue & Thu · ONE FIXED DEADLINE"; null unfolded */
        val folded: String?,
        /** the callout under the folded card for one fixed deadline ([fixedCallout]), else null */
        val callout: String?,
    )

    /**
     * The card for [draft], or null when it doesn't repeat. [body] is the
     * body the card has swapped in. While [deadlineArmed] (the calendar's
     * Deadline tile), the card folds to one row, unless its Deadline body is
     * open.
     */
    fun of(draft: TaskDraft, body: CardBody = CardBody.RULE, deadlineArmed: Boolean = false): View? {
        val rule = draft.recurrence?.rule ?: return null
        val toDo = Dates.parse(draft.toDoDate ?: draft.occurrenceDate ?: return null)
        val type = typeOf(rule)
        val kind = kindOf(draft)
        val shown = if (tap(draft, spotOf(body)) == CardTap.Show(body)) body else CardBody.RULE
        val folded = deadlineArmed && shown != CardBody.DEADLINE
        return View(
            summary = summary(rule),
            type = type,
            readOnly = rule.pattern == Pattern.INTERVAL && rule.unit != IntervalUnit.DAY,
            every = when (rule.pattern) {
                Pattern.WEEKDAY -> Every(rule.weekInterval, plural(rule.weekInterval, "week"))
                Pattern.MONTHDAY -> Every(rule.monthInterval, plural(rule.monthInterval, "month"))
                Pattern.INTERVAL -> Every(rule.interval, plural(rule.interval, rule.unit.name.lowercase()))
            },
            weekdays = rule.weekdays.toSet(),
            monthly = if (type != RepeatType.MONTHLY) null else {
                val own = rule.monthMode
                val anchored = anchored(Pattern.MONTHDAY, rule, toDo)
                Monthly(
                    onDate = "On the " + Dates.ordinal((if (own == MonthMode.DATE) rule else anchored).monthDate),
                    onWeekday = "On the " + weekdayPosition(if (own == MonthMode.WEEKDAY) rule else anchored.copy(monthMode = MonthMode.WEEKDAY)),
                    mode = own,
                )
            },
            ends = if (kind == DeadlineKind.FIXED) {
                Half("Ends", "at the deadline", tappable = false)
            } else {
                Half("Ends", draft.recurrence.endDate?.let { "on " + DraftDiff.day(it) } ?: "never", tappable = true)
            },
            deadline = Half(
                "Deadline",
                when (kind) {
                    DeadlineKind.NONE -> "none"
                    DeadlineKind.FIXED -> "one fixed"
                    DeadlineKind.PER_OCCURRENCE -> offsetWords(offsetOf(draft))
                    DeadlineKind.CUSTOM_RULE -> "custom rule"
                },
                tappable = true,
            ),
            deadlineKind = kind,
            offset = if (kind == DeadlineKind.PER_OCCURRENCE) offsetOf(draft) else null,
            body = shown,
            folded = if (!folded) null else summary(rule) + " · " + when (kind) {
                DeadlineKind.NONE -> "NO DEADLINE"
                DeadlineKind.FIXED -> "ONE FIXED DEADLINE"
                DeadlineKind.PER_OCCURRENCE -> "DEADLINE " + offsetWords(offsetOf(draft)).uppercase()
                DeadlineKind.CUSTOM_RULE -> "CUSTOM DEADLINE RULE"
            },
            callout = if (folded && kind == DeadlineKind.FIXED) fixedCallout(rule, draft.dueDate!!) else null,
        )
    }

    /**
     * A tap on the card. Ends and Deadline swap their body in; "‹ Repeat"
     * goes back to the rule. A locked Ends (a fixed deadline) does nothing.
     * With no deadline yet, the Deadline half arms the calendar's Deadline
     * tile instead, and the folded row (already armed) does nothing;
     * otherwise the folded row opens the Deadline body.
     */
    fun tap(draft: TaskDraft, spot: CardSpot): CardTap {
        if (draft.recurrence == null) return CardTap.Nothing
        val kind = kindOf(draft)
        return when (spot) {
            CardSpot.BACK -> CardTap.Show(CardBody.RULE)
            CardSpot.ENDS -> if (kind == DeadlineKind.FIXED) CardTap.Nothing else CardTap.Show(CardBody.ENDS)
            CardSpot.DEADLINE -> if (kind == DeadlineKind.NONE) CardTap.ArmDeadline else CardTap.Show(CardBody.DEADLINE)
            CardSpot.FOLDED -> if (kind == DeadlineKind.NONE) CardTap.Nothing else CardTap.Show(CardBody.DEADLINE)
        }
    }

    private fun spotOf(body: CardBody) = when (body) {
        CardBody.RULE -> CardSpot.BACK
        CardBody.ENDS -> CardSpot.ENDS
        CardBody.DEADLINE -> CardSpot.DEADLINE
    }

    /**
     * Picks a type. The new type's days are anchored on the to-do date (its
     * weekday; its day of the month), and the "every N" step carries over.
     * Picking the type already shown changes nothing, so an old rule stays as
     * it is.
     */
    fun type(draft: TaskDraft, type: RepeatType): TaskDraft {
        val rec = draft.recurrence ?: return draft
        if (typeOf(rec.rule) == type) return draft
        val toDo = Dates.parse(draft.toDoDate ?: return draft)
        val step = stepOf(rec.rule)
        val pattern = when (type) {
            RepeatType.WEEKLY -> Pattern.WEEKDAY
            RepeatType.MONTHLY -> Pattern.MONTHDAY
            RepeatType.EVERY_N -> Pattern.INTERVAL
        }
        return draft.copy(recurrence = rec.copy(rule = withStep(anchored(pattern, rec.rule, toDo), step)))
    }

    /** The "every N" stepper, for the shown type; at least 1. An old rule doesn't change. */
    fun step(draft: TaskDraft, n: Int): TaskDraft = editRule(draft) { withStep(it, n.coerceAtLeast(1)) }

    /** A Weekly chip. The last chosen day can't be taken away: a weekly repeat needs one. */
    fun weekday(draft: TaskDraft, day: Int): TaskDraft {
        val rule = draft.recurrence?.rule ?: return draft
        if (rule.pattern != Pattern.WEEKDAY) return draft
        val days = rule.weekdays.toSet()
        val next = if (day in days) days - day else days + day
        if (next.isEmpty()) return draft
        return editRule(draft) { it.copy(weekdays = next.sorted()) }
    }

    /** Monthly's choice: "On the 17th" or "On the 3rd Thursday", both read off the to-do date. */
    fun monthly(draft: TaskDraft, mode: MonthMode): TaskDraft {
        val rule = draft.recurrence?.rule ?: return draft
        if (rule.pattern != Pattern.MONTHDAY || rule.monthMode == mode) return draft
        val toDo = Dates.parse(draft.toDoDate ?: return draft)
        return editRule(draft) { anchored(Pattern.MONTHDAY, it, toDo).copy(monthMode = mode, monthInterval = it.monthInterval) }
    }

    /** Ends: `Never` (null) or on a date. A fixed deadline is the end, so Ends can't change then. */
    fun ends(draft: TaskDraft, endDate: String?): TaskDraft {
        val rec = draft.recurrence ?: return draft
        if (kindOf(draft) == DeadlineKind.FIXED) return draft
        return draft.copy(recurrence = rec.copy(endDate = endDate))
    }

    /**
     * The Deadline body's `One fixed · Per occurrence`. Per occurrence keeps
     * the gap to this occurrence's deadline as the offset (held to
     * [OFFSET_RANGE]); one fixed keeps this occurrence's deadline as the date,
     * which also ends the series. No deadline, or an old rule, doesn't switch.
     */
    fun deadlineKind(draft: TaskDraft, kind: DeadlineKind): TaskDraft {
        val rec = draft.recurrence ?: return draft
        val from = kindOf(draft)
        if (from == kind || from == DeadlineKind.NONE || from == DeadlineKind.CUSTOM_RULE) return draft
        return when (kind) {
            DeadlineKind.FIXED -> draft.copy(recurrence = rec.copy(dueMode = DueMode.SINGULAR, dueOffsetDays = null, endDate = null))
            DeadlineKind.PER_OCCURRENCE -> {
                val toDo = draft.toDoDate ?: return draft
                withOffset(draft.copy(recurrence = rec.copy(dueMode = DueMode.OFFSET)), Dates.dayDiff(draft.dueDate!!, toDo).toInt())
            }
            DeadlineKind.NONE, DeadlineKind.CUSTOM_RULE -> draft
        }
    }

    /** The per-occurrence offset stepper, held to [OFFSET_RANGE]; it moves this occurrence's deadline with it. */
    fun offset(draft: TaskDraft, days: Int): TaskDraft =
        if (kindOf(draft) != DeadlineKind.PER_OCCURRENCE) draft else withOffset(draft, days)

    /**
     * "Switch to offset", the only way out of an old deadline rule: the offset
     * is the gap between [series]' next occurrence and its deadline
     * ([Recurrence.switchToOffsetDays]).
     */
    fun switchToOffset(draft: TaskDraft, series: SeriesSpec, today: String): TaskDraft {
        val rec = draft.recurrence ?: return draft
        if (kindOf(draft) != DeadlineKind.CUSTOM_RULE) return draft
        val moved = draft.copy(recurrence = rec.copy(dueMode = DueMode.OFFSET, dueRule = null))
        return withOffset(moved, Recurrence.switchToOffsetDays(series, today))
    }

    /**
     * The header's on/off toggle. Turning repeat on dates the draft, since a
     * repeating task always has a to-do date: a Backlog draft becomes Open
     * today (`FR-BACKLOG-5.3`), an undated one is set to today, and the rule
     * is anchored on that date. A draft that already has a deadline keeps it
     * as one fixed deadline. Turning repeat off clears only the rule.
     */
    fun toggle(draft: TaskDraft, today: String): TaskDraft {
        if (draft.recurrence != null) return draft.copy(recurrence = null)
        val backlog = draft.state == TaskState.BACKLOG
        val toDo = draft.toDoDate.takeUnless { backlog } ?: today
        return draft.copy(
            state = if (backlog) TaskState.OPEN else draft.state,
            toDoDate = toDo,
            recurrence = RecurrenceDraft(
                rule = Recurrence.defaultRule(Dates.parse(toDo)),
                dueMode = if (draft.dueDate != null) DueMode.SINGULAR else DueMode.NONE,
            ),
        )
    }

    // ── deadlines ─────────────────────────────────────────────

    private fun kindOf(draft: TaskDraft): DeadlineKind = when (draft.recurrence?.dueMode) {
        DueMode.SINGULAR -> if (draft.dueDate != null) DeadlineKind.FIXED else DeadlineKind.NONE
        DueMode.OFFSET -> DeadlineKind.PER_OCCURRENCE
        DueMode.RECURRING -> DeadlineKind.CUSTOM_RULE
        DueMode.NONE, null -> DeadlineKind.NONE
    }

    private fun offsetOf(draft: TaskDraft) = draft.recurrence?.dueOffsetDays ?: 0

    /** Sets the offset (held to [OFFSET_RANGE]) and this occurrence's deadline that far after its to-do date. */
    private fun withOffset(draft: TaskDraft, days: Int): TaskDraft {
        val rec = draft.recurrence ?: return draft
        val toDo = draft.toDoDate ?: return draft
        val n = days.coerceIn(OFFSET_RANGE)
        return draft.copy(dueDate = Dates.addDays(toDo, n.toLong()), recurrence = rec.copy(dueOffsetDays = n))
    }

    private fun offsetWords(n: Int) = "+$n ${plural(n, "day")} each"

    // ── rules ─────────────────────────────────────────────────

    private fun typeOf(rule: Rule) = when (rule.pattern) {
        Pattern.WEEKDAY -> RepeatType.WEEKLY
        Pattern.MONTHDAY -> RepeatType.MONTHLY
        Pattern.INTERVAL -> RepeatType.EVERY_N
    }

    private fun stepOf(rule: Rule) = when (rule.pattern) {
        Pattern.WEEKDAY -> rule.weekInterval
        Pattern.MONTHDAY -> rule.monthInterval
        Pattern.INTERVAL -> rule.interval
    }

    private fun withStep(rule: Rule, n: Int) = when (rule.pattern) {
        Pattern.WEEKDAY -> rule.copy(weekInterval = n)
        Pattern.MONTHDAY -> rule.copy(monthInterval = n)
        Pattern.INTERVAL -> rule.copy(interval = n)
    }

    /** Edits a rule the card can edit; an old read-only rule is left alone. */
    private fun editRule(draft: TaskDraft, edit: (Rule) -> Rule): TaskDraft {
        val rec = draft.recurrence ?: return draft
        if (rec.rule.pattern == Pattern.INTERVAL && rec.rule.unit != IntervalUnit.DAY) return draft
        return draft.copy(recurrence = rec.copy(rule = edit(rec.rule)))
    }

    /**
     * [rule] switched to [pattern], with that pattern's days taken from
     * [toDo]: its weekday, its day of the month, and its weekday's place in
     * the month (a fifth one is the last). Every N is by the day.
     */
    private fun anchored(pattern: Pattern, rule: Rule, toDo: LocalDate): Rule {
        val weekday = Dates.dayOfWeek(toDo)
        val position = (toDo.dayOfMonth - 1) / 7 + 1
        return when (pattern) {
            Pattern.WEEKDAY -> rule.copy(pattern = pattern, weekdays = listOf(weekday))
            Pattern.MONTHDAY -> rule.copy(
                pattern = pattern,
                monthMode = MonthMode.DATE,
                monthDate = toDo.dayOfMonth,
                monthWeekPos = if (position > 4) -1 else position,
                monthWeekday = weekday,
            )
            Pattern.INTERVAL -> rule.copy(pattern = pattern, unit = IntervalUnit.DAY)
        }
    }

    // ── words ─────────────────────────────────────────────────

    /**
     * The header's summary. Weekly abbreviates its days ("Every Tue & Thu");
     * the other types read as [Recurrence.summary] does.
     */
    private fun summary(rule: Rule): String {
        if (rule.pattern != Pattern.WEEKDAY || rule.weekdays.size !in 1..2) return Recurrence.summary(rule)
        val days = rule.weekdays.sorted().joinToString(" & ") { Dates.WD[it] }
        return "Every $days" + if (rule.weekInterval > 1) ", every ${rule.weekInterval} weeks" else ""
    }

    /**
     * The deck's callout, filled in: the deadline's day, and the rule's
     * weekdays in full ("Tuesday and Thursday"), or "occurrence" for any
     * other rule.
     */
    private fun fixedCallout(rule: Rule, deadline: String): String {
        val days = rule.weekdays.sorted().map { Dates.WD_FULL[it] }
        val every = if (rule.pattern != Pattern.WEEKDAY || days.isEmpty() || days.size == 7) "occurrence"
        else if (days.size == 1) days[0]
        else days.dropLast(1).joinToString(", ") + " and " + days.last()
        val day = Dates.parse(deadline)
        return "One fixed deadline also ends the series — no occurrences are generated after " +
            "${Dates.WD[Dates.dayOfWeek(day)]} ${day.dayOfMonth}. " +
            "Switch the repeat to per occurrence to give every $every its own."
    }

    /** "3rd Thursday" or "last Friday". */
    private fun weekdayPosition(rule: Rule): String =
        (if (rule.monthWeekPos == -1) "last" else Dates.ordinal(rule.monthWeekPos)) + " " + Dates.WD_FULL[rule.monthWeekday]

    private fun plural(n: Int, unit: String) = if (n == 1) unit else "${unit}s"
}
