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

    fun quickAdd(title: String) {
        viewModelScope.launch {
            repo.saveDraft(
                com.thefoxworks.tzafon.domain.model.TaskDraft(id = null, title = title),
                com.thefoxworks.tzafon.domain.model.EditScope.ONE,
                today,
            )
        }
    }
}
