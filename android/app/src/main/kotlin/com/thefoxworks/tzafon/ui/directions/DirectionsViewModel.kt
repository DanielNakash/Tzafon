package com.thefoxworks.tzafon.ui.directions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeRepository
import com.thefoxworks.tzafon.domain.model.ThemeState
import com.thefoxworks.tzafon.domain.themes.ThemeLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ThemeBoardEntry(
    val theme: Theme,
    /** mean of its ongoing goals' endowed pcts — goal progress, never adherence */
    val ringPct: Int,
    /** "QUARTER · WEEK 7" — orientation, not pressure (FR-DIR-2) */
    val windowLabel: String,
    /** "this quarter: 4 aligned tasks done · 2 habits held · 1 goal advancing" */
    val mirror: String,
    val goals: List<Goal>,            // primary-owned, ongoing
    val sharedGoals: List<Goal>,      // FR-DIR-5 read-only refs (owned elsewhere)
    val habits: List<Habit>,
    val taskCount: Int,               // open tasks directly aligned (D1)
    val windowEnded: Boolean,
)

data class DirectionsUiState(
    val today: String = Dates.todayIso(),
    val active: List<ThemeBoardEntry> = emptyList(),
    val upcoming: List<Theme> = emptyList(),
    val archived: List<Theme> = emptyList(),
    val orphanGoals: List<Goal> = emptyList(),
    val enginesByGoal: Map<String, List<Habit>> = emptyMap(),
    val themesById: Map<String, Theme> = emptyMap(),
    /** FR-DIR-6 — an arrived upcoming theme needs a slot and none is free */
    val arrivalPrompt: Theme? = null,
)

