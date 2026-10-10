package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState

/** What stops Save, in the order Save reports them, and the pane that holds each. */
enum class SaveProblem(val pane: Pane) { BLANK_TITLE(Pane.TASK), INCOMPLETE_CUE_TIME(Pane.ALIGNMENT) }

/** What Save does once nothing stops it. */
sealed interface SaveStep {
    /** An existing task with nothing changed: close the editor and write nothing. */
    data object Close : SaveStep

    /** Save under [scope] without asking. */
    data class Write(val scope: EditScope) : SaveStep

    /** An existing repeating task with field changes: the "This task repeats" sheet. */
    data object AskScope : SaveStep
}

/**
 * v2.13.0 — one open editor: the opening [snapshot], the [draft] being edited
 * and the active [tab]. Immutable; every edit returns a new session.
 */
data class EditorSession(
    val snapshot: TaskDraft,
    val draft: TaskDraft,
    val tab: Pane,
) {
    private val diff: DraftChanges get() = DraftDiff.of(snapshot, draft)

    /** The panes whose tab shows the changed dot. */
    val changedPanes: Set<Pane> get() = diff.panes

    /** Cancel and Back ask "Discard your changes?" only when something changed. */
    val asksBeforeClosing: Boolean get() = !diff.isEmpty

    /** The task as opened is an occurrence of a series, so Save and Delete ask a scope. */
    val repeats: Boolean get() = snapshot.seriesId != null

    /**
     * Save asks "where does this land?" only for an existing repeating task
     * with field changes; a status-only save stays on this occurrence.
     */
    val saveStep: SaveStep get() {
        val changes = diff
        return when {
            snapshot.id != null && changes.isEmpty -> SaveStep.Close
            repeats && changes.asksScope -> SaveStep.AskScope
            else -> SaveStep.Write(changes.defaultScope)
        }
    }

    fun edit(change: (TaskDraft) -> TaskDraft) = copy(draft = change(draft))

    fun show(pane: Pane) = copy(tab = pane)

    /**
     * A status the interim status sheet has already written to the task, so
     * it is no longer a change and Discard can't throw it away. Goes once the
     * Task pane's status strip holds the status in the draft until Save.
     */
    fun stored(state: TaskState) = copy(snapshot = snapshot.copy(state = state), draft = draft.copy(state = state))

    /**
     * The first thing stopping Save, or null when it can save. A blank title
     * comes first, then an incomplete cue time ([cueTimeIncomplete] is the
     * Alignment pane's own input state, not part of the draft).
     */
    fun saveProblem(cueTimeIncomplete: Boolean = false): SaveProblem? = when {
        draft.title.isBlank() -> SaveProblem.BLANK_TITLE
        cueTimeIncomplete -> SaveProblem.INCOMPLETE_CUE_TIME
        else -> null
    }

    companion object {
        /** The editor always opens on Task; the snapshot includes any presets. */
        fun open(draft: TaskDraft) = EditorSession(snapshot = draft, draft = draft, tab = Pane.TASK)
    }
}
