package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.dates.MonthGrid
import com.thefoxworks.tzafon.domain.editor.Armed
import com.thefoxworks.tzafon.domain.editor.CardBody
import com.thefoxworks.tzafon.domain.editor.CardSpot
import com.thefoxworks.tzafon.domain.editor.CardTap
import com.thefoxworks.tzafon.domain.editor.DeadlineKind
import com.thefoxworks.tzafon.domain.editor.RepeatCard
import com.thefoxworks.tzafon.domain.editor.RepeatType
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Recurrence
import com.thefoxworks.tzafon.domain.recurrence.SeriesSpec
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a

/**
 * v2.13.0 — the repeat card under the shared calendar (deck slides 3 and 5):
 * the header with its toggle, the `Weekly · Monthly · Every N days` segment
 * and its body, and the `Ends | Deadline` footer, whose halves swap their
 * settings into the body in place. While the calendar's Deadline tile is
 * armed the card folds to one row, with the callout for one fixed deadline.
 * Every rule comes from [RepeatCard]; this only draws it. The frame owns
 * [body], like [armed], so it outlives tab switches and rotation. [series]
 * is the stored series, which "Switch to offset" reads; null for a new task.
 */
@Composable
internal fun TimingRepeatCard(
    draft: TaskDraft,
    today: String,
    armed: Armed,
    body: CardBody,
    weekStart: String,
    series: SeriesSpec?,
    onArm: (Armed) -> Unit,
    onBody: (CardBody) -> Unit,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    val view = RepeatCard.of(draft, body, deadlineArmed = armed == Armed.DEADLINE)

    // End mode belongs to the Ends body: leaving it hands the calendar back to To do
    fun show(next: CardBody) {
        onBody(next)
        if (next != CardBody.ENDS && armed == Armed.END) onArm(Armed.TODO)
    }
    fun tap(spot: CardSpot) = when (val t = RepeatCard.tap(draft, spot)) {
        is CardTap.Show -> show(t.body)
        CardTap.ArmDeadline -> onArm(Armed.DEADLINE)
        CardTap.Nothing -> Unit
    }
    fun toggle() {
        if (view != null) show(CardBody.RULE)
        onEdit { RepeatCard.toggle(it, today) }
    }

    if (view?.folded != null) {
        Folded(view, onTap = { tap(CardSpot.FOLDED) })
        view.callout?.let { Callout(it) }
        return
    }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
    ) {
        Header(view?.summary ?: "Repeat this task", on = view != null, onToggle = ::toggle)
        if (view == null) return@Column

        Hairline()
        Column(Modifier.padding(vertical = 14.dp)) {
            when (view.body) {
                CardBody.RULE -> RuleBody(view, weekStart, onEdit)
                CardBody.ENDS -> EndsBody(draft, view, armed, onBack = { tap(CardSpot.BACK) }, onArm = onArm, onEdit = onEdit)
                CardBody.DEADLINE -> DeadlineBody(draft, view, today, series, onBack = { tap(CardSpot.BACK) }, onEdit = onEdit)
            }
        }
        Hairline()
        Footer(view, onEnds = { tap(CardSpot.ENDS) }, onDeadline = { tap(CardSpot.DEADLINE) })
    }
}

// ── header and footer ─────────────────────────────────────────

@Composable
private fun Header(summary: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .toggleable(on, remember { MutableInteractionSource() }, null, role = Role.Switch) { onToggle() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TzIcons.Repeat(18.dp, if (on) Tz.colors.rust else Tz.colors.muted)
        Text(summary, style = TextStyle(fontFamily = DenType.body, fontSize = 17.sp), color = Tz.colors.ink, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(width = 48.dp, height = 28.dp).clip(RoundedCornerShape(999.dp))
                .background(if (on) Tz.colors.rust else Tz.colors.ink.a(0.18f)),
        ) {
            Box(
                Modifier.padding(3.dp).size(22.dp).clip(CircleShape).background(Tz.colors.cream)
                    .align(if (on) Alignment.CenterEnd else Alignment.CenterStart),
            )
        }
    }
}

/** `Ends never | Deadline one fixed ›`: each half opens its body; a locked Ends is faint and inert. */
@Composable
private fun Footer(view: RepeatCard.View, onEnds: () -> Unit, onDeadline: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        HalfLabel(view.ends, Tz.colors.ink, Modifier, onEnds)
        Box(Modifier.padding(horizontal = 12.dp).size(width = 1.dp, height = 18.dp).background(Tz.colors.line))
        HalfLabel(view.deadline, Tz.colors.due, Modifier.weight(1f), onDeadline)
        TzIcons.Chevron(16.dp, Tz.colors.ink.a(0.3f))
    }
}

