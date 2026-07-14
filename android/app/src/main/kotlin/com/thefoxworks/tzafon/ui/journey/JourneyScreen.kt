package com.thefoxworks.tzafon.ui.journey

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.journey.JourneyLogic
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.ui.components.Compass
import com.thefoxworks.tzafon.ui.components.Dot
import com.thefoxworks.tzafon.ui.components.GroupHeader
import com.thefoxworks.tzafon.ui.components.Kicker
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.nav.AppMenuSheet
import com.thefoxworks.tzafon.ui.components.Serves
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir

@Composable
@androidx.compose.runtime.ReadOnlyComposable
private fun accentFor(theme: Theme?): Color =
    theme?.let { Tz.colors.themeAccents[it.accentSlot % 3] } ?: Tz.colors.rust

/**
 * Journey (FR-JOURNEY, design: JourneyScreen) — the identity mirror.
 * Completed goals, long habit arcs, directions over time, reviewed weeks.
 * Deliberately no badges, streaks, points, or comparisons (FR-JOURNEY-3).
 */
@Composable
fun JourneyScreen(
    vm: JourneyViewModel,
    onOpenAllTasks: () -> Unit = {},
    onOpenBacklog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Tz.colors.surface)) {
        RustHeader(
            title = "How far you've come",
            kicker = "JOURNEY · THE MIRROR",
            compass = true,
            onMenu = { menu = true },
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 200.dp),
        ) {
            if (state.isEmpty) {
                item(key = "empty") { EmptyMirror() }
                return@LazyColumn
            }

            // FR-NAV-5 — the migrated LayerHeader `sub` line lives as a body
            // intro paragraph after the palette collapse.
            item(key = "intro") {
                Text(
                    "Not a trophy case — a mirror. Who you're becoming, in your own past and your own words.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 20.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            // ── the honest aggregate (only once there is something to reflect) ──
            if (state.quarterAligned > 0) {
                item(key = "aggregate") { AggregateCard(state.quarterAligned) }
            }

            if (state.milestones.isNotEmpty()) {
                item(key = "milestones-h") {
                    GroupHeader("Milestones", count = state.milestones.size, accent = Tz.colors.rust)
                }
                items(count = state.milestones.size, key = { "m-" + state.milestones[it].goal.id }) { i ->
                    MilestoneRow(state.milestones[i], topPad = if (i == 0) 0.dp else 10.dp)
                }
            }

            if (state.arcs.isNotEmpty()) {
                item(key = "arcs-h") { GroupHeader("Who you're becoming", accent = Tz.colors.rust) }
                items(count = state.arcs.size, key = { "a-" + state.arcs[it].habit.id }) { i ->
                    ArcRow(state.arcs[i], topPad = if (i == 0) 0.dp else 9.dp)
                }
            }

            if (state.transitions.isNotEmpty()) {
                item(key = "dir-h") { GroupHeader("Directions over time", accent = Tz.colors.rust) }
                items(count = state.transitions.size, key = { "t-" + state.transitions[it].theme.id }) { i ->
                    TransitionRow(state.transitions[i], last = i == state.transitions.size - 1)
                }
            }

            if (state.reviewRows.isNotEmpty()) {
                item(key = "rev-h") { GroupHeader("Weeks, reflected", count = state.reviewRows.size, accent = Tz.colors.rust) }
                items(count = state.reviewRows.size, key = { "r-" + state.reviewRows[it].kicker + it }) { i ->
                    ReviewTimelineRow(state.reviewRows[i], last = i == state.reviewRows.size - 1)
                }
            }
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
}

/** The rust mirror card — real quarter numbers, compass watermark. */
@Composable
private fun AggregateCard(aligned: Int) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tz.colors.rust),
    ) {
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 24.dp, y = 24.dp).alpha(0.16f)) {
            Compass(size = 120.dp, ring = Tz.colors.cream, needleN = Tz.colors.cream, needleS = Tz.colors.cream, stroke = 1.2f)
        }
        Column(Modifier.padding(18.dp)) {
            Kicker("THIS QUARTER", color = Tz.colors.cream.a(0.75f))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$aligned") }
                    append(" of your completed tasks served a direction you chose.")
                },
                style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp, fontWeight = FontWeight.Medium, lineHeight = 27.sp),
                color = Tz.colors.cream,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "That's not busywork. That's who you're becoming.",
                style = TextStyle(fontFamily = DenType.serif, fontStyle = FontStyle.Italic, fontSize = 14.sp),
                color = Tz.colors.cream.a(0.85f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** A completed goal — sparkle roundel, title, "serves" chip, month stamp. */
@Composable
private fun MilestoneRow(m: Milestone, topPad: androidx.compose.ui.unit.Dp) {
    val accent = accentFor(m.theme)
    Row(
        Modifier.fillMaxWidth()
            .padding(top = topPad)
            .clip(RoundedCornerShape(13.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(accent.a(0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            TzIcons.Sparkle(17.dp, accent)
        }
        Column(Modifier.weight(1f)) {
            Text(
                m.goal.title,
                style = TextStyle(fontFamily = DenType.serif, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold).contentDir(),
                color = Tz.colors.ink,
            )
            if (m.theme != null) {
                Serves(label = m.theme.name, accent = accent, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Text(
            m.dateLabel,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
            color = Tz.colors.faint,
            maxLines = 1,
        )
    }
}

/** A living practice — sprout, name, the long arc (never a streak). */
@Composable
private fun ArcRow(arc: Arc, topPad: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier.fillMaxWidth().padding(top = topPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        TzIcons.Sprout(17.dp, accentFor(arc.theme))
        Text(
            arc.habit.name,
            style = TextStyle(fontFamily = DenType.serif, fontSize = 16.sp, fontWeight = FontWeight.SemiBold).contentDir(),
            color = Tz.colors.ink,
        )
        Text(
            "— ${arc.detail}",
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
            color = Tz.colors.muted,
            maxLines = 1,
        )
    }
}

/** One archived direction on the timeline — dot, connective, where it went. */
@Composable
private fun TransitionRow(t: JourneyLogic.Transition, last: Boolean) {
    val liveAccent = t.successor?.let { accentFor(it) }
    Row(Modifier.fillMaxWidth().padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 3.dp)) { Dot(liveAccent ?: Tz.colors.closed, 11.dp) }
            if (!last) {
                Box(Modifier.padding(top = 3.dp).width(2.dp).height(26.dp).background(Tz.colors.line))
            }
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Tz.colors.ink)) { append(t.theme.name) }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Tz.colors.faint)) { append(" ${t.mid} ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = liveAccent ?: Tz.colors.muted)) { append(t.to) }
            },
            style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 20.sp),
            modifier = Modifier.padding(bottom = 14.dp),
        )
    }
}

