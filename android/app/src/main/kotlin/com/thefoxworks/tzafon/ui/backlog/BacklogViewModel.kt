package com.thefoxworks.tzafon.ui.backlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BacklogUiState(
    val today: String = Dates.todayIso(),
    val items: List<Task> = emptyList(),
)

/**
 * Backlog (FR-BACKLOG) — the someday/maybe pool. Excluded from Today,
 * Planning and the overload count by construction (those views keep only
 * OPEN tasks); this screen is where parked things wait to be pulled.
 */
class BacklogViewModel(
    private val repo: TaskRepository,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    val uiState: StateFlow<BacklogUiState> =
        repo.observeTasks().map { tasks ->
            BacklogUiState(
                today = Dates.todayIso(),
                // most recently parked first — the top of the pool is freshest
                items = tasks.filter { it.state == TaskState.BACKLOG }
                    .sortedByDescending { it.stateChangedAt },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BacklogUiState())

    /**
     * FR-BACKLOG-3 — the "pull when inspired" flow: → Open, optionally with
     * a fresh To Do date (user-initiated, so DM-TASK-8 holds).
     */
    fun pull(id: String, toDoDate: String?) {
        viewModelScope.launch {
            repo.setState(id, TaskState.OPEN, today)
            if (toDoDate != null) repo.reschedule(id, toDoDate)
        }
    }

    /** Done ("did it without scheduling") / Closed ("decided against it"). */
    fun setState(id: String, target: TaskState) {
        viewModelScope.launch { repo.setState(id, target, today) }
    }

    /**
     * FR-BACKLOG-5.1 — quick-adds from the Backlog view default to
     * `state = BACKLOG` and `toDoDate = null`. The Backlog surface is
     * state-scoped: what you put here stays here until you pull it. Adds
     * from Today/Planning/All Tasks still default to OPEN.
     */
    fun quickAdd(title: String, cueTime: String? = null) {
        viewModelScope.launch {
            repo.saveDraft(
                quickAddDraft(title, cueTime),
                com.thefoxworks.tzafon.domain.model.EditScope.ONE,
                today,
            )
        }
    }

    companion object {
        /**
         * FR-BACKLOG-5.1 — quick-adds from the Backlog view default to
         * `state = BACKLOG` and `toDoDate = null`. The Backlog surface is
         * state-scoped: what you put here stays here until you pull it. Adds
         * from Today/Planning/All Tasks still default to OPEN (FR-BACKLOG-5.5).
         *
         * FR-CAPTURE-3.8 — a parsed time is stored rather than suppressed. It
         * cannot ring while the task sits in Backlog (NotifyLogic skips non-OPEN
         * tasks), but the cue is lossless: pull the task out to a date later and
         * it already knows when it wants to happen. FR-PLAN-7's date rule is
         * Planning-only and deliberately does not reach here.
         */
        fun quickAddDraft(
            title: String,
            cueTime: String? = null,
        ): com.thefoxworks.tzafon.domain.model.TaskDraft =
            com.thefoxworks.tzafon.domain.model.TaskDraft(
                id = null,
                title = title,
                state = TaskState.BACKLOG,
                toDoDate = null,
                cue = com.thefoxworks.tzafon.domain.action.QuickAddParse.cueFor(cueTime),
            )
    }
}
