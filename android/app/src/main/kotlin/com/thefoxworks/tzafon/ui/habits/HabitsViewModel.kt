package com.thefoxworks.tzafon.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeRepository
import com.thefoxworks.tzafon.domain.model.ThemeState
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
    /**
     * FR-HAB-8 — this habit's raw log rows, so the picked-date affordance can
     * look up "is there a done log for `pickedDate`?" (frequency toggle) and
     * the amount-sheet suggestion for a picked date (quantitative edit).
     */
    val logs: List<HabitLog>,
)

data class HabitsUiState(
    val today: String = Dates.todayIso(),
    val weekStart: String = "SUNDAY",
    val cards: List<HabitCardState> = emptyList(),
    /** for the editor's "serves a goal" pick (DM-HABIT-6) */
    val goals: List<com.thefoxworks.tzafon.domain.model.Goal> = emptyList(),
    val goalsById: Map<String, com.thefoxworks.tzafon.domain.model.Goal> = emptyMap(),
    /**
     * FR-HAB-10.3 — active themes are the only options togglable in the
     * habit editor's direction picker (mirrors `FR-DIR-8.4`).
     */
    val activeThemes: List<Theme> = emptyList(),
    /**
     * FR-HAB-10.3 — full theme list resolves read-only annotated chips for
     * habits already serving an upcoming/archived theme.
     */
    val allThemes: List<Theme> = emptyList(),
)

/** Habits (FR-HAB) — forgiving rate + cue + arc; no streaks, no scores. */
class HabitsViewModel(
    private val repo: HabitRepository,
    settings: SettingsStore,
    goalRepo: com.thefoxworks.tzafon.domain.model.GoalRepository,
    themeRepo: ThemeRepository,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    val uiState: StateFlow<HabitsUiState> =
        combine(
            repo.observeHabits(),
            repo.observeLogs(),
            settings.weekStart,
            goalRepo.observeGoals(),
            themeRepo.observeThemes(),
        ) { habits, logs, weekStart, goals, themes ->
            val today = Dates.todayIso()
            HabitsUiState(
                today = today,
                weekStart = weekStart,
                cards = habits.sortedBy { it.createdAt }.map { h ->
                    val habitLogs = logs.filter { it.habitId == h.id }
                    val todayLog = habitLogs.firstOrNull { it.date == today && it.done }
                    HabitCardState(
                        habit = h,
                        week = HabitMath.week(h, logs, today, weekStart),
                        arc = HabitMath.arcLabel(HabitMath.arcWeeks(h, logs, today, weekStart)),
                        freshStart = HabitMath.freshStart(h, logs, today, weekStart),
                        grid = HabitMath.historyGrid(h, logs, today, weekStart),
                        loggedToday = todayLog != null,
                        todayAmount = todayLog?.amount,
                        hasHistory = habitLogs.isNotEmpty(),
                        logs = habitLogs,
                    )
                },
                goals = goals,
                goalsById = goals.associateBy { it.id },
                activeThemes = themes.filter { it.state == ThemeState.ACTIVE },
                allThemes = themes,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    /** DM-HABIT-5 — the direct log path ("Mark today done" / amount sheet). */
    fun logToday(habitId: String, done: Boolean, amount: Double? = null) {
        viewModelScope.launch { repo.logDirect(habitId, today, done, amount) }
    }

    /**
     * FR-HAB-8 — the direct log path, on an arbitrary picked date. Thin
     * passthrough to [HabitRepository.logDirect]; the "any date" contract
     * has been in `DM-HABIT-5` since v2.0.0 and only the UI affordance was
     * missing before v2.4.0.
     */
    fun logForDate(habitId: String, date: String, done: Boolean, amount: Double? = null) {
        viewModelScope.launch { repo.logDirect(habitId, date, done, amount) }
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
