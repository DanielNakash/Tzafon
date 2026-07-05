package com.thefoxworks.tzafon.domain.model

/**
 * DM-TASK-1/2/3 — the task state machine, pure and unit-tested.
 *
 * Skip is non-punitive (D3): Closing / Freezing / Backlogging stops a task's
 * contribution but never severs its Theme/Goal/Habit links. Thaw (DM-TASK-2)
 * lets a Frozen task resume Open (dates preserved, series restored), complete
 * as Done, or settle as Closed.
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
        /** FR-REC-5: terminate the series (needs user confirmation first) */
        val freezeSeries: Boolean = false,
        /** DM-TASK-2: restore the series on thaw (topUp regenerates forward) */
        val restoreSeries: Boolean = false,
        /** FR-BACKLOG-1: Backlog is unscheduled by definition */
        val clearToDoDate: Boolean = false,
    )

    /** Transitions requiring a confirmation prompt before they run. */
    sealed interface Guard {
        /** FR-REC-5 — freezing a recurring task stops the series. */
        data object ConfirmFreezeSeries : Guard

        /** FR-BACKLOG-2 — a live series can't be "someday"; offer Frozen. */
        data object RecurringCannotBacklog : Guard
    }

    /** Returns the guard the UI must clear before applying, if any. */
    fun guardFor(current: TaskState, target: TaskState, isRecurring: Boolean): Guard? = when {
        target == TaskState.FROZEN && isRecurring && current != TaskState.FROZEN ->
            Guard.ConfirmFreezeSeries
        target == TaskState.BACKLOG && isRecurring ->
            Guard.RecurringCannotBacklog
        else -> null
    }

    /**
     * The effect table. Throws on the one forbidden move (recurring→Backlog);
     * every other pair is legal — "every state is reversible" (PRIN-2).
     */
    fun transition(current: TaskState, target: TaskState, isRecurring: Boolean): Effects {
        require(!(target == TaskState.BACKLOG && isRecurring)) {
            "A recurring task cannot enter Backlog (FR-BACKLOG-2) — offer Frozen instead"
        }
        if (current == target) return Effects(target)

        val wasDone = current == TaskState.DONE
        val wasFrozen = current == TaskState.FROZEN

        return when (target) {
            TaskState.OPEN -> Effects(
                newState = TaskState.OPEN,
                clearCompleted = wasDone,
                reverseContribution = wasDone,
                // thaw → Open: original To Do Date preserved (no date change),
                // recurrence restored, next occurrence generated Open
                restoreSeries = wasFrozen && isRecurring,
            )

            TaskState.DONE -> Effects(
                newState = TaskState.DONE,
                setCompleted = true,
                applyContribution = true,
                // thaw → Done also restores recurrence (DM-TASK-2)
                restoreSeries = wasFrozen && isRecurring,
            )

            TaskState.CLOSED -> Effects(
                newState = TaskState.CLOSED,
                clearCompleted = wasDone,
                reverseContribution = wasDone,
                // thaw → Closed just marks Closed; the series stays terminated
            )

            TaskState.FROZEN -> Effects(
                newState = TaskState.FROZEN,
                clearCompleted = wasDone,
                reverseContribution = wasDone,
                freezeSeries = isRecurring,
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
