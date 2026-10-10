package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.GoalState
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.ui.components.DenSheet
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a

/**
 * v2.13.0 — the Alignment pane: the habit and goal links, then the cue. Each
 * row opens its sheet ([HabitSheet], [GoalsSheet], `CueSheet`), hosted by the
 * editor frame. Emits into the editor's scrolling column.
 */
@Composable
internal fun AlignmentPane(
    draft: TaskDraft,
    habits: List<Habit>,
    goals: List<Goal>,
    onHabit: () -> Unit,
    onGoals: () -> Unit,
    onCue: () -> Unit,
) {
    // FR-EDITOR-1 — Serves before Cue: alignment (goal/habit) is a more
    // identity-shaping choice than the trigger, so it comes first.
    // ── serves (M4: the habit link; themes M6, goals M5) ──
    EdSection("Serves · alignment (optional)")
    val linkedHabit = habits.firstOrNull { it.id == draft.habitId }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .let {
                if (linkedHabit != null) it.background(Tz.colors.card).border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                else it.border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
            }
            .pressable(onHabit)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (linkedHabit != null) {
            TzIcons.Repeat(14.dp, Tz.colors.green)
            Text(
                linkedHabit.name,
                style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp),
                color = Tz.colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                "HABIT",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                color = Tz.colors.faint,
            )
        } else {
            TzIcons.Plus(14.dp, Tz.colors.muted)
            Text(
                "Link a habit — done ticks it",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                color = Tz.colors.muted,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // goal links (M5) — many allowed, one typical
    val linkedGoals = goals.filter { it.id in draft.goalIds }
    Row(
        Modifier.fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .let {
                if (linkedGoals.isNotEmpty()) it.background(Tz.colors.card).border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
                else it.border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
            }
            .pressable(onGoals)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (linkedGoals.isNotEmpty()) {
            TzIcons.Target(14.dp, Tz.colors.rust)
            Text(
                linkedGoals.joinToString(" · ") { it.title },
                style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp),
                color = Tz.colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (linkedGoals.size > 1) "GOALS +${linkedGoals.size - 1}" else "GOAL",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                color = Tz.colors.faint,
            )
        } else {
            TzIcons.Plus(14.dp, Tz.colors.muted)
            Text(
                "Link a goal — done moves it",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp),
                color = Tz.colors.muted,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // ── cue (DM-TASK-4: "When will you do this?") ──
    EdSection("When will you do this? · cue")
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (draft.cue != null) Tz.colors.amber.a(0.1f) else Tz.colors.card)
            .border(1.dp, if (draft.cue != null) Tz.colors.amber.a(0.4f) else Tz.colors.line, RoundedCornerShape(13.dp))
            .pressable(onCue)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        TzIcons.Cue(17.dp, if (draft.cue != null) Tz.colors.rust else Tz.colors.faint)
        Column(Modifier.weight(1f)) {
            Text(
                draft.cue?.display() ?: "Anchor it to a routine — optional",
                style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                color = if (draft.cue != null) Tz.colors.ink else Tz.colors.faint,
            )
            Text(
                draft.cue?.let { "${it.type.name.replace('_', '-')} · A TRIGGER BEATS A CLOCK" }
                    ?: "WHEN X, I WILL DO Y",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp),
                color = Tz.colors.faint,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        TzIcons.Chevron(17.dp, Tz.colors.ink.a(0.28f))
    }
}

/** The habit link's sheet: one habit at most (DM-REL); picking or clearing closes it. */
@Composable
internal fun HabitSheet(
    draft: TaskDraft,
    habits: List<Habit>,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
    onClose: () -> Unit,
) {
    DenSheet("Serves a habit", onClose = onClose) {
        Column {
            Text(
                "Done ticks the habit for the day — counted once, reversed exactly if you undo.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
            )
            if (habits.isEmpty()) {
                Text(
                    "No habits yet — start one in the Habits tab first.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
            habits.forEach { h ->
                Row(
                    Modifier.fillMaxWidth()
                        .pressable { onEdit { it.copy(habitId = h.id) }; onClose() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    TzIcons.Repeat(15.dp, Tz.colors.green)
                    Text(
                        h.name,
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                        color = Tz.colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (draft.habitId == h.id) TzIcons.Check(15.dp, Tz.colors.rust, 2.6f)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))
            }
            if (draft.habitId != null) {
                Row(
                    Modifier.fillMaxWidth()
                        .pressable { onEdit { it.copy(habitId = null) }; onClose() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    TzIcons.X(14.dp, Tz.colors.muted)
                    Text(
                        "No habit for this one",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp),
                        color = Tz.colors.muted,
                    )
                }
            }
        }
    }
}

/** The goal links' sheet: ongoing goals toggle in and out; it stays open. */
@Composable
internal fun GoalsSheet(
    draft: TaskDraft,
    goals: List<Goal>,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
    onClose: () -> Unit,
) {
    DenSheet("Serves goals", onClose = onClose) {
        Column {
            Text(
                "Done moves each linked goal — once, and exactly reversed if you undo. One is plenty.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
            )
            if (goals.isEmpty()) {
                Text(
                    "No goals yet — aim at one in the Directions tab first.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
                    color = Tz.colors.muted,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
            goals.filter { it.state == GoalState.ONGOING }.forEach { g ->
                val on = g.id in draft.goalIds
                Row(
                    Modifier.fillMaxWidth()
                        .pressable {
                            onEdit { it.copy(
                                goalIds = if (on) draft.goalIds - g.id else draft.goalIds + g.id,
                            ) }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    TzIcons.Target(15.dp, if (on) Tz.colors.rust else Tz.colors.muted)
                    Text(
                        g.title,
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                        color = Tz.colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (on) TzIcons.Check(15.dp, Tz.colors.rust, 2.6f)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))
            }
        }
    }
}
