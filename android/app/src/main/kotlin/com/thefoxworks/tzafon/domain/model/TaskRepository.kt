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

    /** Delete a task or occurrence; ALL deletes the whole series. */
    suspend fun deleteTask(id: String, scope: EditScope)

    /** M0 parity toggle: OPEN <-> DONE. The full state machine lands in M1. */
    suspend fun toggleDone(id: String)

    /**
     * DM-TASK-1/2/3 — apply a state transition with its effects (thaw, series
     * freeze/restore per FR-REC-5, contribution apply/reverse once the M5
     * ledger exists). Callers clear StateMachine.guardFor(...) first.
     */
    suspend fun setState(id: String, target: TaskState, today: String)

    /**
     * Materialize missing occurrences for every live series up to the
     * effective horizon (FR-REC-3), incl. the FR-REC-1 nearest floor.
     * Idempotent; safe to call on every app start / horizon change.
     */
    suspend fun topUp(today: String, horizonDays: Long)
}
