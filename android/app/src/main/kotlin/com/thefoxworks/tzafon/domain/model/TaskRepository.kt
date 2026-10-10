package com.thefoxworks.tzafon.domain.model

import kotlinx.coroutines.flow.Flow

/**
 * The persistence contract for tasks + series. Room implements it in v2.0.0
 * (local-first); the deferred Firebase sync layer (M9b) will wrap or replace
 * this binding — nothing above this interface may touch storage directly.
 */
interface TaskRepository {

    fun observeTasks(): Flow<List<Task>>
    fun observeSeries(): Flow<List<Series>>

    suspend fun getTask(id: String): Task?
    suspend fun getSeries(id: String): Series?

    /**
     * Save an editor draft. What changed is measured against the task as
     * stored; nothing is written when nothing changed. On a repeating task
     * `scope` says how far the edits reach (one / forward / all), and a scope
     * the changes don't allow (DraftChanges.allows) throws
     * IllegalArgumentException. Turning repeat off ends the series the day
     * before this occurrence, which carries on as a one-off. A to-do date
     * move re-counts the rhythm only under Every N days; otherwise only this
     * occurrence moves. A status waiting in the draft (with its "How much?"
     * amount) applies last, to this occurrence only.
     */
    suspend fun saveDraft(draft: TaskDraft, scope: EditScope, today: String)

    /**
     * v2.13.0 — the whole-series badge on the scope sheets: how many of this
     * occurrence's series are dated before it, or with [notDoneOnly] only
     * those not Done. 0 for a one-off task.
     */
    suspend fun countEarlierOccurrences(taskId: String, notDoneOnly: Boolean): Int

    /**
     * Delete a task or occurrence. FORWARD ends the series the day before
     * this one and removes this and later occurrences; ALL removes the series.
     * Either way Done occurrences stay (ALL keeps them as one-off tasks).
     */
    suspend fun deleteTask(id: String, scope: EditScope)

    /**
     * OPEN <-> DONE. [habitAmount] carries the "how much?" answer when the
     * task completes against a quantitative habit (DM-HABIT-5).
     */
    suspend fun toggleDone(id: String, habitAmount: Double? = null)

    /**
     * DM-TASK-1/2/3 — apply a state transition with its effects (the DM-ATTR-2
     * habit ledger, goal attribution, and on a repeating task the v2.13.0
     * series rules: freeze, thaw, close and reopen the whole series).
     * Callers put StateMachine.askFor(...) to the user first and pass the
     * answer as [scope]; where nothing was asked the scope has no effect.
     */
    suspend fun setState(
        id: String,
        target: TaskState,
        today: String,
        habitAmount: Double? = null,
        scope: StatusScope = StatusScope.THIS_ONE,
    )

    /**
     * v2.13.0 — the task with what it has ticked and moved, taken before a
     * list status change so the toast's Undo can put it all back.
     */
    suspend fun statusSnapshot(id: String): com.thefoxworks.tzafon.domain.status.StatusSnapshot?

    /**
     * Undo a status change: if the task is still exactly [after], restore
     * [before] (status, completion time, to-do date) and its ledger rows, so
     * what it ticked and moved comes back or goes with the same amounts.
     * Returns false, changing nothing, when the task has changed since.
     */
    suspend fun restoreStatus(
        before: com.thefoxworks.tzafon.domain.status.StatusSnapshot,
        after: Task,
    ): Boolean

    /**
     * Materialize missing occurrences for every live series up to the
     * effective horizon (FR-REC-3), incl. the FR-REC-1 nearest floor.
     * Idempotent; safe to call on every app start / horizon change.
     */
    suspend fun topUp(today: String, horizonDays: Long)

    /** FR-TODAY-2 — persist a manual reorder as explicit sortOrder values. */
    suspend fun setSortOrders(orders: Map<String, Long>)

    /**
     * FR-PLAN-3 decide actions — move a task to a new To Do date (user-
     * initiated, so DM-TASK-8 holds). An occurrence reschedules as an
     * overridden one-off, series untouched (v1.1.0 semantics).
     */
    suspend fun reschedule(id: String, newToDoDate: String?)

    /** DM-FOCUS-1 — mark/unmark Today's Focus (a marker, not a store). */
    suspend fun setFocusDate(id: String, date: String?)

    /** DM-FOCUS-1 — set the week's priority marks in one pass (Review Plan). */
    suspend fun setWeekPriorities(ids: List<String>, weekStart: String)
}
