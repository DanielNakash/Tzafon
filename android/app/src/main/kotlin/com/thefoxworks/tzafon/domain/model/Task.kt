package com.thefoxworks.tzafon.domain.model

import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Rule
import com.thefoxworks.tzafon.domain.recurrence.SeriesSpec

/**
 * DM-TASK-1 — the five-state machine, the native task shape from the first
 * write (DEC-1a: no legacy boolean `done`, no migration).
 */
enum class TaskState { OPEN, DONE, CLOSED, FROZEN, BACKLOG }

/** DM-CUE-2 — the trigger half of "When X, I will do Y". */
enum class CueType { AFTER_ROUTINE, AT_TIME, AT_PLACE }

data class Cue(
    val type: CueType,
    val label: String,          // "After the first coffee" / "8:30" / "At the studio"
    val time: String? = null,   // HH:mm — reminders for AT_TIME (and optionally AFTER_ROUTINE)
) {
    /**
     * FR-CUE-3 — the canonical human-readable form of a saved cue, shared by
     * every display surface (task rows, editor cue slot, habit editor cue row,
     * notification body). `label · time` when both are set, `label` alone when
     * there's no time, or `time` alone when the label is empty (post-FR-CUE-2
     * label-less AT_TIME cues).
     */
    fun display(): String {
        val l = label.trim()
        val t = time?.trim()
        return when {
            l.isEmpty() -> t.orEmpty()
            t.isNullOrEmpty() || l == t -> l
            else -> "$l · $t"
        }
    }
}

data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val toDoDate: String? = null,      // FR-CAPTURE-2: undated by default
    val dueDate: String? = null,
    val state: TaskState = TaskState.OPEN,
    val seriesId: String? = null,
    val occurrenceDate: String? = null,
    val overridden: Boolean = false,
    val cue: Cue? = null,              // DM-TASK-4
    val themeId: String? = null,       // DM-TASK-5 / D1 direct link
    val habitId: String? = null,       // at most one (DM-REL)
    val goalIds: List<String> = emptyList(),
    val commitment: String? = null,    // DM-TASK-7
    val focusDate: String? = null,     // DM-FOCUS-1 markers
    val focusWeekStart: String? = null,
    val sortOrder: Long = 0,
    val createdAt: Long = 0,
    val stateChangedAt: Long = 0,
    val completedAt: Long? = null,
)

data class Series(
    val id: String,
    val title: String,
    val description: String = "",
    val rule: Rule,
    val ruleAnchor: String,
    val startDate: String,
    val endDate: String? = null,
    val dueMode: DueMode = DueMode.NONE,
    val dueRule: Rule? = null,
    val dueSingular: String? = null,
    val exceptions: List<String> = emptyList(),
    val generatedThrough: String? = null,
    val frozen: Boolean = false,       // FR-REC-5: freeze terminates generation
    // template alignment inherited by newly generated occurrences
    val cue: Cue? = null,
    val themeId: String? = null,
    val habitId: String? = null,
    val goalIds: List<String> = emptyList(),
    val createdAt: Long = 0,
) {
    fun toSpec() = SeriesSpec(
        rule = rule,
        ruleAnchor = ruleAnchor,
        startDate = startDate,
        endDate = endDate,
        dueMode = dueMode,
        dueRule = dueRule,
        dueSingular = dueSingular,
        exceptions = exceptions,
    )
}

/**
 * The editor's working draft — a flattened task + recurrence, mirroring the
 * v1.1.0 hydrateEditorTask shape so save semantics stay oracle-faithful.
 */
data class TaskDraft(
    val id: String?,                   // null = new
    val title: String,
    val description: String = "",
    val toDoDate: String? = null,
    val dueDate: String? = null,
    val state: TaskState = TaskState.OPEN,
    val recurrence: RecurrenceDraft? = null,
    val seriesId: String? = null,
    val occurrenceDate: String? = null,
    val cue: Cue? = null,
    val themeId: String? = null,
    val habitId: String? = null,
    val goalIds: List<String> = emptyList(),
    val commitment: String? = null,
)

/** The editor's flattened recurrence: the rule + its meta. */
data class RecurrenceDraft(
    val rule: Rule,
    val endDate: String? = null,
    val dueMode: DueMode = DueMode.NONE,
    val dueRule: Rule? = null,
)

/** Recurring-edit scope, carried from v1.1.0: this one / this & future / all. */
enum class EditScope { ONE, FORWARD, ALL }
