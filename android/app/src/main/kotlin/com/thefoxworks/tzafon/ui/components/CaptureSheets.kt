package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir

/**
 * DM-CUE — "When X, I will do Y", always one line, never a wizard.
 * Shared by the task editor (DM-TASK-4) and the habit editor (DM-HABIT-1).
 */
@Composable
fun CueSheet(
    current: Cue?,
    onSave: (Cue?) -> Unit,
    onClose: () -> Unit,
) {
    var type by remember { mutableStateOf(current?.type ?: CueType.AFTER_ROUTINE) }
    var label by remember { mutableStateOf(current?.label ?: "") }
    var time by remember { mutableStateOf(current?.time ?: "") }

    DenSheet(title = "When will you do this?", onClose = onClose) {
        Column {
            Text(
                "A trigger beats a clock — anchor it to something you already do.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                color = Den.muted,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CueKindChip("AFTER A ROUTINE", type == CueType.AFTER_ROUTINE) { type = CueType.AFTER_ROUTINE }
                CueKindChip("AT A TIME", type == CueType.AT_TIME) { type = CueType.AT_TIME }
                CueKindChip("AT A PLACE", type == CueType.AT_PLACE) { type = CueType.AT_PLACE }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Den.amber.a(0.1f))
                    .border(1.dp, Den.amber.a(0.4f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                TzIcons.Cue(17.dp, Den.rust)
                BasicTextField(
                    value = label,
                    onValueChange = { label = it },
                    textStyle = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, color = Den.ink).contentDir(),
                    cursorBrush = SolidColor(Den.rust),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (label.isEmpty()) {
                                Text(
                                    when (type) {
                                        CueType.AFTER_ROUTINE -> "After the first coffee"
                                        CueType.AT_TIME -> "Right after lunch"
                                        CueType.AT_PLACE -> "At the studio"
                                    },
                                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                                    color = Den.faint,
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            if (type == CueType.AT_TIME) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Den.card)
                        .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    TzIcons.Clock(16.dp, Den.muted)
                    BasicTextField(
                        value = time,
                        onValueChange = { if (it.length <= 5) time = it },
                        textStyle = TextStyle(fontFamily = DenType.mono, fontSize = 15.sp, color = Den.ink),
                        cursorBrush = SolidColor(Den.rust),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { inner ->
                            Box {
                                if (time.isEmpty()) {
                                    Text(
                                        "08:30",
                                        style = TextStyle(fontFamily = DenType.mono, fontSize = 15.sp),
                                        color = Den.faint,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "REMINDS AT THIS TIME",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                        color = Den.faint,
                    )
                }
            }

            SheetPrimaryButton(
                label = "Set the cue",
                enabled = label.isNotBlank(),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                onSave(Cue(type, label.trim(), time.trim().takeIf { it.isNotBlank() }))
                onClose()
            }
            if (current != null) {
                SheetGhostButton(label = "No cue for this one", modifier = Modifier.padding(top = 9.dp)) {
                    onSave(null); onClose()
                }
            }
        }
    }
}

@Composable
private fun CueKindChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) Den.rust else Color.Transparent)
            .border(1.dp, if (on) Den.rust else Den.line, RoundedCornerShape(999.dp))
            .pressable(onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = if (on) Color.White else Den.muted,
        )
    }
}

/** DM-HABIT-5 — the "how much?" prompt for quantitative completions. */
@Composable
fun AmountSheet(
    title: String,
    unit: String?,
    suggested: Double? = null,
    onConfirm: (Double) -> Unit,
    onClose: () -> Unit,
) {
    var text by remember {
        mutableStateOf(suggested?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    val value = text.toDoubleOrNull()

    DenSheet(title = title, onClose = onClose) {
        Column {
            Text(
                "How much did it come to? Any number is a real number.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                color = Den.muted,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Den.card)
                    .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    textStyle = TextStyle(
                        fontFamily = DenType.serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Den.ink,
                    ),
                    cursorBrush = SolidColor(Den.rust),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    "0",
                                    style = TextStyle(fontFamily = DenType.serif, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                                    color = Den.faint,
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                if (unit != null) {
                    Text(
                        unit.uppercase(),
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 0.5.sp),
                        color = Den.muted,
                    )
                }
            }
            SheetPrimaryButton(
                label = "Log it",
                enabled = value != null && value > 0,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                value?.let(onConfirm)
                onClose()
            }
        }
    }
}

/**
 * FR-HAB-6 — the primary confirmation button used by the create-habit,
 * create-goal, create-theme (and analogous) flows. The label routes through
 * the Material 3 [ColorScheme] so it always pairs correctly with the container
 * (default: `primary` + `onPrimary`, i.e. rust + cream). Callers overriding
 * `color` should override [contentColor] with the matching on-role to keep
 * WCAG AA ≥ 4.5:1 (e.g. `error` + `onError` for delete confirmations).
 */
@Composable
fun SheetPrimaryButton(
    label: String,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Opacity is baked into the colors (not applied via a Modifier.alpha layer): an
    // alpha(1f) graphics layer over .background() was dropping the fill, leaving the
    // enabled button invisible (cream text on the sheet). See FR-HAB-6.
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (enabled) color else color.copy(alpha = 0.45f))
            .then(if (enabled) Modifier.pressable(onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun SheetGhostButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
            .pressable(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = Den.muted,
        )
    }
}
