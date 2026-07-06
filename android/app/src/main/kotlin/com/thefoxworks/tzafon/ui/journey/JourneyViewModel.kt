package com.thefoxworks.tzafon.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.journey.JourneyLogic
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.ReviewKind
import com.thefoxworks.tzafon.domain.model.ReviewRepository
import com.thefoxworks.tzafon.domain.model.ReviewStatus
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A finished finite pursuit — the legitimate core of the mirror. */
data class Milestone(val goal: Goal, val theme: Theme?, val dateLabel: String)

/** A living practice — "Morning pages — 12 weeks of showing up". */
data class Arc(val habit: Habit, val detail: String, val theme: Theme?)

/** One reviewed period, strung into the narrative. */
data class ReviewRow(val kicker: String, val note: String?, val quiet: String?)

data class JourneyUiState(
    val loaded: Boolean = false,
    val quarterAligned: Int = 0,
    val milestones: List<Milestone> = emptyList(),
    val arcs: List<Arc> = emptyList(),
    val transitions: List<JourneyLogic.Transition> = emptyList(),
    val reviewRows: List<ReviewRow> = emptyList(),
) {
    /** FR-JOURNEY-4 — thin by design; one gentle line when nothing accrued. */
    val isEmpty: Boolean
        get() = loaded && quarterAligned == 0 && milestones.isEmpty() &&
            arcs.isEmpty() && transitions.isEmpty() && reviewRows.isEmpty()
}

class JourneyViewModel(
    taskRepo: TaskRepository,
    goalRepo: GoalRepository,
    habitRepo: HabitRepository,
    themeRepo: ThemeRepository,
    reviewRepo: ReviewRepository,
    settings: SettingsStore,
) : ViewModel() {

    private data class HabitSrc(val habits: List<Habit>, val logs: List<HabitLog>, val weekStart: String)

    val uiState: StateFlow<JourneyUiState> =
        combine(
            taskRepo.observeTasks(),
            goalRepo.observeGoals(),
            themeRepo.observeThemes(),
            reviewRepo.observeReviews(),
            combine(habitRepo.observeHabits(), habitRepo.observeLogs(), settings.weekStart) { h, l, ws ->
                HabitSrc(h, l, ws)
            },
        ) { tasks, goals, themes, reviews, habitSrc ->
            build(tasks, goals, themes, reviews, habitSrc)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JourneyUiState())

    private fun build(
        tasks: List<com.thefoxworks.tzafon.domain.model.Task>,
        goals: List<Goal>,
        themes: List<Theme>,
        reviews: List<Review>,
        habitSrc: HabitSrc,
    ): JourneyUiState {
        val today = Dates.todayIso()
        val themesById = themes.associateBy { it.id }

        val milestones = goals
            .filter { it.state == GoalState.COMPLETED }
            .sortedByDescending { it.completedAt ?: it.createdAt }
            .map { g ->
                Milestone(
                    goal = g,
                    theme = g.primaryThemeId?.let { themesById[it] },
                    dateLabel = JourneyLogic.monthYear(g.completedAt ?: g.createdAt),
                )
            }

        val arcs = habitSrc.habits
            .map { h ->
                val weeks = HabitMath.arcWeeks(h, habitSrc.logs, today, habitSrc.weekStart)
                Arc(
                    habit = h,
                    detail = JourneyLogic.arcDetail(weeks),
                    theme = h.primaryThemeId?.let { themesById[it] },
                ) to weeks
            }
            .sortedByDescending { it.second }
            .map { it.first }

        val reviewRows = reviews
            .filter { it.status == ReviewStatus.DONE || (it.status == ReviewStatus.PARTIAL && !it.reflectNote.isNullOrBlank()) }
            .sortedByDescending { it.periodStart }
            .take(8)
            .map { r ->
                val counts = JourneyLogic.snapshotCounts(r.snapshot)
                ReviewRow(
                    kicker = JourneyLogic.timelineKicker(r.periodStart, r.kind == ReviewKind.MONTHLY),
                    note = r.reflectNote?.takeIf { it.isNotBlank() },
                    quiet = counts?.let { (done, aligned) ->
                        "$done done · $aligned served a direction"
                    },
                )
            }

        return JourneyUiState(
            loaded = true,
            quarterAligned = JourneyLogic.quarterAligned(tasks, today),
            milestones = milestones,
            arcs = arcs,
            transitions = JourneyLogic.transitions(themes),
            reviewRows = reviewRows,
        )
    }
}
