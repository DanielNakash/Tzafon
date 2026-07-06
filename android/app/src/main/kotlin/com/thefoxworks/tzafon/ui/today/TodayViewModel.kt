package com.thefoxworks.tzafon.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.action.ActionLogic
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class TodayUiState(
    val today: String = Dates.todayIso(),
    val focus: List<Task> = emptyList(),          // DM-FOCUS markers (set in M7)
    val alsoToday: List<Task> = emptyList(),      // the reorderable main list
    val doneToday: List<Task> = emptyList(),
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val slippedCount: Int = 0,                    // FR-TODAY-4
    val showSlippage: Boolean = false,
    val showOverload: Boolean = false,            // FR-TODAY-5
    val habitsById: Map<String, Habit> = emptyMap(), // M4 chips + quant prompt
    val goalsById: Map<String, com.thefoxworks.tzafon.domain.model.Goal> = emptyMap(), // M5 prompt rule
)

class TodayViewModel(
    private val repo: TaskRepository,
    private val settings: SettingsStore,
    habitRepo: HabitRepository,
    goalRepo: com.thefoxworks.tzafon.domain.model.GoalRepository,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    val uiState: StateFlow<TodayUiState> =
        combine(
            repo.observeTasks(),
            settings.slippageDismissedOn,
            settings.overloadDismissedOn,
            habitRepo.observeHabits(),
            goalRepo.observeGoals(),
        ) { tasks, slipDismissed, overDismissed, habits, goals ->
            build(tasks, slipDismissed, overDismissed).copy(
                habitsById = habits.associateBy { it.id },
                goalsById = goals.associateBy { it.id },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    init {
        viewModelScope.launch { repo.topUp(today, Recurrence.HORIZON_DAYS) }
    }

    private fun build(tasks: List<Task>, slipDismissed: String?, overDismissed: String?): TodayUiState {
        val today = Dates.todayIso()
        val zone = ZoneId.systemDefault()
        val dayStart = LocalDate.parse(today).atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = LocalDate.parse(today).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val open = tasks.filter { ActionLogic.isTodayTask(it, today) }
        val focus = ActionLogic.todayOrder(open.filter { ActionLogic.isFocusToday(it, today) })
        val also = ActionLogic.todayOrder(open.filterNot { ActionLogic.isFocusToday(it, today) })
        val done = tasks.filter { ActionLogic.isDoneToday(it, today, dayStart, dayEnd) }
            .sortedByDescending { it.completedAt ?: 0 }
        val slipped = ActionLogic.slippedCount(tasks, today)

        return TodayUiState(
            today = today,
            focus = focus,
            alsoToday = also,
            doneToday = done,
            doneCount = done.size,
            totalCount = open.size + done.size,
            slippedCount = slipped,
            showSlippage = slipped > 0 && slipDismissed != today,
            // FR-TODAY-5: static threshold; must never nag alongside the focus
            // cap nudge (which lives in the M7 review flow, not here)
            showOverload = ActionLogic.isOverloaded(open.size) && overDismissed != today,
        )
    }

    fun toggleDone(id: String, habitAmount: Double? = null) {
        viewModelScope.launch { repo.toggleDone(id, habitAmount) }
    }

    fun dismissSlippage() {
        viewModelScope.launch { settings.dismissSlippage(today) }
    }

    fun dismissOverload() {
        viewModelScope.launch { settings.dismissOverload(today) }
    }

    /** FR-TODAY-2 — persist the drag result as explicit sort orders. */
    fun persistOrder(idsInOrder: List<String>) {
        viewModelScope.launch {
            repo.setSortOrders(idsInOrder.mapIndexed { i, id -> id to (i + 1).toLong() }.toMap())
        }
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
