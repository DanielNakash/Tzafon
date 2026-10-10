package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.editor.EditorSession
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.editor.SaveProblem
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.ui.components.CalendarPicker
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.Field
import com.thefoxworks.tzafon.ui.components.RecurrenceFields
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.Segmented
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir

/**
 * FR-BACKLOG-5.3 — toggling recurrence on the editor draft. Adding recurrence to a
 * Backlog-defaulted draft auto-promotes it to Open with today's To Do date, because
 * Backlog and recurrence are model-incompatible (FR-BACKLOG-4); the user's latest
 * explicit signal (adding recurrence) wins. Turning recurrence off clears the rule and
 * leaves the state untouched.
 */
internal fun toggleRecurrence(draft: TaskDraft, today: String): TaskDraft =
    if (draft.recurrence != null) {
        draft.copy(recurrence = null)
    } else {
        val promotingFromBacklog =
            draft.state == com.thefoxworks.tzafon.domain.model.TaskState.BACKLOG
        draft.copy(
            recurrence = RecurrenceDraft(
                rule = Recurrence.defaultRule(),
                dueMode = if (draft.dueDate != null) DueMode.SINGULAR else DueMode.NONE,
            ),
            state = if (promotingFromBacklog)
                com.thefoxworks.tzafon.domain.model.TaskState.OPEN
            else draft.state,
            toDoDate = if (promotingFromBacklog) today else draft.toDoDate,
        )
    }

/**
 * Full-screen task editor. v2.13.0: three panes behind a tab row —
 * Task (title, notes, status, delete), Timing (dates, repeat) and Alignment
 * (habit, goals, cue). It always opens on Task; a tab's dot means that pane
 * changed since opening; Cancel and Back ask before discarding changes; Save
 * jumps to the first problem instead of dimming. The snapshot, draft and tab
 * survive rotation and process death ([EditorSessionCodec]).
 * FR-CAPTURE-2: a new task starts undated.
 */
