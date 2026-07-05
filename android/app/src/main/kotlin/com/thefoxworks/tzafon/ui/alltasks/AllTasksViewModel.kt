package com.thefoxworks.tzafon.ui.alltasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Series
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskGroup(val key: String, val label: String, val order: Long, val items: List<Task>)

data class AllTasksUiState(
    val today: String = Dates.todayIso(),
    val groups: List<TaskGroup> = emptyList(),
    val undated: List<Task> = emptyList(),
    val done: List<Task> = emptyList(),
    val showDone: Boolean = false,
    val doneCount: Int = 0,
    val total: Int = 0,
    val ruleSummaries: Map<String, String> = emptyMap(), // seriesId -> summary
    val empty: Boolean = false,
)

class AllTasksViewModel(
    private val repo: TaskRepository,
) : ViewModel() {

    private val showDone = MutableStateFlow(false)
    val today: String get() = Dates.todayIso()

    val uiState: StateFlow<AllTasksUiState> =
        combine(repo.observeTasks(), repo.observeSeries(), showDone) { tasks, series, show ->
            build(tasks, series, show)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllTasksUiState())

    init {
        viewModelScope.launch { repo.topUp(today, Recurrence.HORIZON_DAYS) }
    }

    private fun build(tasks: List<Task>, series: List<Series>, show: Boolean): AllTasksUiState {
        val today = Dates.todayIso()
        // M0 parity: active = not-done; M1 generalizes visibility per state
        val active = tasks.filter { it.state == TaskState.OPEN }
        val done = tasks.filter { it.state == TaskState.DONE }

        val dated = active.filter { it.toDoDate != null }.sortedBy { it.toDoDate }
        val undated = active.filter { it.toDoDate == null }.sortedBy { it.createdAt }

        val groups = dated
            .groupBy { Dates.groupFor(it.toDoDate!!, today) }
            .map { (g, items) -> TaskGroup(g.key, g.label, g.order, items) }
            .sortedBy { it.order }

        val summaries = series.associate { it.id to Recurrence.summary(it.rule) }

        return AllTasksUiState(
            today = today,
            groups = groups,
            undated = undated,
            done = done.sortedByDescending { it.completedAt ?: 0 },
            showDone = show,
            doneCount = done.size,
            total = tasks.size,
            ruleSummaries = summaries,
            empty = active.isEmpty(),
        )
    }

    fun toggleShowDone() {
        showDone.value = !showDone.value
    }

    fun toggleDone(id: String) {
        viewModelScope.launch { repo.toggleDone(id) }
    }
}
