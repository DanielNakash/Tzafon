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

/** FR-ALL-3 — Done · Frozen · Closed · Backlog each independently toggleable. */
data class VisibleStates(
    val done: Boolean = false,
    val frozen: Boolean = false,
    val closed: Boolean = false,
    val backlog: Boolean = false,
)

data class AllTasksUiState(
    val today: String = Dates.todayIso(),
    val groups: List<TaskGroup> = emptyList(),
    val undated: List<Task> = emptyList(),
    val visible: VisibleStates = VisibleStates(),
    val doneCount: Int = 0,
    val total: Int = 0,
    val ruleSummaries: Map<String, String> = emptyMap(), // seriesId -> summary
    val empty: Boolean = false,
)

class AllTasksViewModel(
    private val repo: TaskRepository,
) : ViewModel() {

    private val visible = MutableStateFlow(VisibleStates())
    val today: String get() = Dates.todayIso()

    val uiState: StateFlow<AllTasksUiState> =
        combine(repo.observeTasks(), repo.observeSeries(), visible) { tasks, series, vis ->
            build(tasks, series, vis)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllTasksUiState())

    init {
        viewModelScope.launch { repo.topUp(today, Recurrence.HORIZON_DAYS) }
    }

    private fun shown(t: Task, vis: VisibleStates): Boolean = when (t.state) {
        TaskState.OPEN -> true
        TaskState.DONE -> vis.done
        TaskState.FROZEN -> vis.frozen
        TaskState.CLOSED -> vis.closed
        TaskState.BACKLOG -> vis.backlog
    }

    private fun build(tasks: List<Task>, series: List<Series>, vis: VisibleStates): AllTasksUiState {
        val today = Dates.todayIso()
        val done = tasks.count { it.state == TaskState.DONE }

        // FR-ALL-1: settled states appear inline in their date groups when
        // toggled visible (the v2 design shows frozen/closed rows in place)
        val visibleTasks = tasks.filter { shown(it, vis) }
        val dated = visibleTasks.filter { it.toDoDate != null }.sortedBy { it.toDoDate }
        val undated = visibleTasks.filter { it.toDoDate == null }
            .sortedWith(compareBy({ it.state != TaskState.OPEN }, { it.createdAt }))

        val groups = dated
            .groupBy { Dates.groupFor(it.toDoDate!!, today) }
            .map { (g, items) -> TaskGroup(g.key, g.label, g.order, items) }
            .sortedBy { it.order }

        return AllTasksUiState(
            today = today,
            groups = groups,
            undated = undated,
            visible = vis,
            doneCount = done,
            total = tasks.size,
            ruleSummaries = series.associate { it.id to Recurrence.summary(it.rule) },
            empty = tasks.none { it.state == TaskState.OPEN },
        )
    }

    fun toggle(state: TaskState) {
        visible.value = visible.value.let { v ->
            when (state) {
                TaskState.DONE -> v.copy(done = !v.done)
                TaskState.FROZEN -> v.copy(frozen = !v.frozen)
                TaskState.CLOSED -> v.copy(closed = !v.closed)
                TaskState.BACKLOG -> v.copy(backlog = !v.backlog)
                TaskState.OPEN -> v
            }
        }
    }

    fun toggleDone(id: String) {
        viewModelScope.launch { repo.toggleDone(id) }
    }

    fun setState(id: String, target: TaskState) {
        viewModelScope.launch { repo.setState(id, target, today) }
    }
}
