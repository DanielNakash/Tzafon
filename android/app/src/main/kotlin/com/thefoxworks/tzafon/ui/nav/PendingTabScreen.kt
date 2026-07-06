package com.thefoxworks.tzafon.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.ui.components.Compass
import com.thefoxworks.tzafon.ui.components.LayerHeader
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType

/**
 * Calm stand-ins for the tabs whose milestones haven't landed yet
 * (Habits — M4, Directions — M6, Journey — M8). Honest, in the fox voice,
 * and free of anything DM-NOT forbids.
 */
@Composable
fun PendingTabScreen(tab: Tab) {
    val (kicker, sub, body) = when (tab) {
        Tab.HABITS -> Triple(
            "RHYTHM, NOT STREAKS",
            "The rhythms you keep, one gentle week at a time.",
            "This room of the den is still being dug.\nYour habits will live here — cues front and centre,\nforgiving weekly rates, long arcs. Never a streak.",
        )
        Tab.DIRECTIONS -> Triple(
            "YOUR NORTH",
            "The few directions everything else serves.",
            "This room of the den is still being dug.\nThemes and goals — the why behind the tasks —\nare on their way.",
        )
        Tab.JOURNEY -> Triple(
            "LOOKING BACK",
            "A mirror, not a trophy case.",
            "This room of the den is still being dug.\nJourney fills in once there's history to reflect —\nno badges, just who you're becoming.",
        )
        else -> Triple("", "", "")
    }

    Column(Modifier.fillMaxSize().background(Den.bg)) {
        LayerHeader(kicker = kicker, title = tab.label, sub = sub, compass = true)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Compass(size = 44.dp, ring = Den.faint, needleN = Den.rust, needleS = Den.faint, stroke = 1.6f)
                Text(
                    "Still being dug",
                    style = TextStyle(fontFamily = DenType.serif, fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
                    color = Den.ink,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    body,
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.5.sp, lineHeight = 19.5.sp),
                    color = Den.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
