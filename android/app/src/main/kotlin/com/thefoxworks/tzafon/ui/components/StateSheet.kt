package com.thefoxworks.tzafon.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.model.StateMachine
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * "Where does this stand?" — the task state sheet (design: StateSheet).
 * Handles the two guards inline: FR-REC-5 freeze-terminates-series confirm,
 * and FR-BACKLOG-2 recurring-can't-backlog (offers Frozen instead).
 */
@Composable
fun StateSheet(
    current: TaskState,
    isRecurring: Boolean,
    onPick: (TaskState) -> Unit,
    onClose: () -> Unit,
    /** open straight on a guard pane (e.g. Planning's Someday on a series) */
    initialGuard: StateMachine.Guard? = null,
) {
    var guard by remember { mutableStateOf(initialGuard) }

    DenSheet(title = "Where does this stand?", onClose = onClose) {
        Column {
            Text(
                "Every state is reversible. Nothing here is a failure.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp),
                color = Tz.colors.muted,
                modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
            )

            when (val g = guard) {
                null -> StateList(current, isRecurring) { target ->
                    val needed = StateMachine.guardFor(current, target, isRecurring)
                    if (needed == null) {
                        onPick(target); onClose()
                    } else {
                        guard = needed
                    }
                }

                StateMachine.Guard.ConfirmFreezeSeries -> GuardPane(
                    icon = { TzIcons.Frozen(18.dp, Tz.colors.frozen) },
                    title = "Freeze the whole series?",
                    body = "This task repeats. Freezing puts the series on ice — no new occurrences until you thaw it. This one stays right here, waiting.",
                    confirmLabel = "Freeze the series",
                    confirmColor = Tz.colors.frozen,
                    onConfirm = { onPick(TaskState.FROZEN); onClose() },
                    onBack = { guard = null },
                )

                StateMachine.Guard.RecurringCannotBacklog -> GuardPane(
                    icon = { TzIcons.Moon(18.dp, Tz.colors.backlog) },
                    title = "A repeating task can't be someday",
                    body = "A live series keeps its rhythm. To park it, freeze it instead — same shelf, easy to thaw.",
                    confirmLabel = "Freeze it instead",
                    confirmColor = Tz.colors.frozen,
                    onConfirm = {
                        val needed = StateMachine.guardFor(current, TaskState.FROZEN, isRecurring)
                        if (needed == null) { onPick(TaskState.FROZEN); onClose() } else guard = needed
                    },
                    onBack = { guard = null },
                )
            }
        }
    }
}

@Composable
private fun StateList(current: TaskState, isRecurring: Boolean, onSelect: (TaskState) -> Unit) {
    val states = listOf(
        Triple(TaskState.OPEN, "Open", "Active and pending — on your plate now."),
        Triple(TaskState.DONE, "Done", "Finished. Ticks the habit, moves the goal, files into Journey."),
        Triple(TaskState.CLOSED, "Closed", "Not done, no longer relevant. A calm skip — no penalty, links kept."),
        Triple(TaskState.FROZEN, "Frozen", "On ice for now. Thaw it any time, right where you left it."),
        Triple(TaskState.BACKLOG, "Someday", "Parked in the backlog. Out of Today — pull it when it calls."),
    )
    Column {
        states.forEachIndexed { i, (state, name, desc) ->
            Column(Modifier.fillMaxWidth().pressable { if (state != current) onSelect(state) }) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    TaskCheckbox(state)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                name,
                                style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                                color = Tz.colors.ink,
                            )
                            if (state == current) {
                                Box(
                                    Modifier.border(1.dp, Tz.colors.rust.a(0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        "CURRENT",
                                        style = TextStyle(fontFamily = DenType.mono, fontSize = 9.5.sp),
                                        color = Tz.colors.rust,
                                    )
                                }
                            }
                        }
                        Text(
                            desc,
                            style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 17.sp),
                            color = Tz.colors.muted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (i < states.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Tz.colors.line2))
            }
        }
    }
}

@Composable
private fun GuardPane(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            icon()
            Text(
                title,
                style = TextStyle(fontFamily = DenType.serif, fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.ink,
            )
        }
        Text(
            body,
            style = TextStyle(fontFamily = DenType.body, fontSize = 14.sp, lineHeight = 21.sp),
            color = Tz.colors.muted,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Row(
            Modifier.fillMaxWidth().height(50.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(confirmColor)
                .pressable(onConfirm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                confirmLabel,
                style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).height(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.dp, Tz.colors.line, RoundedCornerShape(13.dp))
                .pressable(onBack),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "Back",
                style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = Tz.colors.muted,
            )
        }
    }
}
