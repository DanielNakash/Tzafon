package com.thefoxworks.tzafon.ui.directions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.ArchivedOutcome
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.domain.model.ThemeState
import com.thefoxworks.tzafon.domain.themes.ThemeLogic
import com.thefoxworks.tzafon.ui.components.CalendarPicker
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
 * Create / edit a theme (DM-THEME-1): approach-framed name, the mandatory
 * why, and a window that orients rather than deadlines (DEC-5).
 */
@Composable
fun ThemeEditorSheet(
    initial: Theme?,
    onSave: (Theme, Boolean) -> Unit,  // (theme, activate)
    onArchive: (Theme) -> Unit,
    onClose: () -> Unit,
) {
    val today = Dates.todayIso()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var why by remember { mutableStateOf(initial?.why ?: "") }
    var start by remember { mutableStateOf(initial?.windowStart ?: today) }
    var end by remember { mutableStateOf(initial?.windowEnd ?: ThemeLogic.defaultWindowEnd(today)) }
    var picking by remember { mutableStateOf<String?>(null) } // start | end

    if (picking != null) {
        DenSheet(
            title = if (picking == "start") "Window starts" else "Review point",
            onClose = { picking = null },
        ) {
            CalendarPicker(
                value = if (picking == "start") start else end,
                today = today,
                onPick = { d ->
                    if (picking == "start") {
                        start = d
                        if (end <= d) end = ThemeLogic.defaultWindowEnd(d)
                    } else end = d
                    picking = null
                },
            )
        }
        return
    }

    DenSheet(
        title = if (initial == null) "Hold a direction" else "Edit bearing",
        onClose = onClose,
    ) {
        Column {
            SectionLabel("The direction · more of what you want")
            Field(
                value = name,
                onChange = { name = it },
                placeholder = "More writing, more often",
                serif = true,
            )
            Text(
                "Frame it toward — “more of…”, never “stop doing…”.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp),
                color = Tz.colors.faint,
                modifier = Modifier.padding(top = 6.dp),
            )

            SectionLabel("Why · what it's really for", modifier = Modifier.padding(top = 14.dp))
            Field(
                value = why,
                onChange = { why = it },
                placeholder = "Because the book won't write itself — and I miss it.",
                italic = true,
            )
            Text(
                "The why is what keeps a theme from going fluffy. It's required — one honest line.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp),
                color = Tz.colors.faint,
                modifier = Modifier.padding(top = 6.dp),
            )

            SectionLabel("Window · a season, not a deadline", modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateBox(label = "FROM", value = start, modifier = Modifier.weight(1f)) { picking = "start" }
                DateBox(label = "REVIEW AT", value = end, modifier = Modifier.weight(1f)) { picking = "end" }
            }
            Text(
                "Default is a quarter. The end is a renew moment — a theme can't fail.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp),
                color = Tz.colors.faint,
                modifier = Modifier.padding(top = 6.dp),
            )

            val canSave = name.isNotBlank() && why.isNotBlank()
            val building = {
                Theme(
                    id = initial?.id ?: "",
                    name = name.trim(),
                    why = why.trim(),
                    windowStart = start,
                    windowEnd = end,
                    state = initial?.state ?: ThemeState.UPCOMING,
                    archivedOutcome = initial?.archivedOutcome,
                    renewedToThemeId = initial?.renewedToThemeId,
                    accentSlot = initial?.accentSlot ?: 0,
                    createdAt = initial?.createdAt ?: 0,
                    archivedAt = initial?.archivedAt,
                )
            }

            if (initial?.state == ThemeState.ACTIVE) {
                SheetPrimaryButton(label = "Save", enabled = canSave, modifier = Modifier.padding(top = 18.dp)) {
                    onSave(building().copy(state = ThemeState.ACTIVE), false)
                    onClose()
                }
                SheetGhostButton(label = "Archive — renew, evolve or rest…", modifier = Modifier.padding(top = 9.dp)) {
                    onArchive(initial)
                }
            } else {
                SheetPrimaryButton(
                    label = if (start <= today) "Hold this bearing now" else "Save — starts ${Dates.fmtDate(start)}",
                    enabled = canSave,
                    modifier = Modifier.padding(top = 18.dp),
                ) {
                    onSave(building(), start <= today) // activate when the window has begun
                    onClose()
                }
                if (start <= today) {
                    SheetGhostButton(label = "Park it as upcoming instead", modifier = Modifier.padding(top = 9.dp)) {
                        onSave(building().copy(state = ThemeState.UPCOMING), false)
                        onClose()
                    }
                }
            }
        }
    }
}

