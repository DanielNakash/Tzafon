package com.thefoxworks.tzafon.domain.status

import com.thefoxworks.tzafon.domain.editor.DraftDiff
import com.thefoxworks.tzafon.domain.editor.ScopeSheetCopy
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.TaskState

/** The task the status sheet is opened on, as its header and rows read it. */
data class StatusSubject(
    val title: String,
    val state: TaskState,
    val toDoDate: String?,
    /** the repeat summary ("Every Tuesday & Thursday"); null for a one-off task */
    val repeat: String? = null,
    /** the series' facts; null for a one-off task */
    val series: SeriesStatus? = null,
    /** the linked habit's name */
    val habit: String? = null,
    /** the linked goals' titles */
    val goals: List<String> = emptyList(),
)

/** One of the sheet's five rows. A disabled row is dimmed and says why in its [tag]. */
data class StatusRow(
    val status: TaskState,
    val name: String,
    val description: String,
    val now: Boolean = false,
    val enabled: Boolean = true,
    val tag: String? = null,
)

/** One choice on the scope panel: its label, what it does, and the button it puts in the footer. */
data class ScopeOption(
    val scope: StatusScope,
    val label: String,
    val line: String,
    val action: String,
    val enabled: Boolean = true,
)

/** What the sheet swaps to when a pick reaches a series (slide 8): no count badge, Back always. */
data class ScopePanel(
    val target: TaskState,
    val heading: String,
    val options: List<ScopeOption>,
    val default: StatusScope,
) {
    fun option(scope: StatusScope): ScopeOption = options.first { it.scope == scope }
}

/** What picking a status does. */
sealed interface StatusPick {
    /** A change to just this task: apply it now (from a list) or put it in the draft (in the editor). */
    data class Apply(val target: TaskState) : StatusPick

    /** A change that reaches a series, or can't be undone: nothing applies until the panel's button. */
    data class AskScope(val panel: ScopePanel) : StatusPick

    /** The current status, or the dimmed Backlog row: nothing happens. */
    data object Stay : StatusPick
}

/**
 * v2.13.0 — the status sheet's words and rules, pure. One set of
 * descriptions for one-off and repeating tasks; Frozen on a repeating task
 * and Done (built from the task's links) state the facts that differ.
 */
object StatusSheet {

    /** The deck's order: Done first, Backlog last. */
    private val ORDER = listOf(TaskState.DONE, TaskState.OPEN, TaskState.CLOSED, TaskState.FROZEN, TaskState.BACKLOG)

    fun name(status: TaskState): String = when (status) {
        TaskState.OPEN -> "Open"
        TaskState.DONE -> "Done"
        TaskState.CLOSED -> "Closed"
        TaskState.FROZEN -> "Frozen"
        TaskState.BACKLOG -> "Backlog"
    }

    fun rows(s: StatusSubject): List<StatusRow> {
        val repeats = s.series != null
        return ORDER.map { status ->
            StatusRow(
                status = status,
                name = name(status),
                description = when (status) {
                    TaskState.DONE -> doneLine(listOfNotNull(s.habit) + s.goals)
                    TaskState.OPEN -> "Back on your list."
                    TaskState.CLOSED -> "No longer relevant. Kept in history, never counted as a miss."
                    TaskState.FROZEN ->
                        if (repeats) "Freezes the whole series until you thaw it."
                        else "On hold and hidden from the calendar until you thaw it."
                    TaskState.BACKLOG -> "Off the calendar, into the backlog."
                },
                now = status == s.state,
                enabled = !(repeats && status == TaskState.BACKLOG),
                tag = if (repeats && status == TaskState.BACKLOG) "REPEATS — USE FREEZE" else null,
            )
        }
    }

    /**
     * The header's meta line: `THU 17 SEP · EVERY TUESDAY & THURSDAY · TICKS RUN`.
     * The last part is the label the list row shows, which names only a habit.
     */
    fun meta(s: StatusSubject): String = listOfNotNull(
        s.toDoDate?.let { DraftDiff.day(it) } ?: "NO DATE",
        s.repeat,
        s.habit?.let { "TICKS $it" },
    ).joinToString(" · ").uppercase()

