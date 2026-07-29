package com.thefoxworks.tzafon.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeState
import com.thefoxworks.tzafon.domain.themes.GoalLinkLogic
import com.thefoxworks.tzafon.ui.components.CueSheet
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.SheetGhostButton
import com.thefoxworks.tzafon.ui.components.SheetPrimaryButton
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir

/**
 * Create / edit a habit (DM-HABIT-1): kind, gentle weekly target, unit,
 * and the cue — nudged, never forced. Delete wipes history after an
 * explicit confirmation (DM-HABIT-7).
 */
@Composable
fun HabitEditorSheet(
    initial: Habit?,
    onSave: (Habit) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
    /** the DM-GOAL-4 rebound pre-fills the name ("make a habit of it") */
    presetName: String? = null,
    /** goals this habit may serve (DM-HABIT-6 / D2 — one) */
    goals: List<Goal> = emptyList(),
    /**
     * FR-HAB-10.5 — goal-by-id map so the pre-fill can read the linked goal's
     * `primaryThemeId` at open time and seed the direction picker with it.
     */
    goalsById: Map<String, Goal> = emptyMap(),
    /**
     * FR-HAB-10.3 — active themes are the only options togglable in the
     * direction picker (mirrors `FR-DIR-8.4`).
     */
    activeThemes: List<Theme> = emptyList(),
    /**
     * FR-HAB-10.3 — full theme list so a habit already serving an
     * upcoming/archived theme renders it as a read-only annotated chip.
     */
    allThemes: List<Theme> = emptyList(),
    /**
     * FR-HAB-7.4 — when the habit already has logged history, the `kind`
     * chooser is locked: switching would reinterpret past logs (a frequency
     * tick has no `amount`; a quantitative entry has no meaningful "did it
     * happen" boolean once the numeric target moves). At create time this is
     * always false.
     */
    hasHistory: Boolean = false,
) {
    var name by remember { mutableStateOf(initial?.name ?: presetName ?: "") }
    var goalId by remember { mutableStateOf(initial?.goalId) }
    var goalPick by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(initial?.kind ?: HabitKind.FREQUENCY) }
    var target by remember { mutableStateOf(initial?.target ?: 3.0) }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var targetDays by remember { mutableStateOf(initial?.targetDays ?: 5) }
    var cue by remember { mutableStateOf(initial?.cue) }
    var cueOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // FR-HAB-10.2 — direction picker state driven by the same pure engine the
    // goal editor uses; primary is a single-select radio slot, extraServes is
    // an ordered "also serves" list.
    // FR-HAB-10.5 — pre-fill (once, on open): if the habit has a goal and no
    // theme of its own, seed primary from the linked goal's primaryThemeId.
    val initialThemePrimary = remember(initial?.id) {
        val ownPrimary = initial?.primaryThemeId
        val ownExtras = initial?.themeIds ?: emptyList()
        if (ownPrimary == null && ownExtras.isEmpty()) {
            initial?.goalId?.let { gid -> goalsById[gid]?.primaryThemeId }
        } else ownPrimary
    }
    var linkSel by remember(initial?.id) {
        mutableStateOf(
            GoalLinkLogic.fromGoal(initialThemePrimary, initial?.themeIds ?: emptyList()),
        )
    }

    if (cueOpen) {
        CueSheet(current = cue, onSave = { cue = it }, onClose = { cueOpen = false })
        return
    }
    if (goalPick) {
        DenSheet(title = "Serves a goal", onClose = { goalPick = false }) {
            Column {
                Text(
                    "The habit becomes the goal's engine — matching units auto-advance the bar.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
                )
                goals.filter { it.state == com.thefoxworks.tzafon.domain.model.GoalState.ONGOING }.forEach { g ->
                    Row(
                        Modifier.fillMaxWidth()
                            .pressable { goalId = g.id; goalPick = false }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        TzIcons.Target(15.dp, Tz.colors.rust)
                        Text(
                            g.title,
                            style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                            color = Tz.colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (goalId == g.id) TzIcons.Check(15.dp, Tz.colors.rust, 2.6f)
                    }
                }
                if (goalId != null) {
                    SheetGhostButton(label = "No goal for this one", modifier = Modifier.padding(top = 9.dp)) {
                        goalId = null; goalPick = false
                    }
                }
            }
        }
        return
    }

    DenSheet(
        title = if (initial == null) "A new rhythm" else "Edit habit",
        onClose = onClose,
    ) {
        if (confirmDelete) {
            Column(Modifier.padding(top = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TzIcons.Trash(18.dp, Tz.colors.due)
                    Text(
                        "Delete this habit?",
                        style = TextStyle(fontFamily = DenType.serif, fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                        color = Tz.colors.ink,
                    )
                }
                Text(
                    "This wipes the habit and its whole history — every logged day. There's no undo. Freezing the linked tasks keeps the history instead.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, lineHeight = 21.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                SheetPrimaryButton(label = "Delete — history and all", color = Tz.colors.due) {
                    initial?.let { onDelete(it.id) }
                    onClose()
                }
                SheetGhostButton(label = "Keep it", modifier = Modifier.padding(top = 9.dp)) {
                    confirmDelete = false
                }
            }
            return@DenSheet
        }

        Column {
            SectionLabel("Name")
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Tz.colors.ink).contentDir(),
                cursorBrush = SolidColor(Tz.colors.rust),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Tz.colors.card)
                            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                "Write in the morning",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                color = Tz.colors.faint,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )

            SectionLabel("Kind", modifier = Modifier.padding(top = 16.dp))
            val kindLocked = hasHistory && initial != null
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                KindChip("TIMES A WEEK", kind == HabitKind.FREQUENCY, enabled = !kindLocked) {
                    kind = HabitKind.FREQUENCY
                }
                KindChip("AN AMOUNT A DAY", kind == HabitKind.QUANTITATIVE, enabled = !kindLocked) {
                    kind = HabitKind.QUANTITATIVE
                }
            }
            if (kindLocked) {
                Text(
                    "Kind is fixed once you've logged this habit — delete and recreate if you need to switch.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp, lineHeight = 16.5.sp),
                    color = Tz.colors.faint,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (kind == HabitKind.FREQUENCY) {
                SectionLabel("Target · times per week", modifier = Modifier.padding(top = 16.dp))
                Stepper(
                    value = target.toInt(),
                    range = 1..7,
                    label = "${target.toInt()}× / week",
                    onChange = { target = it.toDouble() },
                )
            } else {
                SectionLabel("Target · per day", modifier = Modifier.padding(top = 16.dp))
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Tz.colors.card)
                            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        var amountText by remember {
                            mutableStateOf(if (target % 1.0 == 0.0) target.toInt().toString() else target.toString())
                        }
                        BasicTextField(
                            value = amountText,
                            onValueChange = { s ->
                                amountText = s.filter { it.isDigit() || it == '.' }
                                amountText.toDoubleOrNull()?.let { target = it }
                            },
                            textStyle = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Tz.colors.ink),
                            cursorBrush = SolidColor(Tz.colors.rust),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Tz.colors.card)
                            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        BasicTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            textStyle = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, color = Tz.colors.ink).contentDir(),
                            cursorBrush = SolidColor(Tz.colors.rust),
                            singleLine = true,
                            decorationBox = { inner ->
                                Box {
                                    if (unit.isEmpty()) {
                                        Text(
                                            "words · km · pages",
                                            style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp),
                                            color = Tz.colors.faint,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }
                SectionLabel("Aimed days a week", modifier = Modifier.padding(top = 14.dp))
                Stepper(
                    value = targetDays,
                    range = 1..7,
                    label = "$targetDays of 7 days",
                    onChange = { targetDays = it },
                )
            }

            if (goals.isNotEmpty() || goalId != null) {
                SectionLabel("Serves a goal · optional", modifier = Modifier.padding(top = 16.dp))
                val linked = goals.firstOrNull { it.id == goalId }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                        .pressable { goalPick = true }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TzIcons.Target(14.dp, if (linked != null) Tz.colors.rust else Tz.colors.muted)
                    Text(
                        linked?.title ?: "Point it at a goal",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                        color = if (linked != null) Tz.colors.ink else Tz.colors.muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (linked != null) "GOAL" else "",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                        color = Tz.colors.faint,
                    )
                }
            }

            // FR-HAB-10 — "Serves a direction · optional": a primary
            // single-select over active themes (plus "No direction"), and an
            // "Also serves" multi-select over the remaining actives. Section
            // hides only when there are no themes at all and the habit
            // carries none (FR-HAB-10.6).
            val hasAnyThemeLink = linkSel.primaryId != null || linkSel.extraServes.isNotEmpty()
            if (activeThemes.isNotEmpty() || hasAnyThemeLink) {
                SectionLabel("Serves a direction · optional", modifier = Modifier.padding(top = 16.dp))
                FlowRow(
                    Modifier
                        .padding(top = 6.dp)
                        .semantics { contentDescription = "Primary direction" },
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    DirectionChip(
                        label = "No direction",
                        selected = linkSel.primaryId == null,
                        role = Role.RadioButton,
                    ) { linkSel = GoalLinkLogic.tapOrphan(linkSel) }
                    activeThemes.forEach { t ->
                        DirectionChip(
                            label = t.name,
                            selected = linkSel.primaryId == t.id,
                            role = Role.RadioButton,
                        ) { linkSel = GoalLinkLogic.tapPrimary(linkSel, t.id) }
                    }
                }

                // FR-HAB-10.3 — an "Also serves" row for the remaining active
                // themes (excludes primary) plus read-only chips for any
                // upcoming/archived theme the habit already carries.
                val eligibleActive = activeThemes.filter { it.id != linkSel.primaryId }
                val nonActiveServed = ((initial?.themeIds ?: emptyList()) +
                    listOfNotNull(initial?.primaryThemeId))
                    .filter { id ->
                        id != linkSel.primaryId &&
                            allThemes.any { it.id == id && it.state != ThemeState.ACTIVE }
                    }
                    .distinct()
                if (eligibleActive.isNotEmpty() || nonActiveServed.isNotEmpty()) {
                    SectionLabel("Also serves", modifier = Modifier.padding(top = 12.dp))
                    FlowRow(
                        Modifier
                            .padding(top = 6.dp)
                            .semantics { contentDescription = "Also serves directions" },
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        eligibleActive.forEach { t ->
                            DirectionChip(
                                label = t.name,
                                selected = t.id in linkSel.extraServes,
                                role = Role.Switch,
                            ) { linkSel = GoalLinkLogic.toggleExtra(linkSel, t.id) }
                        }
                        nonActiveServed.forEach { id ->
                            val t = allThemes.first { it.id == id }
                            val stateLabel = when (t.state) {
                                ThemeState.UPCOMING -> "upcoming"
                                ThemeState.ARCHIVED -> "archived"
                                ThemeState.ACTIVE -> "active"
                            }
                            DirectionReadOnlyChip("${t.name} · $stateLabel")
                        }
                    }
                }
            }

            SectionLabel("Cue · when will you do it?", modifier = Modifier.padding(top = 16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (cue != null) Tz.colors.amber.a(0.1f) else Tz.colors.card)
                    .border(1.dp, if (cue != null) Tz.colors.amber.a(0.4f) else Tz.colors.line, RoundedCornerShape(13.dp))
                    .pressable { cueOpen = true }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                TzIcons.Cue(17.dp, if (cue != null) Tz.colors.rust else Tz.colors.faint)
                Column(Modifier.weight(1f)) {
                    Text(
                        cue?.display() ?: "Anchor it to a routine",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                        color = if (cue != null) Tz.colors.ink else Tz.colors.faint,
                    )
                    Text(
                        if (cue != null) "${cue!!.type.name.replace('_', '-')} · A TRIGGER BEATS A CLOCK"
                        else "WITHOUT A CUE IT'S JUST A TRACKER",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                        color = Tz.colors.faint,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                TzIcons.Chevron(17.dp, Tz.colors.ink.a(0.28f))
            }

            SheetPrimaryButton(
                label = if (initial == null) "Start the rhythm" else "Save",
                enabled = name.isNotBlank(),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                // FR-HAB-10.4 — persist both the primary direction and the
                // full serve set from the editor's LinkSelection (the v2.6.0
                // save silently dropped themeIds and inherited primaryThemeId
                // unchanged).
                onSave(
                    Habit(
                        id = initial?.id ?: "",
                        name = name.trim(),
                        kind = kind,
                        target = target,
                        unit = unit.trim().takeIf { it.isNotBlank() && kind == HabitKind.QUANTITATIVE },
                        targetDays = targetDays.takeIf { kind == HabitKind.QUANTITATIVE },
                        cue = cue,
                        primaryThemeId = linkSel.primaryId,
                        themeIds = linkSel.themeIds(),
                        goalId = goalId,
                        startedAt = initial?.startedAt ?: 0,
                        createdAt = initial?.createdAt ?: 0,
                    ),
                )
                onClose()
            }
            if (initial != null) {
                SheetGhostButton(label = "Delete this habit…", modifier = Modifier.padding(top = 9.dp)) {
                    confirmDelete = true
                }
            }
        }
    }
}

