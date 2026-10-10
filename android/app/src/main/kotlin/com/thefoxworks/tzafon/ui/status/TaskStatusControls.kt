package com.thefoxworks.tzafon.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.AmountPrompt
import com.thefoxworks.tzafon.domain.status.StatusChanges
import com.thefoxworks.tzafon.domain.status.StatusSubject
import com.thefoxworks.tzafon.domain.status.TapAction
import com.thefoxworks.tzafon.domain.status.TapRule
import com.thefoxworks.tzafon.ui.components.AmountSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The app's [StatusChanges] with the scope its changes run in. The scope
 * outlives any one screen, so a change isn't cut off by leaving the tab.
 */
class StatusHost(val changes: StatusChanges, val scope: CoroutineScope)

val LocalStatusHost = staticCompositionLocalOf<StatusHost?> { null }

/** Opens the task editor on a task at a pane: Task from "Open editor", Timing from Reschedule. */
val LocalEditorOpener = staticCompositionLocalOf<((taskId: String, tab: Pane) -> Unit)?> { null }

/**
 * One list's checkbox wiring (v2.13.0): the tap rule, the hold, the status
 * sheet and the "How much?" prompt, shared by Today, Planning, All Tasks and
 * Backlog. Render its sheets with [TaskStatusSheets].
 */
@Stable
class TaskStatusControls internal constructor(
    private val host: StatusHost?,
    private val openEditor: ((String, Pane) -> Unit)?,
) {
    /** A Done waiting on "How much?", with the scope the sheet chose. */
    internal class AmountAsk(val task: Task, val prompt: AmountPrompt, val scope: StatusScope)

    internal var sheet by mutableStateOf<Pair<Task, StatusSubject>?>(null)
    internal var amountAsk by mutableStateOf<AmountAsk?>(null)

    /** A plain tap: Open ↔ Done, else the sheet; "How much?" first where it applies. */
    fun tap(task: Task) {
        val h = host ?: return
        h.scope.launch {
            val prompt = if (task.state == TaskState.OPEN) h.changes.amountPrompt(task) else null
            when (val action = TapRule.forTap(task.state, needsAmount = prompt != null)) {
                is TapAction.SetStatus -> h.changes.change(task.id, action.target)
                TapAction.AskAmount -> amountAsk = AmountAsk(task, prompt!!, StatusScope.THIS_ONE)
                TapAction.OpenSheet -> sheet = task to h.changes.subject(task)
            }
        }
    }

    /** The hold (or TalkBack's "Change status"): raise the status sheet. */
    fun hold(task: Task) {
        val h = host ?: return
        h.scope.launch { sheet = task to h.changes.subject(task) }
    }

    /** Reschedule: the editor on Timing, where the date applies at Save. */
    fun reschedule(task: Task) {
        openEditor?.invoke(task.id, Pane.TIMING)
    }

    internal fun openEditor(task: Task) {
        openEditor?.invoke(task.id, Pane.TASK)
    }

    internal val canOpenEditor: Boolean get() = openEditor != null

    /** A pick the sheet has settled, scope and all: "How much?" first for a Done that needs it. */
    internal fun apply(task: Task, target: TaskState, scope: StatusScope) {
        val h = host ?: return
        h.scope.launch {
            val prompt = if (target == TaskState.DONE && task.state != TaskState.DONE) h.changes.amountPrompt(task) else null
            if (prompt != null) amountAsk = AmountAsk(task, prompt, scope)
            else h.changes.change(task.id, target, scope = scope)
        }
    }

    internal fun confirmAmount(ask: AmountAsk, amount: Double) {
        val h = host ?: return
        h.scope.launch { h.changes.change(ask.task.id, TaskState.DONE, habitAmount = amount, scope = ask.scope) }
    }
}

@Composable
fun rememberTaskStatusControls(): TaskStatusControls {
    val host = LocalStatusHost.current
    val opener = LocalEditorOpener.current
    return remember(host, opener) { TaskStatusControls(host, opener) }
}

/** The status sheet and the "How much?" prompt for [controls]. */
@Composable
fun TaskStatusSheets(controls: TaskStatusControls) {
    controls.sheet?.let { (task, subject) ->
        val close = { controls.sheet = null }
        TaskStatusSheet(
            subject = subject,
            onApply = { target, scope -> close(); controls.apply(task, target, scope) },
            onClose = close,
            onReschedule = if (controls.canOpenEditor) ({ close(); controls.reschedule(task) }) else null,
            onOpenEditor = if (controls.canOpenEditor) ({ close(); controls.openEditor(task) }) else null,
        )
    }
    controls.amountAsk?.let { ask ->
        AmountSheet(
            title = ask.prompt.title,
            unit = ask.prompt.unit,
            suggested = ask.prompt.suggested,
            onConfirm = { controls.confirmAmount(ask, it) },
            onClose = { controls.amountAsk = null },
        )
    }
}
