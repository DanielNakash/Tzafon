package com.thefoxworks.tzafon.ui.goals

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextDecoration
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.ui.components.AmountSheet
import com.thefoxworks.tzafon.ui.components.Bar
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.GroupHeader
import com.thefoxworks.tzafon.ui.components.LayerHeader
import com.thefoxworks.tzafon.ui.components.Nudge
import com.thefoxworks.tzafon.ui.components.PillButton
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.habits.HabitEditorSheet
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * Goals (DM-GOAL) — the interim Directions home until the M6 hub nests
 * them under themes. Endowed bars, steps with strikethrough, ENGINE habit
 * chips, near-done "what's left" framing; soft nudges, never blocks.
 */
@Composable
fun GoalsScreen(vm: GoalsViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Goal?>(null) }
    var creating by remember { mutableStateOf(false) }
    var updateFor by remember { mutableStateOf<Goal?>(null) }     // direct progress edit
    var celebrate by remember { mutableStateOf<Goal?>(null) }     // DM-GOAL-4
    var habitFrom by remember { mutableStateOf<Goal?>(null) }     // rebound → habit
    var overFiveDismissed by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Den.bg)) {
        Column(Modifier.fillMaxSize()) {
            LayerHeader(
                kicker = "DIRECTIONS · AIM",
                title = "Goals",
                accent = Den.rust,
                compass = true,
                sub = "The finite pursuits — each one done is real. Themes arrive soon to hold them.",
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 200.dp),
            ) {
                if (state.overFive && !overFiveDismissed) {
                    item(key = "overfive") {
                        Nudge(
                            text = "Six goals pulling at once — plenty. Want to freeze one until another lands? Your call, always.",
                            modifier = Modifier.padding(top = 12.dp),
                            actions = {
                                PillButton("IT'S FINE", onClick = { overFiveDismissed = true })
                            },
                        )
                    }
                }

                items(state.ongoing, key = { it.goal.id }) { card ->
                    GoalCard(
                        card = card,
                        onOpen = { editing = card.goal },
                        onToggleStep = { i -> vm.toggleStep(card.goal, i) },
                        onUpdate = { updateFor = card.goal },
                        onComplete = { celebrate = card.goal },
                        onFreeze = { vm.setState(card.goal, GoalState.FROZEN) },
                    )
                }

                if (state.frozen.isNotEmpty()) {
                    item(key = "h_frozen") { GroupHeader("On ice", state.frozen.size, accent = Den.frozen) }
                    items(state.frozen, key = { "f_${it.goal.id}" }) { card ->
                        FrozenGoalRow(
                            goal = card.goal,
                            onThaw = { vm.setState(card.goal, GoalState.ONGOING) },
                            onOpen = { editing = card.goal },
                        )
                    }
                }

                if (state.completed.isNotEmpty()) {
                    item(key = "h_done") { GroupHeader("Finished", state.completed.size, accent = Den.green) }
                    items(state.completed, key = { "c_${it.goal.id}" }) { card ->
                        CompletedGoalRow(goal = card.goal, onOpen = { editing = card.goal })
                    }
                }

                if (state.ongoing.isEmpty() && state.frozen.isEmpty() && state.completed.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 70.dp, start = 30.dp, end = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TzIcons.Target(30.dp, Den.rust)
                            Text(
                                "Nothing aimed at yet",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
                                color = Den.ink,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Text(
                                "A goal is a finish line you choose — steps to walk, an amount to gather, or simply a direction. Set one small enough to finish.",
                                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.5.sp),
                                color = Den.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)) {
            Fab(onClick = { creating = true }, label = "Goal")
        }
    }

    if (creating || editing != null) {
        GoalEditorSheet(
            initial = editing,
            onSave = { vm.save(it) },
            onDelete = { vm.delete(it) },
            onComplete = { celebrate = it },
            onClose = { creating = false; editing = null },
        )
    }

    updateFor?.let { g ->
        AmountSheet(
            title = "Where does it stand?",
            unit = g.unit,
            suggested = g.currentQty,
            onConfirm = { vm.setCurrent(g.id, it) },
            onClose = { updateFor = null },
        )
    }

    celebrate?.let { g ->
        CompletionSheet(
            goal = g,
            onEnjoy = { vm.complete(g); celebrate = null },
            onFollowOn = { vm.complete(g); celebrate = null; creating = true },
            onMakeHabit = { vm.complete(g); habitFrom = g; celebrate = null },
        )
    }

    habitFrom?.let { g ->
        HabitEditorSheet(
            initial = null,
            presetName = g.title,
            onSave = { vm.saveHabit(it) },
            onDelete = { },
            onClose = { habitFrom = null },
        )
    }
}

