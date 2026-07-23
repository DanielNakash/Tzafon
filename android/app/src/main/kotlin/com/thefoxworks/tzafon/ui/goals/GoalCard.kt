package com.thefoxworks.tzafon.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalType
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.ui.components.Bar
import com.thefoxworks.tzafon.ui.components.Dot
import com.thefoxworks.tzafon.ui.components.PillButton
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir
import com.thefoxworks.tzafon.ui.theme.isRtl

/**
 * The design GoalCard (FR-DIR-4): type tag, endowed bar, steps with
 * strikethrough, ENGINE habit chips, near-done "what's left" framing.
 * Shared by the Directions board and any goal list.
 */
@Composable
fun GoalCard(
    goal: Goal,
    engines: List<Habit>,
    accent: Color = Tz.colors.rust,
    servesLabel: String? = null,       // "serves: <theme>" (+N via sharedCount)
    sharedCount: Int = 0,              // FR-DIR-5 "+N" multiplicity
    stale: Boolean = false,
    nearDone: Boolean = false,
    onOpen: () -> Unit,
    onToggleStep: (Int) -> Unit = {},
    onUpdate: () -> Unit = {},
    onComplete: () -> Unit = {},
    onFreeze: () -> Unit = {},
) {
    val pct = goal.pct()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
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
                    goal.type.name,
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.8.sp),
                    color = accent,
                )
            }
            Box(Modifier.weight(1f))
            if (servesLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Dot(accent, 7.dp)
                    Text(
                        "serves: $servesLabel" + if (sharedCount > 0) " +$sharedCount" else "",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                        color = Tz.colors.muted,
                    )
                }
            } else {
                Text(
                    "NO THEME",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                    color = Tz.colors.faint,
                )
            }
        }
        Text(
            goal.title,
            style = TextStyle(fontFamily = DenType.serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.5.sp).contentDir(),
            color = Tz.colors.ink,
            modifier = Modifier.padding(top = 7.dp),
        )

        when (goal.type) {
            GoalType.ACCUMULATIVE -> {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        fmtQty(goal.currentQty),
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                    Text(
                        "${fmtQty(goal.targetQty)} ${goal.unit ?: ""}",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp),
                        color = Tz.colors.muted,
                    )
                }
                Bar(pct = pct.toFloat(), fill = accent)
                if (nearDone && goal.targetQty > goal.currentQty) {
                    Text(
                        "↗ ${fmtQty(goal.targetQty - goal.currentQty)} ${goal.unit ?: ""} to go — nearly there",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                        color = Tz.colors.amber,
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
                        "${goal.steps.count { it.done }} of ${goal.steps.size} steps",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp),
                        color = Tz.colors.muted,
                    )
                    Text(
                        "$pct%",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    goal.steps.forEachIndexed { i, s ->
                        // FR-DESIGN-3.6 — Hebrew/Arabic step labels flip the row's
                        // start/end so the checkbox sits on the trailing (right) edge
                        // and the glyphs anchor to the reading margin.
                        val dir = if (s.label.isRtl()) LayoutDirection.Rtl else LocalLayoutDirection.current
                        CompositionLocalProvider(LocalLayoutDirection provides dir) {
                            Row(
                                Modifier.pressable { onToggleStep(i) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).let {
                                        if (s.done) it.background(accent)
                                        else it.border(1.5.dp, Tz.colors.ink.a(0.28f), RoundedCornerShape(4.dp))
                                    },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (s.done) TzIcons.Check(10.dp, Color.White)
                                }
                                Text(
                                    s.label,
                                    style = TextStyle(
                                        fontFamily = DenType.body, fontSize = 12.sp,
                                        textDecoration = if (s.done) TextDecoration.LineThrough else TextDecoration.None,
                                    ).contentDir(),
                                    color = if (s.done) Tz.colors.muted else Tz.colors.ink,
                                )
                            }
                        }
                    }
                }
            }

            GoalType.GENERIC -> if (goal.description.isNotBlank()) {
                Text(
                    goal.description,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, fontStyle = FontStyle.Italic).contentDir(),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (engines.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 11.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))
                Row(
                    Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        "ENGINE",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp, letterSpacing = 0.5.sp),
                        color = Tz.colors.faint,
                    )
                    engines.forEach { h ->
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp)).background(accent.a(0.1f))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            TzIcons.Repeat(11.dp, accent)
                            Text(
                                h.name,
                                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp).contentDir(),
                                color = accent,
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (goal.type == GoalType.ACCUMULATIVE) PillButton("UPDATE PROGRESS", onClick = onUpdate)
            if (pct >= 100) PillButton("COMPLETE ✓", onClick = onComplete, color = Tz.colors.green)
            if (stale) PillButton("QUIET LATELY — FREEZE?", onClick = onFreeze, color = Tz.colors.frozen)
        }
    }
}

fun fmtQty(v: Double): String =
    if (v % 1.0 == 0.0) "%,d".format(v.toLong()) else v.toString()
