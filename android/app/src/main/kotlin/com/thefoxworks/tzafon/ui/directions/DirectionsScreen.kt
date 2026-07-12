package com.thefoxworks.tzafon.ui.directions

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.model.ArchivedOutcome
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.ui.components.AmountSheet
import com.thefoxworks.tzafon.ui.components.Compass
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.Fab
import com.thefoxworks.tzafon.ui.components.GroupHeader
import com.thefoxworks.tzafon.ui.components.Nudge
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.PillButton
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.goals.CompletionSheet
import com.thefoxworks.tzafon.ui.goals.GoalCard
import com.thefoxworks.tzafon.ui.goals.GoalEditorSheet
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.habits.HabitEditorSheet
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir
import kotlin.math.cos
import kotlin.math.sin

private fun accentFor(theme: Theme): Color = Den.themeAccents[theme.accentSlot % 3]

/**
 * The Directions hub (FR-DIR, design: DirectionsBoard) — Tzafon's namesake.
 * Active bearings with rings, one expandable in place; orphan goals;
 * upcoming and archive behind the scope selector.
 */
@Composable
fun DirectionsScreen(
    vm: DirectionsViewModel,
    onOpenAllTasks: () -> Unit = {},
    onOpenBacklog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var scope by rememberSaveable { mutableStateOf("active") }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTheme by remember { mutableStateOf<Theme?>(null) }
    var creatingTheme by remember { mutableStateOf(false) }
    var archiveFor by remember { mutableStateOf<Theme?>(null) }
    var capBlocked by remember { mutableStateOf<Theme?>(null) }
    // FR-DIR-7 — hub-level two-option create chooser + hub-scoped goal editor.
    var hubChooser by remember { mutableStateOf(false) }
    var creatingHubGoal by remember { mutableStateOf(false) }
    // goal/habit flows
    var goalEditor by remember { mutableStateOf<Pair<Goal?, String?>?>(null) }  // (goal, themeId)
    var habitEditor by remember { mutableStateOf<Pair<Habit?, String?>?>(null) }
    var updateFor by remember { mutableStateOf<Goal?>(null) }
    var celebrate by remember { mutableStateOf<Goal?>(null) }
    var menu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Den.surface)) {
        Column(Modifier.fillMaxSize()) {
            RustHeader(
                title = "Your bearings",
                kicker = "DIRECTIONS",
                onMenu = { menu = true },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 200.dp),
            ) {
                // FR-NAV-5 — the migrated LayerHeader `sub` line lives as a
                // body intro paragraph after the palette collapse.
                item(key = "intro") {
                    Text(
                        "The few directions everything serves — three at most. Tap a bearing to open it.",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 20.sp),
                        color = Den.muted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                item(key = "scope") {
                    ThemeScope(
                        active = scope,
                        counts = Triple(state.active.size, state.upcoming.size, state.archived.size),
                        onSelect = { scope = it },
                    )
                }

                if (state.arrivalPrompt != null && scope == "active") {
                    item(key = "arrival") {
                        Nudge(
                            text = "“${state.arrivalPrompt!!.name}” wants to start, but three bearings are live. Retire or park one when you're ready — no rush.",
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                when (scope) {
                    "active" -> {
                        item(key = "rose") {
                            Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
                                BearingRose(entries = state.active)
                            }
                        }

                        items(state.active, key = { it.theme.id }) { entry ->
                            if (expandedId == entry.theme.id) {
                                ExpandedTheme(
                                    entry = entry,
                                    engines = state.enginesByGoal,
                                    onCollapse = { expandedId = null },
                                    onEdit = { editingTheme = entry.theme },
                                    onArchive = { archiveFor = entry.theme },
                                    onAddGoal = { goalEditor = null to entry.theme.id },
                                    onAddHabit = { habitEditor = null to entry.theme.id },
                                    onOpenGoal = { goalEditor = it to entry.theme.id },
                                    onToggleStep = { g, i -> vm.toggleStep(g, i) },
                                    onUpdate = { updateFor = it },
                                    onComplete = { celebrate = it },
                                    onFreeze = { vm.setGoalState(it, GoalState.FROZEN) },
                                    onEditHabit = { habitEditor = it to entry.theme.id },
                                )
                            } else {
                                ThemeRow(
                                    entry = entry,
                                    modifier = Modifier.padding(top = 10.dp),
                                    onOpen = { expandedId = entry.theme.id },
                                )
                            }
                        }

                        if (state.active.isEmpty()) {
                            item(key = "noactive") { EmptyBearings() }
                        }

                        if (state.orphanGoals.isNotEmpty()) {
                            item(key = "h_orphans") {
                                GroupHeader("Goals without a theme", state.orphanGoals.size, accent = Den.muted)
                            }
                            items(state.orphanGoals, key = { "o_${it.id}" }) { g ->
                                Box(Modifier.padding(bottom = 10.dp)) {
                                    GoalCard(
                                        goal = g,
                                        engines = state.enginesByGoal[g.id] ?: emptyList(),
                                        accent = Den.rust,
                                        servesLabel = null,
                                        onOpen = { goalEditor = g to null },
                                        onToggleStep = { i -> vm.toggleStep(g, i) },
                                        onUpdate = { updateFor = g },
                                        onComplete = { celebrate = g },
                                        onFreeze = { vm.setGoalState(g, GoalState.FROZEN) },
                                    )
                                }
                            }
                            item(key = "addorphan") {
                                AddRow("Add a goal", Den.muted) { goalEditor = null to null }
                            }
                        }
                    }

                    "upcoming" -> {
                        items(state.upcoming, key = { it.id }) { t ->
                            UpcomingRow(
                                theme = t,
                                modifier = Modifier.padding(top = 10.dp),
                                onOpen = { editingTheme = t },
                                onActivate = {
                                    vm.activateNow(t, onBlocked = { capBlocked = t })
                                },
                            )
                        }
                        if (state.upcoming.isEmpty()) {
                            item { QuietNote("Nothing waiting. A fourth direction parks here until a slot opens.") }
                        }
                    }

                    else -> {
                        items(state.archived, key = { it.id }) { t ->
                            ArchivedRow(theme = t, modifier = Modifier.padding(top = 10.dp)) { editingTheme = t }
                        }
                        if (state.archived.isEmpty()) {
                            item { QuietNote("Finished windows rest here — they feed the Journey.") }
                        }
                    }
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)) {
            // FR-DIR-7.1 — one hub-level primary create, routed via a chooser to
            // "Add a goal" or "Add a theme". The FAB itself no longer prejudges
            // which one you meant.
            Fab(onClick = { hubChooser = true }, label = "Create")
        }
    }

    if (hubChooser) {
        HubCreateChooser(
            onAddGoal = { hubChooser = false; creatingHubGoal = true },
            onAddTheme = { hubChooser = false; creatingTheme = true },
            onClose = { hubChooser = false },
        )
    }

    if (creatingHubGoal) {
        GoalEditorSheet(
            initial = null,
            onSave = { vm.saveGoal(it, null) },
            onDelete = { vm.deleteGoal(it) },
            onComplete = { celebrate = it },
            onClose = { creatingHubGoal = false },
            showThemePicker = true,
            activeThemes = state.active.map { it.theme },
        )
    }

    // ── sheets ──
    if (creatingTheme || editingTheme != null) {
        ThemeEditorSheet(
            initial = editingTheme,
            onSave = { theme, activate ->
                if (activate) vm.activateNow(theme) { capBlocked = theme }
                else vm.saveKeepState(theme)
            },
            onArchive = { archiveFor = it },
            onClose = { creatingTheme = false; editingTheme = null },
        )
    }

    capBlocked?.let { t ->
        CapSheet(
            theme = t,
            onPark = { vm.parkAsUpcoming(t); capBlocked = null },
            onClose = { capBlocked = null },
        )
    }

    archiveFor?.let { t ->
        ArchiveSheet(
            theme = t,
            onPick = { outcome -> vm.archive(t, outcome); archiveFor = null; editingTheme = null },
            onClose = { archiveFor = null },
        )
    }

    goalEditor?.let { (goal, themeId) ->
        GoalEditorSheet(
            initial = goal,
            onSave = { vm.saveGoal(it, themeId) },
            onDelete = { vm.deleteGoal(it) },
            onComplete = { celebrate = it },
            onClose = { goalEditor = null },
        )
    }

    habitEditor?.let { (habit, themeId) ->
        HabitEditorSheet(
            initial = habit,
            onSave = { vm.saveHabit(it, themeId) },
            onDelete = { vm.deleteHabit(it) },
            onClose = { habitEditor = null },
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
            onEnjoy = { vm.completeGoal(g); celebrate = null },
            onFollowOn = { vm.completeGoal(g); celebrate = null; goalEditor = null to g.primaryThemeId },
            onMakeHabit = { vm.completeGoal(g); celebrate = null; habitEditor = null to g.primaryThemeId },
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

// ── pieces ─────────────────────────────────────────────────────

/** Circular progress ring (design: Ring). */
@Composable
fun Ring(pct: Int, size: Dp, color: Color, stroke: Dp = 5.dp, label: Boolean = true) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            drawArc(
                color = Color(0xFF241A12).copy(alpha = 0.12f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(sw),
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * (pct.coerceIn(0, 100) / 100f), useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        if (label) {
            Text(
                "$pct%",
                style = TextStyle(fontFamily = DenType.mono, fontSize = (size.value / 4.2).sp, fontWeight = FontWeight.Bold),
                color = color,
            )
        }
    }
}

/** The compass rose with one bearing dot per active theme (design). */
@Composable
private fun BearingRose(entries: List<ThemeBoardEntry>, size: Dp = 128.dp) {
    Box(Modifier.size(size)) {
        Compass(
            size = size,
            ring = Den.rust.a(0.55f),
            needleN = Den.rust,
            needleS = Den.faint,
            stroke = 1.2f,
            ticks = true,
        )
        entries.take(3).forEachIndexed { i, entry ->
            val rad = Math.toRadians((-90 + i * 120).toDouble())
            val r = size / 2 - 16.dp
            val x = (size / 2) + (r * cos(rad).toFloat()) - 8.dp
            val y = (size / 2) + (r * sin(rad).toFloat()) - 8.dp
            Box(
                Modifier
                    .offset(x = x, y = y)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accentFor(entry.theme))
                    .border(2.5.dp, Den.surface, CircleShape),
            )
        }
    }
}

/** ACTIVE · n / UPCOMING · n / ARCHIVE · n selector (design: ThemeScope). */
@Composable
private fun ThemeScope(active: String, counts: Triple<Int, Int, Int>, onSelect: (String) -> Unit) {
    val opts = listOf(
        "active" to "ACTIVE · ${counts.first}",
        "upcoming" to "UPCOMING · ${counts.second}",
        "archive" to "ARCHIVE · ${counts.third}",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Den.surfaceAlt)
            .border(1.dp, Den.line, RoundedCornerShape(999.dp))
            .padding(3.dp),
    ) {
        opts.forEach { (key, label) ->
            val on = key == active
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) Den.rust else Color.Transparent)
                    .pressable { onSelect(key) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.2.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (on) Color.White else Den.muted,
                )
            }
        }
    }
}