@Composable
fun TaskEditorScreen(
    initial: TaskDraft?,               // null = new task
    today: String,
    onSave: (TaskDraft, EditScope) -> Unit,
    onDelete: (String, EditScope) -> Unit,
    onSetState: ((String, com.thefoxworks.tzafon.domain.model.TaskState) -> Unit)? = null,
    onClose: () -> Unit,
    /** M4 — the habits available to link (DM-REL: at most one per task). */
    habits: List<com.thefoxworks.tzafon.domain.model.Habit> = emptyList(),
    /** M5 — goals to link (many allowed, one typical — DM-TASK-5). */
    goals: List<com.thefoxworks.tzafon.domain.model.Goal> = emptyList(),
) {
    // a quick-add expand passes a title-only draft (id = null) — still a new task
    val isNew = initial?.id == null
    var session by rememberSaveable(stateSaver = EditorSessionCodec.saver) {
        mutableStateOf(
            EditorSession.open(initial ?: TaskDraft(id = null, title = "", toDoDate = null)) // undated by default
        )
    }
    val draft = session.draft
    fun update(change: (TaskDraft) -> TaskDraft) { session = session.edit(change) }
    var scope by rememberSaveable { mutableStateOf(EditScope.ALL) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) } // todo | due | end | delete | cue | habit | goals | state | discard
    val rec = draft.recurrence
    val repeat = rec != null
    val singular = repeat && rec!!.dueMode == DueMode.SINGULAR
    // each pane keeps its own scroll position while tabs switch
    val scrolls = Pane.entries.associateWith { rememberScrollState() }
    val titleFocus = remember { FocusRequester() }
    var focusProblem by remember { mutableStateOf<SaveProblem?>(null) }

    val requestClose = { if (session.asksBeforeClosing) sheet = "discard" else onClose() }
    // system Back and predictive back ask the same as Cancel; with no changes
    // the nav host pops the editor as before
    BackHandler(enabled = session.asksBeforeClosing && sheet == null) { sheet = "discard" }

    LaunchedEffect(focusProblem, session.tab) {
        if (focusProblem == SaveProblem.BLANK_TITLE && session.tab == Pane.TASK) {
            scrolls.getValue(Pane.TASK).animateScrollTo(0)
            titleFocus.requestFocus()
            focusProblem = null
        }
    }

    Column(Modifier.fillMaxSize().background(Tz.colors.bg)) {
        // ── rust header bar ──
        Row(
            Modifier.fillMaxWidth().background(Tz.colors.rust).statusBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.pressable(requestClose).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TzIcons.Back(22.dp, Tz.colors.cream)
                Text("Cancel", style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp), color = Tz.colors.cream)
            }
            Text(
                if (isNew) "NEW TASK" else "EDIT TASK",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 2.sp),
                color = Tz.colors.cream.a(0.9f),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            // Save always looks enabled; a problem opens its pane and focuses the field
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Tz.colors.cream)
                    .pressable {
                        val problem = session.saveProblem()
                        if (problem == null) onSave(draft, scope)
                        else {
                            session = session.show(problem.pane)
                            focusProblem = problem
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text("Save", style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Tz.colors.rust)
            }
        }

        EditorTabs(
            active = session.tab,
            changed = session.changedPanes,
            onSelect = { session = session.show(it) },
        )

        Column(
            Modifier.weight(1f).verticalScroll(scrolls.getValue(session.tab))
                .padding(horizontal = 18.dp).padding(bottom = 40.dp),
        ) {
            when (session.tab) {
                Pane.TASK -> {
                    // ── recurring edit scope (until scope is asked at Save) ──
                    if (!isNew && repeat) {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 14.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Tz.colors.amber.a(0.14f))
                                .border(1.dp, Tz.colors.amber.a(0.4f), RoundedCornerShape(13.dp))
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                TzIcons.Repeat(15.dp, Tz.colors.rust)
                                Text(
                                    buildString { append("Repeating task. Apply changes to:") },
                                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                                    color = Tz.colors.ink,
                                )
                            }
                            Box(Modifier.padding(top = 10.dp)) {
                                Segmented(
                                    options = listOf(EditScope.ONE to "This one", EditScope.FORWARD to "This & future", EditScope.ALL to "All"),
                                    value = scope,
                                    onSelect = { scope = it },
                                    small = true,
                                )
                            }
                            Text(
                                when (scope) {
                                    EditScope.ONE -> "Reschedules or edits only this occurrence — the series is untouched."
                                    EditScope.FORWARD -> "Edits this and every future occurrence in place. The series is not split."
                                    EditScope.ALL -> "Edits the whole series, past and future occurrences alike."
                                },
                                style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp),
                                color = Tz.colors.muted,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }

                    // ── title ──
                    EdSection("Title")
                    EdInput(
                        value = draft.title,
                        onChange = { v -> update { it.copy(title = v) } },
                        placeholder = "What needs doing?",
                        focusRequester = titleFocus,
                        textStyle = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Tz.colors.ink),
                    )

                    // ── notes ──
                    EdSection("Notes · optional")
                    EdInput(
                        value = draft.description,
                        onChange = { v -> update { it.copy(description = v) } },
                        placeholder = "Add detail, links, context…",
                        textStyle = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, color = Tz.colors.ink, lineHeight = 23.sp),
                        minLines = 3,
                    )

                    // status row — opens the "Where does this stand?" sheet (M1) until the
                    // Task pane's status strip replaces it; never on a new task
                    if (!isNew && onSetState != null) {
                        EdSection("Status")
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Tz.colors.card)
                                .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                                .pressable { sheet = "state" }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                        ) {
                            com.thefoxworks.tzafon.ui.components.TaskCheckbox(draft.state)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when (draft.state) {
                                        com.thefoxworks.tzafon.domain.model.TaskState.OPEN -> "Open"
                                        com.thefoxworks.tzafon.domain.model.TaskState.DONE -> "Done"
                                        com.thefoxworks.tzafon.domain.model.TaskState.CLOSED -> "Closed"
                                        com.thefoxworks.tzafon.domain.model.TaskState.FROZEN -> "Frozen"
                                        com.thefoxworks.tzafon.domain.model.TaskState.BACKLOG -> "Backlog"
                                    },
                                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp),
                                    color = Tz.colors.ink,
                                )
                            }
                            Text(
                                "CHANGE",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                                color = Tz.colors.rust,
                            )
                        }
                    }

                    // ── delete ──
                    if (!isNew) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 26.dp).height(50.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .border(1.dp, Tz.colors.due.a(0.45f), RoundedCornerShape(13.dp))
                                .pressable {
                                    if (repeat) sheet = "delete" else onDelete(draft.id!!, EditScope.ONE)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            TzIcons.Trash(18.dp, Tz.colors.due)
                            Text("Delete task", style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.due)
                        }
                    }
                }
                Pane.ALIGNMENT -> {
                    // FR-EDITOR-1 — Serves before Cue: alignment (goal/habit) is a more
                    // identity-shaping choice than the trigger, so it comes first.
                    // ── serves (M4: the habit link; themes M6, goals M5) ──
                    EdSection("Serves · alignment (optional)")
                    val linkedHabit = habits.firstOrNull { it.id == draft.habitId }
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .let {
                                if (linkedHabit != null) it.background(Tz.colors.card).border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                                else it.border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                            }
                            .pressable { sheet = "habit" }
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (linkedHabit != null) {
                            TzIcons.Repeat(14.dp, Tz.colors.green)
                            Text(
                                linkedHabit.name,
                                style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp),
                                color = Tz.colors.ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "HABIT",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                                color = Tz.colors.faint,
                            )
                        } else {
                            TzIcons.Plus(14.dp, Tz.colors.muted)
                            Text(
                                "Link a habit — done ticks it",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                                color = Tz.colors.muted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // goal links (M5) — many allowed, one typical
                    val linkedGoals = goals.filter { it.id in draft.goalIds }
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .let {
                                if (linkedGoals.isNotEmpty()) it.background(Tz.colors.card).border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                                else it.border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                            }
                            .pressable { sheet = "goals" }
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (linkedGoals.isNotEmpty()) {
                            TzIcons.Target(14.dp, Tz.colors.rust)
                            Text(
                                linkedGoals.joinToString(" · ") { it.title },
                                style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp),
                                color = Tz.colors.ink,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(
                                if (linkedGoals.size > 1) "GOALS +${linkedGoals.size - 1}" else "GOAL",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                                color = Tz.colors.faint,
                            )
                        } else {
                            TzIcons.Plus(14.dp, Tz.colors.muted)
                            Text(
                                "Link a goal — done moves it",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                                color = Tz.colors.muted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // ── cue (DM-TASK-4: "When will you do this?") ──
                    EdSection("When will you do this? · cue")
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (draft.cue != null) Tz.colors.amber.a(0.1f) else Tz.colors.card)
                            .border(1.dp, if (draft.cue != null) Tz.colors.amber.a(0.4f) else Tz.colors.line, RoundedCornerShape(13.dp))
                            .pressable { sheet = "cue" }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        TzIcons.Cue(17.dp, if (draft.cue != null) Tz.colors.rust else Tz.colors.faint)
                        Column(Modifier.weight(1f)) {
                            Text(
                                draft.cue?.display() ?: "Anchor it to a routine — optional",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                                color = if (draft.cue != null) Tz.colors.ink else Tz.colors.faint,
                            )
                            Text(
                                draft.cue?.let { "${it.type.name.replace('_', '-')} · A TRIGGER BEATS A CLOCK" }
                                    ?: "WHEN X, I WILL DO Y",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                                color = Tz.colors.faint,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        TzIcons.Chevron(17.dp, Tz.colors.ink.a(0.28f))
                    }

                }
                Pane.TIMING -> {
                    // ── dates ──
                    EdSection("Dates")
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        DateRow(
                            label = "To Do Date",
                            value = draft.toDoDate,
                            placeholder = "None — undated",
                            today = today,
                            onClick = { sheet = "todo" },
                            onClear = { update { it.copy(toDoDate = null) } },
                        )
                        DateRow(
                            label = if (singular) "Due Date · singular" else "Due Date",
                            value = draft.dueDate,
                            placeholder = "None",
                            today = today,
                            onClick = { sheet = "due" },
                            onClear = {
                                update { it.copy(
                                    dueDate = null,
                                    recurrence = rec?.copy(dueMode = DueMode.NONE),
                                ) }
                            },
                            note = when {
                                draft.dueDate != null && draft.toDoDate != null && draft.dueDate == draft.toDoDate ->
                                    "Scheduled date is also the deadline"
                                draft.dueDate == today -> "Due today"
                                else -> null
                            },
                        )

                    }

                    // ── recurrence ──
                    EdSection("Recurrence")
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Tz.colors.card)
                            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                            .pressable { update { toggleRecurrence(it, today) } }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TzIcons.Repeat(18.dp, if (repeat) Tz.colors.rust else Tz.colors.muted)
                        Text("Repeat this task", style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp), color = Tz.colors.ink, modifier = Modifier.weight(1f))
                        // toggle pill
                        Box(
                            Modifier.size(width = 48.dp, height = 28.dp).clip(RoundedCornerShape(999.dp))
                                .background(if (repeat) Tz.colors.rust else Tz.colors.ink.a(0.18f)),
                        ) {
                            Box(
                                Modifier.padding(3.dp).size(22.dp).clip(CircleShape).background(Color.White)
                                    .align(if (repeat) Alignment.CenterEnd else Alignment.CenterStart),
                            )
                        }
                    }
                    // FR-BACKLOG-5.3 — one-line hint whenever a task has recurrence:
                    // Backlog is not selectable for a series. Mirrors the reverse-direction
                    // guard the StateSheet already shows (FR-BACKLOG-2).
                    if (repeat) {
                        Text(
                            "Backlog is for non-recurring tasks — a recurring series can't be “someday”.",
                            style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp, lineHeight = 16.sp),
                            color = Tz.colors.faint,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    if (repeat) {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 11.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Tz.colors.surface)
                                .border(1.dp, Tz.colors.line, RoundedCornerShape(16.dp))
                                .padding(horizontal = 15.dp, vertical = 16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                                TzIcons.Calendar(16.dp, Tz.colors.rust)
                                Text("To Do repeats…", style = TextStyle(fontFamily = DenType.serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.ink)
                            }
                            RecurrenceFields(rule = rec!!.rule, onChange = { r -> update { it.copy(recurrence = rec.copy(rule = r)) } })

                            Box(
                                Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(9.dp)).background(Tz.colors.rust.a(0.08f)).padding(horizontal = 12.dp, vertical = 9.dp),
                            ) {
                                Text("↻ " + Recurrence.summary(rec.rule), style = TextStyle(fontFamily = DenType.mono, fontSize = 12.5.sp), color = Tz.colors.rust)
                            }

                            // due-date mode (only when a due date is set)
                            if (draft.dueDate != null) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp).height(1.dp).background(Tz.colors.line))
                                Field("Due date on this series") {
                                    Column {
                                        Segmented(
                                            options = listOf(DueMode.SINGULAR to "Single deadline", DueMode.RECURRING to "Recurring deadline"),
                                            value = if (rec.dueMode == DueMode.NONE) DueMode.SINGULAR else rec.dueMode,
                                            onSelect = { mode ->
                                                update { it.copy(
                                                    recurrence = rec.copy(
                                                        dueMode = mode,
                                                        dueRule = if (mode == DueMode.RECURRING) (rec.dueRule ?: Recurrence.defaultRule()) else rec.dueRule,
                                                    )
                                                ) }
                                            },
                                            small = true,
                                        )
                                        Text(
                                            if (rec.dueMode == DueMode.RECURRING)
                                                "Each occurrence gets its own deadline from a separate rule. The two need not align."
                                            else
                                                "One fixed deadline for the whole series. It also ends the recurrence — no occurrences are generated after it.",
                                            style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 18.sp),
                                            color = Tz.colors.muted,
                                            modifier = Modifier.padding(top = 9.dp),
                                        )
                                    }
                                }
                                if (rec.dueMode == DueMode.RECURRING) {
                                    Column(
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(Tz.colors.card)
                                            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                                            .padding(horizontal = 13.dp, vertical = 14.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                            TzIcons.Flag(14.dp, Tz.colors.due)
                                            Text("Deadline repeats…", style = TextStyle(fontFamily = DenType.serif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.ink)
                                        }
                                        val dr = rec.dueRule ?: Recurrence.defaultRule()
                                        RecurrenceFields(rule = dr, onChange = { r -> update { it.copy(recurrence = rec.copy(dueRule = r)) } })
                                        Box(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Tz.colors.due.a(0.08f)).padding(horizontal = 12.dp, vertical = 9.dp),
                                        ) {
                                            Text("⚑ " + Recurrence.summary(dr), style = TextStyle(fontFamily = DenType.mono, fontSize = 12.5.sp), color = Tz.colors.due)
                                        }
                                    }
                                }
                            }

                            // end date — unavailable in singular mode
                            Box(Modifier.fillMaxWidth().padding(vertical = 18.dp).height(1.dp).background(Tz.colors.line))
                            if (singular) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    TzIcons.Flag(15.dp, Tz.colors.faint)
                                    Text(
                                        "End date is set by the single deadline — ${draft.dueDate?.let { Dates.fmtLong(it, today) } ?: "none"}.",
                                        style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                                        color = Tz.colors.muted,
                                    )
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth().pressable { sheet = "end" },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    TzIcons.Calendar(18.dp, Tz.colors.muted)
                                    Column(Modifier.weight(1f)) {
                                        SectionLabel("Ends")
                                        Text(
                                            rec.endDate?.let { Dates.fmtLong(it, today) } ?: "Never",
                                            style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                                            color = if (rec.endDate != null) Tz.colors.ink else Tz.colors.faint,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                    if (rec.endDate != null) {
                                        Box(
                                            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Tz.colors.surfaceAlt)
                                                .pressable { update { it.copy(recurrence = rec.copy(endDate = null)) } },
                                            contentAlignment = Alignment.Center,
                                        ) { TzIcons.X(14.dp, Tz.colors.muted) }
                                    } else {
                                        TzIcons.Chevron(18.dp, Tz.colors.ink.a(0.3f))
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    // ── sheets ──
    when (sheet) {
        "todo" -> DenSheet("To Do Date", onClose = { sheet = null }) {
            CalendarPicker(draft.toDoDate, today,
                onPick = { d -> update { it.copy(toDoDate = d) }; sheet = null },
                onClear = { update { it.copy(toDoDate = null) }; sheet = null })
        }
        "due" -> DenSheet("Due Date", onClose = { sheet = null }) {
            CalendarPicker(draft.dueDate, today,
                onPick = { picked ->
                    update { it.copy(
                        dueDate = picked,
                        recurrence = rec?.copy(dueMode = if (rec.dueMode == DueMode.NONE) DueMode.SINGULAR else rec.dueMode),
                    ) }
                    sheet = null
                },
                onClear = { update { it.copy(dueDate = null) }; sheet = null })
        }
        "end" -> DenSheet("Recurrence ends", onClose = { sheet = null }) {
            CalendarPicker(rec?.endDate, today,
                onPick = { d -> update { it.copy(recurrence = rec!!.copy(endDate = d)) }; sheet = null },
                onClear = { update { it.copy(recurrence = rec!!.copy(endDate = null)) }; sheet = null })
        }
        "state" -> com.thefoxworks.tzafon.ui.components.StateSheet(
            current = draft.state,
            isRecurring = draft.seriesId != null,
            onPick = { target ->
                // the interim sheet writes the status at once, so it's no draft change
                onSetState?.invoke(draft.id!!, target)
                session = session.stored(target)
            },
            onClose = { sheet = null },
        )
        "cue" -> com.thefoxworks.tzafon.ui.components.CueSheet(
            current = draft.cue,
            onSave = { c -> update { it.copy(cue = c) } },
            onClose = { sheet = null },
        )
        "habit" -> DenSheet("Serves a habit", onClose = { sheet = null }) {
            Column {
                Text(
                    "Done ticks the habit for the day — counted once, reversed exactly if you undo.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
                )
                if (habits.isEmpty()) {
                    Text(
                        "No habits yet — start one in the Habits tab first.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                        color = Tz.colors.muted,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
                habits.forEach { h ->
                    Row(
                        Modifier.fillMaxWidth()
                            .pressable { update { it.copy(habitId = h.id) }; sheet = null }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        TzIcons.Repeat(15.dp, Tz.colors.green)
                        Text(
                            h.name,
                            style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                            color = Tz.colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (draft.habitId == h.id) TzIcons.Check(15.dp, Tz.colors.rust, 2.6f)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))
                }
                if (draft.habitId != null) {
                    Row(
                        Modifier.fillMaxWidth()
                            .pressable { update { it.copy(habitId = null) }; sheet = null }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        TzIcons.X(14.dp, Tz.colors.muted)
                        Text(
                            "No habit for this one",
                            style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp),
                            color = Tz.colors.muted,
                        )
                    }
                }
            }
        }
        "goals" -> DenSheet("Serves goals", onClose = { sheet = null }) {
            Column {
                Text(
                    "Done moves each linked goal — once, and exactly reversed if you undo. One is plenty.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
                )
                if (goals.isEmpty()) {
                    Text(
                        "No goals yet — aim at one in the Directions tab first.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                        color = Tz.colors.muted,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
                goals.filter { it.state == com.thefoxworks.tzafon.domain.model.GoalState.ONGOING }.forEach { g ->
                    val on = g.id in draft.goalIds
                    Row(
                        Modifier.fillMaxWidth()
                            .pressable {
                                update { it.copy(
                                    goalIds = if (on) draft.goalIds - g.id else draft.goalIds + g.id,
                                ) }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        TzIcons.Target(15.dp, if (on) Tz.colors.rust else Tz.colors.muted)
                        Text(
                            g.title,
                            style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                            color = Tz.colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (on) TzIcons.Check(15.dp, Tz.colors.rust, 2.6f)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))
                }
            }
        }
        "discard" -> DenSheet("Discard your changes?", onClose = { sheet = null }) {
            Column {
                Text(
                    "Your edits to this task haven't been saved.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 21.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Tz.colors.rust)
                        .pressable { sheet = null },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Keep editing", style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.cream)
                }
                Box(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, Tz.colors.due.a(0.45f), RoundedCornerShape(13.dp))
                        .pressable { sheet = null; onClose() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Discard", style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.due)
                }
            }
        }
        "delete" -> DenSheet("Delete repeating task", onClose = { sheet = null }) {
            Column {
                Text(
                    "This task repeats — choose what to remove.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 21.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, Tz.colors.due.a(0.45f), RoundedCornerShape(13.dp))
                        .pressable { sheet = null; onDelete(draft.id!!, EditScope.ONE) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Delete this occurrence", style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.due)
                }
                Box(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Tz.colors.due)
                        .pressable { sheet = null; onDelete(draft.id!!, EditScope.ALL) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Delete all occurrences", style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
                }
            }
        }
    }
}

/**
 * v2.13.0 — Task · Timing · Alignment on the kraft `surfaceAlt` layer between
 * the rust title bar and the fields. The active tab is rust with a rust
 * underline, the others muted ink; a dot marks a pane changed since opening.
 * Tap only: there is no pager, so nothing competes with the panes' own gestures.
 */
@Composable
private fun EditorTabs(active: Pane, changed: Set<Pane>, onSelect: (Pane) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Tz.colors.surfaceAlt)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            Pane.entries.forEach { pane ->
                val on = pane == active
                val dot = pane in changed
                Column(
                    Modifier.weight(1f)
                        .semantics {
                            role = Role.Tab
                            selected = on
                            if (dot) stateDescription = "Changed"
                        }
                        .pressable { onSelect(pane) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.padding(top = 14.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            when (pane) {
                                Pane.TASK -> "Task"
                                Pane.TIMING -> "Timing"
                                Pane.ALIGNMENT -> "Alignment"
                            },
                            style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                            color = if (on) Tz.colors.rust else Tz.colors.muted,
                        )
                        if (dot) Box(Modifier.size(6.dp).clip(CircleShape).background(Tz.colors.amber))
                    }
                    Box(
                        Modifier.fillMaxWidth().height(3.dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(if (on) Tz.colors.rust else Color.Transparent),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line))
    }
}

@Composable
private fun EdSection(text: String) {
    SectionLabel(text, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 4.dp))
}

/**
 * FR-DESIGN-3 — task title & description are user-authored; the input's
 * TextStyle carries TextDirection.Content so Hebrew/Arabic titles render RTL
 * with the caret at the correct edge (see `TextStyle.contentDir()`).
 */
@Composable
private fun EdInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    minLines: Int = 1,
    focusRequester: FocusRequester? = null,
) {
    val styled = textStyle.contentDir()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = styled,
        minLines = minLines,
        cursorBrush = SolidColor(Tz.colors.rust),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Tz.colors.card)
                    .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                    .padding(14.dp),
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = styled.copy(color = Tz.colors.faint, fontWeight = FontWeight.Normal))
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth().let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
    )
}

@Composable
private fun DateRow(
    label: String,
    value: String?,
    placeholder: String,
    today: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    note: String? = null,
) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Tz.colors.card)
                .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                .pressable(onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            TzIcons.Calendar(19.dp, Tz.colors.rust)
            Column(Modifier.weight(1f)) {
                SectionLabel(label)
                Text(
                    value?.let { Dates.fmtLong(it, today) } ?: placeholder,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp),
                    color = if (value != null) Tz.colors.ink else Tz.colors.faint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (value != null) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Tz.colors.surfaceAlt).pressable(onClear),
                    contentAlignment = Alignment.Center,
                ) { TzIcons.X(14.dp, Tz.colors.muted) }
            } else {
                TzIcons.Chevron(18.dp, Tz.colors.ink.a(0.3f))
            }
        }
        if (note != null) {
            Row(
                Modifier.padding(top = 8.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                TzIcons.Alert(13.dp, Tz.colors.due)
                Text(note, style = TextStyle(fontFamily = DenType.mono, fontSize = 12.5.sp), color = Tz.colors.due)
            }
        }
    }
}
