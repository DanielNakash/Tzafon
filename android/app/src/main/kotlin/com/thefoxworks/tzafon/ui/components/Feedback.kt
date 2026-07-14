package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/** Calm, closeable banner (tz-ui Banner) — e.g. FR-TODAY-4 slippage. */
@Composable
fun Banner(
    text: androidx.compose.ui.text.AnnotatedString,
    tone: Color = Tz.colors.rust,
    onClose: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tone.a(0.08f))
            .border(1.dp, tone.a(0.28f), RoundedCornerShape(12.dp))
            .let { if (onClick != null) it.pressable(onClick) else it }
            .padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        TzIcons.Alert(16.dp, tone)
        Text(
            text,
            style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, lineHeight = 17.5.sp),
            color = Tz.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).pressable(onClose)
                .semantics { contentDescription = "Dismiss" },
            contentAlignment = Alignment.Center,
        ) { TzIcons.X(13.dp, Tz.colors.muted) }
    }
}

/** Supportive nudge in the fox voice (tz-ui Nudge) — overload, stale goal. */
@Composable
fun Nudge(
    text: String,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Tz.colors.amber.a(0.14f))
            .border(1.dp, Tz.colors.amber.a(0.4f), RoundedCornerShape(13.dp))
            .padding(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Compass(size = 17.dp, ring = Tz.colors.rust, needleN = Tz.colors.rust, needleS = Tz.colors.faint, stroke = 2f)
            Text(
                text,
                style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.5.sp),
                color = Tz.colors.ink,
                modifier = Modifier.weight(1f),
            )
        }
        if (actions != null) {
            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { actions() }
        }
    }
}

/** Small bordered pill button used in nudge actions and decide-cards. */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    color: Color = Tz.colors.muted,
    filled: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) color else Tz.colors.card)
            .border(1.dp, if (filled) color else Tz.colors.line, RoundedCornerShape(999.dp))
            .let { if (enabled) it.pressable(onClick) else it }
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 0.2.sp),
            color = when {
                filled -> Color.White
                enabled -> color
                else -> Tz.colors.faint.a(0.6f)
            },
        )
    }
}