@Composable
private fun KindChip(label: String, on: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = when {
        on && enabled -> Tz.colors.green
        on && !enabled -> Tz.colors.green.a(0.45f)
        else -> Color.Transparent
    }
    val border = when {
        on -> Tz.colors.green.a(if (enabled) 1f else 0.45f)
        else -> Tz.colors.line
    }
    val textColor = when {
        on -> Color.White.copy(alpha = if (enabled) 1f else 0.85f)
        enabled -> Tz.colors.muted
        else -> Tz.colors.faint
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .let { if (enabled) it.pressable(onClick) else it }
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = textColor,
        )
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit) {
    Row(
        Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepBtn("−") { if (value > range.first) onChange(value - 1) }
        Text(
            label,
            style = TextStyle(fontFamily = DenType.serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            color = Tz.colors.ink,
        )
        StepBtn("+") { if (value < range.last) onChange(value + 1) }
        Text(
            "GENTLE BEATS PERFECT",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
            color = Tz.colors.faint,
        )
    }
}

/**
 * FR-HAB-10.1 — the primary/serves-also chip shape used by the habit editor's
 * direction picker. Selection state uses the theme accent; unselected chips
 * carry the app's neutral outline. Announces itself with the caller-supplied
 * role (radio for primary, switch for serves-also) so TalkBack matches the
 * goal editor's picker semantics.
 */
@Composable
private fun DirectionChip(
    label: String,
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Tz.colors.rust else Color.Transparent)
            .border(1.dp, if (selected) Tz.colors.rust else Tz.colors.line, RoundedCornerShape(999.dp))
            .semantics { this.selected = selected }
            .pressable(label = label, role = role, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = if (selected) Color.White else Tz.colors.muted,
        )
    }
}

/**
 * FR-HAB-10.3 — a habit already serving an upcoming/archived theme renders
 * the theme as a read-only annotated chip (not togglable). Mirrors the
 * goal editor's ReadOnlyChip.
 */
@Composable
private fun DirectionReadOnlyChip(label: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Tz.colors.line.a(0.30f))
            .border(1.dp, Tz.colors.line, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = Tz.colors.faint,
        )
    }
}

@Composable
private fun StepBtn(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(10.dp))
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = TextStyle(fontFamily = DenType.body, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = Tz.colors.rust,
        )
    }
}
