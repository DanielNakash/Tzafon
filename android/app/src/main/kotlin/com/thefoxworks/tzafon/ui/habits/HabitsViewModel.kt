package com.thefoxworks.tzafon.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class HabitCardState(
    val habit: Habit,
    val week: HabitMath.WeekSnapshot,
    val arc: String,
    val freshStart: Boolean,
    val grid: List<List<Int>>,     // 5 weeks × 7 days
    val loggedToday: Boolean,
    val todayAmount: Double?,
    /**
     * FR-HAB-7.4 — any logged history at all locks the `kind` field in the
     * editor. Includes days marked "not done" and quantitative zero-amount
     * entries: once a log row exists the "did it happen" projection is real
     * data, and changing `kind` would reinterpret it in place.
     */
    val hasHistory: Boolean,
)

data class HabitsUiState(
    val today: String = Dates.todayIso(),
    val weekStart: String = "SUNDAY",
    val cards: List<HabitCardState> = emptyList(),
    /** for the editor's "serves a goal" pick (DM-HABIT-6) */
    val goals: List<com.thefoxworks.tzafon.domain.model.Goal> = emptyList(),
    val goalsById: Map<String, com.thefoxworks.tzafon.domain.model.Goal> = emptyMap(),
)

/** Habits (FR-HAB) — forgiving rate + cue + arc; no streaks, no scores. */
class HabitsViewModel(
    private val repo: HabitRepository,
    settings: SettingsStore,
    goalRepo: com.thefoxworks.tzafon.domain.model.GoalRepository,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    val uiState: StateFlow<HabitsUiState> =
        combine(
            repo.observeHabits(),
            repo.observeLogs(),
            settings.weekStart,
            goalRepo.observeGoals(),
        ) { habits, logs, weekStart, goals ->
            val today = Dates.todayIso()
            HabitsUiState(
                today = today,
                weekStart = weekStart,
                cards = habits.sortedBy { it.createdAt }.map { h ->
                    val todayLog = logs.firstOrNull { it.habitId == h.id && it.date == today && it.done }
                    HabitCardState(
                        habit = h,
                        week = HabitMath.week(h, logs, today, weekStart),
                        arc = HabitMath.arcLabel(HabitMath.arcWeeks(h, logs, today, weekStart)),
                        freshStart = HabitMath.freshStart(h, logs, today, weekStart),
                        grid = HabitMath.historyGrid(h, logs, today, weekStart),
                        loggedToday = todayLog != null,
                        todayAmount = todayLog?.amount,
                        hasHistory = logs.any { it.habitId == h.id },
                    )
                },
                goals = goals,
                goalsById = goals.associateBy { it.id },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    /** DM-HABIT-5 — the direct log path ("Mark today done" / amount sheet). */
    fun logToday(habitId: String, done: Boolean, amount: Double? = null) {
        viewModelScope.launch { repo.logDirect(habitId, today, done, amount) }
    }

    fun save(habit: Habit) {
        viewModelScope.launch {
            repo.upsert(
                if (habit.id.isBlank()) {
                    habit.copy(
                        id = UUID.randomUUID().toString(),
                        startedAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis(),
                    )
                } else habit,
            )
        }
    }

    /** DM-HABIT-7 — destructive; the UI confirms before calling. */
    fun delete(habitId: String) {
        viewModelScope.launch { repo.delete(habitId) }
    }
}
