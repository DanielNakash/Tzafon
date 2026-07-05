package com.thefoxworks.tzafon.ui.welcome

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.ui.components.Compass
import com.thefoxworks.tzafon.ui.components.FoxLogo
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/**
 * First-run hero (design: SignIn artboard). Google SSO ships with the
 * deferred Firebase step (M9b, PLAN §3) — until then the button starts the
 * app locally, everything else matches the design.
 */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Den.bg)) {
        // soft color orbs
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 90.dp, y = (-110).dp)
                .size(300.dp).blur(10.dp).clip(CircleShape).background(Den.amber.a(0.18f))
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-100).dp, y = 130.dp)
                .size(280.dp).blur(8.dp).clip(CircleShape).background(Den.rust.a(0.12f))
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // fox roundel + compass badge
            Box {
                FoxLogo(size = 116.dp, ring = Den.surface)
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 6.dp)
                        .size(40.dp).clip(CircleShape).background(Den.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Compass(size = 26.dp, ring = Den.rust, needleN = Den.rust, needleS = Den.faint, stroke = 2.2f)
                }
            }

            Text(
                "THE FOX WORKS · DON'T PANIC",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 11.5.sp, letterSpacing = 3.5.sp, fontWeight = FontWeight.SemiBold),
                color = Den.rust,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(
                "Tzafon",
                style = TextStyle(fontFamily = DenType.serif, fontSize = 52.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
                color = Den.ink,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                "צָפוֹן · “north”",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 13.sp, letterSpacing = 2.sp),
                color = Den.faint,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "A compass for the life you're actually building. Hold a direction — then do one small thing today.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 16.sp, lineHeight = 24.sp),
                color = Den.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp).widthIn(max = 290.dp),
            )

            Row(
                Modifier.padding(top = 30.dp).fillMaxWidth().widthIn(max = 300.dp).height(56.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Den.surface)
                    .border(1.dp, Den.line, RoundedCornerShape(15.dp))
                    .pressable(onStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Compass(size = 20.dp, ring = Den.rust, needleN = Den.rust, needleS = Den.faint, stroke = 2f, ticks = false)
                Text(
                    "Get started",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold),
                    color = Den.ink,
                )
            }

            Text(
                "Your directions are yours alone — they live on this device.",
                style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, lineHeight = 19.sp),
                color = Den.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp).widthIn(max = 260.dp),
            )
        }

        Text(
            "🔒 STORED ON THIS DEVICE",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, letterSpacing = 1.sp),
            color = Den.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 34.dp),
        )
    }
}
