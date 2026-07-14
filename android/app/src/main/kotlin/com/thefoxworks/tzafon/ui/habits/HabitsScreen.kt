package com.thefoxworks.tzafon.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.ui.components.AmountSheet
import com.thefoxworks.tzafon.ui.components.CalendarPicker
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Habits (FR-HAB, design: HabitsScreen) — each card: cue front and centre,
 * the forgiving in-period rate with endowed dots/bars, a 5-week history
 * grid and the long arc. No streaks, no adherence % (DEC-2/4).
 */
@Composable
fun HabitsScreen(
    vm: HabitsViewModel,
    onOpenAllTasks: () -> Unit = {},
    onOpenBacklog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Habit?>(null) }   // non-null = editor open
    var creating by remember { mutableStateOf(false) }
    var amountFor by remember { mutableStateOf<HabitCardState?>(null) }
    // FR-HAB-8 — picked-date flows: the "Log which day?" sheet and, for a
    // quantitative habit, the follow-up AmountSheet keyed to that picked date.
    var pickDateFor by remember { mutableStateOf<HabitCardState?>(null) }
    var amountForDate by remember { mutableStateOf<Pair<HabitCardState, String>?>(null) }
    var menu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Tz.colors.bg)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "Habits",
                kicker = "HABITS · RHYTHM",
                onMenu = { menu = true },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 200.dp),
            ) {
                // FR-NAV-5 — the migrated LayerHeader `sub` line moves into a
                // body intro paragraph so the copy is not lost after the header
                // palette collapses to a single rust chrome.
                item(key = "intro") {
                    Text(
                        "Aim for the rate, not perfection. Miss one — that's normal. It's the long arc that counts.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 20.sp),
                        color = Tz.colors.muted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                items(state.cards, key = { it.habit.id }) { card ->
                    HabitCard(
                        card = card,
                        servesGoal = state.goalsById[card.habit.goalId]?.title,
                        onLog = {
                            // FR-HAB-9.1 — the today pill now un-logs directly when
                            // already logged (both kinds; quantitative un-log does not
                            // re-open AmountSheet). The "mark" path keeps v2.5.0 wiring.
                            when {
                                card.loggedToday -> vm.logToday(card.habit.id, done = false)
                                card.habit.kind == HabitKind.QUANTITATIVE -> amountFor = card
                                else -> vm.logToday(card.habit.id, done = true)
                            }
                        },
                        onLogDate = { pickDateFor = card },
                        onEdit = { editing = card.habit },
                    )
                }

                if (state.cards.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 70.dp, start = 30.dp, end = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TzIcons.Sprout(30.dp, Tz.colors.green)
                            Text(
                                "No rhythms yet",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
                                color = Tz.colors.ink,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Text(
                                "A habit is a small thing you keep — anchored to a cue, measured gently by the week. Start with one.",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.5.sp),
                                color = Tz.colors.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)) {
            Fab(onClick = { creating = true }, label = "Habit")
        }
    }

    if (creating || editing != null) {
        val hasHistory = editing?.let { e ->
            state.cards.firstOrNull { it.habit.id == e.id }?.hasHistory ?: false
        } ?: false
        HabitEditorSheet(
            initial = editing,
            onSave = { vm.save(it) },
            onDelete = { vm.delete(it) },
            onClose = { creating = false; editing = null },
            goals = state.goals,
            hasHistory = hasHistory,
        )
    }

    amountFor?.let { card ->
        AmountSheet(
            title = "Log today's ${card.habit.unit ?: "amount"}",
            unit = card.habit.unit,
            suggested = card.todayAmount ?: card.habit.target,
            onConfirm = { vm.logToday(card.habit.id, done = true, amount = it) },
            onClose = { amountFor = null },
        )
    }

    // FR-HAB-8.2 — the "Log which day?" picker; future dates are disabled.
    pickDateFor?.let { card ->
        DenSheet(title = "Log which day?", onClose = { pickDateFor = null }) {
            CalendarPicker(
                value = null,
                today = state.today,
                maxDate = state.today,
                loggedDates = card.logs.filter { it.done }.map { it.date }.toSet(),
                onPick = { pickedDate ->
                    pickDateFor = null
                    val h = card.habit
                    if (h.kind == HabitKind.QUANTITATIVE) {
                        // FR-HAB-8.2.1 — quantitative branch defers to AmountSheet.
                        amountForDate = card to pickedDate
                    } else {
                        // FR-HAB-8.3 — a second tap on an already-done day un-logs it.
                        val existing = card.logs.firstOrNull { it.date == pickedDate && it.done }
                        vm.logForDate(h.id, pickedDate, done = existing == null)
                    }
                },
            )
        }
    }

    // FR-HAB-8.2.1 — the quantitative-picked-date confirm sheet.
    amountForDate?.let { (card, pickedDate) ->
        val existing = card.logs.firstOrNull { it.date == pickedDate }
        val alreadyLogged = existing?.done == true
        AmountSheet(
            title = "Log ${Dates.fmtDate(pickedDate, state.today)} · ${card.habit.name}",
            unit = card.habit.unit,
            suggested = existing?.amount ?: card.habit.target,
            onConfirm = { vm.logForDate(card.habit.id, pickedDate, done = true, amount = it) },
            onClose = { amountForDate = null },
            // FR-HAB-9.2 — reopening on an already-logged date surfaces the clear row.
            onClear = if (alreadyLogged) {
                { vm.logForDate(card.habit.id, pickedDate, done = false, amount = null) }
            } else null,
            clearDateLabel = if (alreadyLogged) Dates.fmtDate(pickedDate, state.today) else null,
        )
    }

    if (menu) {
        AppMenuSheet(
            onClose = { menu = false },
            onAllTasks = onOpenAllTasks,
            onBacklog = onOpenBacklog,
            onSettings = onOpenSettings,
            onAbout = onOpenAbout,
        )
    }
}

@Composable
internal fun HabitCard(
    card: HabitCardState,
    servesGoal: String?,
    onLog: () -> Unit,
    onLogDate: () -> Unit,
    onEdit: () -> Unit,
) {
    val h = card.habit
    val accent = Tz.colors.green // per-theme accents arrive with M6
    val isQuant = h.kind == HabitKind.QUANTITATIVE
    // FR-HAB-5.2 — plain remember (not saveable): navigating away and back
    // re-collapses every card. Persisting expand state would be a stealth
    // "sticky detail" surface, which is the very density regression the
    // collapse-by-default is trying to remove.
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(16.dp)),
    ) {
        // ── FR-HAB-5 + FR-HAB-7.1 collapsed header: name + edit + log + expand
        //     — four affordances, each an isolated hit target. Tap the name
        //     area to toggle expand; the edit pencil and log pill route
        //     separately per FR-HAB-7.2.
        val headerCd = if (expanded) "Collapse habit ${h.name}" else "Expand habit ${h.name}"
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = false) { contentDescription = headerCd }
                    .pressable(
                        label = headerCd,
                        role = Role.Button,
                    ) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TzIcons.Chevron(
                    18.dp,
                    Tz.colors.muted,
                    dir = if (expanded) TzIcons.Dir.DOWN else TzIcons.Dir.RIGHT,
                )
                Text(
                    h.name,
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp).contentDir(),
                    color = Tz.colors.ink,
                    modifier = Modifier.weight(1f),
                )
            }
            CollapsedEditPill(
                habitName = h.name,
                onEdit = onEdit,
            )
            CollapsedLogPill(
                habitName = h.name,
                loggedToday = card.loggedToday,
                accent = accent,
                onLog = onLog,
            )
        }

        if (expanded) {
            // Body sits under a distinct pressable that opens the editor — the
            // collapsed row still owns expand/collapse, so tapping body vs
            // header is unambiguous.
            Column(
                Modifier
                    .fillMaxWidth()
                    .pressable(
                        label = "Edit habit ${h.name}",
                        role = Role.Button,
                        onClick = onEdit,
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 15.dp),
            ) {
                // ── FR-HAB-1 details: target chip + serves link ──
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(accent.a(0.1f))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            if (isQuant) "${fmt(h.target)} ${h.unit ?: ""}/day" else "${h.target.toInt()}× / week",
                            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                            color = accent,
                        )
                    }
                    if (servesGoal != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            TzIcons.Target(12.dp, Tz.colors.rust)
                            Text(
                                "serves: $servesGoal",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                                color = Tz.colors.muted,
                            )
                        }
                    }
                }

                // ── FR-HAB-5.4: cue immediately below the header once open ──
                if (h.cue != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Tz.colors.amber.a(0.1f))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TzIcons.Cue(14.dp, Tz.colors.rust)
                        Text(
                            buildAnnotatedString {
                                append("When ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(h.cue.label.removePrefix("After ").removePrefix("after "))
                                }
                            },
                            style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp).contentDir(),
                            color = Tz.colors.ink,
                        )
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Tz.colors.line2, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TzIcons.Cue(14.dp, Tz.colors.faint)
                        Text(
                            "No cue yet — without one it's just a tracker",
                            style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp),
                            color = Tz.colors.faint,
                        )
                    }
                }

                // ── this week: forgiving rate ──
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "THIS WEEK",
                            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.5.sp),
                            color = Tz.colors.faint,
                        )
                        Text(
                            buildAnnotatedString {
                                if (isQuant) {
                                    append("${fmt(card.week.amountSum)} ${h.unit ?: ""}")
                                    withStyle(SpanStyle(fontSize = 13.sp, color = Tz.colors.muted, fontWeight = FontWeight.Normal)) {
                                        append(" · ${card.week.doneDays} of ${h.targetDays ?: 7} days")
                                    }
                                } else {
                                    append("${card.week.doneDays} of ${h.target.toInt()}")
                                    withStyle(SpanStyle(fontSize = 13.sp, color = Tz.colors.muted, fontWeight = FontWeight.Normal)) {
                                        append(" done")
                                    }
                                }
                            },
                            style = TextStyle(fontFamily = DenType.serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                            color = Tz.colors.ink,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (!isQuant) {
                        WeekDots(done = card.week.doneDays, total = h.target.toInt(), accent = accent)
                    }
                }
                if (isQuant) {
                    QuantBars(amounts = card.week.amounts, target = h.target, accent = accent)
                }

                // ── log today (full-width, the same affordance as collapsed) ──
                // FR-HAB-9.1 — the expanded today pill re-labels on loggedToday for
                // both kinds ("Mark today done" ⇄ "Un-mark today"); a logged
                // quantitative habit shows its amount in the caption below.
                val pillLabel = if (card.loggedToday) "Un-mark today" else "Mark today done"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent.a(if (card.loggedToday) 0.22f else 0.1f))
                        .border(1.dp, accent.a(0.5f), RoundedCornerShape(11.dp))
                        .semantics { contentDescription = "$pillLabel ${h.name}" }
                        .pressable(label = "$pillLabel ${h.name}", role = Role.Button, onClick = onLog),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TzIcons.Check(16.dp, accent, 2.6f)
                    Text(
                        if (isQuant && card.loggedToday) {
                            "Un-mark today · ${fmt(card.todayAmount ?: 0.0)} ${h.unit ?: ""}"
                        } else {
                            pillLabel
                        },
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = accent,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                // ── FR-HAB-8 — Log a chosen date (expanded card only) ──
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(1.dp, accent.a(0.4f), RoundedCornerShape(11.dp))
                        .semantics { contentDescription = "Log a chosen date" }
                        .pressable(label = "Log a chosen date", role = Role.Button, onClick = onLogDate),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TzIcons.Calendar(14.dp, accent)
                    Text(
                        "Log a date…",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = accent,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                // ── fresh start after a lapse (DM-HABIT-4) ──
                if (card.freshStart) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Tz.colors.surface)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TzIcons.Sprout(16.dp, Tz.colors.green)
                        Text(
                            buildAnnotatedString {
                                append("Missed a few last week? Normal. ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Tz.colors.ink)) {
                                    append("New week, clean slate.")
                                }
                            },
                            style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 17.5.sp),
                            color = Tz.colors.muted,
                        )
                    }
                }

                // ── 5-week history + long arc ──
                Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HistoryGrid(weeks = card.grid, accent = accent)
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TzIcons.Sprout(15.dp, Tz.colors.green)
                                Text(
                                    card.arc,
                                    style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp),
                                    color = Tz.colors.muted,
                                )
                            }
                            Text(
                                "LAST 5 WEEKS",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.sp, letterSpacing = 0.3.sp),
                                color = Tz.colors.faint,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * FR-HAB-7.1 / FR-HAB-7.2 — the compact edit affordance for the collapsed row.
 * A single-purpose icon button beside the log pill so edit no longer requires
 * an expand tap first (the discoverability regression `FR-HAB-5`'s collapse
 * introduced). Owns its own hit target so the log control never doubles as
 * the edit route (`FR-HAB-7.2`).
 */
