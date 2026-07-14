package com.thefoxworks.tzafon.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/** FR-ABOUT-1.5 — mailto target used by Contact Us. */
const val ABOUT_CONTACT_MAILTO = "mailto:thefoxworksdotnet@gmail.com"

/**
 * About (FR-ABOUT-1, refined by FR-ABOUT-2). Reference view — reachable
 * only from AppMenuSheet (FR-NAV-4). Renders producer / implementer /
 * running version, the FoxLogo identity mark, and a Contact Us mailto
 * link. Body is vertically centered in the available area with a
 * viewport-proportional FoxLogo; scroll fallback engages when a large
 * font scale would otherwise clip content (FR-ABOUT-2.4.1).
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

    Column(Modifier.fillMaxSize().background(Tz.colors.bg)) {
        RustHeader(
            title = "About",
            kicker = "TZAFON",
            right = {
                Box(Modifier.pressable("Close", Role.Button, onClose).padding(4.dp)) { TzIcons.X(20.dp, Tz.colors.cream) }
            },
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // FR-ABOUT-2.2 — clamp(min(0.50 × w, 0.45 × h), 96.dp, 240.dp).
            // `maxWidth`/`maxHeight` are the *body* envelope inside BoxWithConstraints,
            // so the ratios are taken against usable space, not the whole screen.
            val logoSize = run {
                val proposed = minOf(maxWidth * 0.50f, maxHeight * 0.45f)
                proposed.coerceIn(96.dp, 240.dp)
            }
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // FR-ABOUT-2.3 — the top spacer + Arrangement.Center + a matching
                // trailing spacer centers the block on default text scale. When
                // the natural content exceeds the viewport, the verticalScroll
                // takes over and the spacers collapse against the padding.
                Spacer(Modifier.height(0.dp))

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Produced by The Fox Works",
                        style = TextStyle(
                            fontFamily = DenType.serif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = Tz.colors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Implemented by Claude",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 15.5.sp),
                        color = Tz.colors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Version $versionName",
                        style = TextStyle(
                            fontFamily = DenType.mono,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.4.sp,
                        ),
                        color = Tz.colors.muted,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(28.dp))
                FoxLogo(size = logoSize, ring = Tz.colors.ink.a(0.06f))
                Spacer(Modifier.height(24.dp))

                val contactLabel = "Contact Us"
                Text(
                    contactLabel,
                    style = TextStyle(
                        fontFamily = DenType.body,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Tz.colors.rust,
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
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No email app is available",
                        style = TextStyle(fontFamily = DenType.body, fontSize = 13.sp, lineHeight = 18.sp),
                        color = Tz.colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
