package com.thefoxworks.tzafon.ui.backlog

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.capture.QuickAddSheet
import com.thefoxworks.tzafon.ui.components.CalendarPicker
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.Dot
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.StateSheet
import com.thefoxworks.tzafon.ui.components.TaskCheckbox
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * Backlog (FR-BACKLOG, design: BacklogScreen) — a quiet someday/maybe pool.
 * PULL → reopens a task (optionally dated); the checkbox / long-press opens
 * the state sheet for Done ("did it anyway") or Closed ("decided against").
 */
@Composable
fun BacklogScreen(
    vm: BacklogViewModel,
    onOpenTask: (String) -> Unit,
    onExpandAdd: (String) -> Unit,
    onOpenAllTasks: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenAbout: (() -> Unit)? = null,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var pullTask by remember { mutableStateOf<Task?>(null) }
    var pullPickDate by remember { mutableStateOf(false) }
    var sheetTask by remember { mutableStateOf<Task?>(null) }
    var quickAdd by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Den.bg)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "Backlog",
                kicker = "SOMEDAY / MAYBE",
                metaLeft = "Parked — not scheduled",
                metaRight = "${state.items.size} item${if (state.items.size == 1) "" else "s"}",
                onMenu = { menu = true },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 140.dp),
            ) {
                item(key = "intro") {
                    Text(
                        "A quiet pool for things you might do. They stay out of Today and Planning, and don't count toward your day. Pull one over whenever it calls.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 20.sp),
                        color = Den.muted,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }

                items(state.items, key = { it.id }) { t ->
                    BacklogRow(
                        task = t,
                        last = t.id == state.items.last().id,
                        onOpen = { onOpenTask(t.id) },
                        onSheet = { sheetTask = t },
                        onPull = { pullTask = t },
                    )
                }

                if (state.items.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 70.dp, start = 30.dp, end = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TzIcons.Moon(30.dp, Den.backlog.a(0.5f))
                            Text(
                                "Nothing parked",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp),
                                color = Den.ink,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Text(
                                "Someday-things land here from Planning or the task editor — waiting without weighing on you.",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.5.sp),
                                color = Den.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                } else {
                    item(key = "footer") {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TzIcons.Moon(26.dp, Den.backlog.a(0.5f))
                            Text(
                                "NO RUSH · NO GUILT",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.4.sp),
                                color = Den.faint,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 24.dp)) {
            Fab(onClick = { quickAdd = true })
        }
    }

    // ── the pull sheet (FR-BACKLOG-3: → Open, optionally dated) ──
    pullTask?.let { t ->
        if (!pullPickDate) {
            DenSheet(title = "Pull it over", onClose = { pullTask = null }) {
                Column {
                    Text(
                        "Back on the plate — where should it land?",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                        color = Den.muted,
                        modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
                    )
                    PullOption("Today", "Do it while it's calling.") {
                        vm.pull(t.id, state.today); pullTask = null
                    }
                    PullOption("Pick a day", "Give it a spot on the calendar.") {
                        pullPickDate = true
                    }
                    PullOption("No date yet", "Open, in the inbox — decide in Planning.", last = true) {
                        vm.pull(t.id, null); pullTask = null
                    }
                }
            }
        } else {
            DenSheet(title = "Pull it to", onClose = { pullTask = null; pullPickDate = false }) {
                CalendarPicker(
                    value = null,
                    today = state.today,
                    onPick = { d ->
                        vm.pull(t.id, d)
                        pullTask = null
                        pullPickDate = false
                    },
                )
            }
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
            onAllTasks = onOpenAllTasks ?: {},
            onSettings = onOpenSettings,
            onAbout = onOpenAbout,
        )
    }
}

@Composable
private fun BacklogRow(
    task: Task,
    last: Boolean,
    onOpen: () -> Unit,
    onSheet: () -> Unit,
    onPull: () -> Unit,
) {
    val hasLinks = task.themeId != null || task.habitId != null || task.goalIds.isNotEmpty()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().pressable(onOpen).padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            TaskCheckbox(task.state, onClick = onSheet)
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, lineHeight = 21.sp),
                    color = Den.ink,
                )
                if (hasLinks) {
                    Row(
                        Modifier.padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Dot(Den.backlog.a(0.5f), 6.dp)
                        Text(
                            "links dormant · kept for later",
                            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                            color = Den.faint,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Den.rust.a(0.35f), RoundedCornerShape(999.dp))
                    .pressable(onPull)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "PULL →",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                    color = Den.rust,
                )
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Den.line))
    }
}

@Composable
private fun PullOption(title: String, sub: String, last: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().pressable(onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TzIcons.Calendar(18.dp, Den.rust)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, color = Den.ink),
                )
                Text(
                    sub,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp),
                    color = Den.muted,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            TzIcons.Chevron(16.dp, Den.ink.a(0.26f))
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Den.line2))
    }
}
