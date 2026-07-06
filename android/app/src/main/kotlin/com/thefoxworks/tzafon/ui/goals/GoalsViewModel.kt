package com.thefoxworks.tzafon.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.GoalStep
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class GoalCardState(
    val goal: Goal,
    /** DM-GOAL-6 / D2 — the habits driving this goal ("ENGINE" chips) */
    val engines: List<Habit>,
    /** DM-GOAL-5 — ~2 weeks without activity */
    val stale: Boolean,
    /** goal-gradient framing: near done → emphasise what's left */
    val nearDone: Boolean,
)

data class GoalsUiState(
    val today: String = Dates.todayIso(),
    val ongoing: List<GoalCardState> = emptyList(),
    val frozen: List<GoalCardState> = emptyList(),
    val completed: List<GoalCardState> = emptyList(),
    /** DM-GOAL-5 — dismissible soft warning past 5 active */
    val overFive: Boolean = false,
)

/** Goals (DM-GOAL) — the finite pursuits; the M6 hub will nest them. */
class GoalsViewModel(
    private val repo: GoalRepository,
    private val habitRepo: HabitRepository,
) : ViewModel() {

    private val staleMs = 14L * 24 * 60 * 60 * 1000 // ~2 weeks, tunable (§11)

    val uiState: StateFlow<GoalsUiState> =
        combine(repo.observeGoals(), habitRepo.observeHabits()) { goals, habits ->
            val now = System.currentTimeMillis()
            fun card(g: Goal) = GoalCardState(
                goal = g,
                engines = habits.filter { it.goalId == g.id },
                stale = g.state == GoalState.ONGOING &&
                    g.lastActivityAt > 0 && now - g.lastActivityAt > staleMs,
                nearDone = g.state == GoalState.ONGOING && g.pct() >= 80,
            )
            val sorted = goals.sortedBy { it.createdAt }
            GoalsUiState(
                today = Dates.todayIso(),
                ongoing = sorted.filter { it.state == GoalState.ONGOING }.map(::card),
                frozen = sorted.filter { it.state == GoalState.FROZEN }.map(::card),
                completed = sorted.filter { it.state == GoalState.COMPLETED }
                    .sortedByDescending { it.completedAt ?: 0 }.map(::card),
                overFive = goals.count { it.state == GoalState.ONGOING } > 5,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    fun save(goal: Goal) {
        viewModelScope.launch {
            val toSave = if (goal.id.isBlank()) {
                val now = System.currentTimeMillis()
                // DM-GOAL-2 — endowed first step, already checked
                val endowed = if (goal.type == GoalType.STEPPED &&
                    goal.steps.none { it.label == "Defined this goal ✓" }
                ) {
                    listOf(GoalStep("Defined this goal ✓", true)) + goal.steps
                } else goal.steps
                goal.copy(
                    id = UUID.randomUUID().toString(),
                    steps = endowed,
                    lastActivityAt = now,
                    createdAt = now,
                )
            } else goal
            repo.upsert(toSave)
        }
    }

    /** Toggle one step on a stepped goal (progress the direct way). */
    fun toggleStep(goal: Goal, index: Int) {
        viewModelScope.launch {
            val steps = goal.steps.mapIndexed { i, s -> if (i == index) s.copy(done = !s.done) else s }
            repo.upsert(goal.copy(steps = steps, lastActivityAt = System.currentTimeMillis()))
        }
    }

    /** DM-GOAL-3 — direct edit of an accumulative goal's progress. */
    fun setCurrent(goalId: String, value: Double) {
        viewModelScope.launch { repo.advanceDirect(goalId, value) }
    }

    /** DM-GOAL-4 — completion (the celebration sheet drives the rebound). */
    fun complete(goal: Goal) {
        viewModelScope.launch {
            repo.upsert(
                goal.copy(
                    state = GoalState.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun setState(goal: Goal, state: GoalState) {
        viewModelScope.launch {
            repo.upsert(goal.copy(state = state, completedAt = goal.completedAt))
        }
    }

    fun delete(goalId: String) {
        viewModelScope.launch { repo.delete(goalId) }
    }

    /** "Make a habit of it" — the completion rebound (DM-GOAL-4). */
    fun saveHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepo.upsert(
                habit.copy(
                    id = UUID.randomUUID().toString(),
                    startedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