/** The Directions hub (FR-DIR) — the trellis made visible. */
class DirectionsViewModel(
    private val themeRepo: ThemeRepository,
    private val goalRepo: GoalRepository,
    private val habitRepo: HabitRepository,
    private val taskRepo: TaskRepository,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    private data class Direction(val themes: List<Theme>, val goals: List<Goal>, val habits: List<Habit>)

    val uiState: StateFlow<DirectionsUiState> =
        combine(
            combine(themeRepo.observeThemes(), goalRepo.observeGoals(), habitRepo.observeHabits()) { t, g, h ->
                Direction(t, g, h)
            },
            taskRepo.observeTasks(),
            habitRepo.observeLogs(),
        ) { d, tasks, logs ->
            build(d.themes, d.goals, d.habits, tasks, logs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DirectionsUiState())

    init {
        // FR-DIR-6 — window arrivals: auto-activate / auto-swap quietly
        viewModelScope.launch {
            val themes = themeRepo.observeThemes().first()
            val today = Dates.todayIso()
            for (arriving in ThemeLogic.arrivedUpcoming(themes, today)) {
                when (val a = ThemeLogic.onWindowArrival(themeRepo.observeThemes().first(), arriving, today)) {
                    ThemeLogic.Arrival.Activate ->
                        themeRepo.upsert(arriving.copy(state = ThemeState.ACTIVE))

                    is ThemeLogic.Arrival.Swap -> {
                        themeRepo.getTheme(a.endedThemeId)?.let { ended ->
                            themeRepo.upsert(
                                ended.copy(
                                    state = ThemeState.ARCHIVED,
                                    archivedOutcome = com.thefoxworks.tzafon.domain.model.ArchivedOutcome.RETIRED,
                                    archivedAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                        themeRepo.upsert(arriving.copy(state = ThemeState.ACTIVE))
                    }

                    ThemeLogic.Arrival.Prompt -> Unit // surfaced via arrivalPrompt
                }
            }
        }
    }

    private fun build(
        themes: List<Theme>,
        goals: List<Goal>,
        habits: List<Habit>,
        tasks: List<Task>,
        logs: List<HabitLog>,
    ): DirectionsUiState {
        val today = Dates.todayIso()
        val weekAgo = Dates.addDays(today, -7)
        val staleMs = 14L * 24 * 60 * 60 * 1000

        fun entry(theme: Theme): ThemeBoardEntry {
            val primary = goals.filter { it.primaryThemeId == theme.id && it.state == GoalState.ONGOING }
            val shared = goals.filter {
                theme.id in it.themeIds && it.primaryThemeId != theme.id && it.state == GoalState.ONGOING
            }
            val themeHabits = habits.filter { it.primaryThemeId == theme.id || theme.id in it.themeIds }
            val themeGoalIds = (primary + shared).map { it.id }.toSet()
            val themeHabitIds = themeHabits.map { it.id }.toSet()

            // aligned tasks done inside the window (identity mirror, FR-DIR-2)
            val alignedDone = tasks.count { t ->
                t.state == TaskState.DONE &&
                    (t.themeId == theme.id || t.habitId in themeHabitIds || t.goalIds.any { it in themeGoalIds }) &&
                    t.completedAt != null &&
                    Dates.iso(java.time.Instant.ofEpochMilli(t.completedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()) >= theme.windowStart
            }
            val habitsHeld = themeHabits.count { h ->
                logs.any { it.habitId == h.id && it.done && it.date >= weekAgo }
            }
            val advancing = primary.count {
                System.currentTimeMillis() - it.lastActivityAt < staleMs && it.lastActivityAt > 0
            }
            val span = if (Dates.dayDiff(theme.windowEnd, theme.windowStart) in 84..98) "quarter" else "window"
            val mirror = "this $span: $alignedDone aligned task${if (alignedDone == 1) "" else "s"} done" +
                " · $habitsHeld habit${if (habitsHeld == 1) "" else "s"} held" +
                " · $advancing goal${if (advancing == 1) "" else "s"} advancing"

            val pcts = primary.map { it.pct() }
            return ThemeBoardEntry(
                theme = theme,
                ringPct = if (pcts.isEmpty()) 0 else pcts.average().toInt(),
                windowLabel = ThemeLogic.windowLabel(theme, today),
                mirror = mirror,
                goals = primary,
                sharedGoals = shared,
                habits = themeHabits,
                taskCount = tasks.count { it.state == TaskState.OPEN && it.themeId == theme.id },
                windowEnded = ThemeLogic.windowEnded(theme, today),
            )
        }

        val sorted = themes.sortedBy { it.createdAt }
        val activeThemeIds = sorted.filter { it.state == ThemeState.ACTIVE }.map { it.id }.toSet()
        val arrivals = ThemeLogic.arrivedUpcoming(sorted, today).firstOrNull { arriving ->
            ThemeLogic.onWindowArrival(sorted, arriving, today) == ThemeLogic.Arrival.Prompt
        }

        return DirectionsUiState(
            today = today,
            active = sorted.filter { it.state == ThemeState.ACTIVE }.map(::entry),
            upcoming = sorted.filter { it.state == ThemeState.UPCOMING },
            archived = sorted.filter { it.state == ThemeState.ARCHIVED }
                .sortedByDescending { it.archivedAt ?: 0 },
            orphanGoals = goals.filter {
                it.state == GoalState.ONGOING &&
                    (it.primaryThemeId == null || it.primaryThemeId !in activeThemeIds)
            },
            enginesByGoal = goals.associate { g -> g.id to habits.filter { it.goalId == g.id } },
            themesById = themes.associateBy { it.id },
            arrivalPrompt = arrivals,
        )
    }

    // ── theme actions ─────────────────────────────────────────

    /** Save; when [activate], the ≤3 cap is checked — false = offer Upcoming. */
    suspend fun saveTheme(theme: Theme, activate: Boolean): Boolean {
        val themes = themeRepo.observeThemes().first()
        val toSave = if (theme.id.isBlank()) {
            theme.copy(
                id = UUID.randomUUID().toString(),
                accentSlot = ThemeLogic.nextAccentSlot(themes),
                createdAt = System.currentTimeMillis(),
            )
        } else theme
        return if (activate) {
            if (ThemeLogic.canActivate(themes, toSave.id)) {
                themeRepo.upsert(toSave.copy(state = ThemeState.ACTIVE))
                true
            } else {
                false // DM-THEME-3 — the UI offers save-as-Upcoming
            }
        } else {
            themeRepo.upsert(toSave)
            true
        }
    }

    fun parkAsUpcoming(theme: Theme) {
        viewModelScope.launch {
            saveTheme(theme.copy(state = ThemeState.UPCOMING), activate = false)
        }
    }

    /** Plain save preserving whatever state the theme already holds. */
    fun saveKeepState(theme: Theme) {
        viewModelScope.launch { saveTheme(theme, activate = false) }
    }

    /** DM-THEME-4 — Review & Renew: renew / evolve / retire, never fail. */
    fun archive(theme: Theme, outcome: com.thefoxworks.tzafon.domain.model.ArchivedOutcome, renewedToId: String? = null) {
        viewModelScope.launch {
            themeRepo.upsert(
                theme.copy(
                    state = ThemeState.ARCHIVED,
                    archivedOutcome = outcome,
                    renewedToThemeId = renewedToId,
                    archivedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun activateNow(theme: Theme, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (!saveTheme(theme, activate = true)) onBlocked()
        }
    }

    // ── goal / habit adds from the board (the auto-bridge) ────

    fun saveGoal(goal: Goal, themeId: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val toSave = if (goal.id.isBlank()) {
                goal.copy(
                    id = UUID.randomUUID().toString(),
                    steps = if (goal.type == com.thefoxworks.tzafon.domain.model.GoalType.STEPPED &&
                        goal.steps.none { it.label == "Defined this goal ✓" }
                    ) {
                        listOf(com.thefoxworks.tzafon.domain.model.GoalStep("Defined this goal ✓", true)) + goal.steps
                    } else goal.steps,
                    lastActivityAt = now,
                    createdAt = now,
                )
            } else goal
            goalRepo.upsert(
                if (themeId != null) {
                    toSave.copy(
                        primaryThemeId = toSave.primaryThemeId ?: themeId,
                        themeIds = (toSave.themeIds + themeId).distinct(),
                    )
                } else toSave,
            )
        }
    }

    fun saveHabit(habit: Habit, themeId: String?) {
        viewModelScope.launch {
            val toSave = if (habit.id.isBlank()) {
                habit.copy(
                    id = UUID.randomUUID().toString(),
                    startedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                )
            } else habit
            habitRepo.upsert(
                if (themeId != null) {
                    toSave.copy(
                        primaryThemeId = toSave.primaryThemeId ?: themeId,
                        themeIds = (toSave.themeIds + themeId).distinct(),
                    )
                } else toSave,
            )
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch { habitRepo.delete(id) }
    }

    // ── goal actions (carried from the interim goals home) ────

    fun toggleStep(goal: Goal, index: Int) {
        viewModelScope.launch {
            val steps = goal.steps.mapIndexed { i, s -> if (i == index) s.copy(done = !s.done) else s }
            goalRepo.upsert(goal.copy(steps = steps, lastActivityAt = System.currentTimeMillis()))
        }
    }

    fun setCurrent(goalId: String, value: Double) {
        viewModelScope.launch { goalRepo.advanceDirect(goalId, value) }
    }

    fun completeGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepo.upsert(goal.copy(state = GoalState.COMPLETED, completedAt = System.currentTimeMillis()))
        }
    }

    fun setGoalState(goal: Goal, state: GoalState) {
        viewModelScope.launch { goalRepo.upsert(goal.copy(state = state)) }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch { goalRepo.delete(goalId) }
    }

    /** DM-TASK auto-bridge: a quick aligned task straight from the theme. */
    fun quickAddTask(title: String, themeId: String) {
        viewModelScope.launch {
            taskRepo.saveDraft(
                com.thefoxworks.tzafon.domain.model.TaskDraft(id = null, title = title, themeId = themeId),
                com.thefoxworks.tzafon.domain.model.EditScope.ONE,
                today,
            )
        }
    }
}
