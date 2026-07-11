package com.thefoxworks.tzafon.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.BuildConfig
import com.thefoxworks.tzafon.ui.components.FoxLogo
import com.thefoxworks.tzafon.ui.components.RustHeader
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/** FR-ABOUT-1.5 — mailto target used by Contact Us. */
const val ABOUT_CONTACT_MAILTO = "mailto:thefoxworksdotnet@gmail.com"

/**
 * About (FR-ABOUT-1). Reference view — reachable only from AppMenuSheet
 * (FR-NAV-4). Displays producer / implementer / running version, the
 * FoxLogo identity mark, and a Contact Us mailto link.
 */
@Composable
fun AboutScreen(
    onClose: () -> Unit,
    versionName: String = BuildConfig.VERSION_NAME,
    launchEmail: ((Intent) -> Unit)? = null,
) {
    val context = LocalContext.current
    val launcher: (Intent) -> Unit = launchEmail ?: remember(context) {
        { intent -> context.startActivity(intent) }
    }
    var noEmailHandler by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Den.bg)) {
        RustHeader(
            title = "About",
            kicker = "TZAFON",
            compact = true,
            onBack = onClose,
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Produced by The Fox Works",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                    color = Den.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Implemented by Claude",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                    color = Den.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Version $versionName",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 13.sp, letterSpacing = 0.4.sp),
                    color = Den.muted,
                    textAlign = TextAlign.Center,
                )
            }

            FoxLogo(size = 108.dp, ring = Den.ink.a(0.06f))

            val contactLabel = "Contact Us"
            Text(
                contactLabel,
                style = TextStyle(
                    fontFamily = DenType.body,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Den.rust,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics {
                        contentDescription = "Contact Us — opens email to thefoxworksdotnet@gmail.com"
                    }
                    .pressable(contactLabel, Role.Button) {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(ABOUT_CONTACT_MAILTO))
                        try {
                            launcher(intent)
                            noEmailHandler = false
                        } catch (_: ActivityNotFoundException) {
                            noEmailHandler = true
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )

            if (noEmailHandler) {
                Text(
                    "No email app is available",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, lineHeight = 18.sp),
                    color = Den.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