/** Collapsed bearing row (design: ThemeRow). */
@Composable
private fun ThemeRow(entry: ThemeBoardEntry, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val accent = accentFor(entry.theme)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
            .pressable(onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 4.dp, height = 92.dp).background(accent))
        Row(
            Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Ring(pct = entry.ringPct, size = 44.dp, color = accent)
            Column(Modifier.weight(1f)) {
                Text(
                    entry.theme.name,
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp).contentDir(),
                    color = Den.ink,
                )
                Text(
                    "“${entry.theme.why}”",
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 12.5.sp, fontStyle = FontStyle.Italic).contentDir(),
                    color = Den.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "${entry.windowLabel} · ${entry.goals.size} GOAL${if (entry.goals.size == 1) "" else "S"} · ${entry.habits.size} HABIT${if (entry.habits.size == 1) "" else "S"}",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                    color = Den.faint,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TzIcons.Chevron(18.dp, Den.ink.a(0.3f))
                Text("OPEN", style = TextStyle(fontFamily = DenType.mono, fontSize = 8.sp), color = Den.faint)
            }
        }
    }
}

/** One theme expanded inline (design: DirectionsBoardExpanded). */
@Composable
private fun ExpandedTheme(
    entry: ThemeBoardEntry,
    engines: Map<String, List<Habit>>,
    onCollapse: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onAddGoal: () -> Unit,
    onAddHabit: () -> Unit,
    onOpenGoal: (Goal) -> Unit,
    onToggleStep: (Goal, Int) -> Unit,
    onUpdate: (Goal) -> Unit,
    onComplete: (Goal) -> Unit,
    onFreeze: (Goal) -> Unit,
    onEditHabit: (Habit) -> Unit,
) {
    val accent = accentFor(entry.theme)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(accent.a(0.12f))
            .border(1.dp, accent.a(0.28f), RoundedCornerShape(15.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().pressable(onCollapse).padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Ring(pct = entry.ringPct, size = 46.dp, color = accent)
            Column(Modifier.weight(1f)) {
                Text(
                    entry.theme.name,
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp).contentDir(),
                    color = Den.ink,
                )
                Text(
                    "“${entry.theme.why}”",
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 13.sp, fontStyle = FontStyle.Italic).contentDir(),
                    color = Den.muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TzIcons.Chevron(18.dp, accent, modifier = Modifier.rotate(-90f))
                Text("CLOSE", style = TextStyle(fontFamily = DenType.mono, fontSize = 8.sp), color = accent.a(0.8f))
            }
        }
        Row(
            Modifier.padding(start = 15.dp, end = 15.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Compass(size = 13.dp, ring = accent, needleN = accent, needleS = accent.a(0.4f), stroke = 2f)
            Text(
                "${entry.windowLabel} · ${entry.mirror}",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                color = Den.muted,
            )
        }
        if (entry.windowEnded) {
            Row(Modifier.padding(start = 15.dp, end = 15.dp, bottom = 12.dp)) {
                PillButton("WINDOW ENDED — RENEW OR REST?", onClick = onArchive, color = accent)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Den.surface)
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // FR-DIR-2 — the auto-bridge nudge
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                TzIcons.Sparkle(14.dp, accent)
                Text(
                    "What would this look like this week?",
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 13.5.sp, fontStyle = FontStyle.Italic),
                    color = Den.muted,
                )
            }

            SectionLabel("Goals", color = accent)
            entry.goals.forEach { g ->
                GoalCard(
                    goal = g,
                    engines = engines[g.id] ?: emptyList(),
                    accent = accent,
                    servesLabel = entry.theme.name.let { if (it.length > 16) it.take(15) + "…" else it },
                    sharedCount = (g.themeIds - setOfNotNull(g.primaryThemeId)).size,
                    onOpen = { onOpenGoal(g) },
                    onToggleStep = { i -> onToggleStep(g, i) },
                    onUpdate = { onUpdate(g) },
                    onComplete = { onComplete(g) },
                    onFreeze = { onFreeze(g) },
                )
            }
            AddRow("Add a goal to this theme", accent, onAddGoal)
            if (entry.sharedGoals.isNotEmpty()) {
                Text(
                    "+ shared goals (${entry.sharedGoals.size}) · ${entry.sharedGoals.joinToString { it.title }}",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                    color = Den.faint,
                )
            }

            SectionLabel("Habits", color = accent, modifier = Modifier.padding(top = 4.dp))
            entry.habits.forEach { h -> HabitChip(h, accent) { onEditHabit(h) } }
            AddRow("Add a habit to this theme", accent, onAddHabit)

            if (entry.taskCount > 0) {
                Text(
                    "${entry.taskCount} OPEN TASK${if (entry.taskCount == 1) "" else "S"} POINT HERE",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.4.sp),
                    color = Den.faint,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PillButton("EDIT", onClick = onEdit)
                PillButton("ARCHIVE…", onClick = onArchive, color = Den.muted)
            }
        }
    }
}

