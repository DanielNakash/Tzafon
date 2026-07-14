package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import java.time.LocalDate

/** Den bottom-sheet shell (CalendarPicker.jsx Sheet). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenSheet(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Tz.colors.surface,
        contentColor = Tz.colors.ink,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(Modifier.padding(top = 14.dp, bottom = 12.dp).size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(Tz.colors.line))
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // imePadding lifts the sheet content (incl. the primary confirm button) above the
        // soft keyboard; without it, focusing a text field pushes the button out of view.
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = TextStyle(fontFamily = DenType.serif, fontSize = 23.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.ink)
                Box(Modifier.weight(1f))
                Box(
                    Modifier.size(34.dp).wrapContentSize(Alignment.Center, unbounded = true).size(48.dp)
                        .pressable("Close", androidx.compose.ui.semantics.Role.Button, onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Tz.colors.surfaceAlt),
                        contentAlignment = Alignment.Center,
                    ) { TzIcons.X(16.dp, Tz.colors.muted) }
                }
            }
            Box(Modifier.padding(top = 8.dp, bottom = 26.dp)) { content() }
        }
    }
}

/**
 * Month calendar + quick picks + clear (CalendarPicker.jsx).
 *
 * `maxDate` (FR-HAB-8.2) — when set, dates strictly after it are disabled:
 * quick picks past `maxDate` are omitted, grid cells past it render greyed
 * and non-pressable. `null` (default) preserves the original open-ended
 * behaviour used by Planning's reschedule and range pickers.
 */
@Composable
fun CalendarPicker(
    value: String?,
    today: String,
    onPick: (String) -> Unit,
    onClear: (() -> Unit)? = null,
    maxDate: String? = null,
    // FR-HAB-9.3 — when supplied (the "Log a date…" habit flow), each day cell
    // announces its log state to TalkBack ("Tap to log …" / "Tap to un-log …")
    // and logged days carry a small filled marker. Null for generic date pickers.
    loggedDates: Set<String>? = null,
) {
    val init = remember(value, today) { Dates.parse(value ?: today) }
    var viewYear by remember { mutableStateOf(init.year) }
    var viewMonth by remember { mutableStateOf(init.monthValue) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        // quick picks
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            listOf(
                "Today" to today,
                "Tomorrow" to Dates.addDays(today, 1),
                "In a week" to Dates.addDays(today, 7),
                "Next month" to Dates.addDays(today, 30),
            ).filter { (_, d) -> maxDate == null || d <= maxDate }
                .forEach { (label, d) ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Tz.colors.card)
                        .border(1.dp, Tz.colors.line, RoundedCornerShape(999.dp))
                        .pressable { onPick(d) }
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(label, style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp), color = Tz.colors.ink)
                }
            }
        }

        // month nav
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Tz.colors.card).border(1.dp, Tz.colors.line, RoundedCornerShape(10.dp))
                    .pressable {
                        if (viewMonth == 1) { viewMonth = 12; viewYear-- } else viewMonth--
                    },
                contentAlignment = Alignment.Center,
            ) { TzIcons.Chevron(18.dp, Tz.colors.ink, dir = TzIcons.Dir.LEFT) }
            Text(
                "${Dates.MO_FULL[viewMonth - 1]} $viewYear",
                style = TextStyle(fontFamily = DenType.serif, fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Tz.colors.card).border(1.dp, Tz.colors.line, RoundedCornerShape(10.dp))
                    .pressable {
                        if (viewMonth == 12) { viewMonth = 1; viewYear++ } else viewMonth++
                    },
                contentAlignment = Alignment.Center,
            ) { TzIcons.Chevron(18.dp, Tz.colors.ink) }
        }

        // weekday header + day grid
        val first = LocalDate.of(viewYear, viewMonth, 1)
        val startPad = Dates.dayOfWeek(first)
        val daysInMonth = first.lengthOfMonth()
        Row(Modifier.fillMaxWidth()) {
            Dates.WD.forEach { d ->
                Text(
                    d.take(1),
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.5.sp),
                    color = Tz.colors.faint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }
        val cells: List<String?> = List(startPad) { null } + (1..daysInMonth).map { Dates.iso(LocalDate.of(viewYear, viewMonth, it)) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { c ->
                    if (c == null) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val isToday = c == today
                        val isSel = c == value
                        val disabled = maxDate != null && c > maxDate
                        val logged = loggedDates?.contains(c) == true
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.5.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Tz.colors.rust else Color.Transparent)
                                .then(if (isToday && !isSel) Modifier.border(1.5.dp, Tz.colors.rust.a(0.4f), RoundedCornerShape(10.dp)) else Modifier)
                                .then(
                                    if (disabled) Modifier
                                    else Modifier.pressable(
                                        // FR-HAB-9.3 — distinct log-state announcement per day.
                                        label = if (loggedDates != null) {
                                            if (logged) "Tap to un-log $c" else "Tap to log $c"
                                        } else null,
                                        role = Role.Button,
                                    ) { onPick(c) }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${Dates.parse(c).dayOfMonth}",
                                    style = TextStyle(
                                        fontFamily = DenType.body, fontSize = 14.5.sp,
                                        fontWeight = if (isToday || isSel || logged) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                    color = when {
                                        disabled -> Tz.colors.faint
                                        isSel -> Color.White
                                        logged -> Tz.colors.green
                                        isToday -> Tz.colors.rust
                                        else -> Tz.colors.ink
                                    },
                                )
                                // FR-HAB-9.3 — logged-day marker so the un-log target is visible.
                                if (logged) {
                                    Box(
                                        Modifier
                                            .padding(top = 1.5.dp)
                                            .size(4.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isSel) Color.White else Tz.colors.green),
                                    )
                                }
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f).aspectRatio(1f)) }
            }
        }

        if (onClear != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                    .pressable(onClear),
                contentAlignment = Alignment.Center,
            ) {
                Text("Clear date", style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.muted)
            }
        }
    }
}
