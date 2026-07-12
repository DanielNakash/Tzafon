package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * The rust Action-layer header (tz-ui.jsx RustHeader): kicker + serif title,
 * optional meta line + amber progress, faded compass motif top-right.
 */
@Composable
fun RustHeader(
    title: String,
    kicker: String = "DON'T PANIC",
    metaLeft: String? = null,
    metaRight: String? = null,
    progress: Float? = null,
    compass: Boolean = false,
    right: (@Composable () -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    // FR-NAV-7 — every rust header renders `title` at the h1 (37.sp) size so
    // the daily-path silhouette is uniform; there is no compact variant.
    Box(Modifier.fillMaxWidth().background(Den.rust).clipToBounds()) {
        if (compass) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 20.dp, y = 38.dp).alpha(0.13f)) {
                Compass(size = 140.dp, ring = Den.cream, needleN = Den.cream, needleS = Den.cream, stroke = 1.2f, ticks = true)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = if (bottomContent != null) 0.dp else 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    TouchTarget(
                        visualSize = 24.dp,
                        onClick = onBack,
                        label = "Back",
                        role = Role.Button,
                        semantics = { contentDescription = "Back" },
                    ) { TzIcons.Back(22.dp, Den.cream) }
                } else {
                    Kicker(kicker, color = Den.cream.a(0.82f))
                }
                Box(Modifier.weight(1f))
                // The menu (when present) is an anchored overlay below, kept at a fixed
                // position so it sits at the same height on every screen (FR-NAV-3).
                when {
                    right != null -> right()
                    // A left-slot back-button surface (e.g. About) is a leaf —
                    // the header carries no identity mark and no menu trigger.
                    onBack != null -> {}
                    onMenu == null -> FoxLogo(30.dp, ring = Den.cream.a(0.9f))
                    else -> {}
                }
            }
            Text(
                title,
                style = DenType.h1,
                color = Den.cream,
                modifier = Modifier.padding(top = 7.dp),
            )
            if (metaLeft != null || metaRight != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(metaLeft?.uppercase() ?: "", style = DenType.meta, color = Den.cream.a(0.92f))
                    Text(metaRight?.uppercase() ?: "", style = DenType.meta, color = Den.cream.a(0.92f))
                }
            }
            if (progress != null) {
                Box(Modifier.padding(top = 9.dp)) {
                    Bar(pct = progress, fill = Den.amber, track = Color.White.a(0.22f), height = 9.dp, radius = 6.dp)
                }
            }
            if (bottomContent != null) {
                Box(Modifier.padding(top = 12.dp, bottom = 16.dp)) { bottomContent() }
            }
        }
        if (onMenu != null) {
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 2.dp, end = 12.dp)) {
                HeaderMenuButton(onClick = onMenu, color = Den.cream)
            }
        }
    }
}

/**
 * FR-NAV-3 — the hamburger that opens `AppMenuSheet` on views that mount it
 * (Today · Planning · All Tasks). Kept as a small shared composable so the
 * three call sites cannot drift: same icon, same hit target, same content
 * description. The Fox Works roundel (`FoxLogo`) is preserved elsewhere as an
 * identity mark per `FR-NAV-3.4`.
 */
@Composable
fun HeaderMenuButton(
    onClick: () -> Unit,
    color: Color = Den.cream,
) {
    TouchTarget(
        visualSize = 30.dp,
        onClick = onClick,
        label = "Menu",
        role = Role.Button,
        semantics = { contentDescription = "Menu" },
    ) {
        TzIcons.Menu(20.dp, color)
    }
}

/** Cream editorial header for the Direction / Reflection layers. */
@Composable
fun LayerHeader(
    kicker: String,
    title: String,
    sub: String? = null,
    accent: Color = Den.rust,
    compass: Boolean = false,
    mascot: (@Composable () -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().background(Den.surface)) {
        if (compass) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 16.dp).statusBarsPadding()) {
                Compass(size = 46.dp, ring = accent, needleN = accent, needleS = Den.faint)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Kicker(kicker, color = accent)
                Box(Modifier.weight(1f))
                when {
                    mascot != null -> mascot()
                    !compass && onMenu == null -> FoxLogo(30.dp, ring = Den.ink.a(0.06f))
                    else -> {}
                }
            }
            Text(
                title,
                style = DenType.h1.copy(fontSize = androidx.compose.ui.unit.TextUnit(33f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = Den.ink,
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.82f),
            )
            if (sub != null) {
                Text(
                    sub,
                    style = DenType.chip.copy(fontFamily = DenType.body, fontSize = androidx.compose.ui.unit.TextUnit(13.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = Den.muted,
                    modifier = Modifier.padding(top = 9.dp).fillMaxWidth(0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onMenu != null) {
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 2.dp, end = 12.dp)) {
                HeaderMenuButton(onClick = onMenu, color = Den.ink.a(0.7f))
            }
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(Den.line))
    }
}
