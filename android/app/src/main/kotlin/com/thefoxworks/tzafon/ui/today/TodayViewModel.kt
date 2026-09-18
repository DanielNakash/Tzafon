package com.thefoxworks.tzafon.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thefoxworks.tzafon.data.audio.ChimePlayer
import com.thefoxworks.tzafon.data.settings.SettingsStore
import com.thefoxworks.tzafon.domain.action.ActionLogic
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.Review
import com.thefoxworks.tzafon.domain.model.ReviewRepository
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.domain.review.ReviewLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class TodayUiState(
    val today: String = Dates.todayIso(),
    val focus: List<Task> = emptyList(),          // DM-FOCUS Today's-Focus marks
    val alsoToday: List<Task> = emptyList(),      // the reorderable main list
    val weekPriorities: List<Task> = emptyList(), // the weekly strip (FR-LOOP-1)
    val doneToday: List<Task> = emptyList(),
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val slippedCount: Int = 0,                    // FR-TODAY-4
    val showSlippage: Boolean = false,
    val showOverload: Boolean = false,            // FR-TODAY-5
    val habitsById: Map<String, Habit> = emptyMap(),
    val goalsById: Map<String, Goal> = emptyMap(),
    /** DM-REVIEW — the gentle invite ("SUNDAY · A GENTLE LOOK BACK") */
    val reviewInvite: String? = null,
    /** DM-FOCUS-2 — the ~3/day soft cap, nudged where it's set */
    val focusOverCap: Boolean = false,
)

class TodayViewModel(
    private val repo: TaskRepository,
    private val settings: SettingsStore,
    habitRepo: HabitRepository,
    goalRepo: GoalRepository,
    reviewRepo: ReviewRepository,
    private val chimePlayer: ChimePlayer? = null,
) : ViewModel() {

    val today: String get() = Dates.todayIso()

    private data class Refs(val habits: List<Habit>, val goals: List<Goal>, val reviews: List<Review>, val weekStart: String)

    val uiState: StateFlow<TodayUiState> =
        combine(
            repo.observeTasks(),
            settings.slippageDismissedOn,
            settings.overloadDismissedOn,
            combine(
                habitRepo.observeHabits(), goalRepo.observeGoals(),
                reviewRepo.observeReviews(), settings.weekStart,
            ) { h, g, r, ws -> Refs(h, g, r, ws) },
        ) { tasks, slipDismissed, overDismissed, refs ->
            build(tasks, slipDismissed, overDismissed, refs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    init {
        viewModelScope.launch { repo.topUp(today, Recurrence.HORIZON_DAYS) }
    }

    private fun build(
        tasks: List<Task>,
        slipDismissed: String?,
        overDismissed: String?,
        refs: Refs,
    ): TodayUiState {
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

        // the weekly strip: priorities set at Review that aren't already on today
        val slot = ReviewLogic.slotFor(today, refs.weekStart)
        val weekPriorities = tasks.filter {
            it.state == TaskState.OPEN && it.focusWeekStart == slot &&
                !ActionLogic.isTodayTask(it, today)
        }.sortedBy { it.toDoDate ?: "~" }

        // the review invite (DM-REVIEW-4 window rules)
        val existing = refs.reviews.firstOrNull { it.id == slot }
        val invite = if (ReviewLogic.shouldSurface(existing, today, refs.weekStart)) {
            ReviewLogic.inviteKicker(ReviewLogic.kindFor(slot), slot)
        } else null

        return TodayUiState(
            today = today,
            focus = focus,
            alsoToday = also,
            weekPriorities = weekPriorities,
            doneToday = done,
            doneCount = done.size,
            totalCount = open.size + done.size,
            slippedCount = slipped,
            showSlippage = slipped > 0 && slipDismissed != today,
            // FR-TODAY-5: static threshold; never nags alongside the focus cap
            showOverload = ActionLogic.isOverloaded(open.size) && overDismissed != today && focus.size <= 3,
            habitsById = refs.habits.associateBy { it.id },
            goalsById = refs.goals.associateBy { it.id },
            reviewInvite = invite,
            focusOverCap = focus.size > 3,
        )
    }

    /**
     * FR-AUDIO-1.2 — the checkbox tap path (Today's list, done list, week
     * priorities strip). Reads state before the write so we can play the chime
     * exactly on Open → Done; toggles back on Done → Open are silent. The
     * chime fires on the same coroutine as the state write but as a
     * fire-and-forget side effect (FR-AUDIO-1.9 — never blocks or throttles).
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

    /** DM-FOCUS-1 — point a task north for today (or unpoint it). */
    fun toggleFocus(task: Task) {
        viewModelScope.launch {
            repo.setFocusDate(task.id, if (ActionLogic.isFocusToday(task, today)) null else today)
        }
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

    fun quickAdd(title: String, cueTime: String? = null) {
        viewModelScope.launch {
            repo.saveDraft(
                quickAddDraft(title, today, cueTime),
                com.thefoxworks.tzafon.domain.model.EditScope.ONE,
                today,
            )
        }
    }

    companion object {
        /**
         * FR-TODAY-7 — Today's own quick-add pre-fills `toDoDate` to today so the task
         * lands in the Today list immediately (FR-TODAY-1 membership rule). All other
         * capture surfaces retain FR-CAPTURE-2's undated default; this default only
         * fires for the initial write from Today's quick-add.
         *
         * FR-CAPTURE-3.9 — a time parsed off the title rides along as a label-less
         * AT_TIME cue; the date rule above is untouched, so the reminder fires the
         * same day with nothing further to do.
         */
        fun quickAddDraft(
            title: String,
            today: String,
            cueTime: String? = null,
        ): com.thefoxworks.tzafon.domain.model.TaskDraft =
            com.thefoxworks.tzafon.domain.model.TaskDraft(
                id = null,
                title = title,
                toDoDate = today,
                cue = com.thefoxworks.tzafon.domain.action.QuickAddParse.cueFor(cueTime),
            )
    }
}
