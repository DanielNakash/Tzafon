package com.thefoxworks.tzafon.ui.alltasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.GroupHeader
import com.thefoxworks.tzafon.ui.components.HeaderMenuButton
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.StateSheet
import com.thefoxworks.tzafon.ui.components.TaskRow
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One lazy-list slot; keeping the list flat makes jump-to indices exact. */
private sealed interface Entry {
    val key: String

    data object JumpBar : Entry {
        override val key = "jumpbar"
    }

    /** repDate = the date this group stands for (null for "No date"). */
    data class Header(val groupKey: String, val label: String, val count: Int, val repDate: String?) : Entry {
        override val key = "h_$groupKey"
    }

    data class Item(val task: Task, val last: Boolean) : Entry {
        override val key = task.id
    }

    data object Horizon : Entry {
        override val key = "horizon"
    }

    data object EmptyState : Entry {
        override val key = "empty"
    }
}

/**
 * All Tasks (FR-ALL) — the complete searchable index. M2 completes it:
 * search (FR-ALL-2), the effective-horizon divider (FR-ALL-4), and the
 * in-view navigation set (FR-ALL-5): sticky collapsible group headers,
 * a compact jump-to row, and the edge date-scrubber.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllTasksScreen(
    vm: AllTasksViewModel,
    onOpenTask: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenBacklog: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenAbout: (() -> Unit)? = null,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var sheetTask by remember { mutableStateOf<Task?>(null) }
    var menu by remember { mutableStateOf(false) }
    var amountTask by remember { mutableStateOf<Task?>(null) } // DM-HABIT-5 prompt
    var query by rememberSaveable { mutableStateOf("") }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val searching = query.isNotBlank()

    fun repDate(groupKey: String): String? = when (groupKey) {
        "overdue" -> null // jump chip targets it directly
        "today" -> state.today
        "tomorrow" -> Dates.addDays(state.today, 1)
        else -> groupKey
    }

    // ── flat entry list (indices feed jump-to + scrubber) ──
    val entries = buildList {
        if (!searching) add(Entry.JumpBar)
        state.groups.forEachIndexed { gi, group ->
            if (!searching && gi == state.horizonIndex && state.groups.isNotEmpty()) add(Entry.Horizon)
            add(Entry.Header(group.key, group.label, group.items.size, repDate(group.key)))
            if (collapsed[group.key] != true) {
                group.items.forEachIndexed { i, t -> add(Entry.Item(t, i == group.items.lastIndex)) }
            }
        }
        if (!searching && state.horizonIndex == state.groups.size && state.groups.isNotEmpty()) add(Entry.Horizon)
        if (state.undated.isNotEmpty()) {
            add(Entry.Header("nodate", "No date", state.undated.size, null))
            if (collapsed["nodate"] != true) {
                state.undated.forEachIndexed { i, t -> add(Entry.Item(t, i == state.undated.lastIndex)) }
            }
        }
        if (state.empty && !searching) add(Entry.EmptyState)
    }

    fun jumpTo(predicate: (Entry) -> Boolean) {
        val i = entries.indexOfFirst(predicate)
        if (i >= 0) scope.launch { listState.animateScrollToItem(i) }
    }

    // months present among the dated groups, in order (for scrubber + jump-to)
    val months = remember(state.groups, state.today) {
        state.groups.mapNotNull { g ->
            when (g.key) {
                "overdue" -> null
                "today" -> state.today
                "tomorrow" -> Dates.addDays(state.today, 1)
                else -> g.key
            }
        }.map { it.substring(0, 7) }.distinct() // yyyy-MM
    }

    Box(Modifier.fillMaxSize().background(Den.bg)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "All Tasks",
                kicker = "COMPLETE INDEX",
                compact = true,
                right = { HeaderMenuButton(onClick = { menu = true }) },
                bottomContent = {
                    Column {
                        // ── search (FR-ALL-2) ──
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White.a(0.16f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            TzIcons.Search(16.dp, Color.White.a(0.8f))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it; vm.setQuery(it) },
                                textStyle = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, color = Color.White),
                                cursorBrush = SolidColor(Den.cream),
                                singleLine = true,
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) {
                                            Text(
                                                "Search title or description…",
                                                style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                                                color = Color.White.a(0.72f),
                                            )
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (query.isNotEmpty()) {
                                Box(Modifier.pressable { query = ""; vm.setQuery("") }.padding(2.dp)) {
                                    TzIcons.X(13.dp, Color.White.a(0.8f))
                                }
                            }
                        }
                        // ── SHOW toggles (FR-ALL-3) ──
                        Row(
                            Modifier.padding(top = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "SHOW:",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                                color = Color.White.a(0.6f),
                            )
                            StateToggle("DONE", state.visible.done) { vm.toggle(TaskState.DONE) }
                            StateToggle("FROZEN", state.visible.frozen) { vm.toggle(TaskState.FROZEN) }
                            StateToggle("CLOSED", state.visible.closed) { vm.toggle(TaskState.CLOSED) }
                            StateToggle("BACKLOG", state.visible.backlog) { vm.toggle(TaskState.BACKLOG) }
                        }
                    }
                },
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                ) {
                    entries.forEach { e ->
                        when (e) {
                            is Entry.Header -> stickyHeader(key = e.key) {
                                val isCollapsed = collapsed[e.groupKey] == true
                                Box(Modifier.background(Den.bg)) {
                                    GroupHeader(
                                        label = e.label,
                                        count = e.count,
                                        accent = when (e.groupKey) {
                                            "overdue" -> Den.due
                                            "today" -> Den.rust
                                            else -> Den.muted
                                        },
                                        modifier = Modifier.pressable {
                                            collapsed[e.groupKey] = !isCollapsed
                                        },
                                        right = {
                                            TzIcons.Chevron(
                                                14.dp, Den.faint,
                                                modifier = Modifier.rotate(if (isCollapsed) 90f else -90f),
                                            )
                                        },
                                    )
                                }
                            }

                            is Entry.Item -> item(key = e.key) {
                                TaskLine(
                                    e.task, state.today, vm, onOpenTask,
                                    onSheet = { sheetTask = it },
                                    onAmount = { amountTask = it },
                                    habitLabel = state.habitsById[e.task.habitId]?.name,
                                    needsPrompt = com.thefoxworks.tzafon.domain.attribution.Attribution.needsAmountPrompt(
                                        e.task, state.habitsById[e.task.habitId], state.goalsById,
                                    ),
                                    last = e.last,
                                    summaries = state.ruleSummaries,
                                )
                            }

                            Entry.JumpBar -> item(key = e.key) {
                                JumpBar(
                                    hasOverdue = state.groups.any { it.key == "overdue" },
                                    hasUndated = state.undated.isNotEmpty(),
                                    months = months,
                                    onOverdue = { jumpTo { it is Entry.Header && it.groupKey == "overdue" } },
                                    onToday = {
                                        jumpTo { it is Entry.Header && (it.repDate ?: "") >= state.today }
                                    },
                                    onUndated = { jumpTo { it is Entry.Header && it.groupKey == "nodate" } },
                                    onMonth = { m -> jumpTo { it is Entry.Header && it.repDate?.startsWith(m) == true } },
                                )
                            }

                            Entry.Horizon -> item(key = e.key) { HorizonMarker(state.horizonDate, state.today) }

                            Entry.EmptyState -> item(key = e.key) { EmptyDen() }
                        }
                    }

                    if (searching && entries.isEmpty()) {
                        item(key = "noresults") {
                            Text(
                                "Nothing matches — try fewer words.",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                                color = Den.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            )
                        }
                    }
                }

                // ── edge date-scrubber (FR-ALL-5) ──
                if (!searching && months.size >= 2) {
                    DateScrubber(
                        months = months,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(top = 8.dp, bottom = 96.dp, end = 3.dp),
                        onMonth = { m -> jumpTo { it is Entry.Header && it.repDate?.startsWith(m) == true } },
                    )
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 24.dp)) {
            Fab(onClick = onAdd)
        }
    }

    sheetTask?.let { task ->
        StateSheet(
            current = task.state,
            isRecurring = task.seriesId != null,
            onPick = { target -> vm.setState(task.id, target) },
            onClose = { sheetTask = null },
        )
    }
    if (menu) {
        AppMenuSheet(
            onClose = { menu = false },
            onAllTasks = { }, // already here
            onBacklog = onOpenBacklog,
            onSettings = onOpenSettings,
            onAbout = onOpenAbout,
        )
    }
    amountTask?.let { t ->
        val habit = state.habitsById[t.habitId]
        com.thefoxworks.tzafon.ui.components.AmountSheet(
            title = "How much? · ${habit?.name ?: t.title}",
            unit = com.thefoxworks.tzafon.domain.attribution.Attribution.promptUnit(t, habit, state.goalsById),
            suggested = habit?.target,
            onConfirm = { vm.toggleDone(t.id, habitAmount = it) },
            onClose = { amountTask = null },
        )
    }
}

@Composable
private fun TaskLine(
    task: Task,
    today: String,
    vm: AllTasksViewModel,
    onOpenTask: (String) -> Unit,
    onSheet: (Task) -> Unit,
    onAmount: (Task) -> Unit,
    habitLabel: String?,
    needsPrompt: Boolean,
    last: Boolean,
    summaries: Map<String, String>,
) {
    val settled = task.state == TaskState.CLOSED || task.state == TaskState.FROZEN || task.state == TaskState.BACKLOG
    TaskRow(
        task = task,
        today = today,
        onToggle = {
            when {
                settled -> onSheet(task)
                task.state != TaskState.DONE && needsPrompt -> onAmount(task) // DM-HABIT-5 / DM-GOAL-3
                else -> vm.toggleDone(task.id)
            }
        },
        onOpen = { onOpenTask(task.id) },
        onLongPress = { onSheet(task) },
        recurSummary = task.seriesId?.let { summaries[it] },
        habitLabel = habitLabel,
        last = last,
    )
}

/** FR-ALL-5 — compact jump-to: Overdue · Today · Undated + month picker. */
@Composable
private fun JumpBar(
    hasOverdue: Boolean,
    hasUndated: Boolean,
    months: List<String>,
    onOverdue: () -> Unit,
    onToday: () -> Unit,
    onUndated: () -> Unit,
    onMonth: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "JUMP TO",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.4.sp),
            color = Den.faint,
            modifier = Modifier.padding(end = 2.dp),
        )
        if (hasOverdue) JumpChip("OVERDUE", Den.due, onOverdue)
        JumpChip("TODAY", Den.rust, onToday)
        if (hasUndated) JumpChip("UNDATED", Den.muted, onUndated)
        months.forEach { m ->
            val mo = Dates.MO[m.substring(5).toInt() - 1].uppercase()
            JumpChip(mo, Den.muted) { onMonth(m) }
        }
    }
}