@Composable
private fun CollapsedEditPill(habitName: String, onEdit: () -> Unit) {
    val cd = "Edit habit $habitName"
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Tz.colors.ink.a(0.05f))
            .border(1.dp, Tz.colors.line, RoundedCornerShape(999.dp))
            .semantics { contentDescription = cd }
            .pressable(
                label = cd,
                role = Role.Button,
                onClick = onEdit,
            ),
        contentAlignment = Alignment.Center,
    ) {
        TzIcons.Pencil(15.dp, Tz.colors.muted)
    }
}

/**
 * FR-HAB-5.1 — the compact one-tap log affordance for the collapsed row.
 * Behaviour matches the full-width log-today pill inside the expanded card:
 * frequency habits toggle done/undo on tap; quantitative habits open the
 * "how much?" prompt (owner: parent, via [onLog]).
 */
@Composable
private fun CollapsedLogPill(
    habitName: String,
    loggedToday: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onLog: () -> Unit,
) {
    // FR-HAB-9.1 — a single re-labelling pill (per the DECISION): logged today ⇒
    // "Un-mark today", else "Mark today done". Both kinds behave the same here;
    // the quantitative un-log clears directly (owner: parent [onLog]). TalkBack
    // reads the full label + habit name; the compact pill shows a short token.
    val label = if (loggedToday) "Un-mark today" else "Mark today done"
    Row(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(accent.a(if (loggedToday) 0.24f else 0.12f))
            .border(1.dp, accent.a(0.5f), RoundedCornerShape(999.dp))
            .pressable(
                label = "$label $habitName",
                role = Role.Button,
                onClick = onLog,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TzIcons.Check(13.dp, accent, 2.6f)
        Text(
            if (loggedToday) "Un-mark" else "Mark",
            style = TextStyle(
                fontFamily = DenType.mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = accent,
        )
    }
}

/** Week dots for a frequency habit — target circles, done ones filled. */
@Composable
private fun WeekDots(done: Int, total: Int, accent: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(total.coerceIn(1, 7)) { i ->
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (i < done) accent else Tz.colors.ink.a(0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                if (i < done) TzIcons.Check(13.dp, androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

/** Seven daily bars for a quantitative habit, week-start order. */
@Composable
private fun QuantBars(amounts: List<Double>, target: Double, accent: androidx.compose.ui.graphics.Color) {
    val scale = max(amounts.maxOrNull() ?: 0.0, target) * 1.15
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val days = listOf("S", "M", "T", "W", "T", "F", "S")
        amounts.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(max(3.0, v / scale * 30.0).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (v > 0) accent else Tz.colors.ink.a(0.1f)),
                )
                Text(
                    days[i % 7],
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 8.sp),
                    color = Tz.colors.faint,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** 5 columns (weeks, oldest → current) × 7 day dots. */
@Composable
private fun HistoryGrid(weeks: List<List<Int>>, accent: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        weeks.forEach { wk ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                wk.forEach { v ->
                    Box(
                        Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(
                            when (v) {
                                2 -> accent
                                1 -> accent.a(0.4f)
                                else -> Tz.colors.ink.a(0.08f)
                            },
                        ),
                    )
                }
            }
        }
    }
}

private fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else v.toString()
