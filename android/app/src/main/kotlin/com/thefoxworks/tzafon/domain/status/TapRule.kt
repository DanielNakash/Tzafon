package com.thefoxworks.tzafon.domain.status

import com.thefoxworks.tzafon.domain.model.TaskState

/** What a plain tap on a list-row checkbox does (v2.13.0, every list). */
sealed interface TapAction {
    /** Mark done or mark open, straight away. */
    data class SetStatus(val target: TaskState) : TapAction

    /** Ask "How much?" first; marking done waits for the answer. */
    data object AskAmount : TapAction

    /** A tap never guesses a way out of Closed, Frozen or Backlog. */
    data object OpenSheet : TapAction
}

/**
 * The one tap rule for Today, Planning, All Tasks and Backlog: Open ↔ Done,
 * and the settled statuses open the status sheet. [needsAmount] is
 * Attribution.needsAmountPrompt for the task (DM-HABIT-5 / DM-GOAL-3), which
 * only matters on the way to Done.
 */
object TapRule {
    fun forTap(current: TaskState, needsAmount: Boolean): TapAction = when (current) {
        TaskState.OPEN -> if (needsAmount) TapAction.AskAmount else TapAction.SetStatus(TaskState.DONE)
        TaskState.DONE -> TapAction.SetStatus(TaskState.OPEN)
        TaskState.CLOSED, TaskState.FROZEN, TaskState.BACKLOG -> TapAction.OpenSheet
    }
}