@Composable
private fun JumpChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(999.dp))
            .pressable(onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = color,
        )
    }
}

/** FR-ALL-4 — the quiet full-width divider at the effective horizon date. */
@Composable
private fun HorizonMarker(horizonDate: String, today: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Den.line))
        Text(
            "SCHEDULED THROUGH ${Dates.fmtDate(horizonDate, today).uppercase()}\nRECURRING TASKS REPEAT BEYOND THIS",
            style = TextStyle(
                fontFamily = DenType.mono,
                fontSize = 9.5.sp,
                letterSpacing = 0.4.sp,
                lineHeight = 13.5.sp,
            ),
            color = Den.faint,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.weight(1f).height(1.dp).background(Den.line))
    }
}

/** FR-ALL-5 — the right-edge month scrubber (design: AllTasksScreen). */
@Composable
private fun DateScrubber(
    months: List<String>,
    modifier: Modifier = Modifier,
    onMonth: (String) -> Unit,
) {
    // month labels with two decorative dots between each pair, as designed
    val slots = buildList {
        months.forEachIndexed { i, m ->
            add(m)
            if (i != months.lastIndex) {
                add(null); add(null)
            }
        }
    }
    Column(
        modifier
            .width(16.dp)
            .pointerInput(months) {
                fun pick(y: Float) {
                    val i = (y / size.height * (months.size - 1))
                        .roundToInt()
                        .coerceIn(0, months.lastIndex)
                    onMonth(months[i])
                }
                detectVerticalDragGestures(
                    onDragStart = { pick(it.y) },
                    onVerticalDrag = { change, _ -> change.consume(); pick(change.position.y) },
                )
            }
            .pointerInput(months) {
                detectTapGestures { offset ->
                    val i = (offset.y / size.height * (months.size - 1))
                        .roundToInt()
                        .coerceIn(0, months.lastIndex)
                    onMonth(months[i])
                }
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        slots.forEach { m ->
            if (m == null) {
                Box(Modifier.size(3.dp).clip(CircleShape).background(Den.ink.a(0.25f)))
            } else {
                Text(
                    Dates.MO[m.substring(5).toInt() - 1].uppercase(),
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = Den.muted,
                )
            }
        }
    }
}

@Composable
private fun EmptyDen() {
    Column(
        Modifier.fillMaxWidth().padding(top = 54.dp, start = 30.dp, end = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🦊", fontSize = 46.sp, modifier = Modifier.rotate(-6f))
        Text(
            "All clear.",
            style = TextStyle(fontFamily = DenType.serif, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            color = Den.ink,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "Nothing left on the list. Go read a book.",
            style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp),
            color = Den.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Rust-header SHOW pill (design: AllTasksScreen Toggle). */
@Composable
private fun StateToggle(label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) Color.White.a(0.16f) else Color.Transparent)
            .border(1.dp, if (on) Den.cream else Color.White.a(0.35f), RoundedCornerShape(999.dp))
            .pressable(onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (on) Den.cream else Color.White.a(0.5f)))
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.3.sp),
            color = if (on) Den.cream else Color.White.a(0.6f),
        )
    }
}
