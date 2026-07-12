package com.thefoxworks.tzafon.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/** FR-NAV-1 — the five primary tabs. All Tasks/Backlog/Settings ride the menu. */
enum class Tab(val route: String, val label: String) {
    TODAY("today", "Today"),
    PLANNING("planning", "Planning"),
    HABITS("habits", "Habits"),
    DIRECTIONS("directions", "Directions"),
    JOURNEY("journey", "Journey"),
}

@Composable
private fun TabIcon(tab: Tab, size: Dp, color: Color, weight: Float) {
    when (tab) {
        Tab.TODAY -> TzIcons.Today(size, color, weight)
        Tab.PLANNING -> TzIcons.Plan(size, color, weight)
        Tab.HABITS -> TzIcons.Habit(size, color, weight)
        Tab.DIRECTIONS -> TzIcons.CompassIcon(size, color, weight)
        Tab.JOURNEY -> TzIcons.Journey(size, color, weight)
    }
}

/**
 * The Den bottom bar (tz-ui BottomNav): 84dp, hairline, rust pill on active.
 * `active = null` renders every tab inactive — used on reference views (All
 * Tasks / Backlog) so the bar serves as a wayfinder without pretending any
 * primary tab is the current destination (FR-NAV-6.1).
 */
@Composable
fun DenBottomNav(active: Tab?, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Den.surface.a(0.97f))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Den.line))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Tab.entries.forEach { tab ->
                val on = tab == active
                Column(
                    Modifier
                        .weight(1f)
                        .pressable(tab.label, Role.Tab) { onSelect(tab) }
                        .padding(bottom = 6.dp)
                        .semantics {
                            contentDescription = tab.label
                            selected = on
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 46.dp, height = 26.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on) Den.rust.a(0.14f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        TabIcon(tab, if (on) 22.dp else 21.dp, if (on) Den.rust else Den.faint, if (on) 2.1f else 1.8f)
                    }
                    Text(
                        tab.label,
                        style = TextStyle(
                            fontFamily = DenType.mono,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.4.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (on) Den.rust else Den.faint,
                    )
                }
            }
        }
        Box(Modifier.navigationBarsPadding())
    }
}

/**
 * FR-NAV-1 — the header menu: the reference views (All Tasks · Backlog) and
 * Settings live here, off the daily path. Entries appear as they are built.
 */
@Composable
fun AppMenuSheet(
    onClose: () -> Unit,
    onAllTasks: () -> Unit,
    onBacklog: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onAbout: (() -> Unit)? = null,
) {
    DenSheet(title = "Around the den", onClose = onClose) {
        Column {
            MenuRow("All Tasks", "The complete, searchable index", { TzIcons.Search(17.dp, Den.rust) }) {
                onClose(); onAllTasks()
            }
            if (onBacklog != null) {
                MenuRow("Backlog", "Someday / maybe — parked, not scheduled", { TzIcons.Moon(17.dp, Den.backlog) }) {
                    onClose(); onBacklog()
                }
            }
            if (onSettings != null) {
                MenuRow("Settings", "Week start, reminders, account", { TzIcons.Bell(17.dp, Den.muted) }) {
                    onClose(); onSettings()
                }
            }
            // FR-NAV-4 — About is unconditionally rendered when a callback is
            // supplied. Placed after Settings so the daily-path ordering holds.
            if (onAbout != null) {
                MenuRow("About", "Producer, implementer, contact", { TzIcons.Info(17.dp, Den.rust) }) {
                    onClose(); onAbout()
                }
            }
        }
    }
}

@Composable
private fun MenuRow(title: String, sub: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Den.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                color = Den.ink,
            )
            Text(
                sub,
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp),
                color = Den.muted,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        TzIcons.Chevron(17.dp, Den.ink.a(0.26f))
    }
}
