package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.editor.AlignmentLists
import com.thefoxworks.tzafon.domain.editor.CueDraft
import com.thefoxworks.tzafon.domain.editor.CueSuggestions
import com.thefoxworks.tzafon.domain.editor.InheritedTheme
import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType
import com.thefoxworks.tzafon.domain.model.Goal
import com.thefoxworks.tzafon.domain.model.Habit
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.model.Theme
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a
import com.thefoxworks.tzafon.ui.theme.contentDir

/**
 * v2.13.0 — the Alignment pane (FR-EDITOR-6, slide 3 right): the cue edited
 * in place, the one habit the task ticks, the goals it moves, and the theme
 * it inherits from them, read-only. The cue as typed ([cue]) belongs to the
 * frame, which checks it at Save; every other edit goes through [onEdit].
 * Emits into the editor's scrolling column.
 */
@Composable
internal fun AlignmentPane(
    draft: TaskDraft,
    /** the goals linked when the editor opened: an unticked non-ongoing one stays until Save */
    openedWithGoals: List<String>,
    today: String,
    habits: List<Habit>,
    goals: List<Goal>,
    themes: List<Theme>,
    /** every cue in use, for the suggestion chips ([CueSuggestions.uses]) */
    cueUses: List<Cue>,
    cue: CueDraft,
    /** Save stopped on this cue's time: the field is marked incomplete */
    timeFlagged: Boolean,
    timeFocus: FocusRequester,
    onCue: (CueDraft) -> Unit,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    // FR-EDITOR-6.1 — the deck's order, which the owner chose over v2.11.0's
    // alignment-first request (FR-2026-07-27-b) when building #28
    EdSection("Cue · when X, I will do Y")
    CueCard(cue, CueSuggestions.chips(cue.type, cueUses), timeFlagged, timeFocus, onCue)

    EdSection("Ticks a habit · one")
    val habitRows = AlignmentLists.habits(habits, draft.habitId)
    if (habitRows.isEmpty()) {
        EmptyLine("No habits yet — start one in the Habits tab.")
    } else {
        ListCard {
            habitRows.forEachIndexed { i, h ->
                if (i > 0) Hairline()
                LinkRow(
                    title = h.name,
                    subtitle = h.cadence,
                    icon = { TzIcons.Repeat(16.dp, if (h.selected) Tz.colors.green else Tz.colors.muted) },
                    modifier = Modifier.selectable(
                        selected = h.selected,
                        role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onEdit { it.copy(habitId = AlignmentLists.tapHabit(it.habitId, h.id)) } },
                    ),
                ) { Radio(h.selected) }
            }
        }
    }

    EdSection("Moves a goal · any")
    val goalRows = AlignmentLists.goals(goals, draft.goalIds, openedWithGoals, today)
    if (goalRows.isEmpty()) {
        EmptyLine("No goals yet — aim at one in the Directions tab.")
    } else {
        ListCard {
            goalRows.forEachIndexed { i, g ->
                if (i > 0) Hairline()
                val accent = goals.firstOrNull { it.id == g.id }?.primaryThemeId
                    ?.let { id -> themes.firstOrNull { it.id == id } }?.let { accentFor(it) } ?: Tz.colors.rust
                LinkRow(
                    title = g.title,
                    subtitle = g.subtitle,
                    icon = { TzIcons.Target(16.dp, if (g.linked) accent else Tz.colors.muted) },
                    modifier = Modifier.toggleable(
                        value = g.linked,
                        role = Role.Checkbox,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onValueChange = { onEdit { it.copy(goalIds = AlignmentLists.tapGoal(it.goalIds, g.id)) } },
                    ),
                ) { Tick(g.linked, accent) }
            }
        }
    }

    ThemeLine(InheritedTheme.of(draft, goals, habits, themes), themes)
}

// ── the cue card ──

@Composable
private fun CueCard(
    cue: CueDraft,
    chips: List<String>,
    timeFlagged: Boolean,
    timeFocus: FocusRequester,
    onCue: (CueDraft) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CueTypeSegment(cue.type) { onCue(cue.select(it)) }

        val filled = cue.label.isNotEmpty() || cue.time.isNotEmpty()
        CueField(
            value = cue.label,
            onChange = { onCue(cue.copy(label = it)) },
            placeholder = when (cue.type) {
                CueType.AFTER_ROUTINE -> "After…"
                CueType.AT_TIME -> "Label · optional"
                CueType.AT_PLACE -> "At…"
            },
            description = "Cue",
            filled = filled,
            leading = {
                val tint = if (filled) Tz.colors.rust else Tz.colors.faint
                when (cue.type) {
                    CueType.AFTER_ROUTINE -> TzIcons.Coffee(17.dp, tint)
                    CueType.AT_TIME -> TzIcons.Clock(17.dp, tint)
                    CueType.AT_PLACE -> TzIcons.Cue(17.dp, tint)
                }
            },
            trailing = {
                // × clears the cue (the type stays); ✎ only hints the field is editable
                if (filled) Box(Modifier.pressable("Clear cue", Role.Button) { onCue(cue.clear()) }.padding(4.dp)
                    .semantics { contentDescription = "Clear cue" }) { TzIcons.X(15.dp, Tz.colors.muted) }
                else TzIcons.Pencil(15.dp, Tz.colors.faint)
            },
        )

        if (cue.showsTime) TimeField(cue, timeFlagged, timeFocus, onCue)

        if (chips.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { chip ->
                    Text(
                        chip,
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 12.sp).contentDir(),
                        color = Tz.colors.muted,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, Tz.colors.line, CircleShape)
                            .pressable(null, Role.Button) {
                                onCue(if (cue.type == CueType.AT_TIME) cue.copy(time = chip) else cue.copy(label = chip))
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CueTypeSegment(selected: CueType, onSelect: (CueType) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Tz.colors.surfaceAlt)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        listOf(CueType.AFTER_ROUTINE to "After a routine", CueType.AT_TIME to "At a time", CueType.AT_PLACE to "At a place")
            .forEach { (type, label) ->
                val on = type == selected
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (on) Tz.colors.card else Color.Transparent)
                        .selectable(
                            selected = on,
                            role = Role.RadioButton,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(type) },
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (on) Tz.colors.rust else Tz.colors.muted,
                        maxLines = 1,
                    )
                }
            }
    }
}

