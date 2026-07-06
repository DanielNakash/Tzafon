package com.thefoxworks.tzafon.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.habits.HabitMath
import com.thefoxworks.tzafon.domain.model.Contribution
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.ReviewKind
import com.thefoxworks.tzafon.domain.model.ReviewRepository
import com.thefoxworks.tzafon.domain.model.ReviewStatus
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.review.ReviewLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class ReviewUiState(
    val today: String = Dates.todayIso(),
    val slot: String = "",
    val kind: ReviewKind = ReviewKind.WEEKLY,
    val kicker: String = "",
    // ── reflect (the mirror, DM-REVIEW-2.1) ──
    val doneCount: Int = 0,
    val alignedCount: Int = 0,
    val habitRates: List<ReviewLogic.HabitRate> = emptyList(),
    val goalDeltas: List<ReviewLogic.GoalDelta> = emptyList(),
    // ── plan (the fresh start, DM-REVIEW-2.2) ──
    val candidates: List<Task> = emptyList(),
    val picked: Set<String> = emptySet(),
    val staleGoal: Goal? = null,
    val servesByTask: Map<String, String> = emptyMap(),
)

/** The weekly/monthly Review (DM-REVIEW) — reflect, then a fresh start. */
class ReviewViewModel(
    private val reviewRepo: ReviewRepository,
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository,
    private val goalRepo: GoalRepository,
    settings: SettingsStore,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    private val picked = kotlinx.coroutines.flow.MutableStateFlow<Set<String>?>(null)

    private data class Src(
        val tasks: List<Task>,
        val habits: List<Habit>,
        val logs: List<HabitLog>,
        val goals: List<Goal>,
        val contributions: List<Contribution>,
    )

    val uiState: StateFlow<ReviewUiState> =
        combine(
            combine(
                taskRepo.observeTasks(), habitRepo.observeHabits(), habitRepo.observeLogs(),
                goalRepo.observeGoals(), goalRepo.observeContributions(),
            ) { t, h, l, g, c -> Src(t, h, l, g, c) },
            settings.weekStart,
            picked,
        ) { src, weekStart, picks ->
            build(src, weekStart, picks)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReviewUiState())

    private fun build(src: Src, weekStart: String, picks: Set<String>?): ReviewUiState {
        val today = Dates.todayIso()
        val slot = ReviewLogic.slotFor(today, weekStart)
        val kind = ReviewLogic.kindFor(slot)
        // reflect on the period behind us: the previous week
        val refStart = Dates.addDays(slot, -7)
        val refEnd = Dates.addDays(slot, -1)
        val zone = ZoneId.systemDefault()

        fun completedDate(t: Task): String? = t.completedAt?.let {
            Dates.iso(Instant.ofEpochMilli(it).atZone(zone).toLocalDate())
        }

        val doneInPeriod = src.tasks.filter { t ->
            t.state == TaskState.DONE && completedDate(t)?.let { it in refStart..refEnd } == true
        }
        val aligned = doneInPeriod.count {
            it.themeId != null || it.habitId != null || it.goalIds.isNotEmpty()
        }

        val rates = src.habits.map { h ->
            val week = HabitMath.week(h, src.logs, refStart, weekStart)
            val target = if (h.kind == HabitKind.FREQUENCY) h.target.toInt() else (h.targetDays ?: 7)
            ReviewLogic.HabitRate(
                name = h.name,
                done = week.doneDays,
                target = target,
                pct = if (target > 0) (week.doneDays * 100 / target).coerceAtMost(100) else 0,
                quantLine = if (h.kind == HabitKind.QUANTITATIVE && week.amountSum > 0) {
                    "${fmt(week.amountSum)} ${h.unit ?: ""}"
                } else null,
            )
        }

        val goalsById = src.goals.associateBy { it.id }
        val deltas = src.contributions
            .filter { c ->
                val d = Dates.iso(Instant.ofEpochMilli(c.createdAt).atZone(zone).toLocalDate())
                d in refStart..refEnd
            }
            .groupBy { it.goalId }
            .mapNotNull { (goalId, rows) ->
                goalsById[goalId]?.let { g ->
                    ReviewLogic.GoalDelta(g.title, "+${fmt(rows.sumOf { it.amount })} ${g.unit ?: ""}")
                }
            }

        // plan: open tasks landing this week, plus fresh undated captures
        val planEnd = Dates.addDays(slot, 6)
        val candidates = src.tasks
            .filter { it.state == TaskState.OPEN }
            .filter { (it.toDoDate != null && it.toDoDate in slot..planEnd) || it.toDoDate == null }
            .sortedWith(compareBy({ it.toDoDate == null }, { it.toDoDate }, { it.createdAt }))
            .take(12)

        val currentPicks = picks
            ?: src.tasks.filter { it.focusWeekStart == slot }.map { it.id }.toSet()

        val staleMs = 14L * 24 * 60 * 60 * 1000
        val stale = src.goals.firstOrNull {
            it.state == GoalState.ONGOING && it.lastActivityAt > 0 &&
                System.currentTimeMillis() - it.lastActivityAt > staleMs
        }

        val serves = src.tasks.associate { t ->
            t.id to when {
                t.habitId != null -> src.habits.firstOrNull { it.id == t.habitId }?.name
                t.goalIds.isNotEmpty() -> goalsById[t.goalIds.first()]?.title
                else -> null
            }.orEmpty()
        }.filterValues { it.isNotEmpty() }

        return ReviewUiState(
            today = today,
            slot = slot,
            kind = kind,
            kicker = ReviewLogic.inviteKicker(kind, slot),
            doneCount = doneInPeriod.size,
            alignedCount = aligned,
            habitRates = rates,
            goalDeltas = deltas,
            candidates = candidates,
            picked = currentPicks,
            staleGoal = stale,
            servesByTask = serves,
        )
    }

    fun togglePick(id: String) {
        picked.value = uiState.value.picked.let { if (id in it) it - id else it + id }
    }

    /** Leaving mid-way keeps the progress (DM-REVIEW-4 — resumable). */
    fun savePartial(note: String) {
        viewModelScope.launch {
            val s = uiState.value
            reviewRepo.upsert(
                Review(
                    id = s.slot, kind = s.kind,
                    periodStart = s.slot, periodEnd = ReviewLogic.periodEnd(s.slot),
                    status = ReviewStatus.PARTIAL,
                    reflectNote = note.trim().takeIf { it.isNotBlank() },
                ),
            )
        }
    }

    /** "Start the week" — persist the artifact + stamp the priorities. */
    fun complete(note: String) {
        viewModelScope.launch {
            val s = uiState.value
            taskRepo.setWeekPriorities(s.picked.toList(), s.slot)
            reviewRepo.upsert(
                Review(
                    id = s.slot, kind = s.kind,
                    periodStart = s.slot, periodEnd = ReviewLogic.periodEnd(s.slot),
                    status = ReviewStatus.DONE,
                    reflectNote = note.trim().takeIf { it.isNotBlank() },
                    snapshot = ReviewLogic.Snapshot(s.doneCount, s.alignedCount, s.habitRates, s.goalDeltas).encode(),
                    completedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun freezeGoal(goal: Goal) {
        viewModelScope.launch { goalRepo.upsert(goal.copy(state = GoalState.FROZEN)) }
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else v.toString()
}
