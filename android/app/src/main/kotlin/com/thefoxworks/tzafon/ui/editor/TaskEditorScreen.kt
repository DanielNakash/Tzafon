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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.editor.Armed
import com.thefoxworks.tzafon.domain.editor.CueDraft
import com.thefoxworks.tzafon.domain.editor.CueSuggestions
import com.thefoxworks.tzafon.domain.editor.DraftDiff
import com.thefoxworks.tzafon.domain.editor.EditorSession
import com.thefoxworks.tzafon.domain.editor.Pane
import com.thefoxworks.tzafon.domain.editor.SaveProblem
import com.thefoxworks.tzafon.domain.editor.SaveStep
import com.thefoxworks.tzafon.domain.editor.ScopeSheetCopy
import com.thefoxworks.tzafon.domain.editor.SharedCalendar
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.domain.model.SeriesStatus
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.status.AmountPrompt
import com.thefoxworks.tzafon.domain.status.StatusPick
import com.thefoxworks.tzafon.domain.status.StatusSubject
import com.thefoxworks.tzafon.ui.components.AmountSheet
import com.thefoxworks.tzafon.ui.status.TaskStatusSheet
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.ui.components.CalendarPicker
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.LocalWeekStart
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import java.time.YearMonth

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
    onClose: () -> Unit,
    /** Task, or Timing from Reschedule (with the to-do date armed) */
    openOn: Pane = Pane.TASK,
    /** the stored series' facts, which the status strip asks its scope from; null for a one-off */
    series: SeriesStatus? = null,
    /** a whole-series status from the strip: applied at once, then the editor closes */
    onSeriesStatus: ((id: String, target: TaskState, amount: Double?) -> Unit)? = null,
    /** M4 — the habits available to link (DM-REL: at most one per task). */
    habits: List<com.thefoxworks.tzafon.domain.model.Habit> = emptyList(),
    /** M5 — goals to link (many allowed, one typical — DM-TASK-5). */
    goals: List<com.thefoxworks.tzafon.domain.model.Goal> = emptyList(),
    /** v2.13.0 — the scope sheets' whole-series badge: earlier occurrences (or only those not Done). */
    countEarlier: suspend (taskId: String, notDoneOnly: Boolean) -> Int = { _, _ -> 0 },
    /** the themes the Alignment pane's inherited line names (FR-EDITOR-6.8) */
    themes: List<Theme> = emptyList(),
    /** every cue in use, for the Alignment pane's suggestion chips ([CueSuggestions.uses]) */
    cueUses: List<Cue> = emptyList(),
    /** a quick-add expand's parsed cue (FR-CAPTURE-3.6): an edit, so the Alignment dot shows */
    parsedCue: Cue? = null,
) {
    // a quick-add expand passes a title-only draft (id = null) — still a new task
    val isNew = initial?.id == null
    var session by rememberSaveable(stateSaver = EditorSessionCodec.saver) {
        mutableStateOf(
            EditorSession.open(initial ?: TaskDraft(id = null, title = "", toDoDate = null), openOn, parsedCue) // undated by default
        )
    }
    // the Alignment pane's cue as typed; each edit also lands in the draft as the cue Save stores
    var cue by rememberSaveable(stateSaver = CueDraftSaver) { mutableStateOf(CueDraft.of(session.draft.cue)) }
    fun editCue(typed: CueDraft) { cue = typed; session = session.edit { it.copy(cue = typed.cue()) } }
    var cueTimeFlagged by rememberSaveable { mutableStateOf(false) }
    val cueTimeFocus = remember { FocusRequester() }
    val draft = session.draft
    fun update(change: (TaskDraft) -> TaskDraft) { session = session.edit(change) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) } // end | scope | delete | confirmDelete | status | amount | discard
    // the shared calendar: To do is armed on open (Reschedule too), and the month
    // only moves by its arrows; both outlive tab switches and rotation
    var armed by rememberSaveable { mutableStateOf(Armed.TODO) }
    var month by rememberSaveable { mutableStateOf(SharedCalendar.openingMonth(session.snapshot, today).toString()) }
    // the row picked on the scope or delete sheet; null = the sheet's default, never remembered between openings
    var chosen by rememberSaveable { mutableStateOf<EditScope?>(null) }
    val earlier by produceState(0, sheet) {
        value = 0
        if (sheet == "scope" || sheet == "delete") value = countEarlier(session.snapshot.id!!, sheet == "delete")
    }
    val rec = draft.recurrence
    // each pane keeps its own scroll position while tabs switch
    val scrolls = Pane.entries.associateWith { rememberScrollState() }
    val titleFocus = remember { FocusRequester() }
    var focusProblem by remember { mutableStateOf<SaveProblem?>(null) }

    // the status strip: the status whose scope panel is open, the scope a "How much?" waits
    // to apply Done with, and a whole-series status waiting on "Discard your changes?"
    var statusPanel by rememberSaveable { mutableStateOf<TaskState?>(null) }
    var amountScope by rememberSaveable { mutableStateOf<StatusScope?>(null) }
    var seriesPending by rememberSaveable { mutableStateOf<TaskState?>(null) }
    var seriesAmount by rememberSaveable { mutableStateOf<Double?>(null) }
    val amountPrompt = {
        AmountPrompt.of(
            Task(id = draft.id ?: "", title = draft.title, habitId = draft.habitId, goalIds = draft.goalIds),
            habits.find { it.id == draft.habitId },
            goals.associateBy { it.id },
        )
    }
    fun seriesStatus(target: TaskState, amount: Double?) {
        if (session.asksBeforeClosing) {
            seriesPending = target; seriesAmount = amount; sheet = "discard"
        } else {
            onSeriesStatus?.invoke(draft.id!!, target, amount)
        }
    }
    // a settled status: "this one" into the draft, the whole series at once; Done asks "How much?" first
    fun commitStatus(target: TaskState, scope: StatusScope) {
        if (target == TaskState.DONE && draft.state != TaskState.DONE && amountPrompt() != null) {
            amountScope = scope; sheet = "amount"
        } else if (scope == StatusScope.WHOLE_SERIES) {
            seriesStatus(target, null)
        } else {
            session = session.status(target)
        }
    }
    fun onChip(target: TaskState) {
        when (val pick = session.chip(target, series)) {
            is StatusPick.Apply -> commitStatus(pick.target, StatusScope.THIS_ONE)
            is StatusPick.AskScope -> { statusPanel = target; sheet = "status" }
            StatusPick.Stay -> {}
        }
    }

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
        if (focusProblem == SaveProblem.INCOMPLETE_CUE_TIME && session.tab == Pane.ALIGNMENT) {
            // the cue sits at the top of the pane
            scrolls.getValue(Pane.ALIGNMENT).animateScrollTo(0)
            cueTimeFlagged = true
            cueTimeFocus.requestFocus()
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
                        val problem = session.saveProblem(cue)
                        if (problem == null) {
                            when (val step = session.saveStep) {
                                SaveStep.Close -> onClose()
                                is SaveStep.Write -> onSave(draft, step.scope)
                                SaveStep.AskScope -> { chosen = null; sheet = "scope" }
                            }
                        } else {
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
                Pane.TASK -> TaskPane(
                    draft = draft,
                    isNew = isNew,
                    repeats = series != null,
                    titleFocus = titleFocus,
                    onEdit = ::update,
                    onChip = ::onChip,
                    onDelete = {
                        chosen = null
                        sheet = if (session.repeats) "delete" else "confirmDelete"
                    },
                )
                Pane.ALIGNMENT -> AlignmentPane(
                    draft = draft,
                    openedWithGoals = session.snapshot.goalIds,
                    today = today,
                    habits = habits,
                    goals = goals,
                    themes = themes,
                    cueUses = cueUses,
                    cue = cue,
                    timeFlagged = cueTimeFlagged,
                    timeFocus = cueTimeFocus,
                    onCue = ::editCue,
                    onEdit = ::update,
                )
                Pane.TIMING -> TimingPane(
                    draft = draft,
                    today = today,
                    armed = armed,
                    month = YearMonth.parse(month),
                    weekStart = LocalWeekStart.current,
                    onArm = { armed = it },
                    onMonth = { month = it.toString() },
                    onEdit = ::update,
                    onEnd = { sheet = "end" },
                )
            }
        }
    }

    // ── sheets ──
    when (sheet) {
        "end" -> DenSheet("Recurrence ends", onClose = { sheet = null }) {
            CalendarPicker(rec?.endDate, today,
                onPick = { d -> update { it.copy(recurrence = rec!!.copy(endDate = d)) }; sheet = null },
                onClear = { update { it.copy(recurrence = rec!!.copy(endDate = null)) }; sheet = null })
        }
        "status" -> statusPanel?.let { target ->
            TaskStatusSheet(
                subject = StatusSubject(
                    title = draft.title,
                    state = draft.state,
                    toDoDate = draft.toDoDate,
                    repeat = draft.recurrence?.takeIf { series != null }?.let { Recurrence.summary(it.rule) },
                    series = series,
                    habit = habits.find { it.id == draft.habitId }?.name,
                    goals = goals.filter { it.id in draft.goalIds }.map { it.title },
                ),
                panelFor = target,
                onApply = { picked, scope -> sheet = null; statusPanel = null; commitStatus(picked, scope) },
                onClose = { sheet = null; statusPanel = null },
            )
        }
        "amount" -> amountPrompt()?.let { prompt ->
            AmountSheet(
                title = prompt.title,
                unit = prompt.unit,
                suggested = prompt.suggested,
                onConfirm = { amount ->
                    val scope = amountScope
                    amountScope = null
                    if (scope == StatusScope.WHOLE_SERIES) seriesStatus(TaskState.DONE, amount)
                    else session = session.status(TaskState.DONE, amount)
                },
                // cancelling leaves the chip as it was
                onClose = { if (sheet == "amount") sheet = null; amountScope = null },
            )
        }
        "discard" -> DenSheet("Discard your changes?", onClose = { sheet = null; seriesPending = null }) {
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
                        .pressable { sheet = null; seriesPending = null },
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
                        .pressable {
                            sheet = null
                            // a whole-series status waiting on this prompt acts now and closes the editor
                            val pending = seriesPending
                            if (pending != null) onSeriesStatus?.invoke(draft.id!!, pending, seriesAmount) else onClose()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Discard", style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.due)
                }
            }
        }
        "scope" -> {
            val changes = DraftDiff.of(session.snapshot, draft, habits.find { it.id == draft.habitId }?.unit)
            val pick = chosen?.takeIf(changes::allows) ?: changes.defaultScope
            SaveScopeSheet(
                // rebuilt per pick: the "· this one only" suffix depends on the chosen row
                sheet = ScopeSheetCopy.save(draft, changes, earlier, pick),
                chosen = pick,
                onChoose = { chosen = it },
                onKeepEditing = { sheet = null },
                onSave = { sheet = null; onSave(draft, pick) },
            )
        }
        "delete" -> {
            val pick = chosen ?: EditScope.ONE
            DeleteScopeSheet(
                rows = ScopeSheetCopy.delete(session.snapshot.occurrenceDate ?: session.snapshot.toDoDate!!, earlier),
                chosen = pick,
                onChoose = { chosen = it },
                onKeep = { sheet = null },
                onDelete = { sheet = null; onDelete(draft.id!!, pick) },
            )
        }
        "confirmDelete" -> DeleteConfirmSheet(
            onKeep = { sheet = null },
            onDelete = { sheet = null; onDelete(draft.id!!, EditScope.ONE) },
        )
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
