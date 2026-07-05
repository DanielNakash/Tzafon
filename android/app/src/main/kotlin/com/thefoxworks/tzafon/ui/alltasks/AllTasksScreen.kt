package com.thefoxworks.tzafon.ui.alltasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.StateSheet
import com.thefoxworks.tzafon.ui.components.TaskRow
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * All Tasks (FR-ALL) — complete index. M1: the four SHOW visibility toggles
 * (FR-ALL-3) and the state sheet (long-press a row, or tap a settled
 * checkbox). Search + horizon marker + scrubber land in M2.
 */
@Composable
fun AllTasksScreen(
    vm: AllTasksViewModel,
    onOpenTask: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val pct = if (state.total > 0) state.doneCount * 100f / state.total else 0f
    var sheetTask by remember { mutableStateOf<Task?>(null) }

    Box(Modifier.fillMaxSize().background(Den.bg)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "All Tasks",
                kicker = "COMPLETE INDEX",
                compact = true,
                bottomContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "h_${group.key}") {
                        GroupHeader(
                            label = group.label,
                            count = group.items.size,
                            accent = when (group.key) {
                                "overdue" -> Den.due
                                "today" -> Den.rust
                                else -> Den.muted
                            },
                        )
                    }
                    items(group.items, key = { it.id }) { task ->
                        Row(task, state.today, vm, onOpenTask, { sheetTask = it }, last = task.id == group.items.last().id, summaries = state.ruleSummaries)
                    }
                }

                if (state.undated.isNotEmpty()) {
                    item(key = "h_undated") { GroupHeader("No date", state.undated.size) }
                    items(state.undated, key = { it.id }) { task ->
                        Row(task, state.today, vm, onOpenTask, { sheetTask = it }, last = task.id == state.undated.last().id, summaries = state.ruleSummaries)
                    }
                }

                if (state.empty) {
                    item {
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
}

@Composable
private fun Row(
    task: Task,
    today: String,
    vm: AllTasksViewModel,
    onOpenTask: (String) -> Unit,
    onSheet: (Task) -> Unit,
    last: Boolean,
    summaries: Map<String, String>,
) {
    val settled = task.state == TaskState.CLOSED || task.state == TaskState.FROZEN || task.state == TaskState.BACKLOG
    TaskRow(
        task = task,
        today = today,
        onToggle = { if (settled) onSheet(task) else vm.toggleDone(task.id) },
        onOpen = { onOpenTask(task.id) },
        onLongPress = { onSheet(task) },
        recurSummary = task.seriesId?.let { summaries[it] },
        last = last,
    )
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