/** Compact habit chip with week dots (design: HabitChip). */
@Composable
private fun HabitChip(h: Habit, accent: Color, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(10.dp))
            .pressable(onOpen)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TzIcons.Repeat(13.dp, accent)
        Text(
            h.name,
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
            color = Den.ink,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (h.kind == HabitKind.QUANTITATIVE) "${fmt(h.target)} ${h.unit ?: ""}/day"
            else "${h.target.toInt()}× / week",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
            color = Den.muted,
        )
    }
}

@Composable
private fun AddRow(label: String, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(accent.a(0.06f))
            .border(1.dp, accent.a(0.5f), RoundedCornerShape(11.dp))
            .pressable(onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TzIcons.Plus(15.dp, accent)
        Text(
            label,
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = accent,
        )
    }
}

@Composable
private fun UpcomingRow(theme: Theme, modifier: Modifier = Modifier, onOpen: () -> Unit, onActivate: () -> Unit) {
    val accent = accentFor(theme)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
            .pressable(onOpen)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(accent.a(0.5f)))
        Column(Modifier.weight(1f)) {
            Text(
                theme.name,
                style = TextStyle(fontFamily = DenType.serif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold).contentDir(),
                color = Den.ink,
            )
            Text(
                "STARTS ${com.thefoxworks.tzafon.domain.dates.Dates.fmtDate(theme.windowStart).uppercase()}",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                color = Den.faint,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        PillButton("START NOW", onClick = onActivate, color = accent)
    }
}

