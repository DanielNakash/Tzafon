package com.thefoxworks.tzafon.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.audio.ChimePlayer
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
    val query: String = "", // FR-PLAN-6 — live search, transient view state
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
    private val chimePlayer: ChimePlayer? = null,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    /** FR-PLAN-6.7 — live search text. Transient: never written to DataStore, never synced. */
    private val query = MutableStateFlow("")

    val uiState: StateFlow<PlanningUiState> =
        combine(
            // `combine` tops out at five flows; the range triple folds into one
            // so the FR-PLAN-6 query can join without a vararg cast.
            combine(
                repo.observeTasks(),
                settings.planningPreset,
                settings.planningCustomEnd,
            ) { tasks, presetName, customEnd -> Triple(tasks, presetName, customEnd) },
            habitRepo.observeHabits(),
            goalRepo.observeGoals(),
            query,
        ) { (tasks, presetName, customEnd), habits, goals, q ->
            build(tasks, RangePreset.parse(presetName), customEnd, q).copy(
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

    private fun build(
        tasks: List<Task>,
        preset: RangePreset,
        customEnd: String?,
        query: String = "",
    ): PlanningUiState {
        val today = Dates.todayIso()
        val days = ActionLogic.rangeDays(preset, today, customEnd)
        // FR-PLAN-6.3 — one predicate, applied before grouping, so every bucket
        // narrows together and empty headers never render (FR-PLAN-6.5).
        val groups = ActionLogic.planningGroups(tasks, today, days, query)
        return PlanningUiState(
            today = today,
            preset = preset,
            customEnd = customEnd,
            rangeEnd = Dates.addDays(today, days),
            overdue = groups.overdue,
            dated = groups.dated,
            inbox = groups.inbox,
            undatedCount = groups.inbox.size,
            query = query,
        )
    }

    /** FR-PLAN-6.3 — the search text is an input to the same combine the range feeds. */
    fun setQuery(q: String) {
        query.value = q
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

    /**
     * FR-AUDIO-1.2 — the checkbox tap path on Planning (dated rows and the
     * FR-PLAN-4 overdue action-set). Reads state before the write so we
     * chime only on Open → Done; toggles back are silent.
     */
    fun toggleDone(id: String, habitAmount: Double? = null) {
        viewModelScope.launch {
            val wasOpen = repo.getTask(id)?.state == TaskState.OPEN
            repo.toggleDone(id, habitAmount)
            if (wasOpen && chimePlayer != null && settings.chimeEnabled.first()) {
                chimePlayer.playDone()
            }
        }
    }

    fun quickAdd(title: String, cueTime: String? = null) {
        viewModelScope.launch {
            repo.saveDraft(quickAddDraft(title, today, cueTime), com.thefoxworks.tzafon.domain.model.EditScope.ONE, today)
        }
    }

    companion object {
        /**
         * FR-CAPTURE-2 — a bare Planning capture is undated and lands in the Inbox;
         * the v2.1.0 decision that keeps Planning's "needs a date" group meaningful
         * stands untouched for it.
         *
         * FR-PLAN-7 — the one exception, conditional on the parse rather than on the
         * view: a typed clock time is the expression of intent a bare capture lacks,
         * and NotifyLogic.remindersFor only ever schedules a task reminder when the
         * task is dated — so an undated task carrying an 08:30 cue is a cue that can
         * never fire. With a parsed time the draft is dated today; without one it is
         * exactly what it was before v2.12.0.
         */
        fun quickAddDraft(
            title: String,
            today: String,
            cueTime: String? = null,
        ): com.thefoxworks.tzafon.domain.model.TaskDraft =
            com.thefoxworks.tzafon.domain.model.TaskDraft(
                id = null,
                title = title,
                toDoDate = if (cueTime != null) today else null,
                cue = com.thefoxworks.tzafon.domain.action.QuickAddParse.cueFor(cueTime),
            )
    }
}