@Composable
private fun HalfLabel(half: RepeatCard.Half, accent: Color, modifier: Modifier, onTap: () -> Unit) {
    val body = TextStyle(fontFamily = DenType.body, fontSize = 15.sp)
    Text(
        buildAnnotatedString {
            append(half.label + " ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = if (half.tappable) accent else Tz.colors.faint)) { append(half.value) }
        },
        style = body,
        color = if (half.tappable) Tz.colors.muted else Tz.colors.faint,
        modifier = modifier
            .then(if (half.tappable) Modifier.pressable(null, Role.Button, onTap) else Modifier.semantics { disabled() })
            .padding(vertical = 12.dp),
    )
}

/** The one-row card while the Deadline tile is armed: the summary, then the deadline's mode. */
@Composable
private fun Folded(view: RepeatCard.View, onTap: () -> Unit) {
    val opens = view.deadlineKind != DeadlineKind.NONE
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(16.dp))
            .then(if (opens) Modifier.pressable(null, Role.Button, onTap) else Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TzIcons.Repeat(18.dp, Tz.colors.rust)
        Text(view.summary, style = TextStyle(fontFamily = DenType.body, fontSize = 17.sp), color = Tz.colors.ink, modifier = Modifier.weight(1f))
        Text(
            view.folded!!.removePrefix(view.summary + " · "),
            style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 1.sp),
            color = Tz.colors.due,
        )
        if (opens) TzIcons.Chevron(16.dp, Tz.colors.ink.a(0.3f))
    }
}

@Composable
private fun Callout(text: String) {
    val bold = "per occurrence"
    val at = text.indexOf(bold)
    Row(
        Modifier.fillMaxWidth().padding(top = 11.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Tz.colors.due.a(0.08f))
            .border(1.dp, Tz.colors.due.a(0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape).background(Tz.colors.due.a(0.6f)))
        Text(
            buildAnnotatedString {
                if (at < 0) append(text) else {
                    append(text.substring(0, at))
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Tz.colors.ink)) { append(bold) }
                    append(text.substring(at + bold.length))
                }
            },
            style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 21.sp),
            color = Tz.colors.muted,
        )
    }
}

// ── the bodies ────────────────────────────────────────────────

@Composable
private fun RuleBody(view: RepeatCard.View, weekStart: String, onEdit: ((TaskDraft) -> TaskDraft) -> Unit) {
    CardSegment(
        listOf(RepeatType.WEEKLY to "Weekly", RepeatType.MONTHLY to "Monthly", RepeatType.EVERY_N to "Every N days"),
        view.type,
    ) { t -> onEdit { RepeatCard.type(it, t) } }

    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (view.type) {
            RepeatType.WEEKLY -> {
                val start = MonthGrid.startIndex(weekStart)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    (0 until 7).map { (start + it) % 7 }.forEach { day ->
                        DayChip(day, on = day in view.weekdays, Modifier.weight(1f)) { onEdit { RepeatCard.weekday(it, day) } }
                    }
                }
            }
            RepeatType.MONTHLY -> {
                val m = view.monthly!!
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice(m.onDate, m.mode == MonthMode.DATE, Modifier.weight(1f)) { onEdit { RepeatCard.monthly(it, MonthMode.DATE) } }
                    Choice(m.onWeekday, m.mode == MonthMode.WEEKDAY, Modifier.weight(1f)) { onEdit { RepeatCard.monthly(it, MonthMode.WEEKDAY) } }
                }
            }
            RepeatType.EVERY_N -> if (view.readOnly) {
                Note("An older rule, kept as it is. Pick Weekly or Monthly to change it.")
            }
        }
        val e = view.every
        CardStepper(
            "every", e.n, e.unit, 1..99,
            less = "Fewer ${e.unit.removeSuffix("s")}s", more = "More ${e.unit.removeSuffix("s")}s",
            enabled = !view.readOnly,
        ) { n -> onEdit { RepeatCard.step(it, n) } }
    }
}