/** DM-THEME-3 — the one hard cap, softened: offer Upcoming, never a wall. */
@Composable
fun CapSheet(theme: Theme, onPark: () -> Unit, onClose: () -> Unit) {
    DenSheet(title = "Three bearings are live", onClose = onClose) {
        Column {
            Text(
                "Three directions is the most the compass holds — that's what keeps each one real. “${theme.name}” can wait as upcoming and step in when a window ends.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, lineHeight = 21.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 3.dp, bottom = 16.dp),
            )
            SheetPrimaryButton(label = "Park it as upcoming") { onPark() }
            SheetGhostButton(label = "Not now", modifier = Modifier.padding(top = 9.dp)) { onClose() }
        }
    }
}

/** DM-THEME-4 — Review & Renew: renew / evolve / retire. Never a failure. */
@Composable
fun ArchiveSheet(theme: Theme, onPick: (ArchivedOutcome) -> Unit, onClose: () -> Unit) {
    DenSheet(title = "The window closes", onClose = onClose) {
        Column {
            Text(
                "“${theme.name}” has run its season. However it went — that was the direction you held. Where next?",
                style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, lineHeight = 21.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
            )
            OutcomeRow(
                title = "Renew it",
                sub = "Same direction, a fresh window — it's still where you're headed.",
                icon = { TzIcons.Repeat(16.dp, Tz.colors.green) },
            ) { onPick(ArchivedOutcome.RENEWED) }
            OutcomeRow(
                title = "It evolved",
                sub = "The direction changed shape — archive this one, start the next.",
                icon = { TzIcons.Sprout(16.dp, Tz.colors.amber) },
            ) { onPick(ArchivedOutcome.EVOLVED) }
            OutcomeRow(
                title = "Let it rest",
                sub = "It served its season. It lives on in the Journey.",
                icon = { TzIcons.Moon(16.dp, Tz.colors.backlog) },
                last = true,
            ) { onPick(ArchivedOutcome.RETIRED) }
        }
    }
}

@Composable
private fun OutcomeRow(
    title: String,
    sub: String,
    icon: @Composable () -> Unit,
    last: Boolean = false,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().pressable(onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                    color = Tz.colors.ink,
                )
                Text(
                    sub,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 17.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TzIcons.Chevron(16.dp, Tz.colors.ink.a(0.26f))
        }
        if (!last) {
            Box(
                Modifier.fillMaxWidth().padding(0.dp)
                    .background(Tz.colors.line2)
                    .padding(vertical = 0.5.dp),
            )
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    serif: Boolean = false,
    italic: Boolean = false,
) {
    val style = when {
        serif -> TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Tz.colors.ink)
        italic -> TextStyle(fontFamily = DenType.serif, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = Tz.colors.ink)
        else -> TextStyle(fontFamily = DenType.body, fontSize = 15.sp, color = Tz.colors.ink)
    }.contentDir()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = style,
        cursorBrush = SolidColor(Tz.colors.rust),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Tz.colors.card)
                    .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = Tz.colors.faint, fontWeight = FontWeight.Normal))
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )
}

@Composable
private fun DateBox(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
            .pressable(onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.6.sp),
            color = Tz.colors.faint,
        )
        Text(
            Dates.fmtDate(value),
            style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp),
            color = Tz.colors.ink,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
