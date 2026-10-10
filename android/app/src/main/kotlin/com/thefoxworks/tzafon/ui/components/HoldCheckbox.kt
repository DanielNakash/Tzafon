package com.thefoxworks.tzafon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.theme.Tz
import kotlinx.coroutines.launch

/** v2.13.0 — how long the checkbox is held before the status sheet rises. */
const val HOLD_MILLIS = 350L

private enum class Press { TAP, CANCEL }

/**
 * The list-row checkbox (v2.13.0 hold-to-choose, NFR-A11Y-1). A press
 * released before [HOLD_MILLIS] is a tap. Holding to the threshold gives one
 * light haptic tick, scales the box to 1.16 and raises the sheet ([onHold])
 * with the finger still down, after a ripple has grown from the box. Moving
 * past touch slop cancels both, so the list scrolls instead.
 *
 * The box consumes its own down and up, so a row's long-press (Also today's
 * reorder) or tap never starts from it. TalkBack reads "<title>, <status>"
 * as a button; double-tap-and-hold and a "Change status" action open the sheet.
 */
@Composable
internal fun HoldCheckbox(
    state: TaskState,
    title: String?,
    onClick: () -> Unit,
    onHold: (() -> Unit)?,
    modifier: Modifier = Modifier,
    visual: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val click by rememberUpdatedState(onClick)
    val hold by rememberUpdatedState(onHold)
    var held by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (held) 1.16f else 1f, label = "hold-scale")
    val ripple = remember { Animatable(0f) }
    val rippleColor = Tz.colors.rust
    val presses = LocalCheckboxPresses.current

    Box(
        modifier
            .size(25.dp)
            .wrapContentSize(Alignment.Center, unbounded = true)
            .size(48.dp)
            .drawBehind {
                val r = ripple.value
                if (r > 0f) drawCircle(rippleColor.copy(alpha = 0.18f * r), radius = size.minDimension / 2f * r)
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    presses?.claim(down.id)
                    val grow = scope.launch {
                        ripple.snapTo(0f)
                        ripple.animateTo(1f, tween(HOLD_MILLIS.toInt(), easing = LinearEasing))
                    }
                    val press = withTimeoutOrNull(HOLD_MILLIS) { awaitTapOrCancel(down) }
                    when (press) {
                        Press.TAP -> click()
                        Press.CANCEL -> {}
                        null -> {
                            // the threshold: the sheet rises with the finger still down
                            val h = hold
                            held = true
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            h?.invoke()
                            val released = awaitTapOrCancel(down, slop = Float.MAX_VALUE)
                            if (h == null && released == Press.TAP) click()
                        }
                    }
                    held = false
                    grow.cancel()
                    scope.launch { ripple.animateTo(0f, tween(150)) }
                }
            }
            .semantics(mergeDescendants = true) {
                contentDescription = title ?: "Task"
                stateDescription = statusName(state)
                role = Role.Button
                onClick(label = tapHint(state)) { click(); true }
                hold?.let { h ->
                    onLongClick(label = "change status") { h(); true }
                    customActions = listOf(CustomAccessibilityAction("Change status") { h(); true })
                }
            }
            .onKeyEvent { e ->
                val activate = e.key == Key.Enter || e.key == Key.NumPadEnter || e.key == Key.Spacebar
                if (activate && e.type == KeyEventType.KeyUp) { click(); true } else false
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) { visual() }
    }
}

/**
 * Waits for the press to end: [Press.TAP] when the finger lifts, consuming
 * the up; [Press.CANCEL] when it moves past [slop] or something else (a
 * scroll) takes the pointer.
 */
private suspend fun AwaitPointerEventScope.awaitTapOrCancel(
    down: PointerInputChange,
    slop: Float = viewConfiguration.touchSlop,
): Press {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return Press.CANCEL
        if (change.changedToUpIgnoreConsumed()) {
            change.consume()
            return Press.TAP
        }
        if (change.isConsumed || (change.position - down.position).getDistance() > slop) return Press.CANCEL
    }
}

/** The status as TalkBack reads it after the title. */
fun statusName(state: TaskState): String = when (state) {
    TaskState.OPEN -> "Open"
    TaskState.DONE -> "Done"
    TaskState.CLOSED -> "Closed"
    TaskState.FROZEN -> "Frozen"
    TaskState.BACKLOG -> "Backlog"
}

/** TalkBack's "Double-tap to …" hint, by what the tap rule does. */
private fun tapHint(state: TaskState): String = when (state) {
    TaskState.OPEN -> "mark done"
    TaskState.DONE -> "mark open"
    else -> "change status"
}

/**
 * Lets a row-level gesture skip presses a list-row checkbox has taken, e.g.
 * Also today's long-press reorder (v2.13.0). The checkbox claims each down
 * before the row sees it; pointer ids never repeat, so one slot is enough.
 */
class CheckboxPresses {
    private var claimed: PointerId? = null
    fun claim(id: PointerId) { claimed = id }
    fun owns(id: PointerId): Boolean = claimed == id
}

val LocalCheckboxPresses = staticCompositionLocalOf<CheckboxPresses?> { null }