@Composable
private fun EndsBody(
    draft: TaskDraft,
    view: RepeatCard.View,
    armed: Armed,
    onBack: () -> Unit,
    onArm: (Armed) -> Unit,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    val end = draft.recurrence?.endDate
    BackLink(onBack)
    CardSegment(listOf(false to "Never", true to "On a date"), end != null || armed == Armed.END) { onDate ->
        if (onDate) onArm(Armed.END) else {
            onEdit { RepeatCard.ends(it, null) }
            if (armed == Armed.END) onArm(Armed.TODO)
        }
    }
    Note(
        when {
            armed == Armed.END -> "Tap the last day on the calendar above."
            end != null -> "The series stops after ${view.ends.value.removePrefix("on ")}."
            else -> "The series keeps going."
        },
        Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun DeadlineBody(
    draft: TaskDraft,
    view: RepeatCard.View,
    today: String,
    series: SeriesSpec?,
    onBack: () -> Unit,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    BackLink(onBack)
    if (view.deadlineKind == DeadlineKind.CUSTOM_RULE) {
        Text("Custom deadline rule", style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.ink)
        draft.recurrence?.dueRule?.let {
            Text("⚑ " + Recurrence.summary(it), style = TextStyle(fontFamily = DenType.mono, fontSize = 12.5.sp), color = Tz.colors.due, modifier = Modifier.padding(top = 4.dp))
        }
        Note("An older series sets its deadlines by a rule of their own. It keeps working, but can't be edited.", Modifier.padding(top = 8.dp))
        Box(
            Modifier.padding(top = 12.dp)
                .alpha(if (series != null) 1f else 0.4f)
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, Tz.colors.due.a(0.6f), RoundedCornerShape(999.dp))
                .then(
                    if (series != null) Modifier.pressable(null, Role.Button) { onEdit { RepeatCard.switchToOffset(it, series, today) } }
                    else Modifier.semantics { role = Role.Button; disabled() },
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("Switch to offset", style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Tz.colors.due)
        }
        return
    }
    CardSegment(
        listOf(DeadlineKind.FIXED to "One fixed", DeadlineKind.PER_OCCURRENCE to "Per occurrence"),
        view.deadlineKind,
    ) { k -> onEdit { RepeatCard.deadlineKind(it, k) } }
    when (view.deadlineKind) {
        DeadlineKind.PER_OCCURRENCE -> Column(Modifier.padding(top = 12.dp)) {
            CardStepper(
                "+", view.offset ?: 0, if (view.offset == 1) "day after each to-do date" else "days after each to-do date",
                RepeatCard.OFFSET_RANGE, less = "Earlier deadline", more = "Later deadline",
            ) { n -> onEdit { RepeatCard.offset(it, n) } }
        }
        DeadlineKind.FIXED -> Note(
            "One date for the whole series${draft.dueDate?.let { ", ${Dates.WD[Dates.dayOfWeek(it)]} ${Dates.parse(it).dayOfMonth}" } ?: ""}. It's also where the series ends.",
            Modifier.padding(top = 12.dp),
        )
        else -> Unit
    }
}

// ── controls ──────────────────────────────────────────────────

/** The card's segment: a kraft track, the picked option lifted onto the card. */
@Composable
private fun <T> CardSegment(options: List<Pair<T, String>>, value: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Tz.colors.surfaceAlt)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (v, label) ->
            val on = v == value
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Tz.colors.card else Color.Transparent)
                    .border(if (on) 1.dp else 0.dp, if (on) Tz.colors.line else Color.Transparent, RoundedCornerShape(9.dp))
                    .semantics { role = Role.Tab; selected = on }
                    .pressable { if (!on) onSelect(v) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (on) Tz.colors.rust else Tz.colors.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A weekday chip, read by TalkBack as the day's full name. */
@Composable
private fun DayChip(day: Int, on: Boolean, modifier: Modifier, onTap: () -> Unit) {
    Box(
        modifier.aspectRatio(1f)
            .clip(CircleShape)
            .background(if (on) Tz.colors.rust else Color.Transparent)
            .border(1.dp, if (on) Tz.colors.rust else Tz.colors.line, CircleShape)
            .toggleable(on, remember { MutableInteractionSource() }, null, role = Role.Checkbox) { onTap() }
            .semantics { contentDescription = Dates.WD_FULL[day] },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Dates.WD[day].take(1),
            style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
            color = if (on) Tz.colors.cream else Tz.colors.muted,
        )
    }
}

/** Monthly's "On the 17th" / "On the 3rd Thursday". */
@Composable
private fun Choice(label: String, on: Boolean, modifier: Modifier, onTap: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) Tz.colors.rust.a(0.09f) else Color.Transparent)
            .border(if (on) 1.5.dp else 1.dp, if (on) Tz.colors.rust.a(0.6f) else Tz.colors.line, RoundedCornerShape(12.dp))
            .semantics { role = Role.RadioButton; selected = on }
            .pressable { if (!on) onTap() }
            .padding(horizontal = 10.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
            color = if (on) Tz.colors.ink else Tz.colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** `every [–] 2 [+] weeks`: the buttons carry [less] / [more] for TalkBack. */
@Composable
private fun CardStepper(
    lead: String,
    value: Int,
    unit: String,
    range: IntRange,
    less: String,
    more: String,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
) {
    val words = TextStyle(fontFamily = DenType.body, fontSize = 15.sp)
    Row(
        Modifier.alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(lead, style = words, color = Tz.colors.muted)
        Row(
            Modifier.clip(RoundedCornerShape(11.dp)).background(Tz.colors.surfaceAlt),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton("–", less, enabled && value > range.first) { onChange(value - 1) }
            Text(
                "$value",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp),
            )
            StepButton("+", more, enabled && value < range.last) { onChange(value + 1) }
        }
        Text(unit, style = words, color = Tz.colors.muted)
    }
}

@Composable
private fun StepButton(glyph: String, label: String, enabled: Boolean, onTap: () -> Unit) {
    Box(
        Modifier.size(40.dp)
            .semantics { contentDescription = label; if (!enabled) disabled() }
            .pressable(null, Role.Button) { if (enabled) onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold), color = if (enabled) Tz.colors.rust else Tz.colors.faint)
    }
}

@Composable
private fun BackLink(onBack: () -> Unit) {
    Text(
        "‹ Repeat",
        style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
        color = Tz.colors.rust,
        modifier = Modifier.padding(bottom = 12.dp).pressable(null, Role.Button, onBack).padding(vertical = 4.dp),
    )
}

@Composable
private fun Note(text: String, modifier: Modifier = Modifier) {
    Text(text, style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 20.sp), color = Tz.colors.muted, modifier = modifier)
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line))
}
