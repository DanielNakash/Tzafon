package com.thefoxworks.tzafon.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.StatusScope
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.domain.status.ScopePanel
import com.thefoxworks.tzafon.domain.status.StatusPick
import com.thefoxworks.tzafon.domain.status.StatusRow
import com.thefoxworks.tzafon.domain.status.StatusSheet
import com.thefoxworks.tzafon.domain.status.StatusSubject
import com.thefoxworks.tzafon.ui.components.DenSheetFrame
import com.thefoxworks.tzafon.ui.components.TaskCheckbox
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.editor.RadioRow
import com.thefoxworks.tzafon.ui.editor.SheetButtons
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir

/**
 * v2.13.0 — the status sheet (slides 7, 8 and 10). From a list it shows the
 * five statuses with [Reschedule…] [Open editor]; a pick that changes just
 * this task calls [onApply] straight away, and one that reaches a series
 * swaps to the scope panel, where Back returns to the list and nothing
 * applies until the panel's button. From the editor's status strip, [panelFor]
 * opens it straight at that status's panel, with no list and no footer, and
 * Back closes it. [onApply] is the caller's to act on and close the sheet.
 */
@Composable
fun TaskStatusSheet(
    subject: StatusSubject,
    onApply: (target: TaskState, scope: StatusScope) -> Unit,
    onClose: () -> Unit,
    panelFor: TaskState? = null,
    onReschedule: (() -> Unit)? = null,
    onOpenEditor: (() -> Unit)? = null,
) {
    var target by rememberSaveable { mutableStateOf(panelFor) }
    val panel = target?.let { (StatusSheet.pick(subject.state, it, subject.series) as? StatusPick.AskScope)?.panel }
    var scope by rememberSaveable(panel?.target) { mutableStateOf(panel?.default) }

    DenSheetFrame(onClose) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 18.dp)) {
            Header(subject)
            if (panel == null) {
                StatusList(subject) { row ->
                    when (val pick = StatusSheet.pick(subject.state, row.status, subject.series)) {
                        is StatusPick.Apply -> onApply(pick.target, StatusScope.THIS_ONE)
                        is StatusPick.AskScope -> target = pick.panel.target
                        StatusPick.Stay -> {}
                    }
                }
                Footer(onReschedule, onOpenEditor)
            } else {
                val chosen = scope ?: panel.default
                Panel(subject, panel, chosen, onChoose = { scope = it })
                SheetButtons(
                    secondary = "Back",
                    onSecondary = { if (panelFor != null) onClose() else target = null },
                    primary = panel.option(chosen).action,
                    primaryColor = actionColor(panel.target),
                    onPrimary = { onApply(panel.target, chosen) },
                )
            }
        }
    }
}

/** The task: its status (not a control here), title and meta line. */
@Composable
private fun Header(subject: StatusSubject) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.padding(top = 2.dp)) { TaskCheckbox(subject.state) }
        Column(Modifier.weight(1f)) {
            Text(
                subject.title,
                style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp, fontWeight = FontWeight.SemiBold).contentDir(),
                color = Tz.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                StatusSheet.meta(subject),
                style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 1.4.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun StatusList(subject: StatusSubject, onPick: (StatusRow) -> Unit) {
    Row(Modifier.padding(start = 2.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "SET STATUS",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 1.8.sp),
            color = Tz.colors.muted,
        )
        if (subject.series != null) {
            Text(
                "  · this task repeats",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                color = Tz.colors.muted,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        StatusSheet.rows(subject).forEach { row -> StatusRowCard(row, onPick = { onPick(row) }) }
    }
    Column(Modifier.padding(top = 12.dp, start = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatusSheet.footnote(subject).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.padding(top = 2.dp)) { TzIcons.Info(12.dp, Tz.colors.faint) }
                Text(line, style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 17.sp), color = Tz.colors.muted)
            }
        }
    }
}

/**
 * One status as a card. TalkBack reads "<status>, <description>", adds
 * "current status" on the current one, and reads the dimmed Backlog row as
 * disabled with its reason.
 */
@Composable
private fun StatusRowCard(row: StatusRow, onPick: (() -> Unit)?, picked: Boolean = false) {
    val shape = RoundedCornerShape(14.dp)
    val tinted = row.now && !picked
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (tinted) Tz.colors.rust.a(0.1f) else Tz.colors.card)
            .border(1.dp, if (tinted) Tz.colors.rust.a(0.45f) else Tz.colors.line, shape)
            .clearAndSetSemantics {
                contentDescription = "${row.name}, ${row.description}"
                when {
                    !row.enabled -> stateDescription = "Unavailable while this repeats. Freeze it instead."
                    row.now -> stateDescription = "current status"
                }
                if (onPick != null) {
                    role = Role.Button
                    if (row.enabled) onClick { onPick(); true } else disabled()
                }
            }
            .let { m ->
                if (onPick != null && row.enabled) {
                    m.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPick)
                } else m
            }
            .alpha(if (row.enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TaskCheckbox(row.status)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    row.name,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.5.sp, fontWeight = FontWeight.Medium),
                    color = if (tinted) Tz.colors.rust else Tz.colors.ink,
                )
                if (row.now && !picked) Badge("NOW")
                row.tag?.let {
                    Text(it, style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 1.2.sp), color = Tz.colors.faint)
                }
            }
            Text(
                row.description,
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (picked) TzIcons.Check(16.dp, Tz.colors.ink.a(0.55f), 2.4f)
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 1.sp),
        color = Tz.colors.muted,
        modifier = Modifier
            .border(1.dp, Tz.colors.line, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Slide 8: the chosen status alone with ✓, then its scope choices. No count badge. */
@Composable
private fun Panel(subject: StatusSubject, panel: ScopePanel, chosen: StatusScope, onChoose: (StatusScope) -> Unit) {
    val row = StatusSheet.rows(subject).first { it.status == panel.target }
    StatusRowCard(row.copy(now = false), onPick = null, picked = true)
    Row(
        Modifier.padding(top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TzIcons.Repeat(15.dp, Tz.colors.rust)
        Text(
            panel.heading,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 1.8.sp),
            color = Tz.colors.muted,
            modifier = Modifier.semantics { heading() },
        )
        Box(Modifier.weight(1f).height(1.dp).background(Tz.colors.line))
    }
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        panel.options.forEach { option ->
            RadioRow(
                label = option.label,
                line = option.line,
                selected = option.scope == chosen,
                enabled = option.enabled,
                onClick = { onChoose(option.scope) },
            )
        }
    }
}

/** From a list: [Reschedule…] [Open editor], for every status. */
@Composable
private fun Footer(onReschedule: (() -> Unit)?, onOpenEditor: (() -> Unit)?) {
    if (onReschedule == null && onOpenEditor == null) return
    Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(Tz.colors.line))
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        onReschedule?.let { FooterButton("Reschedule…", { TzIcons.Calendar(17.dp, Tz.colors.muted) }, it) }
        onOpenEditor?.let { FooterButton("Open editor", { TzIcons.Pencil(16.dp, Tz.colors.muted) }, it) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.FooterButton(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.weight(1f).height(52.dp)
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
            .pressable(null, Role.Button, onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
    ) {
        icon()
        Text(label, style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.Medium), color = Tz.colors.ink)
    }
}

/** The panel's button takes the colour of where the task is going. */
@Composable
private fun actionColor(target: TaskState): Color = when (target) {
    TaskState.FROZEN -> Tz.colors.frozen
    TaskState.CLOSED -> Tz.colors.closed
    else -> Tz.colors.rust
}
