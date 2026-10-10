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
     * Save an editor draft. `scope` applies to recurring edits only
     * (v1.1.0 saveTask semantics: one / forward / all).
     */
    suspend fun saveDraft(draft: TaskDraft, scope: EditScope, today: String)

    /**
     * v2.13.0 — the whole-series badge on the scope sheets: how many of this
     * occurrence's series are dated before it, or with [notDoneOnly] only
     * those not Done. 0 for a one-off task.
     */
    suspend fun countEarlierOccurrences(taskId: String, notDoneOnly: Boolean): Int

    /** Delete a task or occurrence; ALL deletes the whole series. */
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
