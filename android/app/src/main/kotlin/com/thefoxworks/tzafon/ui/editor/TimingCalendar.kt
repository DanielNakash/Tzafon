package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.dates.MonthGrid
import com.thefoxworks.tzafon.domain.editor.Armed
import com.thefoxworks.tzafon.domain.editor.SharedCalendar
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a
import java.time.YearMonth

/**
 * v2.13.0 — the shared calendar (deck slides 3, 5, 6): the To do and
 * Deadline tiles, the rail for the [armed] field, one month grid holding
 * both dates and the window between, the legend, and the inline conflict
 * warning. Every rule comes from [SharedCalendar]; this only draws it.
 * The frame owns [armed] and [month], so they survive tab switches and
 * rotation; the grid starts on [weekStart] (FR-SET-1).
 */
@Composable
internal fun TimingCalendar(
    draft: TaskDraft,
    today: String,
    armed: Armed,
    month: YearMonth,
    weekStart: String,
    onArm: (Armed) -> Unit,
    onMonth: (YearMonth) -> Unit,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    val cal = SharedCalendar(draft, armed, today)
    // a deadline's marks and chips are red, the to-do date's and the end's rust
    val accent = if (armed == Armed.DEADLINE) Tz.colors.due else Tz.colors.rust

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        // ── the two tiles: the lit one is what a tap fills ──
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DateTile("To-do", cal.toDoTile, armed == Armed.TODO, Tz.colors.rust, Modifier.weight(1f), { onArm(Armed.TODO) }) { c ->
                TzIcons.Calendar(17.dp, c)
            }
            DateTile("Deadline", cal.deadlineTile, armed == Armed.DEADLINE, Tz.colors.due, Modifier.weight(1f), { onArm(Armed.DEADLINE) }) { c ->
                TzIcons.Flag(16.dp, c)
            }
        }

        cal.conflict?.let { conflict ->
            Column(
                Modifier.fillMaxWidth().padding(top = 11.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Tz.colors.due.a(0.08f))
                    .border(1.dp, Tz.colors.due.a(0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TzIcons.Alert(14.dp, Tz.colors.due)
                    Text(conflict.warning, style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp), color = Tz.colors.due)
                }
                if (conflict.fixable) {
                    Box(
                        Modifier.padding(top = 9.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, Tz.colors.due.a(0.6f), RoundedCornerShape(999.dp))
                            .pressable(null, Role.Button) { onEdit { SharedCalendar(it, armed, today).fix() } }
                            .padding(horizontal = 13.dp, vertical = 7.dp),
                    ) {
                        Text(conflict.fix, style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.due)
                    }
                }
            }
        }

        // ── the separator caption ──
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Tz.colors.line))
            Text(
                cal.caption.uppercase(),
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 1.8.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Box(Modifier.weight(1f).height(1.dp).background(Tz.colors.line))
        }

        // ── the rail ──
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            cal.rail.forEach { chip ->
                Box(
                    Modifier
                        .alpha(if (chip.enabled) 1f else 0.4f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (chip.highlighted) accent else Tz.colors.card)
                        .border(1.dp, if (chip.highlighted) accent else Tz.colors.line, RoundedCornerShape(999.dp))
                        .semantics {
                            selected = chip.highlighted
                            if (!chip.enabled) disabled()
                        }
                        .pressable(null, Role.Button) { onEdit { SharedCalendar(it, armed, today).pick(chip) } }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        chip.label,
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, fontWeight = if (chip.highlighted) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (chip.highlighted) Tz.colors.cream else Tz.colors.ink,
                    )
                }
            }
        }

        // ── month header ──
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).semantics { contentDescription = "Previous month" }.pressable(null, Role.Button) { onMonth(month.minusMonths(1)) }, contentAlignment = Alignment.Center) {
                TzIcons.Chevron(18.dp, Tz.colors.muted, dir = TzIcons.Dir.LEFT)
            }
            Text(
                "${Dates.MO_FULL[month.monthValue - 1]} ${month.year}",
                style = TextStyle(fontFamily = DenType.serif, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(36.dp).semantics { contentDescription = "Next month" }.pressable(null, Role.Button) { onMonth(month.plusMonths(1)) }, contentAlignment = Alignment.Center) {
                TzIcons.Chevron(18.dp, Tz.colors.muted)
            }
        }

        // ── the grid ──
        val grid = MonthGrid.of(month, weekStart)
        Row(Modifier.fillMaxWidth()) {
            grid.header.forEach { d ->
                Text(
                    d.take(1),
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.5.sp),
                    color = Tz.colors.faint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }
        grid.weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).height(46.dp)) {
                        if (day != null) DayCell(cal, day, armed) { onEdit { SharedCalendar(it, armed, today).tap(day) } }
                    }
                }
            }
        }

        // ── legend and hint ──
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(Tz.colors.line))
        Legend(showEnd = armed == Armed.END || draft.recurrence?.endDate != null)
        Text(
            cal.hint,
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 20.sp),
            color = Tz.colors.muted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun DateTile(
    label: String,
    value: String,
    lit: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (lit) accent.a(0.09f) else Tz.colors.surfaceAlt)
            .border(if (lit) 1.5.dp else 0.dp, if (lit) accent.a(0.6f) else Color.Transparent, RoundedCornerShape(13.dp))
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                selected = lit
            }
            .pressable(onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon(if (lit) accent else Tz.colors.muted)
        Column {
            Text(
                label.uppercase(),
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = if (lit) FontWeight.Bold else FontWeight.Normal),
                color = if (lit) accent else Tz.colors.muted,
            )
            Text(
                value,
                style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Normal),
                color = if (lit) Tz.colors.ink else Tz.colors.muted,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/**
 * One day: filled for the to-do date, ringed for the deadline, both when they
 * share a day; the window shaded edge to edge so it reads as one run; a halo
 * on the armed field's own day; a dot for today; a bar for the end date.
 */
@Composable
private fun DayCell(cal: SharedCalendar, day: String, armed: Armed, onTap: () -> Unit) {
    val m = cal.mark(day)
    val rust = Tz.colors.rust
    val due = Tz.colors.due
    val halo = when {
        armed == Armed.TODO && m.toDo -> rust
        armed == Armed.DEADLINE && m.deadline -> due
        armed == Armed.END && m.end -> rust
        else -> null
    }
    Box(
        Modifier.fillMaxSize()
            .background(if (m.window) rust.a(0.09f) else Color.Transparent)
            .alpha(if (m.enabled) 1f else 0.3f)
            .pressable(onTap)
            .clearAndSetSemantics {
                contentDescription = cal.describe(day)
                if (!m.enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (halo != null) Circle(44.dp, halo.a(0.16f))
        when {
            m.toDo && m.deadline -> { Circle(38.dp, due); Circle(33.dp, rust) }
            m.toDo -> Circle(36.dp, rust)
            m.deadline -> Box(Modifier.size(36.dp).border(2.dp, due, CircleShape))
            m.today -> Box(Modifier.size(36.dp).border(1.dp, Tz.colors.faint, CircleShape))
        }
        Text(
            Dates.parse(day).dayOfMonth.toString(),
            style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, fontWeight = if (m.toDo || m.deadline) FontWeight.SemiBold else FontWeight.Normal),
            color = when {
                m.toDo -> Tz.colors.cream
                m.deadline -> due
                else -> Tz.colors.ink
            },
        )
        if (m.today) {
            Box(
                Modifier.align(Alignment.Center).padding(top = 25.dp).size(4.dp).clip(CircleShape)
                    .background(if (m.toDo) Tz.colors.cream else Tz.colors.muted),
            )
        }
        if (m.end) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp).size(width = 18.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(rust))
        }
    }
}

@Composable
private fun Circle(size: Dp, color: Color) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(showEnd: Boolean) {
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem("To-do") { Circle(15.dp, Tz.colors.rust) }
        LegendItem("Deadline") { Box(Modifier.size(15.dp).border(2.dp, Tz.colors.due, CircleShape)) }
        LegendItem("The window between") {
            Box(Modifier.size(15.dp).clip(RoundedCornerShape(3.dp)).background(Tz.colors.rust.a(0.12f)))
        }
        LegendItem("Today") {
            Box(Modifier.size(15.dp).border(1.dp, Tz.colors.faint, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.padding(top = 6.dp).size(3.dp).clip(CircleShape).background(Tz.colors.muted))
            }
        }
        if (showEnd) LegendItem("End") {
            Box(Modifier.size(width = 15.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(Tz.colors.rust))
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        swatch()
        Text(
            label.uppercase(),
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 1.2.sp),
            color = Tz.colors.muted,
        )
    }
}
