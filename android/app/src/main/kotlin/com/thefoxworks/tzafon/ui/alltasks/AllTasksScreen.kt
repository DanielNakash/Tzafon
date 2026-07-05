package com.thefoxworks.tzafon.ui.alltasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.GroupHeader
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.TaskRow
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType

/**
 * All Tasks (FR-ALL) — M0 parity slice: grouped index, Done toggle, FAB.
 * (Search, the full SHOW toggles, horizon marker and scrubber land M1–M2.)
 */
@Composable
fun AllTasksScreen(
    vm: AllTasksViewModel,
    onOpenTask: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val pct = if (state.total > 0) state.doneCount * 100f / state.total else 0f

    Box(Modifier.fillMaxSize().background(Den.bg)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "All Tasks",
                kicker = "DON'T PANIC",
                metaLeft = Dates.fmtLong(state.today, state.today),
                metaRight = "${state.doneCount}/${state.total} done",
                progress = pct,
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
            ) {
                // done toggle bar
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (state.showDone) Den.surface else androidx.compose.ui.graphics.Color.Transparent)
                                .border(1.dp, Den.line, RoundedCornerShape(999.dp))
                                .pressable { vm.toggleShowDone() }
                                .padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            TzIcons.Eye(15.dp, Den.muted, off = !state.showDone)
                            Text(
                                if (state.showDone) "HIDE DONE" else "SHOW DONE",
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 11.5.sp, letterSpacing = 0.5.sp),
                                color = Den.muted,
                            )
                        }
                    }
                }

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
                        TaskRow(
                            task = task,
                            today = state.today,
                            onToggle = { vm.toggleDone(task.id) },
                            onOpen = { onOpenTask(task.id) },
                            recurSummary = task.seriesId?.let { state.ruleSummaries[it] },
                            last = task.id == group.items.last().id,
                        )
                    }
                }

                if (state.undated.isNotEmpty()) {
                    item(key = "h_undated") { GroupHeader("No date", state.undated.size) }
                    items(state.undated, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            today = state.today,
                            onToggle = { vm.toggleDone(task.id) },
                            onOpen = { onOpenTask(task.id) },
                            recurSummary = task.seriesId?.let { state.ruleSummaries[it] },
                            last = task.id == state.undated.last().id,
                        )
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

                if (state.showDone && state.done.isNotEmpty()) {
                    item(key = "h_done") { GroupHeader("Done", state.done.size) }
                    items(state.done, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            today = state.today,
                            onToggle = { vm.toggleDone(task.id) },
                            onOpen = { onOpenTask(task.id) },
                            recurSummary = task.seriesId?.let { state.ruleSummaries[it] },
                            last = task.id == state.done.last().id,
                        )
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 24.dp)) {
            Fab(onClick = onAdd)
        }
    }
}
