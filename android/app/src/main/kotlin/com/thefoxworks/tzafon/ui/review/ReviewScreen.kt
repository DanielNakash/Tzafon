package com.thefoxworks.tzafon.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.ui.components.Bar
import com.thefoxworks.tzafon.ui.components.Compass
import com.thefoxworks.tzafon.ui.components.Dot
import com.thefoxworks.tzafon.ui.components.Kicker
import com.thefoxworks.tzafon.ui.components.Nudge
import com.thefoxworks.tzafon.ui.components.PillButton
import com.thefoxworks.tzafon.ui.components.SectionLabel
import com.thefoxworks.tzafon.ui.components.Serves
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * The Review loop (DM-REVIEW, design: ReviewReflect/ReviewPlan) — two
 * halves: a no-score mirror, then a fresh start. Never blocking:
 * closeable in one tap, resumable with progress kept.
 */
@Composable
fun ReviewScreen(
    vm: ReviewViewModel,
    onClose: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(1) }
    var note by remember { mutableStateOf("") }

    fun leave() {
        if (step == 2 || note.isNotBlank()) vm.savePartial(note)
        onClose()
    }

    Column(Modifier.fillMaxSize().background(Den.surface)) {
        // ── header (design: ReviewHeader) ──
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Kicker(state.kicker, color = Den.rust)
                    Text(
                        if (step == 1) "Your week, reflected" else "Plan the week",
                        style = DenType.h1Compact,
                        color = Den.ink,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(Den.card)
                        .border(1.dp, Den.line, CircleShape)
                        .pressable { leave() },
                    contentAlignment = Alignment.Center,
                ) { TzIcons.X(15.dp, Den.muted) }
            }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                StepDot(1, "REFLECT", step)
                Box(Modifier.weight(1f).height(1.dp).background(Den.line).padding(horizontal = 7.dp))
                StepDot(2, "PLAN", step)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Den.line))

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            if (step == 1) {
                ReflectStep(state, note, onNote = { note = it })
            } else {
                PlanStep(state, onTogglePick = { vm.togglePick(it) }, onFreeze = { vm.freezeGoal(it) })
            }
        }

        // ── footer ──
        Row(
            Modifier
                .fillMaxWidth()
                .background(Den.surface.a(0.96f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (step == 1) {
                Box(
                    Modifier.height(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, Den.line, RoundedCornerShape(13.dp))
                        .pressable { leave() }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Later",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Den.muted,
                    )
                }
                Row(
                    Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Den.rust)
                        .pressable { step = 2 },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Now, plan the week",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    TzIcons.Chevron(17.dp, Color.White, modifier = Modifier.padding(start = 6.dp))
                }
            } else {
                Row(
                    Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Den.rust)
                        .pressable { vm.complete(note); onClose() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Start the week",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDot(n: Int, label: String, current: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .background(if (n <= current) Den.rust else Den.surfaceAlt)
                .border(1.dp, if (n <= current) Color.Transparent else Den.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$n",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = if (n <= current) Color.White else Den.faint,
            )
        }
        Text(
            label,
            style = TextStyle(
                fontFamily = DenType.mono, fontSize = 11.sp, letterSpacing = 0.4.sp,
                fontWeight = if (n == current) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (n == current) Den.ink else Den.faint,
        )
    }
}

@Composable
private fun ReflectStep(state: ReviewUiState, note: String, onNote: (String) -> Unit) {
    Text(
        buildAnnotatedString {
            append("You finished ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${state.doneCount} thing${if (state.doneCount == 1) "" else "s"}") }
            append(" this week. ")
            withStyle(SpanStyle(color = Den.rust)) { append("${state.alignedCount} of them") }
            append(" served a direction you chose.")
        },
        style = TextStyle(fontFamily = DenType.serif, fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 25.5.sp),
        color = Den.ink,
        modifier = Modifier.padding(top = 16.dp),
    )

    if (state.habitRates.isNotEmpty()) {
        SectionLabel("Your habits, honestly", modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.habitRates.forEach { r ->
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Den.card)
                        .border(1.dp, Den.line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dot(Den.green, 8.dp)
                        Text(
                            r.name,
                            style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                            color = Den.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            r.quantLine ?: "${r.done} of ${r.target}",
                            style = TextStyle(fontFamily = DenType.mono, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = Den.green,
                        )
                    }
                    Box(Modifier.padding(top = 8.dp)) { Bar(pct = r.pct.toFloat(), fill = Den.green, height = 6.dp) }
                }
            }
        }
        Text(
            "Missed a few? Normal. The rate has slack built in — that's the point.",
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, lineHeight = 18.5.sp),
            color = Den.muted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }

    if (state.goalDeltas.isNotEmpty()) {
        SectionLabel("Goals that moved", modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.goalDeltas.forEach { d ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TzIcons.Target(16.dp, Den.rust)
                    Text(
                        d.title,
                        style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                        color = Den.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        d.delta,
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.5.sp),
                        color = Den.rust,
                    )
                }
            }
        }
    }

    Column(
        Modifier.fillMaxWidth()
            .padding(top = 18.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Den.card)
            .border(1.dp, Den.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SectionLabel("A line for yourself · optional")
        BasicTextField(
            value = note,
            onValueChange = onNote,
            textStyle = TextStyle(fontFamily = DenType.serif, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = Den.ink),
            cursorBrush = SolidColor(Den.rust),
            decorationBox = { inner ->
                Box(Modifier.padding(top = 6.dp)) {
                    if (note.isEmpty()) {
                        Text(
                            "What mattered this week…",
                            style = TextStyle(fontFamily = DenType.serif, fontSize = 15.sp, fontStyle = FontStyle.Italic),
                            color = Den.faint,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlanStep(state: ReviewUiState, onTogglePick: (String) -> Unit, onFreeze: (com.thefoxworks.tzafon.domain.model.Goal) -> Unit) {
    Text(
        "Clean slate. Pick a few things worth pointing at this week.",
        style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 24.5.sp),
        color = Den.ink,
        modifier = Modifier.padding(top = 16.dp),
    )

    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Compass(size = 16.dp, ring = Den.rust, needleN = Den.rust, needleS = Den.faint, stroke = 2f)
        SectionLabel("This week's priorities", color = Den.rust)
        Box(Modifier.weight(1f).height(1.dp).background(Den.line))
        val n = state.picked.size
        Text(
            when {
                n <= 3 -> "$n OF 3 · A GOOD NUMBER"
                else -> "$n PICKED · STILL YOUR CALL"
            },
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
            color = if (n <= 3) Den.green else Den.amber, // DM-FOCUS-2 soft, never a block
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        state.candidates.forEach { t ->
            val on = t.id in state.picked
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) Den.card else Color.Transparent)
                    .border(1.dp, if (on) Den.rust.a(0.3f) else Den.line, RoundedCornerShape(12.dp))
                    .pressable { onTogglePick(t.id) }
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).let {
                        if (on) it.background(Den.rust)
                        else it.border(2.dp, Den.ink.a(0.25f), RoundedCornerShape(7.dp))
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) {
                        Compass(size = 15.dp, ring = Color.White, needleN = Color.White, needleS = Color.White.a(0.5f), stroke = 2f)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title,
                        style = TextStyle(
                            fontFamily = DenType.body, fontSize = 14.5.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (on) Den.ink else Den.muted,
                    )
                    state.servesByTask[t.id]?.let { serves ->
                        Box(Modifier.padding(top = 3.dp)) { Serves(serves, Den.rust) }
                    }
                }
            }
        }
        if (state.candidates.isEmpty()) {
            Text(
                "Nothing on the slate yet — capture a task or two first, then point the week at them.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.sp),
                color = Den.muted,
            )
        }
    }

    state.staleGoal?.let { g ->
        Box(Modifier.padding(top = 16.dp)) {
            Nudge(
                text = "“${g.title}” hasn't moved in a while. Freeze it for now? You can thaw it any time — no harm done.",
                actions = {
                    PillButton("FREEZE IT", onClick = { onFreeze(g) }, color = Den.frozen)
                },
            )
        }
    }
}