/** One reviewed period — the artifacts strung into a narrative. */
@Composable
private fun ReviewTimelineRow(row: ReviewRow, last: Boolean) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 3.dp)) { Dot(Tz.colors.rust.a(0.55f), 11.dp) }
            if (!last) {
                Box(Modifier.padding(top = 3.dp).width(2.dp).height(30.dp).background(Tz.colors.line))
            }
        }
        Column(Modifier.padding(bottom = 14.dp)) {
            Text(
                row.kicker,
                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.6.sp),
                color = Tz.colors.faint,
            )
            if (row.note != null) {
                Text(
                    "“${row.note}”",
                    style = TextStyle(fontFamily = DenType.serif, fontStyle = FontStyle.Italic, fontSize = 14.5.sp, lineHeight = 20.sp).contentDir(),
                    color = Tz.colors.ink,
                    modifier = Modifier.padding(top = 3.dp),
                )
            } else if (row.quiet != null) {
                Text(
                    row.quiet,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** FR-JOURNEY-4 — the gentle empty mirror; never a wall of grey trophies. */
@Composable
private fun EmptyMirror() {
    Column(
        Modifier.fillMaxWidth().padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Compass(size = 52.dp, ring = Tz.colors.faint, needleN = Tz.colors.rust, needleS = Tz.colors.faint, stroke = 1.4f)
        Text(
            "The mirror fills as you go",
            style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            color = Tz.colors.ink,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Finish a goal, keep a habit alive, let a season pass — this page will remember it with you.",
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.sp),
            color = Tz.colors.muted,
            modifier = Modifier.padding(top = 7.dp).fillMaxWidth(0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
