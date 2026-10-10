package com.thefoxworks.tzafon.domain.status

import com.thefoxworks.tzafon.domain.attribution.Attribution
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalRepository
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitRepository
import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskRepository
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The toast on screen; [id] tells a replacement from the toast it replaced. */
data class ShownToast(val id: Long, val text: String, val undoable: Boolean)

/** The "How much?" prompt a Done waits on (DM-HABIT-5 / DM-GOAL-3). */
data class AmountPrompt(val title: String, val unit: String?, val suggested: Double?) {
    companion object {
        /** The prompt to put before marking [task] done with these links, or null if none is needed. */
        fun of(task: Task, habit: Habit?, goalsById: Map<String, Goal>): AmountPrompt? {
            if (!Attribution.needsAmountPrompt(task, habit, goalsById)) return null
            return AmountPrompt(
                title = "How much? · ${habit?.name ?: task.title}",
                unit = Attribution.promptUnit(task, habit, goalsById),
                suggested = habit?.target,
            )
        }
    }
}

/**
 * v2.13.0 — every status change made from a list (a checkbox tap or a
 * status-sheet pick) goes through here: it applies the change, raises the
 * app's one toast, and holds what Undo needs for as long as that toast shows.
 * A new toast replaces the old one, and the old one's Undo goes with it.
 * Editor saves don't come through here, so they raise no toast.
 */
class StatusChanges(
    private val tasks: TaskRepository,
    private val habits: HabitRepository?,
    private val goals: GoalRepository?,
    private val today: () -> String,
    /** FR-AUDIO-1.2 — runs on Open → Done only (the chime) */
    private val onDone: suspend () -> Unit = {},
) {
    private class Ticket(val toastId: Long, val before: StatusSnapshot, val after: Task)

    private val lock = Mutex()
    private var nextId = 1L
    private var ticket: Ticket? = null
    private val _toast = MutableStateFlow<ShownToast?>(null)
    val toast: StateFlow<ShownToast?> = _toast.asStateFlow()

    /** What the status sheet shows for [task]: its series' facts and the names of its links. */
    suspend fun subject(task: Task): StatusSubject {
        val series = task.seriesId?.let { tasks.getSeries(it) }
        return StatusSubject(
            title = task.title,
            state = task.state,
            toDoDate = task.toDoDate,
            repeat = series?.let { Recurrence.summary(it.rule) },
            series = series?.let { SeriesStatus.of(it, today()) },
            habit = task.habitId?.let { habits?.getHabit(it)?.name },
            goals = task.goalIds.mapNotNull { goals?.getGoal(it)?.title },
        )
    }

    /** The prompt to put before marking [task] done, or null if none is needed. */
    suspend fun amountPrompt(task: Task): AmountPrompt? {
        val habit = task.habitId?.let { habits?.getHabit(it) }
        val goalsById = task.goalIds.mapNotNull { goals?.getGoal(it) }.associateBy { it.id }
        return AmountPrompt.of(task, habit, goalsById)
    }

    /**
     * Apply [target] to the task and raise its toast. [scope] is the answer
     * to StateMachine.askFor, where the sheet asked.
     */
    suspend fun change(
        taskId: String,
        target: TaskState,
        habitAmount: Double? = null,
        scope: StatusScope = StatusScope.THIS_ONE,
    ) = lock.withLock {
        val day = today()
        val before = tasks.statusSnapshot(taskId) ?: return@withLock
        val series = before.task.seriesId?.let { tasks.getSeries(it) }
        val fx = runCatching {
            StateMachine.transition(before.task.state, target, series?.let { SeriesStatus.of(it, day) }, scope)
        }.getOrNull() ?: return@withLock

        tasks.setState(taskId, target, day, habitAmount, scope)
        val after = tasks.getTask(taskId)
        if (before.task.state == TaskState.OPEN && target == TaskState.DONE) onDone()

        val seriesChange = StatusUndo.seriesChange(fx)
        val doneNow = target == TaskState.DONE && before.task.state != TaskState.DONE
        val copy = StatusToastCopy.of(
            title = before.task.title,
            target = target,
            series = seriesChange,
            habit = if (doneNow) before.task.habitId?.let { habits?.getHabit(it)?.name } else null,
            goals = if (doneNow) goalMoves(taskId) else emptyList(),
        )
        val id = nextId++
        ticket = if (copy.undoable && after != null) Ticket(id, before, after) else null
        _toast.value = ShownToast(id, copy.text, copy.undoable)
    }

    /** Undo the change behind toast [toastId], if it's still the one showing. */
    suspend fun undo(toastId: Long) = lock.withLock {
        val t = ticket?.takeIf { it.toastId == toastId }
        ticket = null
        if (_toast.value?.id == toastId) _toast.value = null
        if (t != null) tasks.restoreStatus(t.before, t.after)
    }

    /** The toast timed out or was swiped away. */
    fun dismiss(toastId: Long) {
        if (_toast.value?.id == toastId) {
            _toast.value = null
            if (ticket?.toastId == toastId) ticket = null
        }
    }

    /** Amount-measuring goals the Done moved, read back after the change. */
    private suspend fun goalMoves(taskId: String): List<GoalMove> {
        val g = goals ?: return emptyList()
        return g.contributionsFor(taskId).map { it.goalId }.distinct().mapNotNull { id ->
            g.getGoal(id)?.let { GoalMove(it.title, it.currentQty, it.unit) }
        }
    }
}
