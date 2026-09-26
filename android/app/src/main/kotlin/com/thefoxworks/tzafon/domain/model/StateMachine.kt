package com.thefoxworks.tzafon.domain.model

/**
 * How far a status change on a repeating task reaches (v2.13.0): the
 * occurrence alone, or its whole series. Only asked where
 * [StateMachine.askFor] says so; elsewhere it has no effect.
 */
enum class StatusScope { THIS_ONE, WHOLE_SERIES }

/**
 * The facts about a repeating task's series that the scope rules read.
 * A one-off task has none (pass null).
 */
data class SeriesStatus(
    /** FR-REC-5: the series is on hold and generates nothing */
    val frozen: Boolean,
    /** the series has an end date of today or earlier (a closed series) */
    val ended: Boolean,
) {
    companion object {
        fun of(series: Series, today: String) = SeriesStatus(
            frozen = series.frozen,
            ended = series.endDate != null && series.endDate <= today,
        )
    }
}

/**
 * DM-TASK-1/2/3 — the task state machine, pure and unit-tested.
 *
 * Skip is non-punitive (D3): Closing / Freezing / Backlogging stops a task's
 * contribution but never severs its Theme/Goal/Habit links.
 *
 * v2.13.0 series rules (FR-REC-5, DM-TASK-2 refined):
 * - Frozen on a repeating task always freezes the whole series; it never asks.
 * - Thawing (Frozen → Open/Done/Closed) asks for a scope while the series is
 *   frozen; a leftover Frozen occurrence of a live series thaws alone.
 * - Closed on a repeating task asks; the whole series closes it.
 * - A Closed occurrence of an ended series asks before Open/Done; the whole
 *   series reopens it.
 * - Open and Done otherwise touch the occurrence only.
 * - A whole-series choice applies even when the occurrence is already in the
 *   target state.
 */
object StateMachine {

    /** What a transition needs beyond the state flip itself. */
    data class Effects(
        val newState: TaskState,
        /** stamp completedAt (Done) */
        val setCompleted: Boolean = false,
        /** clear completedAt (leaving Done) */
        val clearCompleted: Boolean = false,
        /** run Done actions: habit tick + goal attribution (M4/M5 hooks) */
        val applyContribution: Boolean = false,
        /** reverse any prior contribution exactly (DM-ATTR provenance) */
        val reverseContribution: Boolean = false,
        /** FR-REC-5: freeze the series, dropping its later Open occurrences */
        val freezeSeries: Boolean = false,
        /** DM-TASK-2: thaw the series and fill it in again from today */
        val thawSeries: Boolean = false,
        /** end the series today: settle what's past, drop what's later */
        val closeSeries: Boolean = false,
        /** clear a closed series' end date and fill it in again from today */
        val reopenSeries: Boolean = false,
        /** FR-BACKLOG-1: Backlog is unscheduled by definition */
        val clearToDoDate: Boolean = false,
    )

    /** What the UI must ask before applying a status change, if anything. */
    sealed interface Ask {
        /** "This one / whole series" — pass the answer to [transition]. */
        data object Scope : Ask

        /** FR-BACKLOG-2 — a live series can't enter Backlog; offer Frozen. */
        data object RecurringCannotBacklog : Ask
    }

    /** Returns the question the UI must put before applying, if any. */
    fun askFor(current: TaskState, target: TaskState, series: SeriesStatus?): Ask? = when {
        series == null -> null
        target == TaskState.BACKLOG -> Ask.RecurringCannotBacklog
        target == TaskState.CLOSED -> Ask.Scope
        current == TaskState.FROZEN && target != TaskState.FROZEN && series.frozen -> Ask.Scope
        current == TaskState.CLOSED && target != TaskState.FROZEN && series.ended -> Ask.Scope
        else -> null
    }

    /**
     * The effect table. Throws on the one forbidden move (recurring→Backlog);
     * every other pair is legal — "every state is reversible" (PRIN-2).
     * [series] is null for a one-off task; [scope] only matters where
     * [askFor] returns [Ask.Scope].
     */
    fun transition(
        current: TaskState,
        target: TaskState,
        series: SeriesStatus?,
        scope: StatusScope = StatusScope.THIS_ONE,
    ): Effects {
        require(!(target == TaskState.BACKLOG && series != null)) {
            "A recurring task cannot enter Backlog (FR-BACKLOG-2) — offer Frozen instead"
        }
        val whole = series != null && scope == StatusScope.WHOLE_SERIES
        val seriesWide = Effects(
            newState = target,
            freezeSeries = target == TaskState.FROZEN && series != null,
            closeSeries = target == TaskState.CLOSED && whole,
        )
        if (current == target) return seriesWide

        // FR-BACKLOG-3: Backlog → Frozen is a no-op — both are "parked"
        if (current == TaskState.BACKLOG && target == TaskState.FROZEN) {
            return Effects(TaskState.BACKLOG)
        }

        val wasDone = current == TaskState.DONE
        // thaw / reopen → Open or Done: the original To Do Date is preserved
        val resumes = target == TaskState.OPEN || target == TaskState.DONE
        val thaws = resumes && whole && current == TaskState.FROZEN && series!!.frozen
        val reopens = resumes && whole && current == TaskState.CLOSED && series!!.ended

        return when (target) {
            TaskState.OPEN -> seriesWide.copy(
                clearCompleted = wasDone,
                reverseContribution = wasDone,
                thawSeries = thaws,
                reopenSeries = reopens,
            )

            TaskState.DONE -> seriesWide.copy(
                setCompleted = true,
                applyContribution = true,
                thawSeries = thaws,
                reopenSeries = reopens,
            )

            TaskState.CLOSED, TaskState.FROZEN -> seriesWide.copy(
                clearCompleted = wasDone,
                reverseContribution = wasDone,
            )

            TaskState.BACKLOG -> Effects(
                newState = TaskState.BACKLOG,
                clearCompleted = wasDone,
                // Done/Closed → Backlog is "resurrect for someday": clears the
                // mark and reverses any prior contribution (FR-BACKLOG-2)
                reverseContribution = wasDone,
                clearToDoDate = true,
            )
        }
    }
}
