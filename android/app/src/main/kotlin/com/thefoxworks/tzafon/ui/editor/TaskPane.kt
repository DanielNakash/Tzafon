package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.StatusSheet
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir

/**
 * v2.13.0 — the Task pane: title, notes, the status strip and Delete. A new
 * task shows only the title and notes. Emits into the editor's scrolling column.
 */
@Composable
internal fun TaskPane(
    draft: TaskDraft,
    isNew: Boolean,
    /** the stored task repeats: Backlog is dimmed on the strip */
    repeats: Boolean,
    titleFocus: FocusRequester,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
    /** a status chip; the frame decides whether it lands in the draft or asks a scope */
    onChip: (TaskState) -> Unit,
    onDelete: () -> Unit,
) {
    // ── title ──
    EdSection("Title")
    EdInput(
        value = draft.title,
        onChange = { v -> onEdit { it.copy(title = v) } },
        placeholder = "What needs doing?",
        focusRequester = titleFocus,
        textStyle = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Tz.colors.ink),
    )

    // ── notes ──
    EdSection("Notes · optional")
    EdInput(
        value = draft.description,
        onChange = { v -> onEdit { it.copy(description = v) } },
        placeholder = "Add detail, links, context…",
        textStyle = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, color = Tz.colors.ink, lineHeight = 23.sp),
        minLines = 3,
    )

    // ── status strip (slide 1): chips pick into the draft; a status
    // that reaches the series opens its scope panel. Never on a new task.
    if (!isNew) {
        EdSection("Status")
        StatusStrip(
            current = draft.state,
            repeats = repeats,
            onPick = onChip,
        )
        if (repeats) {
            Text(
                "Backlog is unavailable while this repeats — freeze it instead.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 17.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 9.dp, start = 2.dp),
            )
        }
    }

    // ── delete ──
    if (!isNew) {
        Row(
            Modifier.fillMaxWidth().padding(top = 26.dp).height(50.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.dp, Tz.colors.due.a(0.45f), RoundedCornerShape(13.dp))
                .pressable(onDelete),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            TzIcons.Trash(18.dp, Tz.colors.due)
            Text("Delete task", style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.due)
        }
    }
}

/**
 * v2.13.0 — the Task pane's status strip (slide 1): five chips, the current
 * one rust. Backlog is dimmed on a repeating task. Each chip is one TalkBack
 * radio stop.
 */
@Composable
private fun StatusStrip(current: TaskState, repeats: Boolean, onPick: (TaskState) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(TaskState.OPEN, TaskState.DONE, TaskState.CLOSED, TaskState.FROZEN, TaskState.BACKLOG).forEach { status ->
            val on = status == current
            val enabled = !(repeats && status == TaskState.BACKLOG)
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.weight(1f)
                    .clip(shape)
                    .background(if (on) Tz.colors.rust.a(0.12f) else Tz.colors.card)
                    .border(1.dp, if (on) Tz.colors.rust.a(0.55f) else Tz.colors.line, shape)
                    .selectable(
                        selected = on,
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPick(status) },
                    )
                    .semantics { if (!enabled) stateDescription = "Unavailable while this repeats" }
                    .alpha(if (enabled) 1f else 0.4f)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.thefoxworks.tzafon.ui.components.TaskCheckbox(status)
                Text(
                    StatusSheet.name(status).uppercase(),
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.6.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                    color = if (on) Tz.colors.rust else Tz.colors.muted,
                    maxLines = 1,
                )
            }
        }
    }
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
                // the field fills the box, so a right-to-left line sits at the right edge
                propagateMinConstraints = true,
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