@Composable
private fun GoalCard(
    card: GoalCardState,
    onOpen: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onUpdate: () -> Unit,
    onComplete: () -> Unit,
    onFreeze: () -> Unit,
) {
    val g = card.goal
    val accent = Den.rust // per-theme accents arrive with M6
    val pct = g.pct()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
            .pressable(onOpen)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TzIcons.Target(15.dp, accent)
            Box(
                Modifier.border(1.dp, accent.a(0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    when (g.type) {
                        GoalType.STEPPED -> "STEPPED"
                        GoalType.ACCUMULATIVE -> "ACCUMULATIVE"
                        GoalType.GENERIC -> "GENERIC"
                    },
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.8.sp),
                    color = accent,
                )
            }
            Box(Modifier.weight(1f))
            Text(
                "NO THEME", // the M6 hub replaces this with "serves: <theme>"
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                color = Den.faint,
            )
        }
        Text(
            g.title,
            style = TextStyle(fontFamily = DenType.serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.5.sp),
            color = Den.ink,
            modifier = Modifier.padding(top = 7.dp),
        )

        when (g.type) {
            GoalType.ACCUMULATIVE -> {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        fmt(g.currentQty),
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                    Text(
                        "${fmt(g.targetQty)} ${g.unit ?: ""}",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp),
                        color = Den.muted,
                    )
                }
                Bar(pct = pct.toFloat(), fill = accent)
                if (card.nearDone && g.targetQty > g.currentQty) {
                    Text(
                        "↗ ${fmt(g.targetQty - g.currentQty)} ${g.unit ?: ""} to go — nearly there",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                        color = Den.amber,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
            }

            GoalType.STEPPED -> {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${g.steps.count { it.done }} of ${g.steps.size} steps",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp),
                        color = Den.muted,
                    )
                    Text(
                        "$pct%",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    g.steps.forEachIndexed { i, s ->
                        Row(
                            Modifier.pressable { onToggleStep(i) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).let {
                                    if (s.done) it.background(accent)
                                    else it.border(1.5.dp, Den.ink.a(0.28f), RoundedCornerShape(4.dp))
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (s.done) TzIcons.Check(10.dp, androidx.compose.ui.graphics.Color.White)
                            }
                            Text(
                                s.label,
                                style = TextStyle(
                                    fontFamily = DenType.body, fontSize = 12.sp,
                                    textDecoration = if (s.done) TextDecoration.LineThrough else TextDecoration.None,
                                ),
                                color = if (s.done) Den.muted else Den.ink,
                            )
                        }
                    }
                }
            }

            GoalType.GENERIC -> if (g.description.isNotBlank()) {
                Text(
                    g.description,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, fontStyle = FontStyle.Italic),
                    color = Den.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // ── ENGINE — habits driving this goal (D2) ──
        if (card.engines.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 11.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Den.line2))
                Row(
                    Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        "ENGINE",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.5.sp),
                        color = Den.faint,
                    )
                    card.engines.forEach { h ->
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp)).background(accent.a(0.1f))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            TzIcons.Repeat(11.dp, accent)
                            Text(
                                h.name,
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                                color = accent,
                            )
                        }
                    }
                }
            }
        }

        // ── quiet actions ──
        Row(
            Modifier.fillMaxWidth().padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (g.type == GoalType.ACCUMULATIVE) PillButton("UPDATE PROGRESS", onClick = onUpdate)
            if (pct >= 100) PillButton("COMPLETE ✓", onClick = onComplete, color = Den.green)
            if (card.stale) PillButton("QUIET LATELY — FREEZE?", onClick = onFreeze, color = Den.frozen)
        }
    }
}

@Composable
private fun FrozenGoalRow(goal: Goal, onThaw: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onOpen).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        TzIcons.Frozen(16.dp, Den.frozen)
        Text(
            goal.title,
            style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp),
            color = Den.muted,
            modifier = Modifier.weight(1f),
        )
        PillButton("THAW", onClick = onThaw, color = Den.frozen)
    }
}

@Composable
private fun CompletedGoalRow(goal: Goal, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onOpen).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(Den.green),
            contentAlignment = Alignment.Center,
        ) { TzIcons.Check(12.dp, androidx.compose.ui.graphics.Color.White) }
        Text(
            goal.title,
            style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, textDecoration = TextDecoration.LineThrough),
            color = Den.muted,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else v.toString()