@Composable
private fun ArchivedRow(theme: Theme, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onOpen)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Compass(size = 16.dp, ring = Den.faint, needleN = Den.faint, needleS = Den.faint, stroke = 1.4f)
        Column(Modifier.weight(1f)) {
            Text(
                theme.name,
                style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp).contentDir(),
                color = Den.muted,
            )
            Text(
                (theme.archivedOutcome?.name ?: "ARCHIVED") + " · " +
                    com.thefoxworks.tzafon.domain.dates.Dates.fmtDate(theme.windowEnd),
                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                color = Den.faint,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun EmptyBearings() {
    Column(
        Modifier.fillMaxWidth().padding(top = 26.dp, start = 30.dp, end = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "No bearings held yet",
            style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
            color = Den.ink,
        )
        Text(
            "A theme is a direction with a why — “more of what I want”, held for a season. Three at most, so each one matters.",
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.5.sp),
            color = Den.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun QuietNote(text: String) {
    Text(
        text,
        style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, lineHeight = 18.5.sp),
        color = Den.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 30.dp, end = 30.dp),
    )
}

/**
 * FR-DIR-7.1 — hub-level two-option create chooser. Kept intentionally sparse:
 * two labelled rows, no extra copy — the chooser's only job is to route the
 * primary create action to the right editor. Direction-layer objects (theme
 * and goal) are peers at this tier, so they get equal visual weight here.
 */
@Composable
internal fun HubCreateChooser(
    onAddGoal: () -> Unit,
    onAddTheme: () -> Unit,
    onClose: () -> Unit,
) {
    DenSheet(title = "What are you adding?", onClose = onClose) {
        Column {
            ChooserRow(
                icon = { TzIcons.Target(18.dp, Den.rust) },
                label = "Add a goal",
                sub = "A finish line — stepped, an amount, or a direction.",
                onClick = onAddGoal,
                contentDescription = "Add a goal",
            )
            ChooserRow(
                icon = { Compass(size = 18.dp, ring = Den.rust, needleN = Den.rust, needleS = Den.rust.a(0.4f), stroke = 1.8f) },
                label = "Add a theme",
                sub = "A direction with a why — a season's bearing.",
                onClick = onAddTheme,
                contentDescription = "Add a theme",
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun ChooserRow(
    icon: @Composable () -> Unit,
    label: String,
    sub: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(13.dp))
            .semanticsCd(contentDescription)
            .pressable(label = contentDescription, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(999.dp)).background(Den.rust.a(0.1f)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = TextStyle(fontFamily = DenType.serif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = Den.ink,
            )
            Text(
                sub,
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 17.sp),
                color = Den.muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        TzIcons.Chevron(16.dp, Den.faint, dir = TzIcons.Dir.RIGHT)
    }
}

private fun Modifier.semanticsCd(cd: String): Modifier =
    this.then(
        Modifier.semantics {
            this.contentDescription = cd
        },
    )

private fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else v.toString()
