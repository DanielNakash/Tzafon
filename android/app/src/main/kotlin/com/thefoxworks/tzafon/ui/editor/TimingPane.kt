package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.recurrence.DueMode
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.ui.components.Field
import com.thefoxworks.tzafon.ui.components.RecurrenceFields
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.Segmented
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a

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
        val promotingFromBacklog = draft.state == TaskState.BACKLOG
        draft.copy(
            recurrence = RecurrenceDraft(
                rule = Recurrence.defaultRule(),
                dueMode = if (draft.dueDate != null) DueMode.SINGULAR else DueMode.NONE,
            ),
            state = if (promotingFromBacklog) TaskState.OPEN else draft.state,
            toDoDate = if (promotingFromBacklog) today else draft.toDoDate,
        )
    }

/**
 * v2.13.0 — the Timing pane: the to-do date and deadline, then the repeat card.
 * The date rows open the frame's date sheets; everything else edits the draft.
 * Emits into the editor's scrolling column.
 */
@Composable
internal fun TimingPane(
    draft: TaskDraft,
    today: String,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
    onToDo: () -> Unit,
    onClearToDo: () -> Unit,
    onDue: () -> Unit,
    onEnd: () -> Unit,
) {
    val rec = draft.recurrence
    val repeat = rec != null
    val singular = repeat && rec!!.dueMode == DueMode.SINGULAR

    // ── dates ──
    EdSection("Dates")
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        DateRow(
            label = "To Do Date",
            value = draft.toDoDate,
            placeholder = "None — undated",
            today = today,
            onClick = onToDo,
            onClear = onClearToDo,
        )
        DateRow(
            label = if (singular) "Due Date · singular" else "Due Date",
            value = draft.dueDate,
            placeholder = "None",
            today = today,
            onClick = onDue,
            onClear = {
                onEdit { it.copy(
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
            .pressable { onEdit { toggleRecurrence(it, today) } }
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
            RecurrenceFields(rule = rec!!.rule, onChange = { r -> onEdit { it.copy(recurrence = rec.copy(rule = r)) } })

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
                                onEdit { it.copy(
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
                        RecurrenceFields(rule = dr, onChange = { r -> onEdit { it.copy(recurrence = rec.copy(dueRule = r)) } })
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
                    Modifier.fillMaxWidth().pressable(onEnd),
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
                                .pressable { onEdit { it.copy(recurrence = rec.copy(endDate = null)) } },
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
