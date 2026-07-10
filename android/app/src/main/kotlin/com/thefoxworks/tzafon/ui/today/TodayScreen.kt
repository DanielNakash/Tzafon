package com.thefoxworks.tzafon.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.ui.capture.QuickAddSheet
import com.thefoxworks.tzafon.ui.components.Banner
import com.thefoxworks.tzafon.ui.components.Compass
import com.thefoxworks.tzafon.ui.components.CueChip
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.HeaderMenuButton
import com.thefoxworks.tzafon.ui.components.GroupHeader
import com.thefoxworks.tzafon.ui.components.Nudge
import com.thefoxworks.tzafon.ui.components.PillButton
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.TaskCheckbox
import com.thefoxworks.tzafon.ui.components.TaskRow
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir
import kotlin.math.roundToInt

/**
 * Today (FR-TODAY) — the default landing view: focus card, "Also today"
 * with long-press drag reorder, calm slippage banner, supportive overload
 * nudge, and the collapsed done strip. Overdue never appears here (DEC-7).
 */
@Composable
fun TodayScreen(
    vm: TodayViewModel,
    onOpenTask: (String) -> Unit,
    onExpandAdd: (String) -> Unit,
    onOpenPlanning: () -> Unit,
    onOpenAllTasks: () -> Unit,
    onOpenBacklog: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenReview: (() -> Unit)? = null,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var doneOpen by remember { mutableStateOf(false) }
    var alsoCollapsed by remember { mutableStateOf(false) }
    var quickAdd by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var amountTask by remember { mutableStateOf<Task?>(null) } // DM-HABIT-5 prompt

    // quantitative habits and direct accumulative goals ask "how much?" first
    fun requestToggle(t: Task) {
        val habit = state.habitsById[t.habitId]
        if (t.state != com.thefoxworks.tzafon.domain.model.TaskState.DONE &&
            com.thefoxworks.tzafon.domain.attribution.Attribution.needsAmountPrompt(t, habit, state.goalsById)
        ) {
            amountTask = t
        } else {
            vm.toggleDone(t.id)
        }
    }

    val d = Dates.parse(state.today)
    val kicker = "${Dates.WD_FULL[Dates.dayOfWeek(d)]} · ${Dates.MO[d.monthValue - 1]} ${d.dayOfMonth}"
    val pct = if (state.totalCount > 0) state.doneCount * 100f / state.totalCount else 0f

    Box(Modifier.fillMaxSize().background(Den.bg)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "Today",
                kicker = kicker,
                compass = true,
                metaLeft = "Your north for the day",
                metaRight = "${state.doneCount} / ${state.totalCount} done",
                progress = pct,
                right = { HeaderMenuButton(onClick = { menu = true }) },
            )

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 200.dp),
            ) {
                // ── the review invite (DM-REVIEW-4 — an invitation, never a block) ──
                if (state.reviewInvite != null && onOpenReview != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Den.surface)
                            .border(1.dp, Den.rust.a(0.3f), RoundedCornerShape(13.dp))
                            .pressable(onOpenReview)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Compass(size = 22.dp, ring = Den.rust, needleN = Den.rust, needleS = Den.faint, stroke = 2f)
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.reviewInvite!!,
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.5.sp),
                                color = Den.rust,
                            )
                            Text(
                                "Your week, reflected — then a fresh start.",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                                color = Den.ink,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        TzIcons.Chevron(17.dp, Den.rust)
                    }
                }

                if (state.showSlippage) {
                    Banner(
                        text = buildAnnotatedString {
                            append("${state.slippedCount} task${if (state.slippedCount == 1) "" else "s"} slipped past — tidy them up in ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Planning") }
                            append(".")
                        },
                        onClose = { vm.dismissSlippage() },
                        onClick = onOpenPlanning,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                if (state.showOverload) {
                    Nudge(
                        text = "That's a full plate today. Want to move a couple to tomorrow? No rush — the list serves you, not the other way round.",
                        modifier = Modifier.padding(top = 12.dp),
                        actions = {
                            PillButton("REVIEW IN PLANNING", onClick = onOpenPlanning, color = Den.rust)
                            PillButton("IT'S FINE", onClick = { vm.dismissOverload() })
                        },
                    )
                }

                // ── Today's focus (DM-FOCUS markers; set at Review from M7) ──
                if (state.focus.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Compass(size = 17.dp, ring = Den.rust, needleN = Den.rust, needleS = Den.faint, stroke = 2f)
                        SectionLabel("Today's focus · ${state.focus.size}", color = Den.rust)
                        Box(Modifier.weight(1f).height(1.dp).background(Den.line))
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Den.surface)
                            .border(1.dp, Den.rust.a(0.18f), RoundedCornerShape(15.dp))
                            .padding(horizontal = 15.dp, vertical = 2.dp),
                    ) {
                        state.focus.forEachIndexed { i, t ->
                            FocusItem(
                                t = t,
                                last = i == state.focus.lastIndex,
                                onToggle = { requestToggle(t) },
                                onOpen = { onOpenTask(t.id) },
                                onUnfocus = { vm.toggleFocus(t) },
                            )
                        }
                    }
                    // DM-FOCUS-2 — the soft cap, nudged right where it's set
                    if (state.focusOverCap) {
                        Nudge(
                            text = "That's more than three pointed north. All fine — but a shorter list pulls harder.",
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }

                // ── Also today — the reorderable main list ──
                if (state.alsoToday.isNotEmpty()) {
                    GroupHeader(
                        label = if (state.focus.isEmpty()) "Today" else "Also today",
                        count = state.alsoToday.size,
                        right = {
                            Row(
                                Modifier.pressable { alsoCollapsed = !alsoCollapsed },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    if (alsoCollapsed) "SHOW" else "COLLAPSE",
                                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.4.sp),
                                    color = Den.faint,
                                )
                                TzIcons.Chevron(14.dp, Den.faint, modifier = Modifier.rotate(if (alsoCollapsed) 90f else -90f))
                            }
                        },
                    )
                    if (!alsoCollapsed) {
                        ReorderableTaskList(
                            tasks = state.alsoToday,
                            today = state.today,
                            onToggle = { id -> state.alsoToday.firstOrNull { it.id == id }?.let(::requestToggle) },
                            onOpen = onOpenTask,
                            onPersist = { vm.persistOrder(it) },
                            habitLabel = { t -> state.habitsById[t.habitId]?.name },
                            onFocusToggle = { t -> vm.toggleFocus(t) },
                        )
                    }
                }

                // ── this week's priorities (the strip set at Review) ──
                if (state.weekPriorities.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionLabel("This week's priorities · ${state.weekPriorities.size}")
                        Box(Modifier.weight(1f).height(1.dp).background(Den.line2))
                    }
                    state.weekPriorities.forEachIndexed { i, t ->
                        TaskRow(
                            task = t,
                            today = state.today,
                            onToggle = { requestToggle(t) },
                            onOpen = { onOpenTask(t.id) },
                            habitLabel = state.habitsById[t.habitId]?.name,
                            last = i == state.weekPriorities.lastIndex,
                        )
                    }
                }

                // ── empty day ──
                if (state.focus.isEmpty() && state.alsoToday.isEmpty() && state.doneToday.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 90.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Compass(size = 44.dp, ring = Den.faint, needleN = Den.rust, needleS = Den.faint, stroke = 1.6f)
                        Text(
                            "A clear day",
                            style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
                            color = Den.ink,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Text(
                            "Nothing on the plate. Add one small thing —\nor enjoy the quiet. Both count.",
                            style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.sp),
                            color = Den.muted,
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }

                // ── done strip ──
                if (state.doneToday.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().pressable { doneOpen = !doneOpen }.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TzIcons.Check(14.dp, Den.green, 2.6f)
                        Text(
                            "${state.doneCount} DONE TODAY",
                            style = TextStyle(fontFamily = DenType.mono, fontSize = 11.5.sp, letterSpacing = 0.4.sp),
                            color = Den.faint,
                        )
                        Box(Modifier.weight(1f).height(1.dp).background(Den.line2))
                        TzIcons.Chevron(16.dp, Den.faint, modifier = Modifier.rotate(if (doneOpen) -90f else 90f))
                    }
                    if (doneOpen) {
                        state.doneToday.forEachIndexed { i, t ->
                            TaskRow(
                                task = t,
                                today = state.today,
                                onToggle = { vm.toggleDone(t.id) },
                                onOpen = { onOpenTask(t.id) },
                                last = i == state.doneToday.lastIndex,
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
            datedByDefault = true, // FR-TODAY-7: Today's quick-add pre-fills toDoDate = today.
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

/** Focus row (design FocusItem): bold title, serves/cue meta, no chevron. */
@Composable
private fun FocusItem(
    t: Task,
    last: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onUnfocus: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().pressable(onOpen)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaskCheckbox(t.state, onClick = onToggle)
            Column(Modifier.weight(1f)) {
                Text(
                    t.title,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.5.sp).contentDir(),
                    color = Den.ink,
                )
                if (t.cue != null) {
                    Row(Modifier.padding(top = 6.dp)) { CueChip(t.cue.label) }
                }
            }
            if (onUnfocus != null) {
                Box(Modifier.pressable(onUnfocus).padding(top = 2.dp)) {
                    Compass(size = 20.dp, ring = Den.rust, needleN = Den.rust, needleS = Den.rust.a(0.5f), stroke = 2f)
                }
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Den.line))
    }
}

/**
 * FR-TODAY-2 — long-press-drag manual reorder. Simple neighbour-swap
 * algorithm over a plain Column (Today lists are short by design).
 * Internal so the Compose UI test can drive the gesture directly.
 */
@Composable
internal fun ReorderableTaskList(
    tasks: List<Task>,
    today: String,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    onPersist: (List<String>) -> Unit,
    habitLabel: (Task) -> String? = { null },
    onFocusToggle: ((Task) -> Unit)? = null,
) {
    val ids = tasks.map { it.id }
    var localOrder by remember { mutableStateOf<List<String>?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }

    // follow the VM order unless a local drag result covers the same ids
    val order = localOrder?.takeIf { it.toSet() == ids.toSet() } ?: ids
    val byId = tasks.associateBy { it.id }
    val displayed = order.mapNotNull { byId[it] }
    // read inside drag callbacks without restarting the gesture (stable key below)
    val currentOrder by rememberUpdatedState(order)

    Column {
        displayed.forEachIndexed { index, task ->
            val isDragged = task.id == draggingId
            Box(
                Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragged) 1f else 0f)
                    .let {
                        if (isDragged) it
                            .offset { IntOffset(0, dragDelta.roundToInt()) }
                            .shadow(6.dp, RoundedCornerShape(10.dp))
                            .background(Den.surface)
                        else it
                    }
                    .onSizeChanged { heights[task.id] = it.height }
                    // keyed on the stable id — keying on the order list would
                    // restart this coroutine at the first swap and kill the drag
                    .pointerInput(task.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                localOrder = currentOrder
                                draggingId = task.id
                                dragDelta = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDelta += amount.y
                                val cur = localOrder ?: return@detectDragGesturesAfterLongPress
                                val idx = cur.indexOf(task.id)
                                if (idx == -1) return@detectDragGesturesAfterLongPress
                                // swap with the neighbour once past half its height
                                if (dragDelta > 0 && idx < cur.lastIndex) {
                                    val nextH = heights[cur[idx + 1]] ?: 0
                                    if (nextH > 0 && dragDelta > nextH / 2f) {
                                        localOrder = cur.toMutableList().apply {
                                            add(idx + 1, removeAt(idx))
                                        }
                                        dragDelta -= nextH
                                    }
                                } else if (dragDelta < 0 && idx > 0) {
                                    val prevH = heights[cur[idx - 1]] ?: 0
                                    if (prevH > 0 && dragDelta < -prevH / 2f) {
                                        localOrder = cur.toMutableList().apply {
                                            add(idx - 1, removeAt(idx))
                                        }
                                        dragDelta += prevH
                                    }
                                }
                            },
                            onDragEnd = {
                                localOrder?.let(onPersist)
                                draggingId = null
                                dragDelta = 0f
                            },
                            onDragCancel = {
                                draggingId = null
                                dragDelta = 0f
                                localOrder = null
                            },
                        )
                    }
                    .alpha(if (isDragged) 0.95f else 1f),
            ) {
                TaskRow(
                    task = task,
                    today = today,
                    onToggle = { onToggle(task.id) },
                    onOpen = { onOpen(task.id) },
                    habitLabel = habitLabel(task),
                    last = index == displayed.lastIndex,
                    focused = false,
                    onFocusToggle = onFocusToggle?.let { f -> { f(task) } },
                )
            }
        }
    }
}
