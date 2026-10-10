package com.thefoxworks.tzafon.domain.status

import com.thefoxworks.tzafon.domain.model.Contribution
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState

/**
 * A task as it stood before a status change, with what it had ticked and
 * moved: its task-sourced habit logs and its goal contributions (DM-ATTR).
 * Undo puts all of it back, so the ledger returns with the same amounts and
 * "How much?" is never asked again.
 */
data class StatusSnapshot(
    val task: Task,
    val habitLogs: List<HabitLog> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
)

/** What a status change did to the task's whole series, if anything. */
enum class SeriesChange { FROZEN, CLOSED, THAWED, REOPENED }

object StatusUndo {

    /**
     * Undo applies only while the task is exactly as the change left it;
     * if anything has changed it since (another tap, an edit, sync), undo
     * does nothing.
     */
    fun canUndo(after: Task, current: Task?): Boolean = current == after

    /** Only a move into or out of Done touched the ledger. */
    fun touchesLedger(before: TaskState, after: TaskState): Boolean =
        (before == TaskState.DONE) != (after == TaskState.DONE)

    /** The series-wide part of a transition; such changes can't be undone. */
    fun seriesChange(fx: StateMachine.Effects): SeriesChange? = when {
        fx.freezeSeries -> SeriesChange.FROZEN
        fx.closeSeries -> SeriesChange.CLOSED
        fx.thawSeries -> SeriesChange.THAWED
        fx.reopenSeries -> SeriesChange.REOPENED
        else -> null
    }
}
