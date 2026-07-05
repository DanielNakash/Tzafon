package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.R
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/** Ripple-free press — the design's .ft-press feel. */
fun Modifier.pressable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
)

// ── text + label primitives (tz-ui.jsx) ─────────────────────

@Composable
fun Kicker(text: String, color: Color = Den.faint, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = DenType.kicker, color = color, modifier = modifier)
}

@Composable
fun SectionLabel(text: String, color: Color = Den.faint, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = DenType.sectionLabel, color = color, modifier = modifier)
}

/** mono caps label + count + hairline, "18px 0 8px" vertical rhythm. */
@Composable
fun GroupHeader(
    label: String,
    count: Int? = null,
    accent: Color = Den.muted,
    modifier: Modifier = Modifier,
    right: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(label.uppercase(), style = DenType.groupLabel, color = accent)
        if (count != null) {
            Text("$count", style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp), color = Den.faint)
        }
        Box(Modifier.weight(1f).height(1.dp).background(Den.line))
        right?.invoke()
    }
}

// ── progress bar ─────────────────────────────────────────────

@Composable
fun Bar(
    pct: Float,
    fill: Color = Den.amber,
    track: Color = Den.ink.a(0.1f),
    height: Dp = 8.dp,
    radius: Dp = 5.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(radius)).background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(pct.coerceIn(0f, 100f) / 100f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(radius))
                .background(fill)
        )
    }
}

@Composable
fun Dot(color: Color, size: Dp = 7.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/** The Fox Works roundel — bundled logo, circle-clipped, optional ring. */
@Composable
fun FoxLogo(size: Dp = 32.dp, ring: Color? = null, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.foxworks_logo),
        contentDescription = "The Fox Works",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (ring != null) Modifier.border(2.dp, ring, CircleShape) else Modifier),
    )
}

// ── chips ────────────────────────────────────────────────────

@Composable
fun Chip(
    text: String,
    color: Color = Den.muted,
    icon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        icon?.invoke()
        Text(text, style = DenType.chip, color = color, maxLines = 1)
    }
}

@Composable
fun CueChip(text: String, modifier: Modifier = Modifier) {
    Chip(text, color = Den.muted, icon = { TzIcons.Cue(12.dp, Den.amber) }, modifier = modifier)
}

/** "serves: <theme>" — the auto-bridge made visible (PRIN-5). */
@Composable
fun Serves(label: String, accent: Color, plus: Int = 0, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Dot(accent, 7.dp)
        Text("serves", style = DenType.chip, color = Den.faint)
        Text(label, style = DenType.chip.copy(fontWeight = FontWeight.SemiBold), color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (plus > 0) Text("+$plus", style = DenType.chip, color = Den.faint)
    }
}

/** state pill for done/closed/frozen/backlog rows. */
@Composable
fun StateTag(state: TaskState, modifier: Modifier = Modifier) {
    val (label, color) = when (state) {
        TaskState.DONE -> "DONE" to Den.green
        TaskState.CLOSED -> "CLOSED" to Den.closed
        TaskState.FROZEN -> "FROZEN" to Den.frozen
        TaskState.BACKLOG -> "SOMEDAY" to Den.backlog
        TaskState.OPEN -> return
    }
    Box(
        modifier
            .border(1.dp, color.a(0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(label, style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.6.sp), color = color)
    }
}

/** "DUE" / "DUE TODAY" red pills (v1.1.0 due indicators). */
@Composable
fun DuePill(today: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(4.dp)).background(Den.due).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (today) TzIcons.Alert(10.dp, Color.White)
        Text(
            if (today) "DUE TODAY" else "DUE",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = Color.White,
        )
    }
}

/** DEADLINE outline pill — To Do date is also the due date. */
@Composable
fun DeadlinePill(modifier: Modifier = Modifier) {
    Row(
        modifier
            .border(1.dp, Den.due.a(0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TzIcons.Flag(11.dp, Den.due)
        Text("DEADLINE", style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 0.3.sp), color = Den.due)
    }
}

// ── the reusable task checkbox (state-aware) ─────────────────

@Composable
fun TaskCheckbox(state: TaskState, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val m = modifier
        .size(25.dp)
        .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
    val shape = RoundedCornerShape(7.dp)
    when (state) {
        TaskState.DONE -> Box(m.clip(shape).background(Den.rust), contentAlignment = Alignment.Center) {
            TzIcons.Check(15.dp, Color.White)
        }
        TaskState.CLOSED -> Box(m.border(2.dp, Den.closed.a(0.5f), shape), contentAlignment = Alignment.Center) {
            TzIcons.Skip(14.dp, Den.closed)
        }
        TaskState.FROZEN -> Box(m.clip(shape).background(Den.frozen.a(0.12f)).border(2.dp, Den.frozen.a(0.5f), shape), contentAlignment = Alignment.Center) {
            TzIcons.Frozen(13.dp, Den.frozen)
        }
        TaskState.BACKLOG -> Box(m.border(2.dp, Den.backlog.a(0.5f), shape), contentAlignment = Alignment.Center) {
            TzIcons.Moon(13.dp, Den.backlog)
        }
        TaskState.OPEN -> Box(m.border(2.dp, Den.ink.a(0.3f), shape))
    }
}

// ── cards, banners, FAB ──────────────────────────────────────

@Composable
fun DenCard(
    modifier: Modifier = Modifier,
    pad: Dp = 16.dp,
    background: Color = Den.card,
    border: Color = Den.line,
    radius: Dp = 15.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(radius))
            .padding(pad),
        content = content,
    )
}

@Composable
fun Fab(
    onClick: () -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Den.rust)
            .pressable(onClick)
            .padding(horizontal = if (label != null) 18.dp else 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TzIcons.Plus(25.dp, Color.White)
        if (label != null) {
            Text(label, style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Color.White)
        }
    }
}
