package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.editor.SaveSheet
import com.thefoxworks.tzafon.domain.editor.ScopeRow
import com.thefoxworks.tzafon.domain.editor.ScopeSheetCopy
import com.thefoxworks.tzafon.domain.model.EditScope
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a

/**
 * v2.13.0 — "This task repeats", asked once at Save (supersedes the
 * FR-EDITOR-1.6 scope banner). The "You changed" chips, then one radio row
 * per scope; a disabled row says why. Keep editing, Back and dragging the
 * sheet down all return to the draft; Save change commits.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SaveScopeSheet(
    sheet: SaveSheet,
    chosen: EditScope,
    onChoose: (EditScope) -> Unit,
    onKeepEditing: () -> Unit,
    onSave: () -> Unit,
) {
    DenSheet("This task repeats", onClose = onKeepEditing) {
        Column {
            Intro("Where should the change land?")
            Column(
                Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Tz.colors.amber.a(0.14f))
                    .border(1.dp, Tz.colors.amber.a(0.4f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp),
            ) {
                Text(
                    "YOU CHANGED",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 1.6.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    sheet.youChanged.forEach { line ->
                        Text(
                            line,
                            style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                            color = Tz.colors.ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Tz.colors.card)
                                .border(1.dp, Tz.colors.line, RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            ScopeRows(sheet.rows, chosen, onChoose)
            SheetButtons(
                secondary = "Keep editing", onSecondary = onKeepEditing,
                primary = "Save change", primaryColor = Tz.colors.rust, onPrimary = onSave,
            )
        }
    }
}

/**
 * v2.13.0 — deleting a repeating task: the save sheet's rows with the delete
 * lines, no "You changed" list, Keep and a red Delete.
 */
@Composable
internal fun DeleteScopeSheet(
    rows: List<ScopeRow>,
    chosen: EditScope,
    onChoose: (EditScope) -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
) {
    DenSheet("Delete a repeating task", onClose = onKeep) {
        Column {
            Intro("What should go?")
            ScopeRows(rows, chosen, onChoose)
            SheetButtons(
                secondary = "Keep", onSecondary = onKeep,
                primary = "Delete", primaryColor = Tz.colors.due, onPrimary = onDelete,
            )
        }
    }
}

/** v2.13.0 — a one-off task's delete: no options, but one tap isn't enough, as there's no undo. */
@Composable
internal fun DeleteConfirmSheet(onKeep: () -> Unit, onDelete: () -> Unit) {
    DenSheet("Delete this task?", onClose = onKeep) {
        Column {
            Intro("It won't come back.")
            SheetButtons(
                secondary = "Keep", onSecondary = onKeep,
                primary = "Delete", primaryColor = Tz.colors.due, onPrimary = onDelete,
            )
        }
    }
}

@Composable
private fun Intro(text: String) {
    Text(
        text,
        style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 21.sp),
        color = Tz.colors.muted,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

/**
 * One radio row per scope, chosen rust-tinted. Each row is a single TalkBack
 * stop that reads its label, badge and consequence line; a disabled row reads
 * as disabled and can't be chosen.
 */
@Composable
private fun ScopeRows(rows: List<ScopeRow>, chosen: EditScope, onChoose: (EditScope) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            RadioRow(
                label = ScopeSheetCopy.label(row.scope),
                line = row.line,
                selected = row.scope == chosen,
                enabled = row.enabled,
                badge = row.badge,
                onClick = { onChoose(row.scope) },
            )
        }
    }
}

/**
 * One scope choice: a radio, its label (and count badge), and what it does.
 * A single TalkBack stop with role RadioButton; a disabled row is dimmed and
 * inert. Put a run of them in a `selectableGroup`.
 */
@Composable
internal fun RadioRow(
    label: String,
    line: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (selected) Tz.colors.rust.a(0.1f) else Tz.colors.card)
            .border(1.dp, if (selected) Tz.colors.rust.a(0.45f) else Tz.colors.line, shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Radio(selected)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    label,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    color = Tz.colors.ink,
                )
                badge?.let {
                    Text(
                        it,
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp),
                        color = Tz.colors.muted,
                        modifier = Modifier
                            .border(1.dp, Tz.colors.line, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                line,
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun Radio(on: Boolean) {
    Box(
        Modifier.padding(top = 1.dp).size(22.dp).clip(CircleShape)
            .background(if (on) Tz.colors.rust else Color.Transparent)
            .border(2.dp, if (on) Tz.colors.rust else Tz.colors.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Box(Modifier.size(8.dp).clip(CircleShape).background(Tz.colors.cream))
    }
}

/** Keep editing / Keep / Back on the left, the action filled on the right (slides 4 and 8). */
@Composable
internal fun SheetButtons(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    primaryColor: Color,
    onPrimary: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.weight(1f).height(52.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                .pressable(null, Role.Button, onSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(secondary, style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.muted)
        }
        Box(
            Modifier.weight(1.4f).height(52.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(primaryColor)
                .pressable(null, Role.Button, onPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(primary, style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.cream)
        }
    }
}
