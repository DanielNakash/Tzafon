package com.thefoxworks.tzafon.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.AmountPrompt
import com.thefoxworks.tzafon.domain.status.StatusChanges
import com.thefoxworks.tzafon.domain.status.TapAction
import com.thefoxworks.tzafon.domain.status.TapRule
import com.thefoxworks.tzafon.ui.components.AmountSheet
import com.thefoxworks.tzafon.ui.components.StateSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The app's [StatusChanges] with the scope its changes run in. The scope
 * outlives any one screen, so a change isn't cut off by leaving the tab.
 */
class StatusHost(val changes: StatusChanges, val scope: CoroutineScope)

val LocalStatusHost = staticCompositionLocalOf<StatusHost?> { null }

/**
 * One list's checkbox wiring (v2.13.0): the tap rule, the hold, the status
 * sheet and the "How much?" prompt, shared by Today, Planning, All Tasks and
 * Backlog. Render its sheets with [TaskStatusSheets].
 */
@Stable
class TaskStatusControls internal constructor(private val host: StatusHost?) {
    internal var sheetTask by mutableStateOf<Task?>(null)
    internal var amountAsk by mutableStateOf<Pair<Task, AmountPrompt>?>(null)

    /** A plain tap: Open ↔ Done, else the sheet; "How much?" first where it applies. */
    fun tap(task: Task) {
        val h = host ?: return
        h.scope.launch {
            val prompt = if (task.state == TaskState.OPEN) h.changes.amountPrompt(task) else null
            when (val action = TapRule.forTap(task.state, needsAmount = prompt != null)) {
                is TapAction.SetStatus -> h.changes.change(task.id, action.target)
                TapAction.AskAmount -> amountAsk = task to prompt!!
                TapAction.OpenSheet -> sheetTask = task
            }
        }
    }

    /** The hold (or TalkBack's "Change status"): raise the status sheet. */
    fun hold(task: Task) {
        sheetTask = task
    }

    internal fun pick(task: Task, target: TaskState) {
        val h = host ?: return
        h.scope.launch {
            val prompt = if (target == TaskState.DONE && task.state != TaskState.DONE) h.changes.amountPrompt(task) else null
            if (prompt != null) amountAsk = task to prompt else h.changes.change(task.id, target)
        }
    }

    internal fun confirmAmount(task: Task, amount: Double) {
        val h = host ?: return
        h.scope.launch { h.changes.change(task.id, TaskState.DONE, habitAmount = amount) }
    }
}

@Composable
fun rememberTaskStatusControls(): TaskStatusControls {
    val host = LocalStatusHost.current
    return remember(host) { TaskStatusControls(host) }
}

/** The status sheet and the "How much?" prompt for [controls]. */
@Composable
fun TaskStatusSheets(controls: TaskStatusControls) {
    controls.sheetTask?.let { task ->
        StateSheet(
            current = task.state,
            isRecurring = task.seriesId != null,
            onPick = { target -> controls.pick(task, target) },
            onClose = { controls.sheetTask = null },
        )
    }
    controls.amountAsk?.let { (task, prompt) ->
        AmountSheet(
            title = prompt.title,
            unit = prompt.unit,
            suggested = prompt.suggested,
            onConfirm = { controls.confirmAmount(task, it) },
            onClose = { controls.amountAsk = null },
        )
    }
}