    fun footnote(s: StatusSubject): List<String> = listOfNotNull(
        "Backlog is unavailable while this repeats. Freeze it instead, or turn the repeat off in the editor."
            .takeIf { s.series != null },
        "Nothing here is a failure.",
    )

    /**
     * What picking [target] does to a task in [current] ([series] is null for
     * a one-off task). A scope is asked where [StateMachine.askFor] says so,
     * and Freeze series always confirms, as it can't be undone. The current
     * status's row does nothing, unless the series can still follow it:
     * Frozen while the series isn't, Closed while the series runs.
     */
    fun pick(current: TaskState, target: TaskState, series: SeriesStatus?): StatusPick {
        if (series == null) return if (target == current) StatusPick.Stay else StatusPick.Apply(target)
        return when {
            target == TaskState.BACKLOG -> StatusPick.Stay
            target == TaskState.FROZEN && !series.frozen -> StatusPick.AskScope(freeze)
            target == TaskState.CLOSED && current == TaskState.CLOSED && !series.ended ->
                StatusPick.AskScope(close(thisOneDone = true))
            target == current -> StatusPick.Stay
            StateMachine.askFor(current, target, series) != StateMachine.Ask.Scope -> StatusPick.Apply(target)
            target == TaskState.CLOSED -> StatusPick.AskScope(close(thisOneDone = false))
            current == TaskState.FROZEN -> StatusPick.AskScope(
                resume(target, "THAW WHAT?", "Thaw series", "Occurrences come back from today. The days it spent frozen stay gone."),
            )
            else -> StatusPick.AskScope(
                resume(target, "REOPEN WHAT?", "Reopen series", "Occurrences come back from today. Past ones are never changed."),
            )
        }
    }

    private const val ONLY_THIS = "Only this occurrence changes."
    private val THIS_ONE = ScopeSheetCopy.label(EditScope.ONE)
    private val WHOLE = ScopeSheetCopy.label(EditScope.ALL)

    private val freeze = ScopePanel(
        target = TaskState.FROZEN,
        heading = "FREEZE THE SERIES",
        options = listOf(
            ScopeOption(StatusScope.WHOLE_SERIES, WHOLE, "No occurrences until you thaw it. This one stays, frozen.", "Freeze series"),
        ),
        default = StatusScope.WHOLE_SERIES,
    )

    /** [thisOneDone]: the occurrence is already Closed, so only the series is left to close. */
    private fun close(thisOneDone: Boolean) = ScopePanel(
        target = TaskState.CLOSED,
        heading = "CLOSE WHAT?",
        options = listOf(
            ScopeOption(StatusScope.THIS_ONE, THIS_ONE, ONLY_THIS, "Close this one", enabled = !thisOneDone),
            ScopeOption(
                StatusScope.WHOLE_SERIES, WHOLE,
                "Ends the series today. Past ones not done are closed; later ones stop appearing.", "Close series",
            ),
        ),
        default = if (thisOneDone) StatusScope.WHOLE_SERIES else StatusScope.THIS_ONE,
    )

    /** Thaw or reopen to Open or Done. */
    private fun resume(target: TaskState, heading: String, series: String, line: String) = ScopePanel(
        target = target,
        heading = heading,
        options = listOf(
            ScopeOption(StatusScope.THIS_ONE, THIS_ONE, ONLY_THIS, if (target == TaskState.DONE) "Mark done" else "Mark open"),
            ScopeOption(StatusScope.WHOLE_SERIES, WHOLE, line, series),
        ),
        default = StatusScope.THIS_ONE,
    )

    /** "Counts toward A and B." from the habit then the goals; past two, "A, B and N more". */
    private fun doneLine(links: List<String>): String = when (links.size) {
        0 -> "Finished. Files into Journey."
        1 -> "Counts toward ${links[0]}."
        2 -> "Counts toward ${links[0]} and ${links[1]}."
        else -> "Counts toward ${links[0]}, ${links[1]} and ${links.size - 2} more."
    }
}
