package com.thefoxworks.tzafon.ui.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thefoxworks.tzafon.domain.status.ShownToast
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.Tz
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** v2.13.0 — how long the status toast shows before TalkBack lengthens it. */
const val TOAST_MILLIS = 6_000L

/** How long a toast stays: [TOAST_MILLIS], or what TalkBack recommends (the Undo is a control). */
fun toastMillis(a11y: AccessibilityManager?, undoable: Boolean): Long =
    a11y?.calculateRecommendedTimeoutMillis(
        TOAST_MILLIS, containsIcons = true, containsText = true, containsControls = undoable,
    ) ?: TOAST_MILLIS

/**
 * The app's one status toast, above the bottom nav (pass its height as
 * [aboveNav]). It lives outside the NavHost, so it survives tab switches;
 * a new toast replaces the old one. Shows for 6 s, lengthened when TalkBack
 * is on (AccessibilityManager.calculateRecommendedTimeoutMillis).
 */
@Composable
fun StatusToastHost(host: StatusHost, aboveNav: Dp, modifier: Modifier = Modifier) {
    val toast by host.changes.toast.collectAsStateWithLifecycle()
    val a11y = LocalAccessibilityManager.current
    // keep the last toast composed while it animates out
    var shown by remember { mutableStateOf<ShownToast?>(null) }
    if (toast != null) shown = toast

    toast?.let { t ->
        LaunchedEffect(t.id) {
            delay(toastMillis(a11y, t.undoable))
            host.changes.dismiss(t.id)
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
        modifier = modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = aboveNav + 12.dp),
    ) {
        shown?.let { t -> ToastCard(t, onUndo = { host.scope.launch { host.changes.undo(t.id) } }) }
    }
}

@Composable
private fun ToastCard(toast: ShownToast, onUndo: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Tz.colors.ink)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TzIcons.Check(15.dp, Tz.colors.bg)
        Text(
            toast.text,
            style = TextStyle(fontFamily = DenType.body, fontSize = 14.5.sp, lineHeight = 19.sp),
            color = Tz.colors.bg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        )
        if (toast.undoable) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .pressable("Undo", Role.Button, onUndo)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    "Undo",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = Tz.colors.amber,
                )
            }
        }
    }
}
