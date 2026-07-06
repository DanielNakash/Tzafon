package com.thefoxworks.tzafon.ui.goals

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
import androidx.compose.runtime.toMutableStateList
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
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalStep
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.SheetGhostButton
import com.thefoxworks.tzafon.ui.components.SheetPrimaryButton
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * Create / edit a goal (DM-GOAL-1): three types, honest starting progress,
 * optional commitment line. The create flow nudges specific-and-finishable
 * (SMART in spirit, never a form-wall).
 */
@Composable
fun GoalEditorSheet(
    initial: Goal?,
    onSave: (Goal) -> Unit,
    onDelete: (String) -> Unit,
    onComplete: (Goal) -> Unit,
    onClose: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: GoalType.STEPPED) }
    var steps = remember {
        (initial?.steps ?: listOf(GoalStep("Defined this goal ✓", true))).toMutableStateList()
    }
    var newStep by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf(initial?.targetQty?.takeIf { it > 0 }?.let(::fmtNum) ?: "") }
    var currentText by remember { mutableStateOf(initial?.currentQty?.takeIf { it > 0 }?.let(::fmtNum) ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var commitment by remember { mutableStateOf(initial?.commitment ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    DenSheet(
        title = if (initial == null) "Aim at something" else "Edit goal",
        onClose = onClose,
    ) {
        if (confirmDelete) {
            Column(Modifier.padding(top = 6.dp)) {
                Text(
                    "Delete this goal?",
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                    color = Den.ink,
                )
                Text(
                    "Its progress record goes with it. Freezing keeps it on ice instead — nothing lost.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, lineHeight = 21.sp),
                    color = Den.muted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                )
                SheetPrimaryButton(label = "Delete it", color = Den.due) {
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
            SectionLabel("What's the finish line?")
            EditorField(
                value = title,
                onChange = { title = it },
                placeholder = "Run a half marathon",
                serif = true,
            )

            SectionLabel("Kind", modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TypeChip("STEPS", type == GoalType.STEPPED) { type = GoalType.STEPPED }
                TypeChip("AN AMOUNT", type == GoalType.ACCUMULATIVE) { type = GoalType.ACCUMULATIVE }
                TypeChip("DIRECTIONAL", type == GoalType.GENERIC) { type = GoalType.GENERIC }
            }

            when (type) {
                GoalType.STEPPED -> {
                    SectionLabel("Steps · first one's free", modifier = Modifier.padding(top = 14.dp))
                    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.forEachIndexed { i, s ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    Modifier.size(16.dp).clip(RoundedCornerShape(5.dp))
                                        .let {
                                            if (s.done) it.background(Den.rust)
                                            else it.border(1.5.dp, Den.ink.a(0.28f), RoundedCornerShape(5.dp))
                                        }
                                        .pressable { steps[i] = s.copy(done = !s.done) },
                                    contentAlignment = Alignment.Center,
                                ) { if (s.done) TzIcons.Check(11.dp, Color.White) }
                                Text(
                                    s.label,
                                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                                    color = Den.ink,
                                    modifier = Modifier.weight(1f),
                                )
                                if (steps.size > 1) {
                                    Box(Modifier.pressable { steps.removeAt(i) }.padding(3.dp)) {
                                        TzIcons.X(12.dp, Den.faint)
                                    }
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TzIcons.Plus(14.dp, Den.muted)
                            BasicTextField(
                                value = newStep,
                                onValueChange = { newStep = it },
                                textStyle = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, color = Den.ink),
                                cursorBrush = SolidColor(Den.rust),
                                singleLine = true,
                                decorationBox = { inner ->
                                    Box {
                                        if (newStep.isEmpty()) {
                                            Text(
                                                "Add a step…",
                                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                                                color = Den.faint,
                                            )
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (newStep.isNotBlank()) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(7.dp)).background(Den.rust.a(0.12f))
                                        .pressable {
                                            steps.add(GoalStep(newStep.trim(), false))
                                            newStep = ""
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "ADD",
                                        style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                                        color = Den.rust,
                                    )
                                }
                            }
                        }
                    }
                }

                GoalType.ACCUMULATIVE -> {
                    SectionLabel("Target · unit · honest start", modifier = Modifier.padding(top = 14.dp))
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            EditorField(targetText, { targetText = it.filter { c -> c.isDigit() || c == '.' } }, "60000", number = true)
                        }
                        Box(Modifier.weight(1f)) {
                            EditorField(unit, { unit = it }, "words")
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            EditorField(currentText, { currentText = it.filter { c -> c.isDigit() || c == '.' } }, "Already at… (optional)", number = true)
                        }
                    }
                    Text(
                        "Count what's already real — the bar never starts empty if you've started.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp, lineHeight = 16.5.sp),
                        color = Den.faint,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }

                GoalType.GENERIC -> {
                    Text(
                        "A direction without a number — tasks and habits feed it; you decide when it's done.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 17.5.sp),
                        color = Den.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            SectionLabel("Commitment · optional", modifier = Modifier.padding(top = 14.dp))
            EditorField(
                value = commitment,
                onChange = { commitment = it },
                placeholder = "\"Drafts before dinner — then the good coffee.\"",
            )

            SheetPrimaryButton(
                label = if (initial == null) "Set the goal" else "Save",
                enabled = title.isNotBlank(),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                onSave(
                    Goal(
                        id = initial?.id ?: "",
                        title = title.trim(),
                        description = initial?.description ?: "",
                        type = type,
                        steps = if (type == GoalType.STEPPED) steps.toList() else emptyList(),
                        targetQty = targetText.toDoubleOrNull() ?: 0.0,
                        unit = unit.trim().takeIf { it.isNotBlank() && type == GoalType.ACCUMULATIVE },
                        currentQty = currentText.toDoubleOrNull() ?: initial?.currentQty ?: 0.0,
                        state = initial?.state ?: com.thefoxworks.tzafon.domain.model.GoalState.ONGOING,
                        deadline = initial?.deadline,
                        commitment = commitment.trim().takeIf { it.isNotBlank() },
                        primaryThemeId = initial?.primaryThemeId,
                        lastActivityAt = initial?.lastActivityAt ?: 0,
                        completedAt = initial?.completedAt,
                        createdAt = initial?.createdAt ?: 0,
                    ),
                )
                onClose()
            }
            if (initial != null && initial.state == com.thefoxworks.tzafon.domain.model.GoalState.ONGOING) {
                SheetGhostButton(label = "Mark it complete", modifier = Modifier.padding(top = 9.dp)) {
                    onClose(); onComplete(initial)
                }
            }
            if (initial != null) {
                SheetGhostButton(label = "Delete this goal…", modifier = Modifier.padding(top = 9.dp)) {
                    confirmDelete = true
                }
            }
        }
    }
}

/** DM-GOAL-4 — celebrate, then the open question. Never a badge. */
@Composable
fun CompletionSheet(
    goal: Goal,
    onEnjoy: () -> Unit,
    onFollowOn: () -> Unit,
    onMakeHabit: () -> Unit,
) {
    DenSheet(title = "A goal, finished.", onClose = onEnjoy) {
        Column {
            Text(
                "“${goal.title}” — done. That happened because you kept showing up.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 21.sp),
                color = Den.ink,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                "Where would you like to go from here?",
                style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                color = Den.ink,
                modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
            )
            SheetPrimaryButton(label = "Make a habit of it", color = Den.green) { onMakeHabit() }
            SheetGhostButton(label = "Set a follow-on goal", modifier = Modifier.padding(top = 9.dp)) { onFollowOn() }
            SheetGhostButton(label = "Just enjoy it", modifier = Modifier.padding(top = 9.dp)) { onEnjoy() }
        }
    }
}

@Composable
private fun TypeChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) Den.rust else Color.Transparent)
            .border(1.dp, if (on) Den.rust else Den.line, RoundedCornerShape(999.dp))
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
private fun EditorField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    serif: Boolean = false,
    number: Boolean = false,
) {
    val style = if (serif) {
        TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Den.ink)
    } else {
        TextStyle(fontFamily = DenType.body, fontSize = 15.sp, color = Den.ink)
    }
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style,
        cursorBrush = SolidColor(Den.rust),
        singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Den.card)
                    .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = Den.faint, fontWeight = FontWeight.Normal))
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
}

private fun fmtNum(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
