package com.thefoxworks.tzafon.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.action.ActionLogic
import com.thefoxworks.tzafon.domain.action.ActionLogic.RangePreset
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanningUiState(
    val today: String = Dates.todayIso(),
    val preset: RangePreset = RangePreset.DAYS_7,
    val customEnd: String? = null,
    val rangeEnd: String = Dates.todayIso(),
    val overdue: List<Task> = emptyList(),
    val dated: List<Pair<Dates.Group, List<Task>>> = emptyList(),
    val inbox: List<Task> = emptyList(),
    val undatedCount: Int = 0,
    val habitsById: Map<String, Habit> = emptyMap(), // M4 chips + quant prompt
    val goalsById: Map<String, com.thefoxworks.tzafon.domain.model.Goal> = emptyMap(), // M5 prompt rule
)

/**
 * Planning (FR-PLAN) — triage. The range presets drive the session's
 * effective generation horizon (FR-REC-3) through the shared state flow.
 */
class PlanningViewModel(
    private val repo: TaskRepository,
    private val settings: SettingsStore,
    private val sessionHorizonDays: MutableStateFlow<Long>,
    habitRepo: HabitRepository,
    goalRepo: com.thefoxworks.tzafon.domain.model.GoalRepository,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    val uiState: StateFlow<PlanningUiState> =
        combine(
            repo.observeTasks(),
            settings.planningPreset,
            settings.planningCustomEnd,
            habitRepo.observeHabits(),
            goalRepo.observeGoals(),
        ) { tasks, presetName, customEnd, habits, goals ->
            build(tasks, RangePreset.parse(presetName), customEnd).copy(
                habitsById = habits.associateBy { it.id },
                goalsById = goals.associateBy { it.id },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanningUiState())

    init {
        // restore the persisted range and generate to its horizon (NFR-PERF-2)
        viewModelScope.launch {
            val preset = RangePreset.parse(settings.planningPreset.first())
            val customEnd = settings.planningCustomEnd.first()
            applyHorizon(ActionLogic.rangeDays(preset, today, customEnd))
        }
    }

    private fun build(tasks: List<Task>, preset: RangePreset, customEnd: String?): PlanningUiState {
        val today = Dates.todayIso()
        val days = ActionLogic.rangeDays(preset, today, customEnd)
        val groups = ActionLogic.planningGroups(tasks, today, days)
        return PlanningUiState(
            today = today,
            preset = preset,
            customEnd = customEnd,
            rangeEnd = Dates.addDays(today, days),
            overdue = groups.overdue,
            dated = groups.dated,
            inbox = groups.inbox,
            undatedCount = groups.inbox.size,
        )
    }

    fun setRange(preset: RangePreset, customEnd: String? = null) {
        viewModelScope.launch {
            settings.setPlanningRange(preset.name, customEnd)
            applyHorizon(ActionLogic.rangeDays(preset, today, customEnd))
        }
    }

    private suspend fun applyHorizon(rangeDays: Long) {
        val horizon = ActionLogic.effectiveHorizonDays(rangeDays)
        sessionHorizonDays.value = maxOf(sessionHorizonDays.value, horizon)
        repo.topUp(today, sessionHorizonDays.value)
    }

    // ── decide actions (FR-PLAN-3) ────────────────────────────

    fun doToday(id: String) {
        viewModelScope.launch { repo.reschedule(id, today) }
    }

    fun rescheduleTo(id: String, date: String) {
        viewModelScope.launch { repo.reschedule(id, date) }
    }

    /** "Drop" — a calm skip, not a failure (Closed keeps its links). */
    fun drop(id: String) {
        viewModelScope.launch { repo.setState(id, TaskState.CLOSED, today) }
    }

    /**
     * "Someday" → Backlog (FR-BACKLOG-2). Callers guard the recurring case
     * first — a live series can't be someday; the UI offers Frozen instead.
     */
    fun someday(id: String) {
        viewModelScope.launch { repo.setState(id, TaskState.BACKLOG, today) }
    }

    /** Guard-pane resolution (e.g. Someday-on-series → Frozen instead). */
    fun toState(id: String, target: TaskState) {
        viewModelScope.launch { repo.setState(id, target, today) }
    }

    fun toggleDone(id: String, habitAmount: Double? = null) {
        viewModelScope.launch { repo.toggleDone(id, habitAmount) }
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