/** The inline `HH:mm` field: typed, no modal picker. Required under At a time, optional after a routine. */
@Composable
private fun TimeField(cue: CueDraft, flagged: Boolean, focus: FocusRequester, onCue: (CueDraft) -> Unit) {
    val incomplete = flagged && cue.timeIncomplete
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (cue.type == CueType.AT_TIME) "TIME" else "TIME · OPTIONAL",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 1.sp),
            color = Tz.colors.faint,
            modifier = Modifier.padding(start = 4.dp),
        )
        BasicTextField(
            value = cue.time,
            onValueChange = { onCue(cue.copy(time = it.take(5))) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            textStyle = TextStyle(fontFamily = DenType.mono, fontSize = 16.sp, color = Tz.colors.ink),
            cursorBrush = SolidColor(Tz.colors.rust),
            decorationBox = { inner ->
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (incomplete) Tz.colors.due.a(0.08f) else Tz.colors.bg)
                        .border(1.dp, if (incomplete) Tz.colors.due else Tz.colors.line, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    if (cue.time.isEmpty()) Text("HH:mm", style = TextStyle(fontFamily = DenType.mono, fontSize = 16.sp), color = Tz.colors.faint)
                    inner()
                }
            },
            modifier = Modifier.width(96.dp).focusRequester(focus).semantics { contentDescription = "Cue time" },
        )
    }
    if (incomplete) {
        Text(
            "Use HH:mm, like 07:30",
            style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp),
            color = Tz.colors.due,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun CueField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    description: String,
    filled: Boolean,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    // FR-DESIGN-3 — a user-authored label renders RTL by its content
    val style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, color = Tz.colors.ink).contentDir()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(Tz.colors.rust),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (filled) Tz.colors.amber.a(0.1f) else Tz.colors.bg)
                    .border(1.dp, if (filled) Tz.colors.amber.a(0.45f) else Tz.colors.line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                leading()
                // the field fills its slot, so a right-to-left label sits at the right edge
                Box(Modifier.weight(1f), propagateMinConstraints = true) {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = Tz.colors.faint))
                    inner()
                }
                trailing()
            }
        },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
    )
}

// ── the habit and goal lists ──

@Composable
private fun ListCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp)
            .selectableGroup(),
    ) { content() }
}

/** One habit or goal: TalkBack reads the title, the subtitle and the selected / ticked state as one stop. */
@Composable
private fun LinkRow(
    title: String,
    subtitle: String?,
    icon: @Composable () -> Unit,
    modifier: Modifier,
    control: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp).contentDir(),
                color = Tz.colors.ink,
            )
            if (subtitle != null) Text(
                subtitle,
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.6.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        control()
    }
}

@Composable
private fun Radio(on: Boolean) {
    Box(
        Modifier.size(24.dp).clip(CircleShape)
            .background(if (on) Tz.colors.green else Color.Transparent)
            .border(1.5.dp, if (on) Tz.colors.green else Tz.colors.faint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Box(Modifier.size(9.dp).clip(CircleShape).background(Tz.colors.card))
    }
}

@Composable
private fun Tick(on: Boolean, accent: Color) {
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
            .background(if (on) accent else Color.Transparent)
            .border(1.5.dp, if (on) accent else Tz.colors.faint, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (on) TzIcons.Check(14.dp, Tz.colors.card, 2.6f)
    }
}

@Composable
private fun Hairline() = Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))

@Composable
private fun EmptyLine(text: String) = Text(
    text,
    style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp),
    color = Tz.colors.muted,
    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
)

// ── the inherited theme ──

/** FR-EDITOR-6.8 — read-only; TalkBack reads it as one sentence. */
@Composable
private fun ThemeLine(theme: InheritedTheme, themes: List<Theme>) {
    val first = theme.names.firstOrNull()?.let { n -> themes.firstOrNull { it.name == n } }
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, start = 4.dp)
            .clearAndSetSemantics { contentDescription = theme.sentence },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape)
                .background(first?.let { accentFor(it) } ?: Tz.colors.faint),
        )
        val body = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 21.sp)
        if (theme.names.isEmpty()) {
            Text(InheritedTheme.NONE, style = body, color = Tz.colors.faint)
        } else {
            Text(
                buildAnnotatedString {
                    append(if (theme.names.size == 1) "Theme " else "Themes ")
                    theme.names.forEachIndexed { i, n ->
                        if (i > 0) append(" · ")
                        val accent = themes.firstOrNull { it.name == n }?.let { accentFor(it) } ?: Tz.colors.ink
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = accent)) { append(n) }
                    }
                    append(" — inherited from ${theme.source}")
                },
                style = body.contentDir(),
                color = Tz.colors.muted,
            )
        }
    }
}

@Composable
private fun accentFor(theme: Theme): Color = Tz.colors.themeAccents[theme.accentSlot % 3]
