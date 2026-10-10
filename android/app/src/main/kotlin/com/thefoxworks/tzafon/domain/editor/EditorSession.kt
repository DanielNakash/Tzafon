package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.StatusPick
import com.thefoxworks.tzafon.domain.status.StatusSheet

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
     * What tapping a status chip does: into the draft, the scope panel, or
     * nothing. [series] is the stored series' facts (null for a one-off).
     * Going back to the status the editor opened with only undoes a draft
     * change, so it never asks.
     */
    fun chip(target: TaskState, series: SeriesStatus?): StatusPick =
        if (target == snapshot.state && target != draft.state) StatusPick.Apply(target)
        else StatusSheet.pick(draft.state, target, series)

    /**
     * A single-task status into the draft, applied at Save after the field
     * edits. Done waits with its "How much?" [amount]; Backlog clears the
     * to-do date (FR-BACKLOG-1).
     */
    fun status(target: TaskState, amount: Double? = null) = edit {
        it.copy(
            state = target,
            habitAmount = amount.takeIf { target == TaskState.DONE },
            toDoDate = if (target == TaskState.BACKLOG) null else it.toDoDate,
        )
    }

    /** A to-do date picked or cleared; a date brings a Backlog draft back to Open (FR-BACKLOG-5). */
    fun toDo(date: String?) = edit {
        it.copy(toDoDate = date, state = if (date != null && it.state == TaskState.BACKLOG) TaskState.OPEN else it.state)
    }

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

    /** [saveProblem] over the Alignment pane's [cue] as typed. */
    fun saveProblem(cue: CueDraft): SaveProblem? = saveProblem(cueTimeIncomplete = cue.timeIncomplete)

    companion object {
        /**
         * The editor opens on Task, except from Reschedule, which opens on
         * Timing; the snapshot includes any presets. A quick-add's
         * [parsedCue] (FR-CAPTURE-3.6) is an edit, not a preset: it shows
         * pre-filled and lights the Alignment dot (FR-EDITOR-6.10).
         */
        fun open(draft: TaskDraft, tab: Pane = Pane.TASK, parsedCue: Cue? = null) =
            EditorSession(snapshot = draft, draft = draft.copy(cue = parsedCue ?: draft.cue), tab = tab)
    }
}
