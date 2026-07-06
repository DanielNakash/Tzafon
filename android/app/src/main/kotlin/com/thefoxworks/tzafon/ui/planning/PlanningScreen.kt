package com.thefoxworks.tzafon.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.action.ActionLogic
import com.thefoxworks.tzafon.domain.action.ActionLogic.RangePreset
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.ui.capture.QuickAddSheet
import com.thefoxworks.tzafon.ui.components.CalendarPicker
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.FoxLogo
import com.thefoxworks.tzafon.ui.components.GroupHeader
import com.thefoxworks.tzafon.ui.components.PillButton
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.StateSheet
import com.thefoxworks.tzafon.ui.components.TaskCheckbox
import com.thefoxworks.tzafon.ui.components.TaskRow
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * Planning (FR-PLAN) — what's on the table: overdue decide-cards at top
 * (FR-PLAN-3, never a guilt wall), dated groups to the chosen range, and
 * the Inbox of undated captures. The range drives the horizon (FR-REC-3).
 */
@Composable
fun PlanningScreen(
    vm: PlanningViewModel,
    onOpenTask: (String) -> Unit,
    onExpandAdd: (String) -> Unit,
    onOpenAllTasks: () -> Unit,
    onOpenBacklog: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var quickAdd by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var rescheduleId by remember { mutableStateOf<String?>(null) }
    var customRange by remember { mutableStateOf(false) }
    // FR-BACKLOG-2 — Someday on a recurring task opens the guard (offers Frozen)
    var somedayGuardTask by remember { mutableStateOf<Task?>(null) }

    Box(Modifier.fillMaxSize().background(Den.bg)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "Planning",
                kicker = "WHAT'S ON THE TABLE",
                compact = true,
                metaLeft = Dates.fmtDate(state.today, state.today),
                metaRight = "${state.undatedCount} undated",
                right = { Box(Modifier.pressable { menu = true }) { FoxLogo(30.dp, ring = Den.cream.a(0.9f)) } },
                bottomContent = {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        RangePreset.entries.forEach { p ->
                            val on = p == state.preset
                            val label = when {
                                p == RangePreset.CUSTOM && on && state.customEnd != null ->
                                    "→ ${Dates.fmtDate(state.customEnd!!, state.today)}"
                                p == RangePreset.CUSTOM -> "Custom…"
                                else -> p.label
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (on) Color.White.a(0.2f) else Color.Transparent)
                                    .border(1.dp, if (on) Den.cream else Color.White.a(0.4f), RoundedCornerShape(999.dp))
                                    .pressable {
                                        if (p == RangePreset.CUSTOM) customRange = true else vm.setRange(p)
                                    }
                                    .padding(horizontal = 11.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    label,
                                    style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 0.3.sp),
                                    color = if (on) Color.White else Color.White.a(0.75f),
                                )
                            }
                        }
                    }
                },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 200.dp),
            ) {
                // ── Overdue — decide what to do with these (FR-PLAN-3) ──
                if (state.overdue.isNotEmpty()) {
                    item(key = "h_overdue") {
                        GroupHeader(
                            label = "Overdue",
                            count = state.overdue.size,
                            accent = Den.due,
                            right = {
                                Text(
                                    "DECIDE →",
                                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                                    color = Den.due,
                                )
                            },
                        )
                    }
                    item(key = "overdue_card") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(Den.due.a(0.05f))
                                .border(1.dp, Den.due.a(0.2f), RoundedCornerShape(13.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            state.overdue.forEachIndexed { i, t ->
                                DecideCard(
                                    task = t,
                                    today = state.today,
                                    last = i == state.overdue.lastIndex,
                                    onOpen = { onOpenTask(t.id) },
                                    onToday = { vm.doToday(t.id) },
                                    onReschedule = { rescheduleId = t.id },
                                    onSomeday = {
                                        if (t.seriesId == null) vm.someday(t.id)
                                        else somedayGuardTask = t
                                    },
                                    onDrop = { vm.drop(t.id) },
                                )
                            }
                        }
                    }
                }

                // ── dated groups today..range ──
                state.dated.forEach { (group, tasks) ->
                    item(key = "h_${group.key}") {
                        GroupHeader(
                            label = group.label,
                            count = tasks.size,
                            accent = if (group.key == "today") Den.rust else Den.muted,
                        )
                    }
                    items(tasks, key = { it.id }) { t ->
                        TaskRow(
                            task = t,
                            today = state.today,
                            onToggle = { vm.toggleDone(t.id) },
                            onOpen = { onOpenTask(t.id) },
                            last = t.id == tasks.last().id,
                        )
                    }
                }

                // ── Inbox — needs a date (FR-PLAN-1) ──
                if (state.inbox.isNotEmpty()) {
                    item(key = "h_inbox") {
                        GroupHeader(label = "Inbox — needs a date", count = state.inbox.size, accent = Den.muted)
                    }
                    items(state.inbox, key = { it.id }) { t ->
                        TaskRow(
                            task = t,
                            today = state.today,
                            onToggle = { vm.toggleDone(t.id) },
                            onOpen = { onOpenTask(t.id) },
                            last = t.id == state.inbox.last().id,
                        )
                    }
                }

                if (state.overdue.isEmpty() && state.dated.isEmpty() && state.inbox.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 90.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "The table is clear",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp),
                                color = Den.ink,
                            )
                            Text(
                                "Captured things land here to be given a day.",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                                color = Den.muted,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)) {
            Fab(onClick = { quickAdd = true })
        }
    }

    if (quickAdd) {
        QuickAddSheet(
            onSave = { vm.quickAdd(it) },
            onExpand = { title -> quickAdd = false; onExpandAdd(title) },
            onClose = { quickAdd = false },
        )
    }
    if (menu) {
        AppMenuSheet(
            onClose = { menu = false },
            onAllTasks = onOpenAllTasks,
            onBacklog = onOpenBacklog,
            onSettings = onOpenSettings,
        )
    }
    rescheduleId?.let { id ->
        DenSheet(title = "Move it to", onClose = { rescheduleId = null }) {
            CalendarPicker(
                value = null,
                today = state.today,
                onPick = { d -> vm.rescheduleTo(id, d); rescheduleId = null },
            )
        }
    }
    if (customRange) {
        DenSheet(title = "Plan through", onClose = { customRange = false }) {
            CalendarPicker(
                value = state.customEnd,
                today = state.today,
                onPick = { d -> vm.setRange(RangePreset.CUSTOM, d); customRange = false },
            )
        }
    }
    somedayGuardTask?.let { t ->
        StateSheet(
            current = t.state,
            isRecurring = true,
            onPick = { target -> vm.toState(t.id, target) },
            onClose = { somedayGuardTask = null },
            initialGuard = StateMachine.Guard.RecurringCannotBacklog,
        )
    }
}

/** An overdue row presented as a calm decision, not a debt (FR-PLAN-3). */
@Composable
private fun DecideCard(
    task: Task,
    today: String,
    last: Boolean,
    onOpen: () -> Unit,
    onToday: () -> Unit,
    onReschedule: () -> Unit,
    onSomeday: (() -> Unit)?,
    onDrop: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Row(
            Modifier.fillMaxWidth().pressable(onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            TaskCheckbox(task.state)
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, lineHeight = 19.5.sp),
                    color = Den.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "SLIPPED ${ActionLogic.slippedDays(task, today)}D AGO",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                    color = Den.due,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp, start = 35.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PillButton("Today", onClick = onToday)
            PillButton("Reschedule", onClick = onReschedule)
            PillButton("Someday", onClick = { onSomeday?.invoke() }, enabled = onSomeday != null)
            PillButton("Drop", onClick = onDrop)
        }
        if (!last) {
            Box(
                Modifier.fillMaxWidth().padding(top = 11.dp)
                    .size(width = 0.dp, height = 1.dp)
                    .fillMaxWidth()
                    .background(Den.line2)
            )
        }
    }
}
