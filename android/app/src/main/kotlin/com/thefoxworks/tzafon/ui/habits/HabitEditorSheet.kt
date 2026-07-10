package com.thefoxworks.tzafon.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.ui.components.CueSheet
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.SheetGhostButton
import com.thefoxworks.tzafon.ui.components.SheetPrimaryButton
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
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
    goals: List<com.thefoxworks.tzafon.domain.model.Goal> = emptyList(),
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
                    color = Den.muted,
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
                        TzIcons.Target(15.dp, Den.rust)
                        Text(
                            g.title,
                            style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                            color = Den.ink,
                            modifier = Modifier.weight(1f),
                        )
                        if (goalId == g.id) TzIcons.Check(15.dp, Den.rust, 2.6f)
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
                    TzIcons.Trash(18.dp, Den.due)
                    Text(
                        "Delete this habit?",
                        style = TextStyle(fontFamily = DenType.serif, fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                        color = Den.ink,
                    )
                }
                Text(
                    "This wipes the habit and its whole history — every logged day. There's no undo. Freezing the linked tasks keeps the history instead.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, lineHeight = 21.sp),
                    color = Den.muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                SheetPrimaryButton(label = "Delete — history and all", color = Den.due) {
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
                textStyle = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Den.ink).contentDir(),
                cursorBrush = SolidColor(Den.rust),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Den.card)
                            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                "Write in the morning",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                color = Den.faint,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )

            SectionLabel("Kind", modifier = Modifier.padding(top = 16.dp))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                KindChip("TIMES A WEEK", kind == HabitKind.FREQUENCY) { kind = HabitKind.FREQUENCY }
                KindChip("AN AMOUNT A DAY", kind == HabitKind.QUANTITATIVE) { kind = HabitKind.QUANTITATIVE }
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
                            .background(Den.card)
                            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
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
                            textStyle = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Den.ink),
                            cursorBrush = SolidColor(Den.rust),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Den.card)
                            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        BasicTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            textStyle = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, color = Den.ink).contentDir(),
                            cursorBrush = SolidColor(Den.rust),
                            singleLine = true,
                            decorationBox = { inner ->
                                Box {
                                    if (unit.isEmpty()) {
                                        Text(
                                            "words · km · pages",
                                            style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp),
                                            color = Den.faint,
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
                        .border(1.dp, Den.line, RoundedCornerShape(12.dp))
                        .pressable { goalPick = true }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TzIcons.Target(14.dp, if (linked != null) Den.rust else Den.muted)
                    Text(
                        linked?.title ?: "Point it at a goal",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                        color = if (linked != null) Den.ink else Den.muted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (linked != null) "GOAL" else "",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                        color = Den.faint,
                    )
                }
            }

            SectionLabel("Cue · when will you do it?", modifier = Modifier.padding(top = 16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (cue != null) Den.amber.a(0.1f) else Den.card)
                    .border(1.dp, if (cue != null) Den.amber.a(0.4f) else Den.line, RoundedCornerShape(13.dp))
                    .pressable { cueOpen = true }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                TzIcons.Cue(17.dp, if (cue != null) Den.rust else Den.faint)
                Column(Modifier.weight(1f)) {
                    Text(
                        cue?.label ?: "Anchor it to a routine",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                        color = if (cue != null) Den.ink else Den.faint,
                    )
                    Text(
                        if (cue != null) "${cue!!.type.name.replace('_', '-')} · A TRIGGER BEATS A CLOCK"
                        else "WITHOUT A CUE IT'S JUST A TRACKER",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                        color = Den.faint,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                TzIcons.Chevron(17.dp, Den.ink.a(0.28f))
            }

            SheetPrimaryButton(
                label = if (initial == null) "Start the rhythm" else "Save",
                enabled = name.isNotBlank(),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                onSave(
                    Habit(
                        id = initial?.id ?: "",
                        name = name.trim(),
                        kind = kind,
                        target = target,
                        unit = unit.trim().takeIf { it.isNotBlank() && kind == HabitKind.QUANTITATIVE },
                        targetDays = targetDays.takeIf { kind == HabitKind.QUANTITATIVE },
                        cue = cue,
                        primaryThemeId = initial?.primaryThemeId,
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
private fun KindChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) Den.green else Color.Transparent)
            .border(1.dp, if (on) Den.green else Den.line, RoundedCornerShape(999.dp))
            .pressable(onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = if (on) Color.White else Den.muted,
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
            color = Den.ink,
        )
        StepBtn("+") { if (value < range.last) onChange(value + 1) }
        Text(
            "GENTLE BEATS PERFECT",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
            color = Den.faint,
        )
    }
}

@Composable
private fun StepBtn(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(10.dp))
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = TextStyle(fontFamily = DenType.body, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = Den.rust,
        )
    }
}
